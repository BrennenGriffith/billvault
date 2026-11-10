CREATE TABLE mobile_command_receipts (
 command_id VARCHAR(36) PRIMARY KEY,
 request_hash VARCHAR(64) NOT NULL,
 result_json TEXT NOT NULL
);
CREATE TABLE mobile_devices (
 id VARCHAR(36) PRIMARY KEY,
 name VARCHAR(80) NOT NULL,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 paired_at TIMESTAMP WITH TIME ZONE NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE
);
