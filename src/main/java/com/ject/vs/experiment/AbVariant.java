package com.ject.vs.experiment;

/**
 * A/B 테스트 시안. 디자인 시안 A안/B안에 그대로 대응한다.
 *
 * <p>응답과 행동 로그에는 이름("A"/"B")이 그대로 실린다. 분석 시 이 값으로 시안별 지표를 가른다.
 */
public enum AbVariant {
    A,
    B
}
