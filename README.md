# Example Ktor

Starter backend Ktor yang siap dipakai ulang untuk project baru. Isinya:
- Auth dengan JWT (access token + refresh token lewat cookie httpOnly)
- RBAC berbasis permission
- Upload file dengan signed URL
- Response envelope standar, semua field JSON snake_case
- Cache Redis
- Rate limiting
- Todo list per user
- Logging request/response & query SQL ke file terpisah, data sensitif di-redact

## Tech stack

| Layer | Teknologi |
|---|---|
| Framework | Ktor 3 (Netty) |
| Database | MySQL + Exposed (DSL) + HikariCP |
| DI | Koin |
| Cache | Redis (Lettuce) |
| Auth | JWT (`ktor-server-auth-jwt`), refresh token via httpOnly cookie |
| Validasi | Validator ringan bawaan project (`common/validation`) |
| Test | JUnit 5 + Ktor `testApplication` + MockK |
| Build | Gradle (wrapper sudah ada) |

## Konsep utama

| Kebutuhan | Cara di project ini |
|---|---|
| Daftar dependency (DI) | Koin, semua dipasang di `AppModule.kt` |
| Controller / endpoint | `fun Route.xxxRoutes()` di tiap module |
| Endpoint public | Route dipasang di luar `authenticate("jwt") { }` |
| Wajib login | Route dipasang di dalam `authenticate("jwt") { }` |
| Batasi role / permission | `authorize(roles = ..., permissions = ...) { }` |
| User yang sedang login | `call.currentUser()` |
| Response sukses | `call.success(data, "pesan")` atau `call.successNoData("pesan")` |
| Response error | Lempar `NotFoundException(...)`, `ForbiddenException(...)`, dst. Otomatis dibungkus oleh `StatusPages` |
| Validasi body / query | `call.receiveValid<Dto>(Dto.rules)` / `call.validateQuery(...)` |
| Rate limit | Plugin `RateLimit` global per IP |
| Tabel & query | Tabel Exposed (`*Table.kt`) + `*Repository` |
| Auto-create tabel | `SchemaUtils.create(...)` saat `APP_ENV` bukan `production` |
| Seed data awal | `./gradlew seed` |

## Alur request

```
Request masuk
   │
   ▼
RateLimit              → 429 kalau kelebihan
   ▼
authenticate("jwt")    → wajib login, kecuali route public
   ▼
authorize(roles=...)   → cek role (cukup salah satu)
   ▼
authorize(permissions=...) → cek permission (wajib semua)
   ▼
receiveValid / validateQuery → validasi body/query (400 kalau gagal)
   ▼
Route → Service → Repository → Database
   ▼
call.success(...)      → { status, code, message, data }
   (atau)
StatusPages            → { status: "error", code, message, data: null }
```

## Struktur folder

```
src/main/kotlin/com/example/
├── Application.kt          # main + pemasangan plugin + routing /api/v1
├── AppModule.kt            # wiring Koin
├── common/
│   ├── logging/            # HttpLogging, SqlFileLogger, Redactor
│   ├── exceptions/         # HttpException, NotFoundException, dst
│   ├── plugins/            # Authorization (roles/permissions), ErrorHandling, Http (CORS, JSON, rate limit), Security (JWT)
│   ├── response/           # ApiResponse + call.success / respondError
│   ├── security/           # AuthenticatedUser, PasswordHasher (bcrypt)
│   ├── utils/              # Paging, SignedUrl, Durations ("15m", "7d"), Dates
│   └── validation/         # aturan validasi body & query
├── config/                 # AppConfig (.env), CacheService (Redis)
├── database/
│   ├── DatabaseFactory.kt  # koneksi + buat tabel + dbQuery {}
│   └── seeds/              # permission, role, user admin
└── modules/
    ├── auth/               # register/login/refresh/logout, roles & permissions, JwtService
    ├── users/              # CRUD user + pagination + search, soft delete
    ├── files/              # upload, file private/public, signed URL
    └── todos/              # todo list milik user yang login
```

## Skema database

```
users ───┐
         │ M:N (tabel: user_roles)
roles ───┤
         │ M:N (tabel: role_has_permission)
permissions

files   (independen)
todos   (milik users, lewat user_id)
```

