-- Password reset tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          TEXT PRIMARY KEY,
    user_id     TEXT NOT NULL,
    token       TEXT NOT NULL UNIQUE,
    expires_at  DATETIME NOT NULL,
    used        INTEGER NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_prt_token   ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_prt_user_id ON password_reset_tokens(user_id);
