package com.ject.vs.admin.port;

import com.ject.vs.admin.adapter.web.dto.AdminVoteForm;
import com.ject.vs.admin.adapter.web.dto.AdminVoteRow;
import com.ject.vs.config.AdminProperties;
import com.ject.vs.image.port.ImageService;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteOption;
import com.ject.vs.vote.domain.VoteOptionRepository;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.port.in.VoteCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 어드민 화면 전용 조합 서비스. 투표 생성은 기존 VoteCommandUseCase를 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class AdminVoteService {

    private static final int RECENT_VOTE_SIZE = 30;

    private final VoteCommandUseCase voteCommandUseCase;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final AdminProperties adminProperties;
    private final Optional<ImageService> imageService;
    private final Clock clock;

    public boolean isAdmin(Long userId) {
        return userId != null
                && adminProperties.userIds() != null
                && adminProperties.userIds().contains(userId);
    }

    /** S3 설정이 없으면 파일 업로드를 쓸 수 없고 URL 직접 입력만 가능하다. */
    public boolean isImageUploadAvailable() {
        return imageService.isPresent();
    }

    @Transactional(readOnly = true)
    public List<AdminVoteRow> findRecentVotes() {
        List<Vote> votes = voteRepository
                .findAll(PageRequest.of(0, RECENT_VOTE_SIZE, Sort.by(Sort.Direction.DESC, "id")))
                .getContent();
        if (votes.isEmpty()) {
            return List.of();
        }

        List<Long> voteIds = votes.stream().map(Vote::getId).toList();

        Map<Long, List<VoteOption>> optionsByVoteId = voteOptionRepository.findAllByVoteIds(voteIds).stream()
                .collect(Collectors.groupingBy(option -> option.getVote().getId()));

        Map<Long, Long> countsByVoteId = voteParticipationRepository.countByVoteIds(voteIds).stream()
                .collect(Collectors.toMap(
                        VoteParticipationRepository.VoteParticipantCount::voteId,
                        VoteParticipationRepository.VoteParticipantCount::count));

        return votes.stream()
                .map(vote -> AdminVoteRow.of(
                        vote,
                        optionsByVoteId.getOrDefault(vote.getId(), List.of()),
                        countsByVoteId.getOrDefault(vote.getId(), 0L),
                        clock))
                .toList();
    }

    public VoteCommandUseCase.VoteCreateResult create(AdminVoteForm form) {
        String content = blankToNull(form.getContent());

        if (form.usesFileUpload()) {
            if (imageService.isEmpty()) {
                throw new IllegalArgumentException(
                        "S3 설정이 없어 파일 업로드를 사용할 수 없습니다. '이미지 URL 직접 입력'을 선택해주세요.");
            }
            if (isEmpty(form.getThumbnailFile())) {
                throw new IllegalArgumentException("썸네일 이미지 파일을 선택해주세요.");
            }
            return voteCommandUseCase.createWithImages(new VoteCommandUseCase.VoteCreateWithImagesCommand(
                    form.getTitle().trim(),
                    content,
                    form.getThumbnailFile(),
                    isEmpty(form.getImageFile()) ? null : form.getImageFile(),
                    form.getDuration(),
                    form.getOptionA().trim(),
                    form.getOptionB().trim()
            ));
        }

        if (!StringUtils.hasText(form.getThumbnailUrl())) {
            throw new IllegalArgumentException("썸네일 이미지 URL을 입력해주세요.");
        }
        return voteCommandUseCase.create(new VoteCommandUseCase.VoteCreateCommand(
                form.getTitle().trim(),
                content,
                form.getThumbnailUrl().trim(),
                blankToNull(form.getImageUrl()),
                form.getDuration(),
                form.getOptionA().trim(),
                form.getOptionB().trim()
        ));
    }

    private static boolean isEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
