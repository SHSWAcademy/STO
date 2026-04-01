-- =====================================================
-- STO Platform DDL (PostgreSQL)
-- 테이블명: TypeScript interface 기준
-- 컬럼명:   Excel 영문 논리명 기준
-- =====================================================

-- =====================================================
-- ENUM 타입 선언
-- =====================================================
CREATE TYPE order_type_enum        AS ENUM ('BUY', 'SELL');
CREATE TYPE order_status_enum      AS ENUM ('OPEN', 'PENDING', 'PARTIAL', 'FILLED', 'CANCELLED', 'FAILED');
CREATE TYPE settlement_status_enum AS ENUM ('ON_CHAIN_PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE queue_status_enum      AS ENUM ('PENDING', 'PROCESSING', 'SUBMITTED', 'CONFIRMED', 'FAILED', 'ABANDONED');
CREATE TYPE tx_status_enum         AS ENUM ('SUBMITTED', 'CONFIRMED', 'REVERTED', 'UNKNOWN');
CREATE TYPE tx_type_enum           AS ENUM ('TRADE', 'DEPLOY');
CREATE TYPE banking_type_enum      AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'ORDER_LOCK', 'ORDER_UNLOCK', 'TRADE_SETTLEMENT', 'DIVIDEND_DEPOSIT');
CREATE TYPE banking_status_enum    AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE asset_dir_enum         AS ENUM ('DEPOSIT', 'WITHDRAWAL');
CREATE TYPE asset_usage_enum       AS ENUM ('ALLOCATION', 'EXTRA');
CREATE TYPE platform_dir_enum      AS ENUM ('DEPOSIT', 'WITHDRAWAL');
CREATE TYPE income_type_enum       AS ENUM ('FEE', 'DIVIDEND');
CREATE TYPE payout_status_enum     AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE disclosure_cat_enum    AS ENUM ('BUILDING', 'DIVIDEND', 'ETC');
CREATE TYPE notice_type_enum       AS ENUM ('SYSTEM', 'GENERAL');
CREATE TYPE login_status_enum      AS ENUM ('SUCCESS', 'FAILED');
CREATE TYPE token_status_enum      AS ENUM ('ISSUED', 'TRADING', 'SUSPENDED', 'CLOSED');
CREATE TYPE wallet_type_enum       AS ENUM ('CUSTODIAL', 'EXTERNAL');
CREATE TYPE wallet_status_enum     AS ENUM ('ACTIVE', 'SUSPENDED', 'REVOKE');

-- =====================================================
-- 1. MEMBERS
-- =====================================================
CREATE TABLE MEMBERS (
                         member_id       BIGINT       NOT NULL,
                         email           VARCHAR(255) NOT NULL,
                         member_password VARCHAR(255) NOT NULL,
                         member_name     VARCHAR(100) NOT NULL,
                         is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                         updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                         CONSTRAINT PK_MEMBERS PRIMARY KEY (member_id),
                         CONSTRAINT UQ_MEMBERS_EMAIL UNIQUE (email)
);

-- =====================================================
-- 2. ADMINS
-- =====================================================
CREATE TABLE ADMINS (
                        admin_id             BIGINT       NOT NULL,
                        admin_login_id       VARCHAR(100) NOT NULL,
                        admin_login_password VARCHAR(255) NOT NULL,
                        CONSTRAINT PK_ADMINS PRIMARY KEY (admin_id)
);

-- =====================================================
-- 3. COMMONS
-- =====================================================
CREATE TABLE COMMONS (
                         base_id           BIGINT       NOT NULL,
                         tax_rate          DECIMAL(5,2) NOT NULL DEFAULT 15.4,
                         charge_rate       DECIMAL(5,4) NOT NULL DEFAULT 0.1,
                         allocate_date     SMALLINT     NOT NULL,
                         allocate_set_date TIMESTAMP    NOT NULL,
                         CONSTRAINT PK_COMMONS PRIMARY KEY (base_id)
);

