package com.ject.vs.vote.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * 투표 조회수.
 *
 * <p>행 생성과 조회수 증가는 {@link VoteStatisticsRepository}의 SQL로 처리한다.
 * 엔티티를 읽어 필드를 더하고 저장하면 같은 투표를 동시에 연 요청끼리 서로의 증가를 덮어써
 * 조회수가 샌다. 여기 쓰기 메서드를 두지 않는 이유다.
 */
@Entity
@Table(name = "vote_statistics")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteStatistics {

    @Id
    private Long voteId;

    @Column(nullable = false)
    private Long viewCount;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vote_id")
    private Vote vote;
}
