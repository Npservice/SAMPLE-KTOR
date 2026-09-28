# Example Ktor

Starter backend Ktor yang siap dipakai ulang untuk project baru. Isinya:
- Auth dengan JWT (access token + refresh token lewat cookie httpOnly)
- RBAC berbasis role & permission
- Upload file dengan signed URL
- Response standar `BaseResponse` (`OK` / `FAILED`) dan `PageResponse`, field JSON snake_case
- Cache Redis, rate limiting
- Todo list per user
- Logging request/response & query SQL ke file terpisah, data sensitif di-redact

## Tech stack

| Layer | Teknologi |
|---|---|
| Framework | Ktor 3 (Netty) |
| Database | MySQL + Exposed (DSL) + HikariCP |
| DI | Koin |
| Validasi | Plugin `RequestValidation` (bawaan Ktor) |
| Cache | Redis (Lettuce) |
| Auth | JWT (`ktor-server-auth-jwt`), bcrypt |
| Test | JUnit 5 + Ktor `testApplication` + MockK |
| Build | Gradle (wrapper sudah ada) |

## Struktur folder

Mengikuti pola **feature-based modules** dari dokumentasi Ktor
([Application structure](https://ktor.io/docs/server-application-structure.html)): setiap fitur adalah modul
mandiri `fun Application.xxxModule()` berisi route, service, repository, dan DTO-nya sendiri.

```
src/main/kotlin/com/example/
├── Application.kt          # main, pasang plugin global, panggil modul tiap fitur
├── AppModule.kt            # wiring Koin (service & repository)
├── config/                 # AppConfig (.env), CacheService (Redis)
├── database/               # DatabaseFactory (koneksi + buat tabel + dbQuery {}), seeds/
├── core/                   # kode bersama semua fitur
│   ├── exception/          # DataNotFoundException, FieldValidationException, ... + GlobalException (StatusPages)
│   ├── response/           # BaseResponse (isSuccess, isError, noData, send), PageResponse + pageRequest()
│   ├── security/           # JwtProvider, configureSecurity, authorize { }, currentUser(), PasswordHasher
│   ├── validation/         # rules { check(...) } untuk RequestValidation
│   ├── logging/            # HttpLogging, SqlLogging, Redactor
│   └── plugins/            # Http (JSON, CORS, rate limit, call id), VirtualThreads
└── features/
    ├── auth/  user/  role/  file/
    └── todo/
        ├── TodoRoutes.kt       # fun Application.todoModule(): route + auth + RequestValidation
        ├── TodoService.kt      # logika bisnis + mapping ke response (private toResponse)
        ├── TodoRepository.kt   # query Exposed
        ├── TodoDto.kt          # request (+ aturan validasi) & response
        └── TodoTable.kt        # tabel Exposed + data class entity
```

Aturan singkat:
- **Routes** → baca request, panggil service, `call.send(data.isSuccess(message = "..."))`.
- **Service** → logika bisnis, `throw DataNotFoundException(...)`. Mapping entity → response sebagai
  `private fun X.toResponse()`, atau di `XxxMapper.kt` kalau dipakai lebih dari satu fitur (contoh: `RoleMapper.kt`).
- **Repository** → query Exposed saja. Soft delete: selalu filter `deletedAt.isNull()`.
- **Dto** → bentuk data request/response + aturan validasi request.

## Response

```json
{ "status": "OK", "code": 200, "message": "Berhasil ambil data todo", "data": { ... } }
{ "status": "FAILED", "code": 404, "message": "Todo tidak ditemukan", "data": null }
{ "status": "FAILED", "code": 400, "message": "Validation Failed", "data": { "title": "wajib diisi" } }
```

List dengan pagination (`?page=0&size=10`, page mulai dari 0):

```json
{ "content": [ ... ], "page": { "size": 10, "number": 0, "total_elements": 25, "total_pages": 3 } }
```

## Auth

- Login / register / refresh mengembalikan `access_token` di body, refresh token di cookie httpOnly `refresh_token`.
  Umur cookie sama dengan umur refresh token (`JWT_REFRESH_EXPIRES_IN`).
- Kirim access token di header `Authorization: Bearer <access_token>`.
- Setiap token membawa claim `type` (`access` / `refresh`). Access token ditolak di `/auth/refresh`, refresh token
  ditolak sebagai access token.
- `authenticate(JWT_AUTH) { }` = wajib login, `authorize(roles = ..., permissions = ...) { }` = role cukup salah satu,
  permission wajib semua. User yang login: `call.currentUser()`.

## Tanggal & waktu

- Kejadian (`created_at`, `updated_at`, `completed_at`) disimpan dalam **UTC** (`LocalDateTime.now(ZoneOffset.UTC)`)
  dan dikirim sebagai ISO, mis. `"2026-09-28T05:59:21.953Z"`. Frontend yang menampilkannya sesuai zona waktu browser.
- Tanggal kalender (tanggal lahir, cuti, dll) pakai `LocalDate` dan dikirim sebagai string `"YYYY-MM-DD"`, jangan
  diubah jadi datetime.
- Perbandingan dengan "hari ini" (kedaluwarsa, terlambat) dihitung di aplikasi dengan zona waktu bisnis yang jelas,
  bukan `NOW()` / `CURDATE()` di SQL.

## Setup

Prasyarat: JDK 21+ (dites dengan JDK 25), MySQL, dan Redis.

```bash
cp .env.example .env      # lalu sesuaikan
./gradlew seed            # bikin tabel + permission, role, user admin
./gradlew run             # jalan di http://localhost:3000
```

Buat dulu database `DB_DATABASE`-nya di MySQL. Tabel dibuat otomatis selama `APP_ENV` bukan `production`
(`SchemaUtils.create` hanya membuat tabel yang belum ada, tidak mengubah tabel lama).

User admin default: **`admin@example.com` / `admin12345`**

| Variable | Keterangan |
|---|---|
| `APP_ENV` | `production` untuk mematikan auto-create tabel dan menyalakan cookie `secure` |
| `PORT` | Port server (default 3000) |
| `DB_*` | Koneksi MySQL |
| `JWT_SECRET` | **Wajib diganti** sebelum production |
| `JWT_ACCESS_EXPIRES_IN`, `JWT_REFRESH_EXPIRES_IN` | Format durasi Kotlin: `15m`, `7d`, `1h 30m` |
| `REDIS_*`, `CACHE_TTL` | Koneksi Redis dan TTL default cache (detik) |
| `THROTTLE_TTL`, `THROTTLE_LIMIT` | Rate limit: jumlah request per sekian detik per IP |
| `APP_URL` | Base URL aplikasi, dipakai untuk signed URL |
| `FILE_TEMP_DIR`, `FILE_STORAGE_DIR` | Folder penyimpanan file |
| `SIGNED_URL_SECRET` | **Wajib diganti** sebelum production |

## Menambah fitur baru

Contoh fitur `unit`: buat folder `features/unit/` berisi 5 file (lihat `features/todo/` sebagai contoh lengkap).

```kotlin
// UnitDto.kt
@Serializable
data class UnitRequest(val kodeUnit: String = "", val namaUnit: String = "")

@Serializable
data class UnitResponse(val id: String, val kodeUnit: String, val namaUnit: String)

fun UnitRequest.validate() = rules {
    check("kode_unit", kodeUnit.isNotBlank(), "wajib diisi")
    check("nama_unit", namaUnit.isNotBlank(), "wajib diisi")
}
```

```kotlin
// UnitService.kt
class UnitService(private val repository: UnitRepository) {
    suspend fun create(request: UnitRequest): UnitResponse {
        if (repository.existsByKodeUnit(request.kodeUnit)) {
            throw FieldValidationException(mapOf("kode_unit" to "kode unit sudah dipakai"))
        }
        return repository.create(request).toResponse()
    }

    suspend fun findById(id: String): UnitResponse =
        repository.findById(id)?.toResponse() ?: throw DataNotFoundException("Unit tidak ditemukan")

    private fun Unit.toResponse() = UnitResponse(id = id, kodeUnit = kodeUnit, namaUnit = namaUnit)
}
```

```kotlin
// UnitRoutes.kt
fun Application.unitModule() {
    val unitService by inject<UnitService>()

    routing {
        authenticate(JWT_AUTH) {
            route("/api/v1/units") {
                install(RequestValidation) { validate<UnitRequest> { it.validate() } }

                authorize(permissions = listOf("units:create")) {
                    post {
                        val request = call.receive<UnitRequest>()   // validasi otomatis jalan di sini
                        call.send(unitService.create(request).isSuccess(201, "Unit berhasil dibuat"))
                    }
                }
            }
        }
    }
}
```

Lalu daftarkan `single { UnitRepository() }` + `single { UnitService(get()) }` di `AppModule.kt`,
panggil `unitModule()` di `Application.kt`, dan tambahkan `UnitTable` ke `ALL_TABLES` di `DatabaseFactory.kt`.

## Ringkasan endpoint

Semua endpoint diawali `/api/v1`.

| Method | Endpoint | Akses | Keterangan |
|---|---|---|---|
| POST | `/auth/register` | Public | Daftar user baru |
| POST | `/auth/login` | Public | Login, access token di body, refresh token di cookie |
| PATCH | `/auth/refresh` | Cookie refresh token | Refresh access token |
| POST | `/auth/logout` | Public | Hapus cookie refresh token |
| GET | `/roles`, `/roles/{id}`, `/permissions` | `roles:read` | List / detail role & permission |
| PATCH | `/roles/{id}/permissions` | `roles:update` | Ganti permission role (`{ "permission_ids": [...] }`) |
| GET | `/users?page=&size=&search=`, `/users/{id}` | `users:read` | List / detail user |
| POST / PATCH / DELETE | `/users`, `/users/{id}` | role `admin` + `users:create/update/delete` | Kelola user (delete = soft delete) |
| POST | `/files/store` | Login | Upload file (multipart, field `files`) |
| GET | `/files/{id}`, `/files/{id}/signed-url` | Login | Isi file / signed URL sementara (2 menit) |
| GET | `/files/public/{id}`, `/files/public/{id}/data` | Public | File berstatus `public` |
| GET | `/files/signed/{id}` | Signature valid | Akses file lewat signed URL |
| GET / POST / PATCH / DELETE | `/todos?page=&size=&search=&is_completed=`, `/todos/{id}` | Login | Todo milik sendiri (`is_completed` hanya `true` / `false`) |

## Docker

```bash
docker compose up -d --build          # app + MySQL + Redis
docker compose run --rm app bin/seed  # seed data awal (sekali saja)
docker compose logs -f app
```

Image memakai JDK 25 (`eclipse-temurin:25-jre`), dibangun multi-stage, dan berjalan sebagai user non-root. Port di host
bisa diganti kalau bentrok: `APP_PORT` (3000), `MYSQL_PORT` (3307), `REDIS_HOST_PORT` (6380).

## Virtual threads

Semua request diproses di virtual thread (JDK 21+) lewat `configureVirtualThreads()`. Kode route, service, dan
repository boleh langsung memanggil I/O blocking (JDBC, file, Redis). Kerja berat CPU seperti hash bcrypt tetap di
`Dispatchers.Default` (lihat `PasswordHasher`).

## Testing

```bash
./gradlew test
```

Unit test service (repository di-mock) dan test route (service di-mock lewat Koin override), jadi tidak butuh MySQL/Redis.
Test per fitur ada di `src/test/kotlin/com/example/features/`.

> Fungsi test yang memakai `runBlocking` ditulis dengan block body (`fun x() { runBlocking { ... } }`).
> Bentuk `fun x() = runBlocking { ... }` yang mengembalikan nilai akan diam-diam dilewati JUnit.

## Logging

Log ditulis ke folder `logs/` (ganti lewat `LOG_DIR`), dirotasi harian / 10MB, disimpan 14 hari.

| File | Isi |
|---|---|
| `logs/http.log` | Detail tiap request & response (body request dibaca ulang lewat plugin `DoubleReceive`) |
| `logs/sql.log` | Tiap query SQL beserta nilai parameternya |
| `logs/app.log` | Log aplikasi (startup, error). Juga tampil di console |

Setiap request punya `request_id` (header `X-Request-Id`) yang muncul di `http.log` dan `sql.log`
(`grep <request_id> logs/*.log`). Password, token, header `Authorization`/`Cookie`, dan parameter SQL kolom `password`
disamarkan jadi `[REDACTED]` (daftar field di `core/logging/Redactor.kt`).

## Contoh pakai cache

```kotlin
class ContohService(private val cache: CacheService) {
    suspend fun stats(): String = cache.remember("stats", ttlSeconds = 60) { hitungStatsYangBerat() }
}
```

## Lisensi

[MIT](LICENSE)