-- =====================================================
-- 4. ASSETS
-- =====================================================
CREATE TABLE ASSETS (
                        asset_id      BIGINT       NOT NULL,
                        total_value   BIGINT       NOT NULL,
                        asset_address VARCHAR(255) NOT NULL,
                        img_url       VARCHAR(255) NULL,
                        created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                        updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                        asset_name    VARCHAR(255) NOT NULL,
                        is_allocated  BOOLEAN      NOT NULL DEFAULT FALSE,
                        CONSTRAINT PK_ASSETS PRIMARY KEY (asset_id)
);

-- =====================================================
-- 5. WALLETS
-- =====================================================
CREATE TABLE WALLETS (
                         wallet_id      BIGINT             NOT NULL,
                         member_id      BIGINT             NOT NULL,
                         wallet_address VARCHAR(255)       NOT NULL,
                         wallet_type    wallet_type_enum   NOT NULL DEFAULT 'CUSTODIAL',
                         wallet_status  wallet_status_enum NOT NULL,
                         created_at     TIMESTAMP          NOT NULL DEFAULT NOW(),
                         update_at      TIMESTAMP          NOT NULL DEFAULT NOW(),
                         CONSTRAINT PK_WALLETS PRIMARY KEY (wallet_id),
                         CONSTRAINT FK_MEMBERS_TO_WALLETS
                             FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id)
);

-- =====================================================
-- 6. TOKENS
-- =====================================================
CREATE TABLE TOKENS (
                        token_id           BIGINT            NOT NULL,
                        asset_id           BIGINT            NOT NULL,
                        total_supply       BIGINT            NOT NULL,
                        circulating_supply BIGINT            NOT NULL,
                        token_name         VARCHAR(255)      NOT NULL,
                        contract_address   VARCHAR(255)      NULL,
                        token_decimals     BIGINT            NULL,
                        init_price         BIGINT            NOT NULL,
                        current_price      DECIMAL(20,4)     NOT NULL,
                        token_status       token_status_enum NOT NULL,
                        issued_at          TIMESTAMP         NOT NULL DEFAULT NOW(),
                        created_at         TIMESTAMP         NOT NULL DEFAULT NOW(),
                        updated_at         TIMESTAMP         NOT NULL DEFAULT NOW(),
                        CONSTRAINT PK_TOKENS PRIMARY KEY (token_id),
                        CONSTRAINT FK_ASSETS_TO_TOKENS
                            FOREIGN KEY (asset_id) REFERENCES ASSETS (asset_id)
);

-- =====================================================
-- 7. ACCOUNTS
-- =====================================================
CREATE TABLE ACCOUNTS (
                          account_id        BIGINT       NOT NULL,
                          member_id         BIGINT       NOT NULL,
                          account_password  VARCHAR(255) NOT NULL,
                          account_number    VARCHAR(255) NOT NULL,
                          available_balance BIGINT       NOT NULL DEFAULT 0,
                          locked_balance    BIGINT       NOT NULL DEFAULT 0,
                          created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
                          updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
                          CONSTRAINT PK_ACCOUNTS PRIMARY KEY (account_id),
                          CONSTRAINT FK_MEMBERS_TO_ACCOUNTS
                              FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id),
                          CONSTRAINT UQ_ACCOUNT_NUMBER UNIQUE (account_number)
);

-- =====================================================
-- 8. BANKINGS
-- =====================================================
CREATE TABLE BANKINGS (
                          banking_id       BIGINT              NOT NULL,
                          account_id       BIGINT              NOT NULL,
                          tx_type          banking_type_enum   NOT NULL,
                          tx_status        banking_status_enum NOT NULL,
                          banking_amount   BIGINT              NOT NULL,
                          balance_snapshot BIGINT              NOT NULL,
                          created_at       TIMESTAMP           NOT NULL DEFAULT NOW(),
                          CONSTRAINT PK_BANKINGS PRIMARY KEY (banking_id),
                          CONSTRAINT FK_ACCOUNTS_TO_BANKINGS
                              FOREIGN KEY (account_id) REFERENCES ACCOUNTS (account_id)
);

