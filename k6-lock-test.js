import http from 'k6/http';
import { check, sleep, fail } from 'k6';

// 100명의 가상 유저(VUs)가 동시에 단 1번 스파이크 요청을 쏘는 시나리오
export const options = {
  scenarios: {
    pessimistic_lock_spike: {
      executor: 'per-vu-iterations',
      vus: 1000,             // 100명의 동시 접속자
      iterations: 1,        // 각 유저당 1회 요청
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_failed': ['rate>0.95'], 
  },
};

const BASE_URL = 'http://localhost:8080';

// [1] 테스트 시작 전: 데이터 자동 초기화 (이전 테스트 잔여물 삭제 및 좌석 AVAILABLE 복구)
export function setup() {
  console.log('--- [Setup] 테스트 데이터 초기화 진행 중... ---');
  const res = http.post(`${BASE_URL}/api/test/reset`);
  if (res.status !== 200) {
    fail(`초기화 실패 (${res.status}): ${res.body}`);
  }
  console.log('--- [Setup] 데이터 초기화 완료! 10초 후 동시성 테스트 시작 ---');
  sleep(10);
}

export default function () {
  const vuId = __VU; // 1 ~ 100 가상 유저 고유 ID
  const url = `${BASE_URL}/api/test/reservations`;

  const payload = JSON.stringify({
    email: `stress_user_${vuId}@test.com`,
    scheduleId: 1,
    seatNumbers: ['A6', 'A7'],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);

  // 결과 검증
      check(res, {
        '성공(201)': (r) => r.status === 201,
        '품절마감(409)': (r) => r.status === 409,
        '서버 연결 튕김(Status 0)': (r) => r.status === 0,
        '서버 내부 에러(500)': (r) => r.status >= 500,
      });
    }

export function teardown() {
  console.log('--- [Teardown] 부하 테스트 완료! 터미널의 http_req_duration 지표를 확인하세요. ---');
}
