package com.ject.vs.experiment;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A/B 테스트 시안을 배정한다.
 *
 * <p>URL은 하나로 두고 같은 주소에서 시안만 갈아끼우는 방식이라, 어떤 시안을 보여줄지는
 * 서버가 정해서 응답과 행동 로그 양쪽에 같은 값을 싣는다.
 *
 * <p>요청마다 난수를 뽑지 않고 anonymous_id를 해시해 배정한다. 난수로 뽑으면 같은 사용자가
 * 새로고침할 때마다 A와 B를 오가게 되어 시안별 지표가 섞이고 A/B 비교 자체가 불가능해진다.
 * 해시 배정은 같은 브라우저에 항상 같은 시안을 주므로 그런 오염이 없다.
 * 다만 시크릿창이나 다른 기기는 anonymous_id가 달라 다른 시안을 볼 수 있다.
 *
 * <p>SHA-256 출력이 균등분포이므로 표본이 쌓일수록 5:5에 수렴한다(정확히 절반은 아니다).
 * 실험 이름을 해시에 섞어두어, 나중에 다른 실험을 추가해도 같은 사용자가 늘 같은 쪽으로
 * 몰리지 않는다.
 */
@Component
public class AbTestAssigner {

    /** 몰입형 투표 화면 시안 A/B 실험. */
    public static final String IMMERSIVE_UI = "immersive_ui";

    /**
     * 실험 참가자에게 시안을 배정한다.
     *
     * @param experiment 실험 이름. 실험마다 배정이 독립적이도록 해시에 섞인다.
     * @param key        참가자 식별자. 몰입형 화면에서는 anonymous_id를 쓴다.
     * @return 배정된 시안. key가 없으면 A로 고정한다(아래 설명 참고).
     */
    public AbVariant assign(String experiment, String key) {
        // 몰입형 엔드포인트는 @AnonymousId가 쿠키를 항상 발급하므로 key가 비는 경우는 없다.
        // 여기 걸린다면 배정 근거가 없다는 뜻이라, 난수 대신 A로 고정해 최소한 재현 가능하게 둔다.
        if (key == null || key.isBlank()) {
            return AbVariant.A;
        }
        byte[] digest = sha256(experiment + ":" + key);
        return (digest[0] & 1) == 0 ? AbVariant.A : AbVariant.B;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM 구현이 제공하도록 표준에 규정되어 있어 실제로는 도달하지 않는다.
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", e);
        }
    }
}
