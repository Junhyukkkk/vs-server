package com.ject.vs.home.port;

import com.github.benmanes.caffeine.cache.Cache;
import com.ject.vs.home.domain.HotTopicScorer;
import com.ject.vs.home.port.in.HomeVoteQueryUseCase;
import com.ject.vs.home.port.in.HotTopicRefreshUseCase;
import com.ject.vs.vote.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeVoteQueryService implements HomeVoteQueryUseCase, HotTopicRefreshUseCase {

    private final VoteRepository voteRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final VoteStatisticsRepository voteStatisticsRepository;
    private final RecommendedVoteRepository recommendedVoteRepository;
    private final Clock clock;
    private final Cache<String, HotTopicResult> hotTopicCache;

    private static final int HOT_TOPIC_SIZE = 5;
    private static final String HOT_TOPIC_CACHE_KEY = "hot-topics";

    @Override
    public RecommendationResult getRecommendations() {
        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);

        List<RecommendedVote> recommendedVotes = recommendedVoteRepository
                .findByDateWithOngoingVotes(today, now);

        List<RecommendationItem> items = recommendedVotes.stream()
                .map(rv -> {
                    Vote vote = rv.getVote();
                    return new RecommendationItem(
                            vote.getId(),
                            vote.getThumbnailUrl(),
                            vote.getTitle(),
                            vote.getContent(),
                            vote.getEndAt()
                    );
                })
                .toList();

        return new RecommendationResult(items);
    }

    /**
     * 핫토픽 TOP 5 조회.
     *
     * <p>순위는 3시간마다 갱신되는 캐시에서 읽는다. 갱신 주기 사이에 종료된 투표는
     * 응답 시점에 제외하고 남은 투표에 순위를 다시 매긴다.
     */
    @Override
    public HotTopicResult getHotTopics() {
        HotTopicResult cached = hotTopicCache.getIfPresent(HOT_TOPIC_CACHE_KEY);
        if (cached == null) {
            cached = computeAndCacheHotTopics();
        }
        return excludeEndedVotes(cached);
    }

    @Override
    public void refreshHotTopics() {
        computeAndCacheHotTopics();
    }

    private HotTopicResult computeAndCacheHotTopics() {
        HotTopicResult result = computeHotTopics();
        hotTopicCache.put(HOT_TOPIC_CACHE_KEY, result);
        return result;
    }

    private HotTopicResult computeHotTopics() {
        Instant now = Instant.now(clock);

        // 진행 중인 투표만 조회
        List<Vote> ongoingVotes = voteRepository.findOngoingVotes(now);

        if (ongoingVotes.isEmpty()) {
            return new HotTopicResult(List.of());
        }

        List<Long> voteIds = ongoingVotes.stream().map(Vote::getId).toList();

        // 참여 수 조회
        Map<Long, Long> participantCounts = voteParticipationRepository
                .countByVoteIds(voteIds)
                .stream()
                .collect(Collectors.toMap(
                        VoteParticipationRepository.VoteParticipantCount::voteId,
                        VoteParticipationRepository.VoteParticipantCount::count
                ));

        // 조회 수 조회
        Map<Long, Long> viewCounts = voteStatisticsRepository
                .findAllByVoteIdIn(voteIds)
                .stream()
                .collect(Collectors.toMap(
                        VoteStatistics::getVoteId,
                        VoteStatistics::getViewCount
                ));

        // 인기 점수는 투표당 한 번만 계산해두고 정렬에 재사용한다
        Map<Long, Double> scores = ongoingVotes.stream()
                .collect(Collectors.toMap(
                        Vote::getId,
                        vote -> HotTopicScorer.score(
                                participantCounts.getOrDefault(vote.getId(), 0L),
                                viewCounts.getOrDefault(vote.getId(), 0L),
                                vote.getCreatedAt(),
                                now
                        )
                ));

        List<Vote> topVotes = ongoingVotes.stream()
                .sorted(Comparator
                        .comparingDouble((Vote vote) -> scores.get(vote.getId())).reversed()
                        // 신규 가중치 덕에 점수가 같기는 어렵지만, 생성 시각까지 같을 때를 대비해 최신 우선으로 고정한다
                        .thenComparing(Vote::getId, Comparator.reverseOrder()))
                .limit(HOT_TOPIC_SIZE)
                .toList();

        List<HotTopicItem> items = new ArrayList<>();
        for (int i = 0; i < topVotes.size(); i++) {
            Vote vote = topVotes.get(i);
            items.add(new HotTopicItem(
                    i + 1,
                    vote.getId(),
                    vote.getThumbnailUrl(),
                    vote.getTitle(),
                    vote.getContent(),
                    participantCounts.getOrDefault(vote.getId(), 0L),
                    vote.getEndAt()
            ));
        }

        return new HotTopicResult(items);
    }

    /**
     * 캐시 갱신 이후 종료된 투표를 제외하고 순위를 1위부터 다시 부여한다.
     * 순위에 구멍이 생기면 프론트의 캐러셀/리스트 분기가 깨지므로 반드시 연속이어야 한다.
     */
    private HotTopicResult excludeEndedVotes(HotTopicResult cached) {
        Instant now = Instant.now(clock);

        List<HotTopicItem> items = new ArrayList<>();
        for (HotTopicItem item : cached.items()) {
            if (!item.endAt().isAfter(now)) {
                continue;
            }
            items.add(new HotTopicItem(
                    items.size() + 1,
                    item.voteId(),
                    item.thumbnailUrl(),
                    item.title(),
                    item.content(),
                    item.participantCount(),
                    item.endAt()
            ));
        }

        return new HotTopicResult(items);
    }

    @Override
    public VoteListResult getVoteList(String cursor, int size, VoteSortType sortType, boolean excludeEnded) {
        PageRequest pageable = PageRequest.of(0, size);
        Instant now = Instant.now(clock);

        boolean effectiveExcludeEnded = excludeEnded || sortType == VoteSortType.ENDING_SOON;

        Slice<Vote> slice = switch (sortType) {
            case LATEST -> {
                Long idCursor = parseLongCursor(cursor);
                yield voteRepository.findForHomeByLatest(idCursor, now, effectiveExcludeEnded, pageable);
            }
            case POPULAR -> {
                PopularCursor popularCursor = parsePopularCursor(cursor);
                yield voteRepository.findForHomeByPopularWithKeyset(
                        popularCursor.lastViewCount(),
                        popularCursor.lastId(),
                        now,
                        effectiveExcludeEnded,
                        pageable
                );
            }
            case ENDING_SOON -> {
                if (cursor == null || cursor.isBlank()) {
                    yield voteRepository.findFirstPageForHomeByEndingSoon(now, pageable);
                } else {
                    EndingSoonCursor endingCursor = parseEndingSoonCursor(cursor);
                    if (endingCursor.lastEndAt() == null || endingCursor.lastId() == null) {
                        yield voteRepository.findFirstPageForHomeByEndingSoon(now, pageable);
                    } else {
                        yield voteRepository.findForHomeByEndingSoonWithKeyset(
                                endingCursor.lastEndAt(),
                                endingCursor.lastId(),
                                now,
                                pageable
                        );
                    }
                }
            }
        };

        List<Vote> votes = slice.getContent();

        List<VoteListItem> items = votes.stream()
                .map(vote -> new VoteListItem(
                        vote.getId(),
                        vote.getThumbnailUrl(),
                        vote.getStatus(clock),
                        vote.getTitle(),
                        vote.getContent(),
                        vote.getEndAt()
                ))
                .toList();

        String nextCursor = (slice.hasNext() && !votes.isEmpty())
                ? encodeNextCursor(sortType, votes)
                : null;

        return new VoteListResult(items, nextCursor, slice.hasNext());
    }

    // === Cursor Parsing ===

    private Long parseLongCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record PopularCursor(Long lastViewCount, Long lastId) {}

    private PopularCursor parsePopularCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new PopularCursor(null, null);
        String[] parts = cursor.split(":");
        if (parts.length != 2) return new PopularCursor(null, null);
        try {
            return new PopularCursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            return new PopularCursor(null, null);
        }
    }

    private record EndingSoonCursor(Instant lastEndAt, Long lastId) {}

    private EndingSoonCursor parseEndingSoonCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new EndingSoonCursor(null, null);
        String[] parts = cursor.split(":");
        if (parts.length != 2) return new EndingSoonCursor(null, null);
        try {
            Instant endAt = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            Long id = Long.parseLong(parts[1]);
            return new EndingSoonCursor(endAt, id);
        } catch (Exception e) {
            return new EndingSoonCursor(null, null);
        }
    }

    // === Next Cursor Encoding ===

    private String encodeNextCursor(VoteSortType sortType, List<Vote> votes) {
        if (votes.isEmpty()) return null;

        Vote last = votes.get(votes.size() - 1);

        return switch (sortType) {
            case LATEST -> String.valueOf(last.getId());
            case POPULAR -> {
                Long viewCount = voteStatisticsRepository.findByVoteId(last.getId())
                        .map(VoteStatistics::getViewCount)
                        .orElse(0L);
                yield viewCount + ":" + last.getId();
            }
            case ENDING_SOON -> {
                long endAtMillis = last.getEndAt().toEpochMilli();
                yield endAtMillis + ":" + last.getId();
            }
        };
    }

}
