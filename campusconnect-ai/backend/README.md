# CampusConnect Backend & API Gateway

Production-grade Node.js/TypeScript backend for the CampusConnect Android Application. Features PostgreSQL relational persistence via Prisma ORM, Socket.IO real-time channels, JWT access/refresh token rotation, and a server-side Gemini AI proxy.

---

## 🚀 Quick Start with Docker (Recommended)

Start PostgreSQL, Redis, and the API service in one command:

```bash
cd backend
docker-compose up -d --build
```

The server will be reachable at `http://localhost:4000` (or `http://10.0.2.2:4000` from the Android Emulator).

---

## 🛠️ Local Development (Node.js & Local PostgreSQL)

### 1. Install Dependencies
```bash
cd backend
npm install
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Ensure your `DATABASE_URL` matches your local PostgreSQL instance:
```env
DATABASE_URL="postgresql://campus_admin:campus_password@localhost:5432/campusconnect?schema=public"
JWT_ACCESS_SECRET="campus_jwt_access_secret_super_secure_key_2026_production"
JWT_REFRESH_SECRET="campus_jwt_refresh_secret_super_secure_key_2026_production"
GEMINI_API_KEY="your_google_gemini_api_key_here"
```

### 3. Generate Prisma Client & Run Migrations
```bash
npx prisma generate
npx prisma migrate dev --name init
```

### 4. Start Development Server
```bash
npm run dev
```

---

## 👑 Bootstrapping Your Super Admin Account

To prevent unauthorized role elevation, **normal user registrations always default to `STUDENT`**.

To initialize the platform owner account (**vinzodarajan@gmail.com**), run the CLI bootstrap tool:

```bash
npm run bootstrap:superadmin
```

Or customize the password directly:
```bash
npx tsx src/scripts/bootstrap-superadmin.ts vinzodarajan@gmail.com "YourSecurePassword2026!" "Super Admin Owner"
```

### Privileges of `SUPER_ADMIN`:
- Full access to the **Admin Center** (`/api/v1/admin/overview`, `/api/v1/admin/users`, `/api/v1/admin/audit-logs`)
- Ability to assign/revoke administrative roles (`UNIVERSITY_ADMIN`, `DEPARTMENT_ADMIN`, `FACULTY`)
- Campus-wide announcements and moderation controls
- Direct access to server sync & metrics operations

---

## 📱 Connecting the Android Application

1. Open **CampusConnect** on your Android device or emulator.
2. Navigate to **Top Bar -> Backend Operations** (cloud sync icon).
3. If using the **Android Emulator**, set Server URL to:
   ```
   http://10.0.2.2:4000/api/v1
   ```
4. If testing on a **physical device over Wi-Fi**, set Server URL to your machine's LAN IP:
   ```
   http://192.168.x.x:4000/api/v1
   ```
5. Tap **Sync Now** to verify bidirectional communication.

---

## 🔒 Security Architecture Highlights

1. **Role Guard**: Any request to `/api/v1/admin/*` strictly requires `UserRole.SUPER_ADMIN` or `UserRole.UNIVERSITY_ADMIN`. Unauthorized requests yield `403 Forbidden` and log an entry in `AuditLog`.
2. **AI Secret Isolation**: `GEMINI_API_KEY` is kept server-side. The Android client sends prompts to `/api/v1/ai/*`, which enforces rate limits (50 requests/day per student) before forwarding to Gemini.
3. **Socket Security**: Real-time Socket.IO connections require a signed JWT token in `auth.token` during the handshake.
