# 🔐 OTP Authentication System

> Secure, multi-channel OTP authentication with JWT — built with Spring Boot + MySQL + Redis + Streamlit

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=flat&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?style=flat&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=flat&logo=jsonwebtokens&logoColor=white)
![Twilio](https://img.shields.io/badge/Twilio-SMS%2FWhatsApp-F22F46?style=flat&logo=twilio&logoColor=white)
![Streamlit](https://img.shields.io/badge/Streamlit-Frontend-FF4B4B?style=flat&logo=streamlit&logoColor=white)

---

## 📖 About

A production-grade OTP Authentication System that eliminates password-based login vulnerabilities. Users authenticate via time-limited, single-use OTPs delivered through **Email**, **SMS**, or **WhatsApp**.

- OTPs stored in **Redis** with automatic TTL expiry
- OTPs verified against **BCrypt hashes** — never stored plain text
- On success, **JWT access + refresh token pair** issued for stateless session management
- Full **admin dashboard** for real-time monitoring, audit trails, and user management

---

## ⚙️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2 |
| Language | Java 17 |
| Database | MySQL 8 |
| Cache / OTP Store | Redis |
| Authentication | JWT (jjwt 0.11.5) |
| SMS / WhatsApp | Twilio SDK |
| Email | JavaMailSender (SMTP) |
| Frontend | Streamlit (Python) |
| Security | Spring Security |
| ORM | Spring Data JPA (Hibernate) |
| Password Hashing | BCrypt |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Streamlit Frontend                  │
│   Register │ Login │ Verify OTP │ Admin Dashboard    │
└─────────────────────┬───────────────────────────────┘
                      │ REST API (HTTP/JSON)
┌─────────────────────▼───────────────────────────────┐
│              Spring Boot Backend                     │
│                                                      │
│  OtpController  AuthController  AdminController      │
│       │               │               │              │
│  OtpService     AuthService     AdminService         │
│       │               │               │              │
│  DeliveryService  JwtUtil      AuditService          │
└──────────┬────────────────────────────┬─────────────┘
           │                            │
┌──────────▼──────────┐    ┌────────────▼─────────────┐
│        Redis         │    │          MySQL            │
│                      │    │                           │
│  otp:{id}:{channel}  │    │  users                   │
│  rate_limit:{id}     │    │  admin_users             │
│  cooldown:{id}       │    │  otp_sessions            │
│                      │    │  otp_delivery_logs       │
│  TTL auto-expiry     │    │  blocked_users           │
└──────────────────────┘    │  refresh_token_blacklist │
                             │  audit_logs              │
                             └──────────────────────────┘
```

---

## 🔄 Authentication Flow

### User Registration
```
POST /api/auth/register  →  Save user  →  Generate OTP  →  Send via channel
POST /api/otp/verify     →  purpose=REGISTER  →  Verify OTP  →  "Please login"
```

### User Login
```
POST /api/auth/login     →  Check user exists  →  Generate OTP  →  Send via channel
POST /api/otp/verify     →  purpose=LOGIN  →  Verify OTP  →  Issue JWT tokens
```

### Token Management
```
POST /api/auth/refresh   →  Validate refresh token  →  Rotate tokens  →  New pair issued
POST /api/auth/logout    →  Blacklist refresh token  →  Session ended
```

### Admin Flow
```
POST /api/admin/login    →  Verify password (BCrypt)  →  Issue Admin JWT
GET  /api/admin/logs     →  View full audit trail
POST /api/admin/users/block    →  Block user with reason
POST /api/admin/users/unblock  →  Unblock user
```

---

## 📡 API Reference

### OTP Service — Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/otp/generate` | Generate and send OTP via selected channel |
| `POST` | `/api/otp/resend` | Resend OTP after 30-second cooldown |
| `POST` | `/api/otp/verify` | Verify OTP — issues JWT on LOGIN purpose |

### Auth Service — Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register new user + trigger OTP |
| `POST` | `/api/auth/login` | Login existing user + trigger OTP |
| `POST` | `/api/auth/refresh` | Rotate refresh token, issue new access token |
| `POST` | `/api/auth/logout` | Blacklist refresh token |
| `POST` | `/api/auth/validate-token` | Validate if JWT token is active |

### Admin Service — Admin JWT Required

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/admin/login` | Admin login with email + password |
| `GET`  | `/api/admin/logs` | Fetch paginated audit logs |
| `GET`  | `/api/admin/logs/stats` | Dashboard summary stats |
| `POST` | `/api/admin/users/block` | Block a user with reason |
| `POST` | `/api/admin/users/unblock` | Unblock a previously blocked user |
| `GET`  | `/api/admin/users/blocked` | List all currently blocked users |

---

## 🗄️ Database Schema

### Tables

| Table | Purpose |
|---|---|
| `users` | End users who authenticate via OTP |
| `admin_users` | Admin accounts for system management |
| `otp_sessions` | Full OTP lifecycle tracking |
| `otp_delivery_logs` | Delivery attempts per session (with retry) |
| `blocked_users` | Users blocked by admins with reason + history |
| `refresh_token_blacklist` | Revoked refresh tokens to prevent reuse |
| `audit_logs` | Complete event trail — retained 90 days |

### Redis Key Schema

| Purpose | Key Pattern | TTL |
|---|---|---|
| Store OTP hash | `otp:{userId}:{channel}` | 300 sec |
| Rate limit counter | `rate_limit:{userId}` | 3600 sec |
| Resend cooldown | `cooldown:{userId}` | 30 sec |

---

## 🛡️ Security Features

- **No plain-text OTPs** — BCrypt hashed before storage
- **Redis TTL** — OTPs auto-expire after 5 minutes
- **Immediate deletion** — OTP deleted from Redis right after verification
- **Rate limiting** — max 5 OTP requests per hour per user
- **Resend cooldown** — 30-second wait between requests
- **JWT rotation** — refresh token blacklisted on each use
- **Masked logs** — email/phone masked in all audit logs (`raj***@gmail.com`)
- **Role-based access** — USER vs ADMIN vs SUPER_ADMIN roles
- **No account lockout** — failed OTP attempts don't lock accounts (by design)

---

## 📋 Audit Events

Every lifecycle event is recorded in `audit_logs`:

```
OTP_REQUESTED       OTP_SENT            OTP_DELIVERY_FAILED
OTP_VERIFIED        OTP_INVALID         OTP_EXPIRED
OTP_RESEND_REQUESTED                    RATE_LIMIT_EXCEEDED
TOKEN_ISSUED        TOKEN_REVOKED
USER_BLOCKED        USER_UNBLOCKED      ADMIN_LOGIN
```

---

## 📁 Project Structure

```
com.project.spring/
├── api/
│   ├── controller/
│   │   ├── OtpController.java
│   │   ├── AuthController.java
│   │   └── AdminController.java
│   ├── request/
│   │   ├── GenerateOtpRequest.java
│   │   ├── VerifyOtpRequest.java
│   │   ├── ResendOtpRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── AdminLoginRequest.java
│   │   └── BlockUserRequest.java
│   └── response/
│       ├── ApiResponse.java
│       ├── VerifyOtpResponse.java
│       ├── RegisterResponse.java
│       └── AdminStatsResponse.java
├── config/
│   ├── AppProperties.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── core/
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── OtpException.java
│   │   ├── RateLimitException.java
│   │   └── UserBlockedException.java
│   └── service/
│       ├── OtpService.java
│       ├── AuthService.java
│       ├── AdminService.java
│       ├── AuditService.java
│       ├── EmailDeliveryService.java
│       ├── SmsDeliveryService.java
│       ├── WhatsAppDeliveryService.java
│       └── OtpDeliveryService.java
├── data/
│   ├── entity/
│   │   ├── User.java
│   │   ├── AdminUser.java
│   │   ├── OtpSession.java
│   │   ├── OtpDeliveryLog.java
│   │   ├── BlockedUser.java
│   │   ├── RefreshTokenBlacklist.java
│   │   └── AuditLog.java
│   ├── enums/
│   │   ├── Channel.java
│   │   ├── OtpStatus.java
│   │   ├── OtpPurpose.java
│   │   ├── EventType.java
│   │   └── AuditStatus.java
│   └── repository/
│       ├── UserRepository.java
│       ├── AdminRepository.java
│       ├── OtpSessionRepository.java
│       ├── OtpDeliveryLogRepository.java
│       ├── BlockedUserRepository.java
│       ├── RefreshTokenBlacklistRepository.java
│       └── AuditLogRepository.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   ├── UserPrincipal.java
│   ├── AdminPrincipal.java
│   └── CustomUserDetailsService.java
├── util/
│   ├── OtpUtil.java
│   └── MaskUtil.java
└── Application.java

streamlit-app/
├── app.py
├── pages/
│   ├── 1_Register.py
│   ├── 2_Login.py
│   ├── 3_Verify_OTP.py
│   ├── 4_Admin_Login.py
│   ├── 5_Dashboard.py
│   ├── 6_Audit_Logs.py
│   └── 7_Manage_Users.py
└── requirements.txt
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- Redis 7+
- Python 3.9+ (for Streamlit)
- Twilio account (for SMS/WhatsApp)

### 1. Clone the repository

```bash
git clone https://github.com/Vetrivel003/otp-authentication-system.git
cd otp-authentication-system
```

### 2. Setup MySQL

```sql
CREATE DATABASE otp_auth_db;
```

Then run `schema.sql` to create all tables:

```bash
mysql -u root -p otp_auth_db < src/main/resources/schema.sql
```

### 3. Configure environment variables

Create a `.env` file or set these in your system:

```properties
DB_USERNAME=root
DB_PASSWORD=yourpassword

REDIS_PASSWORD=yourredispassword

JWT_SECRET=your-minimum-256-bit-secret-key-here

MAIL_USERNAME=youremail@gmail.com
MAIL_PASSWORD=your-app-password

TWILIO_ACCOUNT_SID=your-account-sid
TWILIO_AUTH_TOKEN=your-auth-token
TWILIO_SMS_FROM=+1234567890
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
```

### 4. Run Spring Boot

```bash
mvn spring-boot:run
```

Backend starts at `http://localhost:8080`

### 5. Create default admin

```
POST http://localhost:8080/api/admin/create
```

### 6. Run Streamlit frontend

```bash
cd streamlit-app
pip install -r requirements.txt
streamlit run app.py
```

Frontend starts at `http://localhost:8501`

---

## 🧪 Postman Testing Order

```
1. POST /api/auth/register       → Register new user
2. POST /api/otp/verify          → Verify OTP (purpose: REGISTER)
3. POST /api/auth/login          → Login → OTP sent
4. POST /api/otp/verify          → Verify OTP (purpose: LOGIN) → Get JWT
5. POST /api/admin/login         → Admin login → Get Admin JWT
6. GET  /api/admin/logs/stats    → View dashboard stats
7. POST /api/admin/users/block   → Block a user
8. POST /api/auth/refresh        → Refresh access token
9. POST /api/auth/logout         → Logout
```

---

## ⚡ Non-Functional Requirements

| Requirement | Target |
|---|---|
| OTP generation response time | < 500ms |
| OTP verification response time | < 300ms |
| Concurrent OTP sessions | 100+ without degradation |
| OTP delivery success rate | 99%+ |
| System uptime | 99.9% |
| Audit log retention | 90 days |
| Redis OTP sessions | Up to 10,000 simultaneous |

---

## 🌐 Streamlit Pages

| Page | Route | Description |
|---|---|---|
| Register | `/Register` | New user registration + OTP trigger |
| Login | `/Login` | Existing user login + OTP trigger |
| Verify OTP | `/Verify_OTP` | OTP verification with resend option |
| Admin Login | `/Admin_Login` | Admin authentication |
| Dashboard | `/Dashboard` | Live stats — requests, success rate, sessions |
| Audit Logs | `/Audit_Logs` | Paginated, filterable event logs |
| Manage Users | `/Manage_Users` | Block / unblock users |

---

## 📝 Error Codes

| Code | Description |
|---|---|
| `OTP_INVALID` | OTP entered is incorrect |
| `OTP_EXPIRED` | OTP has expired (after 5 minutes) |
| `OTP_NOT_FOUND` | No OTP found for this user/channel |
| `RATE_LIMIT_EXCEEDED` | More than 5 requests in 1 hour |
| `RESEND_COOLDOWN_ACTIVE` | Must wait 30 seconds before resending |
| `USER_BLOCKED` | User has been blocked by admin |
| `DELIVERY_FAILED` | OTP could not be delivered via channel |
| `INVALID_CREDENTIALS` | Wrong admin email or password |
| `TOKEN_INVALID` | JWT token is invalid or expired |
| `TOKEN_REVOKED` | Refresh token has been blacklisted |
| `INVALID_IDENTIFIER` | Wrong identifier type for selected channel |

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: your feature description"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request to `develop` branch

### Commit Convention

```
feat:     new feature
fix:      bug fix
chore:    setup or config changes
refactor: code improvement
test:     adding tests
docs:     documentation updates
```

---

## 👨‍💻 Author

**Vetrivel A**
B.Tech — AI & Data Science
Sri Eshwar College of Engineering, 2026

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">
  Built with ❤️ using Spring Boot + Redis + MySQL + Streamlit
</div>