-- =====================================================
-- 9. ASSETACCOUNTS
-- =====================================================
CREATE TABLE ASSETACCOUNTS (
                               asset_account_id      BIGINT    NOT NULL,
                               asset_id              BIGINT    NOT NULL,
                               asset_account_balance BIGINT    NOT NULL DEFAULT 0,
                               created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
                               CONSTRAINT PK_ASSETACCOUNTS PRIMARY KEY (asset_account_id),
                               CONSTRAINT FK_ASSETS_TO_ASSETACCOUNTS
                                   FOREIGN KEY (asset_id) REFERENCES ASSETS (asset_id)
);

-- =====================================================
-- 10. ASSETBANKINGS
-- =====================================================
CREATE TABLE ASSETBANKINGS (
                               asset_banking_id        BIGINT           NOT NULL,
                               asset_account_id        BIGINT           NOT NULL,
                               asset_banking_amount    BIGINT           NOT NULL,
                               asset_banking_direction asset_dir_enum   NOT NULL,
                               asset_banking_type      asset_usage_enum NOT NULL,
                               created_at              TIMESTAMP        NOT NULL DEFAULT NOW(),
                               CONSTRAINT PK_ASSETBANKINGS PRIMARY KEY (asset_banking_id),
                               CONSTRAINT FK_ASSETACCOUNTS_TO_ASSETBANKINGS
                                   FOREIGN KEY (asset_account_id) REFERENCES ASSETACCOUNTS (asset_account_id)
);

-- =====================================================
-- 11. PLATFORMACCOUNTS
-- =====================================================
CREATE TABLE PLATFORMACCOUNTS (
                                  platform_account_id      BIGINT    NOT NULL,
                                  platform_account_balance BIGINT    NOT NULL DEFAULT 0,
                                  total_earned             BIGINT    NOT NULL DEFAULT 0,
                                  total_withdrawn          BIGINT    NOT NULL DEFAULT 0,
                                  updated_at               TIMESTAMP NOT NULL DEFAULT NOW(),
                                  CONSTRAINT PK_PLATFORMACCOUNTS PRIMARY KEY (platform_account_id)
);

-- =====================================================
-- 12. PLATFORMTOKENHOLDINGS
-- =====================================================
CREATE TABLE PLATFORMTOKENHOLDINGS (
                                       platform_token_holding_id BIGINT    NOT NULL,
                                       admin_id                  BIGINT    NOT NULL,
                                       token_id                  BIGINT    NOT NULL,
                                       holding_supply            BIGINT    NOT NULL DEFAULT 0,
                                       init_price                BIGINT    NOT NULL DEFAULT 0,
                                       created_at                TIMESTAMP NOT NULL DEFAULT NOW(),
                                       CONSTRAINT PK_PLATFORMTOKENHOLDINGS PRIMARY KEY (platform_token_holding_id),
                                       CONSTRAINT FK_ADMINS_TO_PLATFORMTOKENHOLDINGS
                                           FOREIGN KEY (admin_id) REFERENCES ADMINS (admin_id),
                                       CONSTRAINT FK_TOKENS_TO_PLATFORMTOKENHOLDINGS
                                           FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                                       CONSTRAINT UQ_PLATFORMTOKENHOLDINGS_TOKEN UNIQUE (token_id)
);

-- =====================================================
-- 13. ORDERS
-- =====================================================
CREATE TABLE ORDERS (
                        order_id           BIGINT            NOT NULL,
                        member_id          BIGINT            NOT NULL,
                        token_id           BIGINT            NOT NULL,
                        order_price        BIGINT            NOT NULL,
                        order_quantity     BIGINT            NOT NULL,
                        filled_quantity    BIGINT            NOT NULL DEFAULT 0,
                        remaining_quantity BIGINT            NOT NULL DEFAULT 0,
                        created_at         TIMESTAMP         NOT NULL DEFAULT NOW(),
                        updated_at         TIMESTAMP         NOT NULL DEFAULT NOW(),
                        order_type         order_type_enum   NOT NULL,
                        order_status       order_status_enum NOT NULL,
                        order_sequence     BIGINT            NOT NULL,
                        CONSTRAINT PK_ORDERS PRIMARY KEY (order_id),
                        CONSTRAINT FK_MEMBERS_TO_ORDERS
                            FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id),
                        CONSTRAINT FK_TOKENS_TO_ORDERS
                            FOREIGN KEY (token_id) REFERENCES TOKENS (token_id)
);

