package com.ject.vs.admin.adapter.web.dto;

import com.ject.vs.vote.domain.VoteDuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 진행 기간 select 박스용 항목. VoteDuration에 값이 추가되면 자동으로 따라온다.
 */
public record DurationOption(String name, String label) {

    public static List<DurationOption> all() {
        return Arrays.stream(VoteDuration.values())
                .map(d -> new DurationOption(d.name(), label(d.getValue())))
                .toList();
    }

    private static String label(Duration duration) {
        return duration.toHours() >= 1
                ? duration.toHours() + "시간"
                : duration.toMinutes() + "분";
    }
}
