CREATE SCHEMA IF NOT EXISTS mprocs;
use mprocs;
-- address Table
CREATE TABLE IF NOT EXISTS address (
    address_id INT AUTO_INCREMENT PRIMARY KEY,
    address_hash CHAR(64) NOT NULL,
    country VARCHAR(50),
    city VARCHAR(50),
    street_name VARCHAR(100),
    street_number VARCHAR(20),
    postal_code VARCHAR(20)
);

CREATE UNIQUE INDEX idx_address_hash ON address(address_hash);

CREATE TABLE IF NOT EXISTS role (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(16) NOT NULL UNIQUE
);

-- Contains general user data. Multiple users can have the same address.
CREATE TABLE IF NOT EXISTS `user` (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    -- address_id INT NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,

    password_hash VARCHAR(256) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(32),
    date_of_birth DATE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

);

CREATE INDEX idx_first_last_name ON `user`(first_name, last_name);
CREATE UNIQUE INDEX idx_username_email ON `user`(username, email);

-- join table defining many-to-many relationship
CREATE TABLE IF NOT EXISTS user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,

    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(role_id)
);
CREATE UNIQUE INDEX idx_user_role ON user_role(user_id, role_id);

CREATE TABLE IF NOT EXISTS user_address (
    user_id INT NOT NULL,
    address_id INT NOT NULL,

    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    FOREIGN KEY (address_id) REFERENCES address(address_id)
);
CREATE UNIQUE INDEX idx_user_address ON user_address(user_id, address_id);

CREATE TABLE IF NOT EXISTS account_details (
    account_details_id INT AUTO_INCREMENT PRIMARY KEY,
    account_name VARCHAR(64) NOT NULL,
    account_number VARCHAR(12) NOT NULL,
    routing_number VARCHAR(9) NOT NULL, -- for US transactions
    iban VARCHAR(64) NOT NULL, -- EU transactions
    bicswift VARCHAR(14) NOT NULL,
    bank_address VARCHAR(256) NOT NULL
);

CREATE INDEX idx_bicswift ON account_details(bicswift);
CREATE UNIQUE INDEX idx_account_number ON account_details(account_number);