-- =====================================================
-- 14. TRADES
-- =====================================================
CREATE TABLE TRADES (
                        trade_id          BIGINT                 NOT NULL,
                        seller_id         BIGINT                 NOT NULL,
                        buyer_id          BIGINT                 NOT NULL,
                        sell_order_id     BIGINT                 NOT NULL,
                        buy_order_id      BIGINT                 NOT NULL,
                        token_id          BIGINT                 NOT NULL,
                        trade_price       BIGINT                 NOT NULL,
                        trade_quantity    BIGINT                 NOT NULL,
                        settlement_status settlement_status_enum NOT NULL,
                        executed_at       TIMESTAMP              NOT NULL,
                        fee_amount        BIGINT                 NOT NULL DEFAULT 0,
                        created_at        TIMESTAMP              NOT NULL DEFAULT NOW(),
                        CONSTRAINT PK_TRADES PRIMARY KEY (trade_id),
                        CONSTRAINT FK_MEMBERS_TO_TRADES_SELLER
                            FOREIGN KEY (seller_id) REFERENCES MEMBERS (member_id),
                        CONSTRAINT FK_MEMBERS_TO_TRADES_BUYER
                            FOREIGN KEY (buyer_id) REFERENCES MEMBERS (member_id),
                        CONSTRAINT FK_ORDERS_TO_TRADES_SELL
                            FOREIGN KEY (sell_order_id) REFERENCES ORDERS (order_id),
                        CONSTRAINT FK_ORDERS_TO_TRADES_BUY
                            FOREIGN KEY (buy_order_id) REFERENCES ORDERS (order_id),
                        CONSTRAINT FK_TOKENS_TO_TRADES
                            FOREIGN KEY (token_id) REFERENCES TOKENS (token_id)
);

-- =====================================================
-- 15. BLOCKCHAINOUTBOXQ
-- =====================================================
CREATE TABLE BLOCKCHAINOUTBOXQ (
                                   queue_id                  BIGINT            NOT NULL,
                                   trade_id                  BIGINT            NOT NULL,
                                   platform_token_holding_id BIGINT            NOT NULL,
                                   payload_json              JSON              NOT NULL,
                                   status                    queue_status_enum NOT NULL,
                                   retry_count               INT               NOT NULL DEFAULT 3,
                                   last_error_message        TEXT              NULL,
                                   created_at                TIMESTAMP         NOT NULL DEFAULT NOW(),
                                   updated_at                TIMESTAMP         NOT NULL DEFAULT NOW(),
                                   idempotency_key           VARCHAR(100)      NOT NULL,
                                   max_retry                 INT               NOT NULL DEFAULT 0,
                                   CONSTRAINT PK_BLOCKCHAINOUTBOXQ PRIMARY KEY (queue_id),
                                   CONSTRAINT FK_TRADES_TO_BLOCKCHAINOUTBOXQ
                                       FOREIGN KEY (trade_id) REFERENCES TRADES (trade_id),
                                   CONSTRAINT FK_PLATFORMTOKENHOLDINGS_TO_BLOCKCHAINOUTBOXQ
                                       FOREIGN KEY (platform_token_holding_id)
                                           REFERENCES PLATFORMTOKENHOLDINGS (platform_token_holding_id),
                                   CONSTRAINT UQ_IDEMPOTENCY_KEY UNIQUE (idempotency_key)
);

