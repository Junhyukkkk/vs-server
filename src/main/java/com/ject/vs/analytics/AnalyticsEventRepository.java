package com.ject.vs.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventRecord, Long> {

    /**
     * 이벤트 하나를 시간 구간(KST) × 속성값으로 묶어 건수를 센다. 어드민 분석 화면의 기본 집계 방식.
     *
     * <p>구간 단위는 {@code unit}으로 받는다("day" 또는 "hour") — 어드민 화면이 조회 기간이 3일 이하면
     * 시간 단위로, 그보다 길면 일 단위로 자동 전환해 보내준다. {@code date_trunc}의 첫 인자는 그냥
     * 텍스트라 바인드 파라미터로 넘겨도 안전하다.
     *
     * <p>{@code occurred_at}은 시간대 없는 TIMESTAMP 컬럼이지만, JVM/컨테이너 기본 시간대가 UTC라
     * 저장된 값 자체가 UTC 벽시계 값이다({@code Clock.systemUTC()}). {@code AT TIME ZONE 'UTC'}로
     * 그 사실을 명시한 뒤 {@code AT TIME ZONE 'Asia/Seoul'}로 변환해야 KST 자정/정시 기준으로 구간이 갈린다.
     *
     * <p>{@code breakdownKey}가 빈 문자열이면 {@code properties} 안에 그런 키가 없으므로
     * {@code ->>}가 항상 null을 돌려주고, COALESCE가 모든 행을 "(전체)" 한 묶음으로 합친다 —
     * 별도 분기 없이 "쪼개지 않음"이 자연스럽게 처리된다.
     *
     * <p>Postgres 전용 함수(date_trunc, AT TIME ZONE, ::jsonb)를 쓰므로 로컬 H2 프로필에서는
     * 동작하지 않는다. {@link com.ject.vs.vote.domain.VoteParticipationRepository#findTopVoteIdsByRecentActivity}
     * 와 같은 이유다.
     */
    @Query(value = """
            SELECT date_trunc(:unit, occurred_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Seoul') AS bucketTime,
                   COALESCE(NULLIF(properties::jsonb ->> :breakdownKey, ''), '(전체)') AS bucketValue,
                   COUNT(*) AS eventCount
            FROM analytics_events
            WHERE event = :event
              AND occurred_at >= :fromUtc
              AND occurred_at <  :toUtc
            GROUP BY bucketTime, bucketValue
            ORDER BY bucketTime, bucketValue
            """, nativeQuery = true)
    List<BucketCountRow> aggregateCount(
            @Param("event") String event,
            @Param("breakdownKey") String breakdownKey,
            @Param("unit") String unit,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc);

    /**
     * "Time to Vote" 평균(초). {@code immersive_vote_participated} 중 action=VOTED인 건만 대상이고,
     * 항상 시안(variant)별로 묶는다 — 지표의 정의 자체가 A/B 비교이므로 쪼개서 보기 선택과 무관하게 고정이다.
     */
    @Query(value = """
            SELECT date_trunc(:unit, occurred_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Seoul') AS bucketTime,
                   COALESCE(NULLIF(properties::jsonb ->> 'variant', ''), '(전체)') AS bucketValue,
                   AVG((properties::jsonb ->> 'time_to_vote_ms')::numeric) AS avgMillis,
                   COUNT(*) AS sampleCount
            FROM analytics_events
            WHERE event = 'immersive_vote_participated'
              AND properties::jsonb ->> 'action' = 'VOTED'
              AND properties::jsonb ->> 'time_to_vote_ms' IS NOT NULL
              AND occurred_at >= :fromUtc
              AND occurred_at <  :toUtc
            GROUP BY bucketTime, bucketValue
            ORDER BY bucketTime, bucketValue
            """, nativeQuery = true)
    List<BucketAvgRow> aggregateTimeToVote(
            @Param("unit") String unit,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc);

    /**
     * "이탈": {@code immersive_content_viewed}는 있는데 같은 {@code impression_id}의
     * {@code immersive_first_action}이 없는 노출. 항상 시안(variant)별로 묶는다.
     *
     * <p>impression_id 비교가 인덱스를 안 타 상관 서브쿼리 비용이 있지만, 현재 이벤트 적재량에서는
     * 무리 없는 수준이다. 데이터가 많이 쌓이면 impression_id에 대한 함수 인덱스를 고려한다.
     */
    @Query(value = """
            SELECT date_trunc(:unit, v.occurred_at AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Seoul') AS bucketTime,
                   COALESCE(NULLIF(v.properties::jsonb ->> 'variant', ''), '(전체)') AS bucketValue,
                   COUNT(*) AS viewedCount,
                   COUNT(*) FILTER (
                       WHERE NOT EXISTS (
                           SELECT 1 FROM analytics_events a
                           WHERE a.event = 'immersive_first_action'
                             AND a.properties::jsonb ->> 'impression_id' = v.properties::jsonb ->> 'impression_id'
                       )
                   ) AS bouncedCount
            FROM analytics_events v
            WHERE v.event = 'immersive_content_viewed'
              AND v.occurred_at >= :fromUtc
              AND v.occurred_at <  :toUtc
            GROUP BY bucketTime, bucketValue
            ORDER BY bucketTime, bucketValue
            """, nativeQuery = true)
    List<BucketBounceRow> aggregateImmersiveBounce(
            @Param("unit") String unit,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc);

    interface BucketCountRow {
        LocalDateTime getBucketTime();

        String getBucketValue();

        Long getEventCount();
    }

    interface BucketAvgRow {
        LocalDateTime getBucketTime();

        String getBucketValue();

        Double getAvgMillis();

        Long getSampleCount();
    }

    interface BucketBounceRow {
        LocalDateTime getBucketTime();

        String getBucketValue();

        Long getViewedCount();

        Long getBouncedCount();
    }
}
