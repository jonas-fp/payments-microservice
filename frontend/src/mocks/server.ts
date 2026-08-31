import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

const handlers = [
  http.get('/api/v1/payments/subledger/trial-balance', () => {
    return new HttpResponse(null, { status: 500 });
  }),
];

export const server = setupServer(...handlers);
