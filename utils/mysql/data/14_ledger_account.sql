-- ledger accounts
-- FX System Accounts
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'FX Bridge EUR', 42, null, null, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'FX Bridge USD', 143, null, null, null, true);


-- 1. Merchant las:
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'Valve Corp. Settlement EUR', 42, null, 2, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
     VALUES (3, 'Tesla Inc. Settlement EUR', 42, null, 3, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'Apple Inc. Settlement EUR', 42, null, 4, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'Microsoft Settlement EUR', 42, null, 5, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'Google Settlement EUR', 42, null, 6, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'Amazon Settlement EUR', 42, null, 7, null, true);
INSERT INTO ledger_account (ledger_account_type_id, ledger_account_name ,currency_id, wallet_id, merchant_id, external_account_id, is_active)
    VALUES (3, 'Netflix Settlement EUR', 42, null, 8, null, true);

-- generate ledger accounts for wallets
INSERT INTO ledger_account (
    ledger_account_type_id,
    ledger_account_name,
    currency_id,
    wallet_id,
    merchant_id,
    external_account_id,
    is_active
)
SELECT
    lat.ledger_account_type_id,

    CONCAT(
        'Account ',
        a.account_id,
        ' – ',
        c.currency_code,
        ' Wallet (',
        CASE lat.type_code
            WHEN 'WALLET_AVAILABLE' THEN 'Available'
            WHEN 'WALLET_HELD' THEN 'Held'
        END,
        ')'
    ) AS ledger_account_name,

    w.currency_id,
    w.wallet_id,
    NULL,
    NULL,
    TRUE

FROM wallet w
JOIN account a
  ON a.account_id = w.account_id
JOIN currency c
  ON c.currency_id = w.currency_id
JOIN ledger_account_type lat
  ON lat.type_code IN ('WALLET_AVAILABLE', 'WALLET_HELD')

LEFT JOIN ledger_account la
  ON la.wallet_id = w.wallet_id
 AND la.currency_id = w.currency_id
 AND la.ledger_account_type_id = lat.ledger_account_type_id

WHERE la.ledger_account_id IS NULL;