-- =====================================================
-- 17. BLOCKCHAINTX
-- =====================================================
CREATE TABLE BLOCKCHAINTX (
                              tx_id                     BIGINT         NOT NULL,
                              queue_id                  BIGINT         NULL,
                              trade_id                  BIGINT         NOT NULL,
                              platform_token_holding_id BIGINT         NOT NULL,
                              tx_hash                   VARCHAR(255)   NULL,
                              from_address              VARCHAR(255)   NULL,
                              to_address                VARCHAR(255)   NULL,
                              contract_address          VARCHAR(255)   NOT NULL,
                              gas_used                  BIGINT         NULL,
                              block_number              BIGINT         NULL,
                              tx_status                 tx_status_enum NOT NULL,
                              submitted_at              TIMESTAMP      NOT NULL DEFAULT NOW(),
                              confirmed_at              TIMESTAMP      NULL,
                              tx_type                   tx_type_enum   NOT NULL,
                              CONSTRAINT PK_BLOCKCHAINTX PRIMARY KEY (tx_id),
                              CONSTRAINT FK_BLOCKCHAINOUTBOXQ_TO_BLOCKCHAINTX
                                  FOREIGN KEY (queue_id) REFERENCES BLOCKCHAINOUTBOXQ (queue_id),
                              CONSTRAINT FK_TRADES_TO_BLOCKCHAINTX
                                  FOREIGN KEY (trade_id) REFERENCES TRADES (trade_id),
                              CONSTRAINT FK_PLATFORMTOKENHOLDINGS_TO_BLOCKCHAINTX
                                  FOREIGN KEY (platform_token_holding_id)
                                      REFERENCES PLATFORMTOKENHOLDINGS (platform_token_holding_id),
                              CONSTRAINT UQ_TX_HASH UNIQUE (tx_hash)
);

-- =====================================================
-- 18. TOKENHOLDINGS
-- =====================================================
CREATE TABLE TOKENHOLDINGS (
                               token_holding_id BIGINT        NOT NULL,
                               member_id        BIGINT        NOT NULL,
                               token_id         BIGINT        NOT NULL,
                               updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
                               current_quantity BIGINT        NOT NULL DEFAULT 0,
                               locked_quantity  BIGINT        NOT NULL DEFAULT 0,
                               avg_buy_price    DECIMAL(20,4) NOT NULL DEFAULT 0,
                               CONSTRAINT PK_TOKENHOLDINGS PRIMARY KEY (token_holding_id),
                               CONSTRAINT FK_MEMBERS_TO_TOKENHOLDINGS
                                   FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id),
                               CONSTRAINT FK_TOKENS_TO_TOKENHOLDINGS
                                   FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                               CONSTRAINT UQ_TOKENHOLDINGS UNIQUE (member_id, token_id)
);

-- =====================================================
-- 19. ALLOCATIONEVENTS
-- =====================================================
CREATE TABLE ALLOCATIONEVENTS (
                                  allocation_event_id     BIGINT    NOT NULL,
                                  asset_id                BIGINT    NOT NULL,
                                  created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at              TIMESTAMP NULL,
                                  allocation_batch_status BOOLEAN   NOT NULL DEFAULT FALSE,
                                  CONSTRAINT PK_ALLOCATIONEVENTS PRIMARY KEY (allocation_event_id),
                                  CONSTRAINT FK_ASSETS_TO_ALLOCATIONEVENTS
                                      FOREIGN KEY (asset_id) REFERENCES ASSETS (asset_id)
);