Semua primary key berupa UUID (string). `users`, `roles`, `permissions`, dan `files` punya kolom `deleted_at` (soft delete), dan data yang sudah dihapus otomatis tidak ikut di query. Semua kolom memakai snake_case.

### Zona waktu

Timestamp (`created_at`, `updated_at`, `deleted_at`) disimpan dan dikirim sesuai zona `APP_TIME`. Defaultnya UTC (`+00:00`), dan bisa diganti ke jam lokal (mis. `+07:00` untuk WIB) kalau aplikasinya dipakai di satu zona waktu.

- Isi `APP_TIME` dengan format offset: `+00:00` (UTC, default di `.env.example`), `+07:00` (WIB), `+08:00` (WITA), atau `+09:00` (WIT). Tulisan `UTC` atau `Asia/Jakarta` **tidak** didukung.
- Di response, timestamp berakhiran `Z` (mis. `2026-09-24T10:15:00.000Z`), tapi jamnya tetap jam lokal. Tampilkan apa adanya di client, jangan dikonversi zona waktu.
- Jangan mengganti `APP_TIME` setelah ada data, karena database tidak mencatat zonanya dan data lama akan terbaca dengan zona yang baru.

## Setup

Prasyarat: JDK 21+ (dites dengan JDK 25), MySQL, dan Redis.

```bash
cp .env.example .env      # lalu sesuaikan
./gradlew seed            # bikin tabel + permission, role, user admin
./gradlew run             # jalan di http://localhost:3000
```

Buat dulu database `DB_DATABASE`-nya di MySQL. Tabel akan dibuat otomatis selama `APP_ENV` bukan `production`.

User admin default: **`admin@example.com` / `admin12345`**

| Variable | Keterangan |
|---|---|
| `APP_ENV` | `production` untuk mematikan auto-create tabel dan menyalakan cookie `secure` |
| `PORT` | Port server (default 3000) |
| `APP_TIME` | **Wajib diisi.** Zona waktu timestamp, format offset: `+00:00` (UTC), `+07:00`, `+08:00`, `+09:00` |
| `DB_*` | Koneksi MySQL |
| `JWT_SECRET` | **Wajib diganti** sebelum production |
| `JWT_ACCESS_EXPIRES_IN`, `JWT_REFRESH_EXPIRES_IN` | Format `15m`, `7d`, `1h`, dst |
| `REDIS_*`, `CACHE_TTL` | Koneksi Redis dan TTL default cache (detik) |
| `THROTTLE_TTL`, `THROTTLE_LIMIT` | Rate limit: jumlah request per sekian detik per IP |
| `APP_URL` | Base URL aplikasi, dipakai untuk signed URL |
| `FILE_TEMP_DIR`, `FILE_STORAGE_DIR` | Folder penyimpanan file |
| `SIGNED_URL_SECRET` | **Wajib diganti** sebelum production |

## Docker

```bash
docker compose up -d --build          # app + MySQL + Redis
docker compose run --rm app bin/seed  # seed data awal (sekali saja)
docker compose logs -f app
```

- Image memakai JDK 25 (`eclipse-temurin:25-jre`), dibangun multi-stage, dan berjalan sebagai user non-root.
- Nilai konfigurasi diambil dari `.env`. Host/port database dan Redis otomatis diarahkan ke service compose (`mysql`, `redis`).
- Port di host bisa diganti kalau bentrok: `APP_PORT` (default 3000), `MYSQL_PORT` (3307), `REDIS_HOST_PORT` (6380).
- File upload disimpan di volume `storage`, log ditulis ke folder `./logs` di host.

## Virtual threads

Semua request diproses di virtual thread (JDK 21+) secara default, lewat `configureVirtualThreads()` di `Application.kt`. Kode route, service, dan repository boleh langsung memanggil I/O blocking (JDBC, file, Redis) tanpa `withContext(...)`.

Pengecualian: kerja berat CPU seperti hash bcrypt tetap dijalankan di `Dispatchers.Default` (lihat `PasswordHasher`).

## Testing

```bash
./gradlew test
```

## Ringkasan endpoint

Semua endpoint diawali `/api/v1`.

