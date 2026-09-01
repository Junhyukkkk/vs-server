package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface VoteStatisticsRepository extends JpaRepository<VoteStatistics, Long> {

    List<VoteStatistics> findAllByVoteIdIn(List<Long> voteIds);

    Optional<VoteStatistics> findByVoteId(Long voteId);

    /**
     * 조회수를 1 올린다.
     *
     * <p>엔티티를 읽어서 더하고 쓰면 같은 투표를 동시에 연 요청끼리 서로의 증가를 덮어써 조회수가 샌다.
     * DB에서 한 문장으로 더해 유실을 막는다.
     *
     * @return 갱신된 행 수. 통계 행이 아직 없는 투표면 0.
     */
    @Transactional
    @Modifying
    @Query("UPDATE VoteStatistics s SET s.viewCount = s.viewCount + 1 WHERE s.voteId = :voteId")
    int incrementViewCount(@Param("voteId") Long voteId);

    /**
     * 통계 행을 0으로 만든다. 투표 생성 시점이 아니라 그 투표가 처음 조회될 때 불린다.
     *
     * <p>{@code save()}를 쓰지 않는 이유: 이 엔티티는 식별자를 직접 들고 있어(@MapsId)
     * Spring Data가 "이미 존재하는 행"으로 판단해 INSERT가 아니라 UPDATE를 보낸다.
     * 없는 행을 UPDATE하니 아무것도 저장되지 않는다. INSERT를 명시한다.
     *
     * <p>이미 있는 투표면 PK 충돌로, 없는 투표 id면 FK 위반으로
     * {@link org.springframework.dao.DataIntegrityViolationException}이 난다. 둘 다 호출부에서 흡수한다.
     */
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO vote_statistics (vote_id, view_count) VALUES (:voteId, 0)",
            nativeQuery = true)
    void createStatistics(@Param("voteId") Long voteId);
}
