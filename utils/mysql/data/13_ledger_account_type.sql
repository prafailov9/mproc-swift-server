-- ledger account types
INSERT INTO ledger_account_type (type_code, type_description) VALUES
('WALLET_AVAILABLE',   'Spendable funds in a user wallet'),
('WALLET_HELD',        'Funds reserved by card authorizations'),
('MERCHANT_SETTLEMENT','Money accumulated for merchant payouts'),
('EXTERNAL_CLEARING',  'Money moving between system and external bank'),
('FEE_INCOME',         'Accumulated fee income'),
('FX_BRIDGE',          'System account to handle FX rates conversions');
