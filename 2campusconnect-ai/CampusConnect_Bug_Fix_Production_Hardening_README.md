# CampusConnect — Bug Fix & Production Hardening Instructions

> **IMPORTANT:** This document is an instruction file for the coding agent working on the existing CampusConnect project.
> **The existing architecture is FINAL. Do not rebuild or migrate the project.**

---

## 1. PRIMARY OBJECTIVE

Audit and improve the **existing CampusConnect project**.

The task is strictly:

**AUDIT → FIND REAL BUG/FLAW → FIX IT → VERIFY THE FIX**

The project was already built in Google AI Studio and already has an established architecture.

The goal is to fix:

- bugs
- security vulnerabilities
- incorrect behavior
- incomplete functionality
- data-integrity problems
- Android/backend synchronization problems
- role/permission problems
- realtime problems
- production configuration problems
- release/build problems
- genuine performance problems

Do **not** redesign the application architecture.

---

# 2. ABSOLUTE ARCHITECTURE PRESERVATION RULE

The existing project architecture MUST be preserved.

### DO NOT:

- rebuild the project from scratch
- migrate to another Android framework
- migrate to another backend framework
- migrate to another database
- replace Kotlin/Jetpack Compose
- replace the existing backend technology
- replace Prisma/PostgreSQL
- replace Socket.IO
- replace the existing authentication architecture unnecessarily
- reorganize the entire folder structure
- rename large groups of files/classes/packages unnecessarily
- create a second competing architecture
- create parallel repositories/services for existing functionality
- remove working functionality
- rewrite working modules simply because another implementation is preferred

### USE THE EXISTING:

- Android architecture
- Kotlin/Jetpack Compose implementation
- navigation
- screens
- components
- repositories
- models
- networking
- authentication
- backend routes
- middleware
- Prisma schema
- PostgreSQL
- Socket.IO
- AI integration
- existing project structure

If an existing implementation can be corrected, **correct it instead of replacing it**.

If a new function is genuinely required, integrate it into the **existing architecture**.

Only make an architectural change if a specific existing implementation is fundamentally broken or insecure and cannot reasonably be fixed without it. Even then, make the **smallest possible change** and preserve compatibility with the rest of the application.

---

# 3. FULL PROJECT AUDIT BEFORE CHANGES

Before modifying code, inspect the complete project.

Do not assume something works because its UI exists.

Inspect:

- Android source
- Compose screens
- navigation
- components
- models
- repositories
- local storage/cache
- networking
- authentication state
- backend source
- middleware
- routes
- services
- realtime/Socket.IO
- Prisma schema
- migrations
- scripts
- environment configuration
- Gradle configuration
- tests
- AI integration
- admin functionality
- all role-related implementations

For important functionality, trace:

```text
Android UI
    ↓
Existing state/ViewModel
    ↓
Existing repository
    ↓
Existing network layer
    ↓
Backend route
    ↓
Authentication middleware
    ↓
Authorization middleware
    ↓
Business logic
    ↓
Prisma
    ↓
PostgreSQL
    ↓
Backend response
    ↓
Android state
    ↓
Android UI
```

Find and fix genuine:

- compilation errors
- runtime crashes
- broken navigation
- incorrect UI state
- incorrect repository behavior
- incorrect API requests
- incorrect API responses
- Android/backend model mismatches
- authentication flaws
- authorization flaws
- privilege escalation
- ownership/security flaws
- IDOR vulnerabilities
- validation weaknesses
- race conditions
- duplicate-data problems
- stale-cache problems
- synchronization problems
- database relationship problems
- missing constraints
- missing indexes where actually required
- incorrect Socket.IO authorization
- notification synchronization problems
- unread-count inconsistencies
- pagination problems
- loading-state problems
- empty-state problems
- error-state problems
- coroutine/threading issues
- lifecycle issues
- memory/resource leaks
- insecure local storage
- exposed secrets
- hard-coded URLs
- development configuration accidentally used in production
- fake/mock/demo data
- placeholder implementations
- unfinished functionality affecting production behavior
- dead/unreachable code where it causes real problems
- inconsistent role behavior
- inconsistent permission checks
- missing audit logging
- unsafe administrative operations
- missing rate limiting where required
- CORS/security configuration problems
- deployment blockers
- Android release/build problems
- backend build/runtime problems

