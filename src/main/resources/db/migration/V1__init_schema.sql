-- ============================================================
-- Fiscal Reporting Platform — Core Database Schema
-- PostgreSQL 15+
-- Companion service to SecureShield API (auth/identity delegated
-- via RS256 JWT — no local user table; org_id/sub are trusted
-- claims from the SecureShield-issued token)
-- ============================================================

-- ------------------------------------------------------------
-- 1. ORGANIZATIONS (tenants / taxpayers)
-- ------------------------------------------------------------
CREATE TABLE organizations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name          VARCHAR(255) NOT NULL,
    trading_name        VARCHAR(255),
    tin                 VARCHAR(20)  NOT NULL UNIQUE,   -- ZIMRA Taxpayer ID Number
    vat_number          VARCHAR(20)  UNIQUE,            -- null if not VAT-registered
    is_vat_registered   BOOLEAN NOT NULL DEFAULT FALSE,
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    country             VARCHAR(100) NOT NULL DEFAULT 'Zimbabwe',
    base_currency        VARCHAR(3)  NOT NULL DEFAULT 'USD',
    fiscal_device_serial VARCHAR(50),                   -- FDMS-registered device id
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- 2. CUSTOMERS (invoice recipients — per tenant)
-- ------------------------------------------------------------
CREATE TABLE customers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    tin                 VARCHAR(20),
    vat_number          VARCHAR(20),
    is_vat_registered   BOOLEAN NOT NULL DEFAULT FALSE,
    email               VARCHAR(255),
    phone               VARCHAR(30),
    address_line1       VARCHAR(255),
    city                VARCHAR(100),
    country             VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, name)
);
CREATE INDEX idx_customers_org ON customers(org_id);

-- ------------------------------------------------------------
-- 3. TAX CODES (VAT rate lookup — ZIMRA-style)
-- ------------------------------------------------------------
CREATE TABLE tax_codes (
    id                  SMALLSERIAL PRIMARY KEY,
    code                VARCHAR(10) NOT NULL UNIQUE,     -- 'STD', 'ZERO', 'EXEMPT'
    description         VARCHAR(100) NOT NULL,
    rate_percent        NUMERIC(5,2) NOT NULL,           -- e.g. 15.00
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from      DATE NOT NULL,
    effective_to        DATE
);

INSERT INTO tax_codes (code, description, rate_percent, effective_from) VALUES
    ('STD',    'Standard rated VAT', 15.00, '2024-01-01'),
    ('ZERO',   'Zero rated',          0.00, '2024-01-01'),
    ('EXEMPT', 'VAT exempt',          0.00, '2024-01-01');

-- ------------------------------------------------------------
-- 4. FISCAL DAYS (FDMS fiscal-day lifecycle per org)
--    Every invoice must fall inside an open fiscal day; the
--    receipt counter chain resets/continues per this record.
-- ------------------------------------------------------------
CREATE TABLE fiscal_days (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    fiscal_day_no       INTEGER NOT NULL,                -- sequential per org
    business_date       DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, CLOSED
    opening_receipt_ctr INTEGER NOT NULL DEFAULT 0,
    closing_receipt_ctr INTEGER,
    opened_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ,
    UNIQUE (org_id, fiscal_day_no)
);
CREATE INDEX idx_fiscal_days_org_status ON fiscal_days(org_id, status);

-- ------------------------------------------------------------
-- 5. INVOICES (hash-chained, fiscalized)
-- ------------------------------------------------------------
CREATE TABLE invoices (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id                UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    customer_id           UUID NOT NULL REFERENCES customers(id),
    fiscal_day_id         UUID NOT NULL REFERENCES fiscal_days(id),
    invoice_number        VARCHAR(50) NOT NULL,           -- human-facing, per org sequence
    global_receipt_no     BIGINT NOT NULL,                -- FDMS global counter, per org, never resets
    receipt_counter       INTEGER NOT NULL,               -- position within current fiscal day
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ISSUED, PAID, CANCELLED, CREDITED
    currency              VARCHAR(3) NOT NULL DEFAULT 'USD',
    issue_date            DATE NOT NULL,
    due_date              DATE,
    subtotal              NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_total             NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                 NUMERIC(14,2) NOT NULL DEFAULT 0,

    -- hash-chain / fiscalization fields (mirrors SecureShield audit pattern)
    previous_receipt_hash VARCHAR(64),                       -- SHA-256 hex of prior receipt in this fiscal day
    receipt_hash          VARCHAR(64) NOT NULL,               -- HMAC-SHA256 over canonical invoice payload
    verification_code     VARCHAR(32),                     -- short device-signed code printed on receipt
    qr_payload            TEXT,                            -- data encoded in the receipt QR code

    created_by            VARCHAR(100) NOT NULL,           -- JWT 'sub' claim, not a local FK
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (org_id, invoice_number),
    UNIQUE (org_id, global_receipt_no)
);
CREATE INDEX idx_invoices_org_date ON invoices(org_id, issue_date);
CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_fiscal_day ON invoices(fiscal_day_id);

