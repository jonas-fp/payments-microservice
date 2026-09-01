import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500']
  }
};

export default function () {
  const url = 'http://127.0.0.1:8080/hello';
  const res = http.get(url);

  check(res, {
    'Status is 200': (r) => r.status === 200,
  });

  sleep(1);
}