Do not change code merely because it could theoretically be written differently.

---

# 4. SECURITY MODEL

Treat the Android application as untrusted.

Assume an attacker can:

- modify the APK
- modify HTTP requests
- modify request bodies
- modify IDs
- modify role values
- modify ownership values
- call APIs manually
- call Socket.IO events manually
- bypass Android UI
- replay requests
- attempt privilege escalation

Therefore the backend must be authoritative for:

- authentication
- authorization
- roles
- permissions
- ownership
- account status
- moderation
- administrative operations

Never rely on hiding an Android button as a security mechanism.

---

# 5. SUPER ADMIN SECURITY

Audit and fix the existing Super Admin implementation.

The current hard-coded/default Super Admin credentials must be removed.

There must be **NO production fallback password or predictable privileged credential**.

Do not keep default values such as:

```text
SuperAdmin@Secure2026!
```

or equivalent credentials in production code.

The initial Super Admin must be created through a secure server-side bootstrap mechanism using protected environment configuration/secrets.

Normal registration must NEVER create:

- SUPER_ADMIN
- UNIVERSITY_ADMIN
- DEPARTMENT_ADMIN
- other privileged administrative accounts

Prevent:

- self-promotion
- client-side role manipulation
- privilege escalation
- unauthorized administrator creation
- unauthorized administrator modification

Preserve the current authentication/bootstrap architecture where possible. Make only the necessary security corrections.

---

# 6. BACKEND AUTHORIZATION

Fix authorization flaws without replacing the existing authorization architecture.

The backend must independently verify:

- authentication
- role
- permission
- ownership
- account status

Never trust role or ownership information supplied by Android.

If the existing project already uses mechanisms such as:

```text
authenticateToken
requireRole
```

or equivalent guards/middleware, reuse and improve them.

Do not create a second authorization architecture unless absolutely necessary.

Every protected operation must be authorized server-side.

---

# 7. EXISTING ROLE SYSTEM

Preserve the existing roles:

```text
SUPER_ADMIN
UNIVERSITY_ADMIN
DEPARTMENT_ADMIN
CLUB_LEADER
FACULTY
ALUMNI
STUDENT
```

Fix incorrect or missing role behavior.

The roles should have genuinely different permissions where the existing application intends them to.

Do not merely change labels.

Do not create a new role architecture.

### STUDENT

Students must not receive unauthorized administrative functionality.

### FACULTY

Faculty should receive faculty-specific functionality and only authorized academic/content management.

### ALUMNI

Alumni should receive the existing alumni-oriented networking/career functionality.

### CLUB_LEADER

Club Leaders should only manage clubs they are authorized to manage.

### DEPARTMENT_ADMIN

Department Admin should only manage authorized department-level functionality.

### UNIVERSITY_ADMIN

University Admin should only have authorized university-level functionality.

### SUPER_ADMIN

Super Admin should have the highest application-level administrative authority.

---

# 8. FEED

Audit the existing Feed implementation.

Fix genuine problems involving:

- fake/demo posts
- post ownership
- create/edit/delete
- comments
- reactions
- refresh
- pagination
- visibility
- author information
- timestamps
- role-based publishing
- synchronization
- moderation
- reporting

Preserve the existing Feed architecture, models, screens, and repositories.

Do not replace the Feed system.

Official/privileged announcements must be protected by existing backend authorization.

If the database is empty, show the application's proper empty state instead of inventing content.

---

# 9. CHAT AND SOCKET.IO

Audit the existing chat and Socket.IO implementation.

Fix genuine problems involving:

- authentication
- channel access
- membership
- message persistence
- message retrieval
- unread counts
- read state
- typing indicators
- presence
- realtime delivery
- reconnect behavior
- duplicate messages
- unauthorized room access

Preserve Socket.IO.

Do not migrate to another realtime technology.

The server must verify channel membership and authorization.

Client-provided IDs must never bypass authorization.

---

# 10. NOTIFICATIONS

Audit existing notifications.

Fix:

- missing notifications
- duplicate notifications
- incorrect recipients
- unread-count problems
- read-state problems
- stale notification state
- realtime delivery problems
- synchronization problems

Preserve the existing notification architecture.

---

# 11. EVENTS

Audit the existing Events implementation.

Fix genuine problems involving:

- creation
- editing
- deletion
- ownership
- permissions
- participation
- timestamps
- visibility
- synchronization
- notifications