-- ------------------------------------------------------------
-- 6. INVOICE LINE ITEMS
-- ------------------------------------------------------------
CREATE TABLE invoice_line_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_no         SMALLINT NOT NULL,
    description     VARCHAR(255) NOT NULL,
    quantity        NUMERIC(12,3) NOT NULL DEFAULT 1,
    unit_price      NUMERIC(14,2) NOT NULL,
    tax_code_id     SMALLINT NOT NULL REFERENCES tax_codes(id),
    line_subtotal   NUMERIC(14,2) NOT NULL,
    line_tax        NUMERIC(14,2) NOT NULL,
    line_total      NUMERIC(14,2) NOT NULL,
    UNIQUE (invoice_id, line_no)
);
CREATE INDEX idx_line_items_invoice ON invoice_line_items(invoice_id);

-- ------------------------------------------------------------
-- 7. PAYMENTS (optional but useful for AR tracking)
-- ------------------------------------------------------------
CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount          NUMERIC(14,2) NOT NULL,
    method          VARCHAR(30) NOT NULL,       -- CASH, BANK_TRANSFER, MOBILE_MONEY, CARD
    reference       VARCHAR(100),
    paid_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    recorded_by     VARCHAR(100) NOT NULL       -- JWT sub
);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);

-- ------------------------------------------------------------
-- 8. VAT RETURNS (periodic filings, ZIMRA VAT7-style)
-- ------------------------------------------------------------
CREATE TABLE vat_returns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    period_start        DATE NOT NULL,
    period_end          DATE NOT NULL,
    return_type         VARCHAR(20) NOT NULL DEFAULT 'VAT7',
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, ACCEPTED, REJECTED
    output_tax_total    NUMERIC(14,2) NOT NULL DEFAULT 0,  -- VAT charged on sales
    input_tax_total     NUMERIC(14,2) NOT NULL DEFAULT 0,  -- VAT paid on purchases (phase 2)
    net_vat_payable     NUMERIC(14,2) NOT NULL DEFAULT 0,  -- output - input
    submission_reference VARCHAR(100),
    submitted_by        VARCHAR(100),                      -- JWT sub
    submitted_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, period_start, period_end, return_type)
);
CREATE INDEX idx_vat_returns_org_period ON vat_returns(org_id, period_start);

-- ------------------------------------------------------------
-- 9. VAT RETURN LINE ITEMS (breakdown by tax code)
-- ------------------------------------------------------------
CREATE TABLE vat_return_line_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vat_return_id   UUID NOT NULL REFERENCES vat_returns(id) ON DELETE CASCADE,
    tax_code_id     SMALLINT NOT NULL REFERENCES tax_codes(id),
    sales_total     NUMERIC(14,2) NOT NULL DEFAULT 0,
    output_tax      NUMERIC(14,2) NOT NULL DEFAULT 0,
    purchases_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    input_tax       NUMERIC(14,2) NOT NULL DEFAULT 0,
    UNIQUE (vat_return_id, tax_code_id)
);

-- ------------------------------------------------------------
-- 10. LOCAL AUDIT LOG (mirrors SecureShield's HMAC hash-chain
--     pattern for fiscal-specific events; SecureShield remains
--     system-of-record for auth events, this covers fiscal
--     domain events like invoice issuance, VAT submission)
-- ------------------------------------------------------------
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    org_id          UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    actor_sub       VARCHAR(100) NOT NULL,       -- JWT sub of actor
    action          VARCHAR(50) NOT NULL,        -- INVOICE_ISSUED, VAT_RETURN_SUBMITTED, etc.
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    payload_json    JSONB NOT NULL,
    previous_hash   VARCHAR(64),
    entry_hash      VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_org ON audit_log(org_id, created_at);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);

-- ------------------------------------------------------------
-- Notes on JWT integration with SecureShield
-- ------------------------------------------------------------
-- Expected claims on incoming RS256 tokens (verified against
-- SecureShield's published JWKS):
--   sub        -> stored as created_by / actor_sub (string, no local FK)
--   org_id     -> must match organizations.id for row-level scoping
--   roles      -> enforced via @PreAuthorize in the fiscal service,
--                 not stored here
--
-- Every write endpoint should verify org_id in the token matches
-- the org_id path/body parameter (BOLA defense — same pattern as
-- SecureShield's @PreAuthorize SpEL checks).
