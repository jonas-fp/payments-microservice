import uuid
import random

TRANSACTION_COUNT = 100000
BUSINESS_DATE = '2026-09-03'

with (
  open("transactions.csv", "w") as transaction_file,
  open("processor_statement.csv", "w") as statement_file,
  ):

    transaction_file.write(
      "idempotencyKey,amount\n"
      )
    statement_file.write(
      "business_date,record_type,processor_reference,amount,currency\n"
      )

    for transaction_num in range(TRANSACTION_COUNT):
        idempotency_key = str(uuid.uuid4())
        amount = str(random.randint(1, 200))
        if transaction_num != TRANSACTION_COUNT - 1:
            transaction_file.write(f"{idempotency_key},{amount}00\n")
            statement_file.write(
                f"{BUSINESS_DATE},CAPTURE,cap_{idempotency_key[0:8]},"
                f"{amount}.00,USD\n"
            )
        else:
            transaction_file.write(f"{idempotency_key},{amount}00")
            statement_file.write(
                f"{BUSINESS_DATE},CAPTURE,cap_{idempotency_key[0:8]},"
                f"{amount}.00,USD"
            )
