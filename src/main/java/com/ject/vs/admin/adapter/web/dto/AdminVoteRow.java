package com.ject.vs.admin.adapter.web.dto;

import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteOption;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 어드민 목록 화면에 뿌리는 투표 한 줄. (LAZY 연관을 화면에서 건드리지 않도록 미리 평탄화한다)
 */
public record AdminVoteRow(
        Long id,
        String title,
        String thumbnailUrl,
        String optionA,
        String optionB,
        boolean ongoing,
        String endAt,
        long participantCount
) {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminVoteRow of(Vote vote, List<VoteOption> options, long participantCount, Clock clock) {
        return new AdminVoteRow(
                vote.getId(),
                vote.getTitle(),
                vote.getThumbnailUrl(),
                labelAt(options, 0),
                labelAt(options, 1),
                vote.isOngoing(clock),
                FORMATTER.format(vote.getEndAt().atZone(KST)),
                participantCount
        );
    }

    private static String labelAt(List<VoteOption> options, int index) {
        return options.size() > index ? options.get(index).getLabel() : "-";
    }
}