Preserve the existing event architecture.

---

# 12. FORUMS

Audit Forums.

Fix:

- topic creation
- replies
- editing
- deletion
- permissions
- ownership
- moderation
- pagination
- synchronization
- fake/demo content

Preserve the current architecture.

---

# 13. RESOURCES

Audit Resources.

Fix:

- ownership
- permissions
- creation
- editing
- deletion
- visibility
- incorrect data
- synchronization
- fake/placeholder resources

Preserve the current implementation.

---

# 14. CAREER

Audit Career functionality.

Fix genuine problems involving:

- opportunities
- ownership
- permissions
- visibility
- deadlines
- synchronization
- unauthorized creation/editing

Do not redesign the Career architecture.

---

# 15. DATABASE / PRISMA

Audit the existing Prisma schema and database implementation.

Fix genuine issues such as:

- incorrect relationships
- missing required relationships
- duplicate records
- missing unique constraints
- missing indexes that cause actual problems
- incorrect cascade behavior
- incorrect nullable fields
- data-integrity problems
- incorrect ownership relationships

Preserve:

- Prisma
- PostgreSQL
- existing schema structure where valid
- existing migration architecture

Do not redesign the entire database unless necessary to fix an actual defect.

---

# 16. ANDROID ↔ BACKEND SYNCHRONIZATION

Audit the existing synchronization flow.

Fix:

- incorrect API calls
- request/response mismatches
- stale state
- failed refresh
- duplicate records
- incorrect cache updates
- missing server updates
- incorrect error handling
- backend changes not appearing in Android
- Android changes not reaching backend

Preserve the existing repository/network architecture.

Do not introduce a new synchronization framework unnecessarily.

---

# 17. LOCAL STORAGE

Audit existing local storage.

Fix actual:

- security problems
- stale-data problems
- synchronization problems
- incorrect cache invalidation
- token-storage problems

Do not replace the storage mechanism unnecessarily.

Server data must remain authoritative for persistent application data.

---

# 18. API VALIDATION

Audit backend validation.

Fix endpoints that incorrectly trust:

- user IDs
- roles
- permissions
- ownership
- department IDs
- club IDs
- administrative status

Use the validation architecture already present.

If Zod is already used, extend the existing Zod implementation instead of replacing it.

---

# 19. IDOR / OWNERSHIP SECURITY

Check endpoints accepting object IDs.

Verify that a user cannot access or modify another user's:

- posts
- profile data
- messages
- channels
- events
- resources
- career records
- administrative data

unless explicitly authorized.

Fix genuine IDOR vulnerabilities using the existing authorization architecture.

---

# 20. SECRETS

Search the entire project for:

- passwords
- API keys
- JWT secrets
- database credentials
- Gemini keys
- bootstrap secrets
- private tokens

Remove actual production secrets from source code.

Remove default privileged credentials.

Use the existing environment configuration architecture.

Do not create unnecessary new secret-management infrastructure.

---

# 21. AI / GEMINI

Audit the existing AI integration.

Fix genuine problems involving:

- exposed API keys
- incorrect requests
- incorrect responses
- error handling
- quota handling
- authentication
- backend/client separation

Preserve the current AI implementation where it works.

Do not replace the AI architecture unnecessarily.

---

# 22. FAKE / DEMO / PLACEHOLDER DATA

Search the project for:

```text
mock
fake
demo
sample
dummy
placeholder
TODO
FIXME
hard-coded users
hard-coded posts
hard-coded messages
hard-coded notifications
hard-coded events
temporary credentials
temporary URLs
```

Remove production-facing fake behavior where it is actually being used.

Do not remove legitimate test fixtures.

When the real database is empty, use the existing empty-state UI rather than inventing content.

---

# 23. API CONTRACTS

Compare Android API calls against actual backend implementations.

Verify:

- URL
- HTTP method
- authentication
- request body
- response body
- status codes
- error format
- model fields
- nullability
- pagination
- authorization

Fix genuine mismatches.

Do not replace the API architecture.

---

# 24. ERROR HANDLING

Fix actual error-handling problems.

Correctly handle:

```text
400 Validation Error
401 Authentication Required
403 Forbidden
404 Not Found
409 Conflict
429 Rate Limited
500 Server Error
```

Also handle:

- network failure
- timeout
- expired authentication
- unavailable backend

