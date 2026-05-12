import requests
import uuid
from pprint import pprint
from datetime import datetime
import time

BASE_URL = "http://localhost:8080/v1/payments"


class PaymentServiceDemo:
    def __init__(self):
        self.customer_id = "cust_123"
        self.currency = "USD"
        self.idempotency_keys = []

    def demo_payment_lifecycle(self):
        print("--- DEMOING PAYMENT LIFECYCLE ---")
        # 1. Authorize
        idem_key = str(uuid.uuid4())
        self.idempotency_keys.append(idem_key)
        authorize_body = {
            "customerId": self.customer_id,
            "invoiceId": str(uuid.uuid4()),
            "amountMinor": 10000,
            "currency": self.currency
        }

        print("Authorizing $100.00...")
        resp = requests.post(
            f"{BASE_URL}/authorize",
            json=authorize_body,
            headers={"Idempotency-Key": idem_key}
        )
        resp.raise_for_status()
        payment_id = resp.json()['id']
        print(f"Success! Payment ID: {payment_id}\n")

        # 2. Capture
        print("Capturing $100.00...")
        capture_body = {
            "customerId": self.customer_id,
            "amountMinor": 10000,
            "currency": self.currency
        }
        resp = requests.post(
            f"{BASE_URL}/{payment_id}/capture",
            json=capture_body,
            headers={"Idempotency-Key": idem_key}
        )

        resp.raise_for_status()
        capture_id = resp.json()['id']
        print(f"Success! Capture ID: {capture_id}\n")

        # 3. Refund
        print("Refunding $25.00...")
        refund_body = {
            "customerId": self.customer_id,
            "amountMinor": 2500,
            "currency": self.currency
        }
        resp = requests.post(
            f"{BASE_URL}/{payment_id}/refunds",
            json=refund_body,
            headers={"Idempotency-Key": idem_key}
        )

        resp.raise_for_status()
        refund_id = resp.json()['id']
        print(f"Success! Refund ID: {refund_id}\n")

    def demo_idempotency(self):
        print("--- DEMOING IDEMPOTENT PAYMENT PROCESSING ---")
        print("Sending the first authorization request...")
        idem_key = str(uuid.uuid4())
        authorize_body = {
            "customerId": self.customer_id,
            "invoiceId": str(uuid.uuid4()),
            "amountMinor": 10000,
            "currency": self.currency
        }

        resp = requests.post(
            f"{BASE_URL}/authorize",
            json=authorize_body,
            headers={"Idempotency-Key": idem_key}
        )

        resp.raise_for_status()
        first_request_payment_id = resp.json()['id']

        print("Re-sending same authorization...")
        resp_retry = requests.post(
            f"{BASE_URL}/authorize",
            json=authorize_body,
            headers={"Idempotency-Key": idem_key}
        )

        # NOTE: Allow service to finish first request and cache response to
        # avoid 409 Conflict, which happens when it is in progress
        time.sleep(1)

        resp.raise_for_status()
        second_request_payment_id = resp_retry.json()['id']

        print(
            f"""Idempotency Check:
                first request payment id: {first_request_payment_id}
                second request payment id: {second_request_payment_id}\n
            """
        )

    def demo_double_entry_subledger(self):
        print("--- DEMOING DOUBLE-ENTRY SUBLEDGER ---")
        # Get account balance
        cash_clearing = '9d96ee97-3526-43a0-914c-190e2fb575c4'

        # NOTE: Account ID comes from V7__seed_ledger_accounts.sql

        print("Getting account balances...")
        resp = requests.get(
            f"{BASE_URL}/subledger/accounts/{cash_clearing}/balance"
        )

        resp.raise_for_status()
        print(
            f"Cash Clearing balance: {resp.json()['balance']}\n"
        )

        # Check Trial Balance
        print("Getting trial balance...")
        trial = requests.get(f"{BASE_URL}/subledger/trial-balance")
        trial.raise_for_status()
        print("Trial Balance:")
        pprint(trial.json())
        print("\n")

    def demo_reconciliation(self):
        print("--- DEMOING RECONCILIATION ---")
        proc_capture_ref = 'cap_' + self.idempotency_keys[-1][:8]
        proc_refund_ref = 'ref_' + self.idempotency_keys[-1][:8]

        # Create a mock CSV
        csv_content = (
            "business_date,record_type,processor_reference,amount,currency\n"
            f"{datetime.now().date()},CAPTURE,{proc_capture_ref},100.00,USD\n"
            f"{datetime.now().date()},REFUND,{proc_refund_ref},25.00,USD\n"
            f"{datetime.now().date()},CAPTURE,missing_internal_cap,42,USD"
        )

        # NOTE: Only money-movement records are reconciled (CAPTURE, REFUND)

        files = {'file': ('statement.csv', csv_content)}
        data = {'businessDate': str(datetime.now().date())}

        print("Importing Processor Statement...")
        import_resp = requests.post(
            f"{BASE_URL}/reconciliation/imports", files=files, data=data)
        import_resp.raise_for_status()
        run_id = import_resp.json()['runId']

        print(f"Running Reconciliation (Run ID: {run_id})...")
        requests.post(f"{BASE_URL}/reconciliation/runs", params=data)

        summary = requests.get(f"{BASE_URL}/reconciliation/runs/{run_id}")
        print("Reconciliation Summary:")
        import_resp.raise_for_status()
        pprint(summary.json())


if __name__ == "__main__":
    demo = PaymentServiceDemo()
    demo.demo_payment_lifecycle()
    demo.demo_idempotency()
    demo.demo_double_entry_subledger()
    demo.demo_reconciliation()
