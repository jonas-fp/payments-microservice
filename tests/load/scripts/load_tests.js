import http from 'k6/http';
import { check, sleep } from 'k6';
import { open as fsOpen } from 'k6/experimental/fs';
import csv from 'k6/experimental/csv';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';
import exec from 'k6/execution';

export const options = {
  vus: 1,
  iterations: 1,
};

// NOTE: Change to env variables later if script used in prod or staging
// NOTE: Update business date to match date of test data 
const rootUrl = 'http://127.0.0.1:8080/v1';
const businessDate = '2026-09-05';

const transactionsFile = await fsOpen('../data/transactions.csv');
const transactions = await csv.parse(transactionsFile, {
  asObjects: true,
  delimiter: ',',
  skipFirstLine: false,
});

const processorStatementCsv = open('../data/processor_statement.csv');

export default async function () {
  createCaptures();
  uploadProcessorStatement();
  const runId = runReconciliation();
  // NOTE: Reconciliation is synchronous, so I can check the result immediately.
  //       I may make reconciliation asynchronous at a later date.
  checkReconciliationRun(runId);
}

function createCaptures() {
  for (let i = 0; i < transactions.length; i++) {
    const paymentId = authorize(
      transactions[i].idempotencyKey,
      transactions[i].amount,
    );
    capture(transactions[i].idempotencyKey, transactions[i].amount, paymentId);
  }
}

function authorize(idempotencyKey, minorAmount) {
  const url = rootUrl + '/payments/authorize';
  const headers = {
    headers: {
      'Idempotency-Key': idempotencyKey,
      'Content-Type': 'application/json',
    },
  };
  const body = JSON.stringify({
    customerId: 'cust_001',
    invoiceId: '9d4a0e07-cf22-4ed4-8c9f-4fc38d4b9ee9',
    minorAmount: minorAmount,
    currency: 'USD',
  });

  const res = http.post(url, body, headers);

  const success = check(res, {
    'authorize status is 201': (r) => r.status == 201,
  });

  if (!success) {
    exec.test.abort(
      'Aborting test: Authorize failed with status ' + res.status,
    );
  }
  return res.json().id;
}

function capture(idempotencyKey, minorAmount, paymentId) {
  const url = rootUrl + `/payments/${paymentId}/capture`;
  const headers = {
    headers: {
      'Idempotency-Key': idempotencyKey,
      'Content-Type': 'application/json',
    },
  };
  const body = JSON.stringify({
    customerId: 'cust_001',
    minorAmount: minorAmount,
    currency: 'USD',
  });

  const res = http.post(url, body, headers);

  const success = check(res, {
    'capture status is 201': (r) => r.status == 201,
  });

  if (!success) {
    exec.test.abort('Aborting test: Capture failed with status ' + res.status);
  }
}

function uploadProcessorStatement() {
  const url = rootUrl + '/payments/reconciliation/imports';
  const fd = new FormData();
  const headers = {
    headers: {
      'Content-Type': 'multipart/form-data; boundary=' + fd.boundary,
    },
  };

  fd.append('businessDate', businessDate);
  fd.append(
    'file',
    http.file(processorStatementCsv, 'processor_statement.csv', 'text/plain'),
  );

  const res = http.post(url, fd.body(), headers);

  const success = check(res, {
    'upload statement status is 201': (r) => r.status == 201,
  });

  if (!success) {
    exec.test.abort(
      'Aborting test: Upload statement failed with status ' + res.status,
    );
  }
}

function runReconciliation() {
  const url =
    rootUrl + `/payments/reconciliation/runs?businessDate=${businessDate}`;
  const headers = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  const body = JSON.stringify({});

  const res = http.post(url, body, headers);

  const success = check(res, {
    'run reconciliation status is 201': (r) => r.status == 201,
  });

  if (!success) {
    exec.test.abort(
      'Aborting test: run reconciliation failed with status ' + res.status,
    );
  }

  console.log(`Total latency: ${res.timings.duration}ms`);
  return res.json().runId;
}

function checkReconciliationRun(runId) {
  const url = rootUrl + `/payments/reconciliation/runs/${runId}`;
  const res = http.get(url);

  const success = check(res, {
    'get reconciliation status is 200': (r) => r.status == 200,
  });

  if (!success) {
    exec.test.abort(
      'Aborting test: Get reconciliation status failed with status ' +
        res.status,
    );
  }

  const responseBody = res.json();
  console.log(responseBody);

  check(res, {
    'reconciliation succeeded': (res) => responseBody.status == 'SUCCEEDED',
  });
}