Do not expose raw stack traces or sensitive server information.

Preserve the existing error-handling architecture.

---

# 25. PERFORMANCE

Fix only genuine performance problems.

Audit for:

- unnecessary database queries
- N+1 queries
- excessive API requests
- missing pagination
- unnecessarily large responses
- inefficient Compose rendering
- repeated synchronization
- unnecessary local database operations

Do not rewrite code solely for theoretical optimization.

---

# 26. PRODUCTION CONFIGURATION

Audit existing production configuration.

Fix genuine problems involving:

- localhost URLs in release builds
- incorrect API endpoints
- insecure CORS
- missing environment variables
- release/debug configuration mistakes
- exposed secrets
- backend startup configuration

Preserve the current deployment architecture.

---

# 27. ANDROID RELEASE

Audit the existing Android release configuration.

Fix genuine issues involving:

- application ID
- versioning
- release build
- signing configuration
- permissions
- production API URL
- debug/release separation
- R8/ProGuard if required
- manifest configuration

Do not unnecessarily change the Android project structure.

---

# 28. TESTING AND VERIFICATION

After changes:

- compile backend
- validate Prisma
- generate Prisma client where required
- build Android
- run existing tests
- fix errors caused by changes
- verify API contracts
- verify authentication
- verify authorization
- verify role restrictions
- verify ownership restrictions
- verify Super Admin security
- verify Socket.IO authentication
- verify database operations
- verify Android/backend synchronization

Test both:

```text
AUTHORIZED
```

and:

```text
UNAUTHORIZED
```

requests.

Security tests must attempt to bypass normal Android UI restrictions.

Do not consider a feature fixed merely because its screen opens.

Verify actual end-to-end behavior.

---

# 29. FINAL ARCHITECTURE PRESERVATION CHECK

Before finishing, verify that the project has NOT been unnecessarily changed architecturally.

Confirm:

- existing Android framework remains
- existing Kotlin/Compose structure remains
- existing navigation remains
- existing repositories remain
- existing backend technology remains
- existing database remains
- existing Prisma architecture remains
- existing Socket.IO implementation remains
- existing authentication architecture remains where possible
- existing AI architecture remains where possible
- existing folder structure remains substantially intact
- existing working features remain
- no duplicate competing architecture was introduced
- no unnecessary framework migration occurred
- no unnecessary mass file renaming occurred
- no unnecessary rewrite occurred

---

# 30. NON-NEGOTIABLE RULES

1. **DO NOT rebuild the project.**
2. **DO NOT redesign the architecture.**
3. **DO NOT migrate technologies.**
4. **DO NOT replace working systems unnecessarily.**
5. **DO NOT remove working features.**
6. **DO NOT create parallel implementations of existing systems.**
7. **DO NOT create fake functionality.**
8. **DO NOT create fake production data.**
9. **DO NOT use hard-coded privileged credentials.**
10. **DO NOT expose server secrets in Android.**
11. **DO NOT trust Android-provided roles.**
12. **DO NOT trust Android-provided ownership.**
13. **DO NOT rely on UI restrictions for security.**
14. **DO NOT allow privilege escalation.**
15. **DO NOT allow IDOR vulnerabilities.**
16. **DO NOT allow unauthorized Socket.IO access.**
17. **DO NOT change the database technology.**
18. **DO NOT change the realtime technology.**
19. **DO NOT change the existing project structure unless absolutely necessary.**
20. **DO NOT rewrite working code merely because you prefer another coding style.**
21. **DO NOT invent new functionality unless it is necessary to fix an identified flaw.**
22. **DO NOT claim a problem is fixed until the actual end-to-end behavior has been verified.**
23. **If a requested correction can be made inside the existing architecture, it MUST be made inside the existing architecture.**
24. **Make the smallest safe change necessary to correct each genuine problem.**

---

# FINAL INSTRUCTION

This is an EXISTING Google AI Studio-built CampusConnect application.

The architecture is ALREADY BUILT.

You are NOT being asked to redesign or rebuild it.

Your job is ONLY:

```text
INSPECT
↓
IDENTIFY REAL BUGS / FLAWS
↓
FIX THEM INSIDE THE EXISTING ARCHITECTURE
↓
VERIFY
```

Preserve the existing project and improve its correctness, security, reliability, and production readiness without unnecessary architectural changes.
