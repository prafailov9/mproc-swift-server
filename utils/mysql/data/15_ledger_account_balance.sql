-- backfill from existing ledger accounts, should be exactly 1:1 account - balance
INSERT INTO ledger_account_balance (ledger_account_id, balance_minor)
SELECT la.ledger_account_id, 0
FROM ledger_account la
LEFT JOIN ledger_account_balance lab
  ON lab.ledger_account_id = la.ledger_account_id
WHERE lab.ledger_account_id IS NULL;