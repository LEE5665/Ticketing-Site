# Ticketing-Site

콘서트, 뮤지컬, 페스티벌, 전시를 조회하고 원하는 회차의 티켓을 예매할 수 있는 티켓 예매 사이트입니다.
공연 검색부터 좌석 선택, 결제, 예매 내역 조회까지 지원하며, 여러 사용자가 같은 좌석을 동시에 예매할 때 발생하는 충돌을 처리하는 데 중점을 두었습니다.

# 프로젝트 정보

### 1. 제작 기간

> 2026.9.21 ~ 2024.10.8

# 사용 기술

- Next.js, React, TypeScript
- Tailwind CSS
- Java 21, Spring Boot
- Spring Security (세션 기반 인증, CSRF 보호)
- Spring Data JPA, MariaDB
- Redis, Redisson (좌석별 분산 락)
- Toss Payments SDK 및 결제 승인 API (테스트 결제)
- Docker Compose (MariaDB, Redis, Adminer 실행)
- k6 (동시 예매 부하 테스트)

> MariaDB에 회원, 공연, 회차, 좌석과 예매 데이터를 저장합니다. 예매 생성 시 Redis와 Redisson으로 좌석별 락을 획득하고, JPA의 낙관적 락과 함께 중복 예매를 방지합니다.

# ERD

<details>
  <summary>ERD</summary>
  <img width="1288" height="713" alt="image" src="https://github.com/user-attachments/assets/968ab030-9097-4080-a41a-96d442e6dbab" />
</details>

# 기능

### 로그인 & 회원가입

- 이름, 이메일, 비밀번호를 통한 회원가입
- 중복 이메일 확인
- 이메일, 비밀번호 로그인 및 로그아웃
- 로그인한 회원 정보 조회
- BCrypt를 이용한 비밀번호 해싱
- 세션 기반 인증 및 CSRF 토큰 검증

### 공연 조회 & 검색

- 공연 목록 및 상세 정보 조회
- 콘서트, 뮤지컬, 페스티벌, 전시 카테고리 필터
- 공연명 또는 장소 검색
- 공연 장소, 가격, 일정 확인
- 공연별 예매 가능한 날짜와 시간 선택

### 좌석 선택 & 예매

- 선택한 회차의 좌석 상태 조회
- 콘서트, 뮤지컬 좌석 직접 선택 및 최대 4개 선택 제한
- 페스티벌, 전시 티켓 수량 선택 및 예매 가능한 좌석 자동 배정
- 선택한 좌석과 수량에 따른 총금액 표시
- 주문번호 생성 및 결제 대기 예매 등록
- 예매 생성 시 좌석을 5분간 선점
- 다른 사용자가 처리 중이거나 이미 선점, 예매된 좌석에 대한 충돌 안내

> 좌석 번호를 정렬하고 중복을 제거한 뒤 Redisson 락을 획득합니다. 락을 얻지 못하면 대기 없이 HTTP 409를 반환하며, 모든 락을 획득한 후 DB 트랜잭션을 시작하고 커밋 또는 롤백 이후 해제합니다. Redis 장애 시 DB만으로 예매를 진행하는 우회 경로는 두지 않았습니다.

> Redis 락은 예매 생성 요청을 처리하는 동안 유지되며 사용자의 결제 시간을 확보하는 5분 선점은 DB의 HOLD 상태와 만료 시각으로 관리합니다. 만료된 선점 좌석은 10초 주기의 스케줄러가 해제합니다. 좌석별 락은 예매 생성 요청에 적용되며 다른 DB API의 접근까지 제한하지는 않습니다.

### 결제

- 토스페이먼츠 SDK를 통한 테스트 결제창 연동
- 서버에서 토스페이먼츠 결제 승인 API 호출
- DB에 저장된 예매 금액과 결제 요청 금액 비교
- 결제 승인 후 예매 완료 및 좌석 예약 상태 확정
- 결제 성공, 실패 화면 제공
- 결제 진행 중 취소, 오류 발생 시 미결제 예매 취소 처리

