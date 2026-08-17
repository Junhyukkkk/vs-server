package com.ject.vs.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ElapsedMillis: 클라이언트가 잰 경과 시간 정규화")
class ElapsedMillisTest {

    @Test
    @DisplayName("정상 범위 값은 그대로 통과한다")
    void 정상_범위_값은_그대로_통과한다() {
        assertThat(ElapsedMillis.normalize(0L)).isZero();
        assertThat(ElapsedMillis.normalize(4_200L)).isEqualTo(4_200L);
        assertThat(ElapsedMillis.normalize(Duration.ofMinutes(30).toMillis())).isEqualTo(1_800_000L);
    }

    @Test
    @DisplayName("측정하지 않아 null이면 null 그대로 둔다")
    void 미측정이면_null이다() {
        assertThat(ElapsedMillis.normalize(null)).isNull();
    }

    @Test
    @DisplayName("음수는 시계 이상이므로 버린다")
    void 음수는_버린다() {
        assertThat(ElapsedMillis.normalize(-1L)).isNull();
    }

    @Test
    @DisplayName("30분을 넘으면 자리비움으로 보고 버린다 - 상한으로 자르면 그 지점에 없는 봉우리가 생긴다")
    void 상한_초과는_자르지_않고_버린다() {
        long overLimit = Duration.ofMinutes(30).toMillis() + 1;

        Long result = ElapsedMillis.normalize(overLimit);

        assertThat(result).isNull();
    }
}
