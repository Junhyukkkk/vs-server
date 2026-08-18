# 홈 화면 클릭 수집 연동 가이드

서버는 배포 완료. 프론트에서 아래 두 엔드포인트를 호출하면 된다.

## 보내는 것

| 영역 | 호출 | 이벤트명 |
|---|---|---|
| 핫토픽 캐러셀 (1\~3위) | `POST /api/home/hot-topics/{voteId}/click` + `{"rank": 1~3}` | `hot_topic_carousel_clicked` |
| 핫토픽 리스트 (4\~5위) | `POST /api/home/hot-topics/{voteId}/click` + `{"rank": 4~5}` | `hot_topic_list_clicked` |
| '모든 투표' 리스트 | `POST /api/home/votes/{voteId}/click` (본문 없음) | `all_votes_clicked` |

- 핫토픽은 **엔드포인트 1개**다. `rank`만 보내면 캐러셀/리스트는 서버가 나눈다.
- `rank`는 `GET /api/home/hot-topics` 응답의 `hotTopics[].rank`를 그대로 넣는다.
- 성공 시 `204 No Content`.

## 호출 코드

```js
// 핫토픽 카드 탭 — 캐러셀/리스트 공통
function trackHotTopicClick(voteId, rank) {
  fetch(`${API_BASE}/api/home/hot-topics/${voteId}/click`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rank }),
    keepalive: true,
  }).catch(() => {});
}

// '모든 투표' 카드 탭
function trackAllVotesClick(voteId) {
  fetch(`${API_BASE}/api/home/votes/${voteId}/click`, {
    method: 'POST',
    credentials: 'include',
    keepalive: true,
  }).catch(() => {});
}
```

```jsx
<HotTopicCard
  onClick={() => {
    trackHotTopicClick(item.voteId, item.rank);
    navigate(`/vote/${item.voteId}`);   // 응답 기다리지 않음
  }}
/>
```

## 주의

**`credentials: 'include'` 빠뜨리지 말 것.** 없어도 요청은 `204`로 성공하고 화면상 이상이
전혀 없는데, 쿠키가 안 실려서 로그인 사용자가 전부 비회원으로 찍히고 매 클릭이 새 익명
사용자로 잡힌다. 숫자 뽑을 때까지 발견이 안 되니 처음에 확인하고 넘어갈 것.

**`keepalive: true` 넣을 것.** 클릭 직후 라우팅이라 없으면 전환이 빠를 때 요청이 취소된다.

그 외:

- 카드 1회 탭 = 호출 1회. 중복 호출하면 클릭이 부풀려진다.
- `area` 같은 영역 값은 보내지 않는다. 보내도 무시된다.
- CSRF 토큰 불필요.
- `.catch(() => {})` 붙일 것. 수집 실패가 화면 동작에 영향을 주면 안 된다.
- GA로 따로 쏘지 말 것. 서버가 DB · GA4 · Amplitude에 같이 넣는다. 따로 쏘면 이중 집계된다.
- `user_id` / `anonymous_id` / `platform` / 시각은 서버가 붙인다. 보낼 필요 없다.

## 에러

| 상황 | 응답 |
|---|---|
| 정상 | `204 No Content` |
| `rank` 누락 | `400` |
| `rank`가 1\~5 밖 | `400` |
| `Content-Type: application/json` 누락 | `415` |

## 확인

1. 핫토픽 1위 탭 → `POST /api/home/hot-topics/{voteId}/click`이 `204`로 나가는지
2. 요청 헤더에 `Cookie`가 실려 있는지 (비어 있으면 `credentials` 누락)
3. GA4 실시간에서 `hot_topic_carousel_clicked` 확인
4. 4\~5위 탭 → `hot_topic_list_clicked`로 **다르게** 잡히는지

Swagger는 **홈 트래킹** 태그.
