package com.ject.vs.home.scheduler;

import com.ject.vs.home.port.in.HotTopicRefreshUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotTopicRefreshScheduler {

    private final HotTopicRefreshUseCase hotTopicRefreshUseCase;

    /**
     * KST 기준 0, 3, 6, 9, 12, 15, 18, 21시에 핫토픽 순위를 다시 계산한다.
     * 서버 시계는 UTC라서 zone을 명시하지 않으면 갱신 시각이 KST 기준과 어긋난다.
     */
    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void refreshHotTopics() {
        hotTopicRefreshUseCase.refreshHotTopics();
        log.info("Refreshed hot topic ranking");
    }
}