-- =====================================================
-- 20. ALLOCATIONPAYOUTS
-- =====================================================
CREATE TABLE ALLOCATIONPAYOUTS (
                                   allocation_payout_id BIGINT             NOT NULL,
                                   member_id            BIGINT             NOT NULL,
                                   allocation_event_id  BIGINT             NOT NULL,
                                   token_id             BIGINT             NOT NULL,
                                   created_at           TIMESTAMP          NOT NULL DEFAULT NOW(),
                                   member_income        BIGINT             NOT NULL DEFAULT 0,
                                   holding_quantity     BIGINT             NOT NULL,
                                   status               payout_status_enum NOT NULL,
                                   CONSTRAINT PK_ALLOCATIONPAYOUTS PRIMARY KEY (allocation_payout_id),
                                   CONSTRAINT FK_MEMBERS_TO_ALLOCATIONPAYOUTS
                                       FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id),
                                   CONSTRAINT FK_ALLOCATIONEVENTS_TO_ALLOCATIONPAYOUTS
                                       FOREIGN KEY (allocation_event_id) REFERENCES ALLOCATIONEVENTS (allocation_event_id),
                                   CONSTRAINT FK_TOKENS_TO_ALLOCATIONPAYOUTS
                                       FOREIGN KEY (token_id) REFERENCES TOKENS (token_id)
);

-- =====================================================
-- 21. PLATFORMBANKING
-- =====================================================
CREATE TABLE PLATFORMBANKING (
                                 platform_banking_id        BIGINT            NOT NULL,
                                 token_id                   BIGINT            NULL,
                                 trade_id                   BIGINT            NULL,
                                 allocation_payout_id       BIGINT            NULL,
                                 account_type               income_type_enum  NOT NULL,
                                 platform_banking_amount    BIGINT            NOT NULL,
                                 platform_banking_direction platform_dir_enum NOT NULL,
                                 created_at                 TIMESTAMP         NOT NULL DEFAULT NOW(),
                                 CONSTRAINT PK_PLATFORMBANKING PRIMARY KEY (platform_banking_id),
                                 CONSTRAINT FK_TOKENS_TO_PLATFORMBANKING
                                     FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                                 CONSTRAINT FK_TRADES_TO_PLATFORMBANKING
                                     FOREIGN KEY (trade_id) REFERENCES TRADES (trade_id),
                                 CONSTRAINT FK_ALLOCATIONPAYOUTS_TO_PLATFORMBANKING
                                     FOREIGN KEY (allocation_payout_id) REFERENCES ALLOCATIONPAYOUTS (allocation_payout_id),
                                 CONSTRAINT CHK_PLATFORMBANKING_SINGLE_REF CHECK (
                                     (CASE WHEN token_id             IS NOT NULL THEN 1 ELSE 0 END +
                                      CASE WHEN trade_id             IS NOT NULL THEN 1 ELSE 0 END +
                                      CASE WHEN allocation_payout_id IS NOT NULL THEN 1 ELSE 0 END) = 1
                                     )
);

-- =====================================================
-- 22. DISCLOSURE
-- =====================================================
CREATE TABLE DISCLOSURE (
                            disclosure_id      BIGINT              NOT NULL,
                            asset_id            BIGINT              NOT NULL,
                            created_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
                            updated_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
                            disclosure_title    VARCHAR(255)        NOT NULL,
                            disclosure_content  VARCHAR(255)        NOT NULL,
                            disclosure_category disclosure_cat_enum NOT NULL,
                            CONSTRAINT PK_DISCLOSURE PRIMARY KEY (disclosure_id),
                            CONSTRAINT FK_ASSETS_TO_DISCLOSURE
                                FOREIGN KEY (asset_id) REFERENCES ASSETS (asset_id)
);

-- =====================================================
-- 23. FILES
-- =====================================================
CREATE TABLE FILES (
                       file_id        BIGINT       NOT NULL,
                       disclosure_id BIGINT       NOT NULL,
                       created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                       origin_name    VARCHAR(255) NOT NULL,
                       stored_name    VARCHAR(255) NOT NULL,
                       size           BIGINT       NOT NULL,
                       path           VARCHAR(500) NOT NULL,
                       CONSTRAINT PK_FILES PRIMARY KEY (file_id),
                       CONSTRAINT FK_DISCLOSURE_TO_FILES
                           FOREIGN KEY (disclosure_id) REFERENCES DISCLOSURE (disclosure_id)
);