> 외부 결제 승인 요청과 DB 상태를 확정하는 트랜잭션을 분리했습니다. 외부 API 응답을 기다리는 동안 DB 트랜잭션을 유지하지 않고, 승인 이후 별도 트랜잭션에서 예매와 좌석 상태를 변경합니다.

### 예매 관리

- 로그인한 사용자의 예매 내역 조회
- 공연명, 장소, 회차, 좌석 번호 및 결제 금액 확인
- 결제 대기, 예매 완료, 예매 취소 상태 표시
- 본인의 미결제 예매 취소 및 선점 좌석 해제
- 선점 토큰을 확인해 취소 대상 예매가 보유한 좌석만 해제

> 예매 내역 조회에서는 Fetch Join으로 공연, 회차, 예매 좌석 정보를 함께 조회합니다. 결제 완료된 예매는 일반 예매 취소 API의 취소 대상에서 제외합니다.

# 스크린샷(기능 설명)

<details>
  <summary>로그인 & 회원가입</summary>
  이메일 로그인 및 회원가입 화면
  <img width="1311" height="858" alt="image" src="https://github.com/user-attachments/assets/549726df-4b48-4786-9209-90fc5ed4e0b1" />

</details>

<details>
  <summary>메인 & 공연 검색</summary>
  공연 목록, 카테고리 필터 및 공연명, 장소 검색 화면
  <img width="1303" height="752" alt="image" src="https://github.com/user-attachments/assets/767ba142-2408-47bc-afd2-8d34645500eb" />

</details>

<details>
  <summary>공연 상세 & 회차 선택</summary>
  공연 정보, 날짜 및 시간 선택 화면
  <img width="1463" height="913" alt="image" src="https://github.com/user-attachments/assets/5e98c54a-a591-49b3-b55d-dc5c40c32e29" />

</details>

<details>
  <summary>좌석 선택 & 티켓 예매</summary>
  좌석 배치, 선택한 좌석, 티켓 수량 및 총금액 확인 화면
  <img width="1250" height="894" alt="image" src="https://github.com/user-attachments/assets/5a555fa8-41fc-4296-ae1f-e3a9486fa9fb" />
</details>

<details>
  <summary>결제 & 결제 결과</summary>
  토스페이먼츠 테스트 결제창 및 결제 화면
  <img width="1263" height="896" alt="image" src="https://github.com/user-attachments/assets/106fb63f-3d17-4991-aa29-ab7e68a67bf9" />

</details>

<details>
  <summary>내 예매 내역</summary>
  예매한 공연, 회차, 좌석, 결제 금액 및 예매 상태 조회 화면
  <img width="1291" height="654" alt="image" src="https://github.com/user-attachments/assets/a42e42ac-cf06-4665-84d7-b1289207e6aa" />

</details>

# 테스트

> k6 스크립트는 준비 단계에서 테스트 회원을 생성하고 기존 예매와 좌석 상태를 초기화합니다.

# 느낀 점

- 좌석 예매는 여러 요청이 같은 좌석을 동시에 선택할 수 있어, 좌석별 분산 락과 DB의 낙관적 락을 함께 고려하면서 동시성 제어의 필요성을 배웠다.
- 락을 사용하는 것뿐 아니라 언제 획득하고 해제하는지도 중요했다. Redis 락을 획득한 뒤 DB 트랜잭션을 시작하고 커밋, 롤백 이후 해제하도록 구성하면서, 락의 범위와 트랜잭션 경계를 함께 설계하는 경험을 했다.
- 결제를 구현하면서 외부 API 통신과 DB 변경을 구분해야 한다. 결제 승인 통신 이후 별도 트랜잭션으로 예매를 확정하고 금액을 비교하도록 구성하며, 외부 시스템과 내부 데이터의 상태를 함께 고려해야 한다.
- k6 부하 테스트를 작성하면서 정상적인 예매 흐름 외에도 동시 요청, 좌석 충돌, 취소와 만료 같은 상황을 검증할 필요성을 느꼈다.
