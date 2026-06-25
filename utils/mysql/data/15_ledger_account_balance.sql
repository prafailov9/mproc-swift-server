-- backfill from existing ledger accounts, should be exactly 1:1 account - balance
-- set available balance for WALLET_AVAIL
-- set 0 balance for held
INSERT INTO ledger_account_balance (ledger_account_id, balance_minor)
SELECT
    la.ledger_account_id,
    CASE
        WHEN la.wallet_id IS NOT NULL
            AND lat.type_code = 'WALLET_AVAILABLE'
            THEN w.balance

        WHEN la.wallet_id IS NOT NULL
            AND lat.type_code = 'WALLET_HELD'
            THEN 0

        WHEN la.merchant_id IS NOT NULL
            THEN 0

        WHEN la.external_account_id IS NOT NULL
            THEN 0

        -- system-level accounts, e.g. FX Bridge EUR/USD
        WHEN la.wallet_id IS NULL
            AND la.merchant_id IS NULL
            AND la.external_account_id IS NULL
            THEN 0

        ELSE 0
        END AS balance_minor
FROM ledger_account la
         JOIN ledger_account_type lat
              ON lat.ledger_account_type_id = la.ledger_account_type_id
         LEFT JOIN wallet w
                   ON w.wallet_id = la.wallet_id
         LEFT JOIN ledger_account_balance lab
                   ON lab.ledger_account_id = la.ledger_account_id
WHERE lab.ledger_account_id IS NULL;