| Method | Endpoint | Auth | Keterangan |
|---|---|---|---|
| POST | `/auth/register` | Public | Daftar user baru |
| POST | `/auth/login` | Public | Login, mengembalikan access token dan set cookie refresh token |
| PATCH | `/auth/refresh` | Public (butuh cookie) | Refresh access token |
| POST | `/auth/logout` | Public | Hapus cookie refresh token |
| GET | `/roles` | `roles:read` | List role |
| GET | `/permissions` | `roles:read` | List permission |
| GET | `/roles/{id}` | `roles:read` | Detail role beserta permission-nya |
| PATCH | `/roles/{id}/permissions` | `roles:update` | Ganti permission suatu role |
| POST | `/users` | `admin` + `users:create` | Buat user |
| GET | `/users?page=&size=&search=` | `users:read` | List user (pagination + search) |
| GET | `/users/{id}` | `users:read` | Detail user |
| PATCH | `/users/{id}` | `admin` + `users:update` | Update user |
| DELETE | `/users/{id}` | `admin` + `users:delete` | Soft-delete user |
| POST | `/files/store` | login | Upload file (multipart, field `files`) |
| GET | `/files/{id}` | login | Ambil isi file (private) |
| GET | `/files/{id}/signed-url` | login | Generate signed URL sementara (2 menit) |
| GET | `/files/public/{id}` | Public | Ambil isi file berstatus `public` |
| GET | `/files/public/{id}/data` | Public | Generate signed URL untuk file public |
| GET | `/files/signed/{id}` | Public + signature valid | Akses file lewat signed URL |
| POST | `/todos` | login | Buat todo |
| GET | `/todos?page=&size=&search=&is_completed=` | login | List todo milik sendiri (pagination, search, filter status) |
| GET | `/todos/{id}` | login | Detail todo |
| PATCH | `/todos/{id}` | login | Update todo (`is_completed: true` mengisi `completed_at`) |
| DELETE | `/todos/{id}` | login | Soft-delete todo |

Todo hanya bisa diakses pemiliknya. Todo milik user lain dianggap tidak ada (404).

## Logging

Log ditulis ke folder `logs/` (bisa diganti lewat env var `LOG_DIR`), dirotasi harian atau per 10MB, dan disimpan 14 hari.

| File | Isi |
|---|---|
| `logs/http.log` | Detail tiap request & response: method, URL, status, durasi, header, body |
| `logs/sql.log` | Tiap query SQL beserta nilai parameternya |
| `logs/app.log` | Log aplikasi lainnya (startup, error, dsb). Juga tampil di console |

Setiap request punya `request_id` (dikirim balik di header `X-Request-Id`). ID yang sama muncul di `http.log` dan `sql.log`, jadi query milik satu request bisa dicari dengan `grep <request_id> logs/*.log`.

**Data sensitif disamarkan jadi `[REDACTED]`:**
- Field JSON yang namanya `password`, `access_token`, `refresh_token`, `token`, `secret`, `signature`, atau mengandung kata `password`/`token`/`secret` (mis. `new_password`)
- Header `Authorization`, `Cookie`, `Set-Cookie`, dan query `signature`
- Nilai parameter SQL saat INSERT/UPDATE kolom `password`

Isi file upload/download tidak dicatat, hanya jenis dan ukurannya. Untuk menambah field sensitif, edit `SENSITIVE_KEYS` di `common/logging/Redactor.kt`. Untuk mematikan log query, ubah level logger `sql` jadi `OFF` di `logback.xml`.

## Cara menambah endpoint yang diproteksi

```kotlin
fun Route.contohRoutes() {
    val contohService by inject<ContohService>()

    route("/contoh") {
        // wajib login + punya permission ini
        authorize(permissions = listOf("contoh:read")) {
            get { call.success(contohService.findAll()) }
        }

        // wajib role admin DAN permission contoh:delete
        authorize(roles = listOf("admin"), permissions = listOf("contoh:delete")) {
            delete("{id}") {
                contohService.remove(call.parameters["id"]!!)
                call.successNoData("Berhasil dihapus")
            }
        }

        get("/me") { call.success(call.currentUser()) }
    }
}
```

Pasang route-nya di `Application.kt`, di dalam `authenticate(JWT_AUTH) { }`. Untuk endpoint public, pasang di luarnya.

## Contoh pakai cache

```kotlin
class ContohService(private val cache: CacheService) {
    suspend fun stats(): String = cache.remember("stats", ttlSeconds = 60) { hitungStatsYangBerat() }
}
```
