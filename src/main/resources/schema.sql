-- ============================================================
-- OTP Authentication System — MySQL Schema
-- Version 1.0 | 2025
-- ============================================================

CREATE DATABASE IF NOT EXISTS otp_auth_db;
USE otp_auth_db;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    email          VARCHAR(255) UNIQUE NULL,
    phone_number   VARCHAR(15) UNIQUE NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_identifier CHECK (email IS NOT NULL OR phone_number IS NOT NULL)
);

-- ============================================================
-- TABLE: admin_users
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_users (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('SUPER_ADMIN', 'ADMIN') NOT NULL DEFAULT 'ADMIN',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: blocked_users
-- ============================================================
CREATE TABLE IF NOT EXISTS blocked_users (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    reason        TEXT NOT NULL,
    blocked_by    BIGINT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    blocked_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unblocked_at  TIMESTAMP NULL,
    CONSTRAINT fk_blocked_users_user_id  FOREIGN KEY (user_id)    REFERENCES users(id)       ON DELETE CASCADE,
    CONSTRAINT fk_blocked_users_admin_id FOREIGN KEY (blocked_by) REFERENCES admin_users(id) ON DELETE SET NULL
);

-- ============================================================
-- TABLE: otp_sessions
-- ============================================================
CREATE TABLE IF NOT EXISTS otp_sessions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    channel     ENUM('EMAIL', 'SMS', 'WHATSAPP') NOT NULL,
    status      ENUM('PENDING', 'SENT', 'VERIFIED', 'EXPIRED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    otp_hash    VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    is_used     BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address  VARCHAR(45) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: otp_delivery_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS otp_delivery_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    otp_session_id  BIGINT NOT NULL,
    channel         ENUM('EMAIL', 'SMS', 'WHATSAPP') NOT NULL,
    status          ENUM('SENT', 'FAILED', 'RETRIED', 'DELIVERED') NOT NULL,
    attempt_number  TINYINT NOT NULL DEFAULT 1,
    error_message   TEXT NULL,
    delivered_at    TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_logs_session FOREIGN KEY (otp_session_id) REFERENCES otp_sessions(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: refresh_token_blacklist
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_token_blacklist (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT NOT NULL,
    token_hash       VARCHAR(255) NOT NULL UNIQUE,
    reason           ENUM('LOGOUT', 'SUSPICIOUS_ACTIVITY', 'ADMIN_REVOKED') NOT NULL,
    revoked_by_admin BIGINT NULL,
    expires_at       TIMESTAMP NOT NULL,
    revoked_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_blacklist_user  FOREIGN KEY (user_id)          REFERENCES users(id)       ON DELETE CASCADE,
    CONSTRAINT fk_token_blacklist_admin FOREIGN KEY (revoked_by_admin) REFERENCES admin_users(id) ON DELETE SET NULL
);

-- ============================================================
-- TABLE: audit_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT NULL,
    admin_id      BIGINT NULL,
    event_type    ENUM(
                    'OTP_REQUESTED', 'OTP_SENT', 'OTP_DELIVERY_FAILED',
                    'OTP_VERIFIED', 'OTP_INVALID', 'OTP_EXPIRED',
                    'OTP_RESEND_REQUESTED', 'RATE_LIMIT_EXCEEDED',
                    'TOKEN_ISSUED', 'TOKEN_REVOKED',
                    'USER_BLOCKED', 'USER_UNBLOCKED', 'ADMIN_LOGIN'
                  ) NOT NULL,
    channel       ENUM('EMAIL', 'SMS', 'WHATSAPP') NULL,
    status        ENUM('SUCCESS', 'FAILED', 'EXPIRED', 'BLOCKED') NOT NULL,
    ip_address    VARCHAR(45) NULL,
    identifier    VARCHAR(255) NULL,
    error_message TEXT NULL,
    metadata      JSON NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user  FOREIGN KEY (user_id)  REFERENCES users(id)       ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES admin_users(id) ON DELETE SET NULL
);

-- ============================================================
-- INDEXES — for faster query performance
-- ============================================================
CREATE INDEX idx_otp_sessions_user_channel   ON otp_sessions(user_id, channel);
CREATE INDEX idx_otp_sessions_status         ON otp_sessions(status);
CREATE INDEX idx_audit_logs_user_id          ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_event_type       ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_created_at       ON audit_logs(created_at);
CREATE INDEX idx_blocked_users_user_active   ON blocked_users(user_id, is_active);
CREATE INDEX idx_token_blacklist_token_hash  ON refresh_token_blacklist(token_hash);

-- ============================================================
-- DEFAULT SUPER ADMIN — change password after first login!
-- Password: Admin@123 (BCrypt hashed)
-- ============================================================
INSERT INTO admin_users (name, email, password_hash, role)
VALUES (
    'Super Admin',
    'admin@otpauth.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'SUPER_ADMIN'
);