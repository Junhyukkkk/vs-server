package com.ject.vs.analytics;

import java.time.Duration;

/**
 * 클라이언트가 재서 보낸 경과 시간(ms)을 지표에 쓸 수 있는 값으로 정규화한다.
 *
 * <p>Time to Vote·첫 행동까지의 시간은 서버가 두 이벤트의 {@code occurred_at} 차이로 계산하지
 * 않고 클라이언트가 단조 시계({@code performance.now()})로 잰 값을 그대로 받는다. 서버 계산은
 * 네트워크 지연과 기기 시계 오차가 그대로 섞이는데, 재려는 값이 수 초 단위라 노이즈와 신호의
 * 크기가 비슷해져 중앙값 비교가 무의미해지기 때문이다.
 *
 * <p>대신 클라이언트 값을 신뢰할 수 없는 구간에서는 버린다. 이상치를 살려두면 중앙값보다
 * 평균·분포가 먼저 망가진다.
 */
public final class ElapsedMillis {

    /**
     * 이 값을 넘으면 측정으로 인정하지 않는다.
     *
     * <p>탭을 백그라운드에 두거나 화면을 켜둔 채 자리를 비운 경우로, "노출 후 얼마 만에 눌렀나"라는
     * 질문의 답이 아니다. 상한으로 자르지 않고 버리는 이유는, 자르면 30분 지점에 실재하지 않는
     * 봉우리가 생겨 분포가 왜곡되기 때문이다.
     */
    private static final long MAX_MILLIS = Duration.ofMinutes(30).toMillis();

    private ElapsedMillis() {
    }

    /**
     * 정규화 결과를 돌려준다. 지표에서 제외할 값은 {@code null}이 되어 로그에는 남되 집계에서 빠진다.
     *
     * @param raw 클라이언트가 보낸 원본 값. 미측정이면 null일 수 있다.
     * @return 사용 가능한 값이면 그대로, 음수·과대값·null이면 null
     */
    public static Long normalize(Long raw) {
        if (raw == null || raw < 0 || raw > MAX_MILLIS) {
            return null;
        }
        return raw;
    }
}
