package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    List<VoteOption> findByVoteIdOrderByPosition(Long voteId);

    boolean existsByIdAndVoteId(Long id, Long voteId);

    /**
     * 여러 투표의 선택지를 한 번에 조회한다. (목록 화면 N+1 방지용)
     */
    @Query("""
            SELECT o FROM VoteOption o
             WHERE o.vote.id IN :voteIds
             ORDER BY o.vote.id ASC, o.position ASC
            """)
    List<VoteOption> findAllByVoteIds(@Param("voteIds") List<Long> voteIds);
}