-- =====================================================
-- 24. LIKES
-- =====================================================
CREATE TABLE LIKES (
                       like_id   BIGINT NOT NULL,
                       member_id BIGINT NOT NULL,
                       asset_id  BIGINT NOT NULL,
                       CONSTRAINT PK_LIKES PRIMARY KEY (like_id),
                       CONSTRAINT FK_MEMBERS_TO_LIKES
                           FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id),
                       CONSTRAINT FK_ASSETS_TO_LIKES
                           FOREIGN KEY (asset_id) REFERENCES ASSETS (asset_id),
                       CONSTRAINT UQ_LIKES UNIQUE (member_id, asset_id)
);

-- =====================================================
-- 25. NOTICES
-- =====================================================
CREATE TABLE NOTICES (
                         notice_id      BIGINT           NOT NULL,
                         admin_id       BIGINT           NOT NULL,
                         notice_type    notice_type_enum NOT NULL,
                         notice_title   VARCHAR(255)     NOT NULL,
                         notice_content TEXT             NOT NULL,
                         created_at     TIMESTAMP        NOT NULL DEFAULT NOW(),
                         updated_at     TIMESTAMP        NOT NULL DEFAULT NOW(),
                         CONSTRAINT PK_NOTICES PRIMARY KEY (notice_id),
                         CONSTRAINT FK_ADMINS_TO_NOTICES
                             FOREIGN KEY (admin_id) REFERENCES ADMINS (admin_id)
);

-- =====================================================
-- 26. ALARMS
-- =====================================================
CREATE TABLE ALARMS (
                        alarm_id      BIGINT       NOT NULL,
                        member_id     BIGINT       NOT NULL,
                        alarm_title   VARCHAR(255) NOT NULL,
                        alarm_content VARCHAR(500) NOT NULL,
                        created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                        is_read       BOOLEAN      NOT NULL DEFAULT FALSE,
                        CONSTRAINT PK_ALARMS PRIMARY KEY (alarm_id),
                        CONSTRAINT FK_MEMBERS_TO_ALARMS
                            FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id)
);

-- =====================================================
-- 27. LOGINLOG
-- =====================================================
CREATE TABLE LOGINLOG (
                          login_log_id BIGINT            NOT NULL,
                          member_id    BIGINT            NULL,
                          ip_address   VARCHAR(45)       NOT NULL,
                          login_status login_status_enum NOT NULL,
                          created_at   TIMESTAMP         NOT NULL DEFAULT NOW(),
                          CONSTRAINT PK_LOGINLOG PRIMARY KEY (login_log_id),
                          CONSTRAINT FK_MEMBERS_TO_LOGINLOG
                              FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id)
);

-- =====================================================
-- 28. APILOG
-- =====================================================
CREATE TABLE APILOG (
                        api_log_id       BIGINT       NOT NULL,
                        member_id        BIGINT       NULL,
                        request_id       VARCHAR(100) NOT NULL,
                        endpoint         VARCHAR(255) NOT NULL,
                        method           VARCHAR(10)  NOT NULL,
                        status_code      INT          NOT NULL,
                        response_time_ms INT          NOT NULL,
                        created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
                        CONSTRAINT PK_APILOG PRIMARY KEY (api_log_id),
                        CONSTRAINT FK_MEMBERS_TO_APILOG
                            FOREIGN KEY (member_id) REFERENCES MEMBERS (member_id)
);

-- =====================================================
-- 29. CANDLEMINUTES
-- =====================================================
CREATE TABLE CANDLEMINUTES (
                               candle_id   BIGINT        NOT NULL,
                               token_id    BIGINT        NOT NULL,
                               open_price  DECIMAL(20,4) NULL,
                               high_price  DECIMAL(20,4) NULL,
                               low_price   DECIMAL(20,4) NULL,
                               close_price DECIMAL(20,4) NULL,
                               volume      DECIMAL(20,4) NULL,
                               candle_time TIMESTAMP     NOT NULL,
                               trade_count INT           NOT NULL DEFAULT 0,
                               CONSTRAINT PK_CANDLEMINUTES PRIMARY KEY (candle_id),
                               CONSTRAINT FK_TOKENS_TO_CANDLEMINUTES
                                   FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                               CONSTRAINT UQ_CANDLEMINUTES_TOKEN_TIME UNIQUE (token_id, candle_time)
);