CREATE TABLE IF NOT EXISTS account (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    account_details_id INT NOT NULL UNIQUE,
    user_id INT NOT NULL,

    total_balance DECIMAL(34, 16) NOT NULL,

    created_date TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (account_details_id) REFERENCES account_details(account_details_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE
);

-- USER-ACCOUNT relation should be 1 to 1
CREATE UNIQUE INDEX idx_user_account ON account(user_id);
-- account details are unique to each account
CREATE UNIQUE INDEX idx_details_account ON account(account_details_id);

-- external accounts table - for accounts that are not part of the network and engage in transactions within the network
CREATE TABLE IF NOT EXISTS external_account (
    external_account_id INT AUTO_INCREMENT PRIMARY KEY,
    account_details_id INT NOT NULL,

    FOREIGN KEY (account_details_id) REFERENCES account_details(account_details_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_ext_acc_details ON external_account(account_details_id);

-- currency table
CREATE TABLE IF NOT EXISTS currency (
    currency_id INT AUTO_INCREMENT PRIMARY KEY,
    currency_code VARCHAR(6) NOT NULL,
    currency_name VARCHAR(50) NOT NULL,

    is_active BOOLEAN DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_currency_code ON currency(currency_code);
CREATE INDEX idx_currency_name ON currency(currency_name);

-- currency exchange rates
CREATE TABLE IF NOT EXISTS currency_exchange_rate (
    currency_exchange_rate_id INT AUTO_INCREMENT PRIMARY KEY,
    source_currency_id INT NOT NULL,
    target_currency_id INT NOT NULL,

    exchange_rate DECIMAL(24, 10) NOT NULL, -- 24 total digits, 10 after decimal point
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (source_currency_id) REFERENCES currency(currency_id) ON DELETE CASCADE,
    FOREIGN KEY (target_currency_id) REFERENCES currency(currency_id) ON DELETE CASCADE
);

CREATE INDEX IDX_exchange_rate ON currency_exchange_rate(exchange_rate);
CREATE UNIQUE INDEX idx_currency_source_target ON currency_exchange_rate(source_currency_id, target_currency_id);

-- wallets
CREATE TABLE IF NOT EXISTS wallet (
    wallet_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    currency_id INT NOT NULL,
    balance DECIMAL(20, 6) NOT NULL,
    is_main BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (account_id) REFERENCES account(account_id) ON DELETE CASCADE,
    FOREIGN KEY (currency_id) REFERENCES currency(currency_id) ON DELETE CASCADE,

    UNIQUE (account_id, currency_id) -- 1:1 an account can't have multiple wallets for the same currency
);

CREATE TABLE IF NOT EXISTS merchant (
    merchant_id INT AUTO_INCREMENT PRIMARY KEY,
    merchant_name VARCHAR(100) NOT NULL,
    merchant_category_code VARCHAR(10), -- https://en.wikipedia.org/wiki/Merchant_category_code
    mid VARCHAR(50), -- Unique Merchant Identifier: https://www.forbes.com/advisor/business/software/merchant-id/
    contact_details VARCHAR(100)
);

CREATE UNIQUE INDEX idx_merchant_name ON merchant(merchant_name);
-- CREATE INDEX idx_merchant_mcc ON merchant(merchant_category_code);
-- CREATE UNIQUE INDEX idx_merchant_mid ON merchant(mid);


-- source of truth for money, ledger accounts and entries
CREATE TABLE IF NOT EXISTS ledger_account_type (
    ledger_account_type_id INT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(64) NOT NULL UNIQUE,   -- 'WALLET_AVAILABLE', etc.
    type_description VARCHAR(256)
);

CREATE UNIQUE INDEX idx_ledger_account_type ON ledger_account_type(type_code);


-- created at wallet/merchant level. 2 for each wallet(available funds, held funds), 1 for merchant as money is owed to the merchant.
CREATE TABLE IF NOT EXISTS ledger_account (
    ledger_account_id INT AUTO_INCREMENT PRIMARY KEY,
    ledger_account_type_id INT NOT NULL,

    ledger_account_name VARCHAR(128) NOT NULL, -- ("User 123 EUR Wallet", "Card Clearing EUR")

    currency_id INT NOT NULL,

    wallet_id INT NULL,
    merchant_id INT NULL,
    external_account_id INT NULL,

    is_active BOOLEAN DEFAULT TRUE,

  /*
     * owner_key normalizes ownership into a single value to enforce uniqueness across
     * wallet, merchant, external, and system
     * ledger accounts.
     * This is required because MySQL UNIQUE indexes allow multiple NULLs,
     * which would allow duplicate system accounts (e.g. multiple
     * FX_BRIDGE EUR accounts).
     */
    owner_key VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN wallet_id IS NOT NULL THEN CONCAT('W:', wallet_id)
                WHEN merchant_id IS NOT NULL THEN CONCAT('M:', merchant_id)
                WHEN external_account_id IS NOT NULL THEN CONCAT('E:', external_account_id)
                ELSE 'SYS'
            END
        ) STORED,

    FOREIGN KEY (ledger_account_type_id) REFERENCES ledger_account_type(ledger_account_type_id),
    FOREIGN KEY (currency_id) REFERENCES currency(currency_id),
    FOREIGN KEY (wallet_id) REFERENCES wallet(wallet_id),
    FOREIGN KEY (merchant_id) REFERENCES merchant(merchant_id),
    FOREIGN KEY (external_account_id) REFERENCES external_account(external_account_id),

    -- Ensure exactly one owner (wallet OR merchant OR external OR system)
    CONSTRAINT chk_ledger_account_owner
        CHECK (
            (wallet_id IS NOT NULL AND merchant_id IS NULL AND external_account_id IS NULL)
            OR (merchant_id IS NOT NULL AND wallet_id IS NULL AND external_account_id IS NULL)
            OR (external_account_id IS NOT NULL AND wallet_id IS NULL AND merchant_id IS NULL)
            OR (wallet_id IS NULL AND merchant_id IS NULL AND external_account_id IS NULL)
        )
);

CREATE INDEX idx_ledger_acc_name ON ledger_account(ledger_account_name);
-- 1:1 ledger per wallet per type( only one available EUR wallet, only one HELD USD wallet, etc. )
CREATE UNIQUE INDEX uq_wallet_available ON ledger_account(wallet_id, ledger_account_type_id, currency_id);
CREATE UNIQUE INDEX uq_wallet_held ON ledger_account(wallet_id, ledger_account_type_id, currency_id);

-- 1:1 ledger per merchant balance
CREATE UNIQUE INDEX uq_merchant_settlement ON ledger_account(merchant_id, ledger_account_type_id, currency_id);
-- enforces one ledger account per (owner + account type + currency)
CREATE UNIQUE INDEX uq_ledger_owner_key_type_currency ON ledger_account(owner_key, ledger_account_type_id, currency_id);

-- cache table for wallet balances
CREATE TABLE ledger_account_balance (
  ledger_account_id INT PRIMARY KEY,
  balance DECIMAL(34, 16) NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (ledger_account_id) REFERENCES ledger_account(ledger_account_id)
);

-- types: virtual, virtual one-time use, etc;
CREATE TABLE IF NOT EXISTS card_type (
    card_type_id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    UNIQUE(type)
);

CREATE TABLE IF NOT EXISTS card (
    card_id INT AUTO_INCREMENT PRIMARY KEY,
    card_type_id INT NOT NULL,
    account_id INT NOT NULL,
    card_id_hash VARCHAR(256) NOT NULL, -- unique identifier hash fro card info
    card_provider VARCHAR(32) NOT NULL, -- ('visa', 'mastercard')
    card_number VARCHAR(19), -- maestro cards can have 19 digits
    expiration_date DATE NOT NULL,
    cvv CHAR(3) NOT NULL,
    pin VARCHAR(256) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'FROZEN') NOT NULL,

    creation_date DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (card_type_id) REFERENCES card_type(card_type_id),
    FOREIGN KEY (account_id) REFERENCES account(account_id) ON DELETE CASCADE,
    UNIQUE(card_provider, card_number, expiration_date, cvv),
    UNIQUE(card_id_hash, card_provider)
);

CREATE TABLE IF NOT EXISTS transaction_type (
    transaction_type_id INT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(64) NOT NULL -- DEPOSIT, WITHDRAWAL, WALLET_TO_WALLET_TRANSFER, INTERNAL_TRANSFER, EXTERNAL_TRANSFER, CARD_PAYMENT
);

CREATE TABLE IF NOT EXISTS transaction_status (
    transaction_status_id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(32) NOT NULL -- ('pending', 'completed', 'cancelled')
);

CREATE TABLE IF NOT EXISTS `transaction` (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_type_id INT NOT NULL,
    transaction_status_id INT NOT NULL,
    currency_id INT NOT NULL,

    related_transaction_id INT DEFAULT NULL,

    amount DECIMAL(20, 6) NOT NULL,
    fees DOUBLE,
    transaction_date DATETIME NOT NULL,
    description VARCHAR(256),

    FOREIGN KEY (transaction_type_id) REFERENCES transaction_type(transaction_type_id),
    FOREIGN KEY (transaction_status_id) REFERENCES transaction_status(transaction_status_id),
    FOREIGN KEY (currency_id) REFERENCES currency(currency_id),
    FOREIGN KEY (related_transaction_id) REFERENCES `transaction`(transaction_id)
);


CREATE TABLE IF NOT EXISTS money_transfer (
    transaction_id INT PRIMARY KEY,
    sender_account_id INT NOT NULL,
    receiver_account_id INT NOT NULL,
    target_currency_code VARCHAR(6),

    FOREIGN KEY (transaction_id) REFERENCES `transaction`(transaction_id),
    FOREIGN KEY (sender_account_id) REFERENCES account(account_id),
    FOREIGN KEY (receiver_account_id) REFERENCES account(account_id)
);

CREATE TABLE IF NOT EXISTS card_authorization (
    transaction_id INT PRIMARY KEY,
    card_id INT NOT NULL,
    merchant_id INT NOT NULL,
    authorization_code VARCHAR(255) NOT NULL, -- proof of auth, will send to card network when auth_hold is successful.
    authorized_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (transaction_id) REFERENCES `transaction`(transaction_id),
    FOREIGN KEY (card_id) REFERENCES card(card_id),
    FOREIGN KEY (merchant_id) REFERENCES merchant(merchant_id)
);

CREATE UNIQUE INDEX idx_auth_code ON card_authorization(authorization_code);

-- pending hold of funds, representing that the transaction is valid and flagged for settlement process.
CREATE TABLE IF NOT EXISTS authorized_hold (
    authorized_hold_id INT AUTO_INCREMENT PRIMARY KEY,
    card_authorization_id INT NOT NULL,         -- references the authorization tx
    wallet_id INT NOT NULL,
    hold_amount DECIMAL(20, 6) NOT NULL,
    hold_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    is_released BOOLEAN DEFAULT FALSE,
    released_at DATETIME,

    FOREIGN KEY (card_authorization_id) REFERENCES card_authorization(transaction_id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES wallet(wallet_id)
);

-- will settle all pending hold transactions when the card network sends a request
CREATE TABLE hold_settlement (
    hold_settlement_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,                  -- this is the settlement transaction
    card_authorization_id INT NOT NULL,    -- references the authorization tx
    card_id INT NOT NULL,
    merchant_id INT NOT NULL,
    settled_amount DECIMAL(20, 6) NOT NULL,
    settled_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (transaction_id) REFERENCES `transaction`(transaction_id) ON DELETE CASCADE,
    FOREIGN KEY (card_authorization_id) REFERENCES card_authorization(transaction_id) ON DELETE CASCADE,
    FOREIGN KEY (card_id) REFERENCES card(card_id),
    FOREIGN KEY (merchant_id) REFERENCES merchant(merchant_id)
);

-- holds the entries of money movements for a user/merchant/external account, etc.
-- For a given transaction_id and currency, the sum of amount across all rows must be 0.
CREATE TABLE IF NOT EXISTS ledger_entry (
    ledger_entry_id INT AUTO_INCREMENT PRIMARY KEY,

    -- ensure uniqueness on entry groups by base transaction
    entry_group_key VARCHAR(128) NOT NULL,

    transaction_id INT NOT NULL,
    ledger_account_id INT NOT NULL,

    amount DECIMAL(34, 16) NOT NULL, -- signed: positive or negative
    entry_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(256),

    FOREIGN KEY (transaction_id) REFERENCES `transaction`(transaction_id),
    FOREIGN KEY (ledger_account_id) REFERENCES ledger_account(ledger_account_id),

    INDEX idx_ledger_tx (transaction_id),
    INDEX idx_ledger_account (ledger_account_id),
    INDEX idx_ledger_account_date (ledger_account_id, entry_date)
);

CREATE UNIQUE INDEX u_idx_entry_group_key ON ledger_entry(entry_group_key, transaction_id);