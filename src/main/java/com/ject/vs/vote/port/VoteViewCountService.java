package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 투표 조회수 적재.
 *
 * <p>조회수는 핫토픽 인기 점수(참여 수 × 0.7 + 조회 수 × 0.3)와 홈 인기순 정렬의 입력이다.
 *
 * <p>메서드에 트랜잭션을 걸지 않는다. 통계 행을 처음 만들 때 경쟁에서 지면
 * {@link DataIntegrityViolationException}이 나는데, 바깥 트랜잭션 안에서 이를 잡으면
 * 트랜잭션이 이미 rollback-only로 찍혀 뒤따르는 증가와 커밋이 통째로 실패한다.
 * 저장소 호출 각각이 자기 트랜잭션에서 끝나도록 두고 여기서는 흐름만 잡는다.
 */
@Service
@RequiredArgsConstructor
public class VoteViewCountService {

    private static final Logger log = LoggerFactory.getLogger(VoteViewCountService.class);

    private final VoteStatisticsRepository voteStatisticsRepository;

    /**
     * 투표 1회 조회를 기록한다.
     *
     * <p>지표 적재라서 실패해도 요청 처리에는 영향을 주지 않는다. 없는 투표 id가 들어와도
     * (몰입형 노출 기록은 클라이언트가 보낸 id를 그대로 받는다) 예외를 밖으로 내보내지 않는다.
     */
    public void countView(Long voteId) {
        try {
            if (voteStatisticsRepository.incrementViewCount(voteId) > 0) {
                return;
            }
            // 통계 행은 투표 생성 시점에 만들어지지 않는다. 그 투표가 처음 조회되는 지금 만든다.
            createStatistics(voteId);
            voteStatisticsRepository.incrementViewCount(voteId);
        } catch (Exception e) {
            log.warn("조회수 적재 실패 voteId={}: {}", voteId, e.getMessage());
        }
    }

    private void createStatistics(Long voteId) {
        try {
            voteStatisticsRepository.createStatistics(voteId);
        } catch (DataIntegrityViolationException e) {
            // 같은 투표를 동시에 처음 연 다른 요청이 먼저 만들었거나(PK 충돌), 없는 투표 id다(FK 위반).
            // 앞의 경우는 이어지는 증가가 처리하고, 뒤의 경우는 증가가 0을 돌려주고 끝난다.
        }
    }
}
