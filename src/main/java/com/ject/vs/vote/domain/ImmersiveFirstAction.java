package com.ject.vs.vote.domain;

/**
 * 몰입형 콘텐츠 노출 한 건에서 사용자가 <b>가장 먼저</b> 한 행동.
 *
 * <p>A/B 시안별 "첫 행동 분포"를 보기 위한 값이다. 노출(impression) 1건당 최대 1회만 기록되며,
 * 두 번째 행동부터는 이 열거형에 잡히지 않는다.
 *
 * <p><b>"아무 인터랙션 없이 이탈"은 여기에 없다.</b> 이탈을 별도 값으로 두려면 화면을 벗어나는
 * 순간 비콘을 쏴야 하는데, {@code pagehide}/{@code sendBeacon}은 브라우저·OS별로 유실률이
 * 높아 이탈만 과소 집계된다. 대신 {@code immersive_content_viewed}는 있는데 같은
 * {@code impression_id}의 {@code immersive_first_action}이 없는 노출을 이탈로 센다.
 * 있는 이벤트의 부재로 유도하는 편이 없는 이벤트의 도착을 기대하는 것보다 견고하다.
 */
public enum ImmersiveFirstAction {

    /** 투표 옵션 선택. */
    VOTE,

    /** 채팅 입력·전송. */
    CHAT,

    /** 이모지 반응. */
    EMOJI,

    /** 공유하기. */
    SHARE,

    /** 본문(설명) 펼치기. */
    EXPAND,

    /** 아무것도 누르지 않고 다음 콘텐츠로 스와이프. */
    SCROLL_NEXT
}