-- =====================================================
-- 30. CANDLEHOURS
-- =====================================================
CREATE TABLE CANDLEHOURS (
                             candle_id   BIGINT        NOT NULL,
                             token_id    BIGINT        NOT NULL,
                             open_price  DECIMAL(20,4) NULL,
                             high_price  DECIMAL(20,4) NULL,
                             low_price   DECIMAL(20,4) NULL,
                             close_price DECIMAL(20,4) NULL,
                             volume      DECIMAL(20,4) NULL,
                             candle_time TIMESTAMP     NOT NULL,
                             trade_count INT           NOT NULL DEFAULT 0,
                             CONSTRAINT PK_CANDLEHOURS PRIMARY KEY (candle_id),
                             CONSTRAINT FK_TOKENS_TO_CANDLEHOURS
                                 FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                             CONSTRAINT UQ_CANDLEHOURS_TOKEN_TIME UNIQUE (token_id, candle_time)
);

-- =====================================================
-- 31. CANDLEDAYS
-- =====================================================
CREATE TABLE CANDLEDAYS (
                            candle_id   BIGINT        NOT NULL,
                            token_id    BIGINT        NOT NULL,
                            open_price  DECIMAL(20,4) NULL,
                            high_price  DECIMAL(20,4) NULL,
                            low_price   DECIMAL(20,4) NULL,
                            close_price DECIMAL(20,4) NULL,
                            volume      DECIMAL(20,4) NULL,
                            candle_time TIMESTAMP     NOT NULL,
                            trade_count INT           NOT NULL DEFAULT 0,
                            CONSTRAINT PK_CANDLEDAYS PRIMARY KEY (candle_id),
                            CONSTRAINT FK_TOKENS_TO_CANDLEDAYS
                                FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                            CONSTRAINT UQ_CANDLEDAYS_TOKEN_TIME UNIQUE (token_id, candle_time)
);

-- =====================================================
-- 32. CANDLEMONTHS
-- =====================================================
CREATE TABLE CANDLEMONTHS (
                              candle_id   BIGINT        NOT NULL,
                              token_id    BIGINT        NOT NULL,
                              open_price  DECIMAL(20,4) NULL,
                              high_price  DECIMAL(20,4) NULL,
                              low_price   DECIMAL(20,4) NULL,
                              close_price DECIMAL(20,4) NULL,
                              volume      DECIMAL(20,4) NULL,
                              candle_time TIMESTAMP     NOT NULL,
                              trade_count INT           NOT NULL DEFAULT 0,
                              CONSTRAINT PK_CANDLEMONTHS PRIMARY KEY (candle_id),
                              CONSTRAINT FK_TOKENS_TO_CANDLEMONTHS
                                  FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                              CONSTRAINT UQ_CANDLEMONTHS_TOKEN_TIME UNIQUE (token_id, candle_time)
);

-- =====================================================
-- 33. CANDLEYEARS
-- =====================================================
CREATE TABLE CANDLEYEARS (
                             candle_id   BIGINT        NOT NULL,
                             token_id    BIGINT        NOT NULL,
                             open_price  DECIMAL(20,4) NULL,
                             high_price  DECIMAL(20,4) NULL,
                             low_price   DECIMAL(20,4) NULL,
                             close_price DECIMAL(20,4) NULL,
                             volume      DECIMAL(20,4) NULL,
                             candle_time TIMESTAMP     NOT NULL,
                             trade_count INT           NOT NULL DEFAULT 0,
                             CONSTRAINT PK_CANDLEYEARS PRIMARY KEY (candle_id),
                             CONSTRAINT FK_TOKENS_TO_CANDLEYEARS
                                 FOREIGN KEY (token_id) REFERENCES TOKENS (token_id),
                             CONSTRAINT UQ_CANDLEYEARS_TOKEN_TIME UNIQUE (token_id, candle_time)
);


select * from assets a ;