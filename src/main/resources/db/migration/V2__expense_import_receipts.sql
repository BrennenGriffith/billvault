-- Durable retry receipts; no source statements or bank credentials are retained here.
CREATE TABLE expense_import_receipts (
 preview_id VARCHAR(36) PRIMARY KEY,
 result_json TEXT NOT NULL
);
