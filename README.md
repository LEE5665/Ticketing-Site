# Ticketing-Site

예매 생성은 Redisson 좌석별 락을 획득한 뒤 DB 트랜잭션을 시작합니다.
획득 실패 시 대기 없이 409를 반환하고, 획득한 락은 커밋/롤백 후 해제합니다.
DB의 5분 HOLD와 낙관적 락은 유지합니다. Redis 장애 시 DB로 우회하지 않습니다.

- Redis 실행: `docker compose up -d redis`
- 기본 주소: `redis://localhost:6379` (`REDIS_ADDRESS`로 변경)
- 락 TTL: watchdog 기본 30초, 처리 중 자동 연장. 고정 5분 HOLD가 아닙니다.
- 테스트: Redis 실행 후 `cd backend`에서 `.\gradlew.bat test`
- 부하 테스트: `k6 run k6-lock-test.js`. 준비 단계에서 테스트 회원을 생성합니다.
  별도로 `/api/test/reservations`를 호출할 때는 회원을 미리 생성해야 합니다.

락은 예매 생성 요청에 적용됩니다. 다른 좌석의 요청이나 다른 DB API의 동시 접근까지 제한하지 않습니다.
