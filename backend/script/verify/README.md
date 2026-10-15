# Simple Boot verify scripts

Automated acceptance for de-branding and scaffold smoke checks.

## Usage

```powershell
cd simple-boot-new/script/verify

# Static only
.\01-brand-scan.ps1
.\02-structure.ps1

# Full gate (build + API). Start simple-server (local) first for API.
.\verify-all.ps1

# Without API / without rebuild
.\verify-all.ps1 -SkipApi
.\verify-all.ps1 -SkipBuild -SkipApi
```

Backend folder defaults to `simple-boot-new`. Override with `-BootDir` if needed.

## Environment (API smoke)

| Variable | Default |
|----------|---------|
| `VERIFY_BASE_URL` | `http://127.0.0.1:48080` |
| `VERIFY_USERNAME` | `admin` |
| `VERIFY_PASSWORD` | `admin123` |
| `VERIFY_TENANT_ID` | `1` |

Reports are written under `out/` (`brand-report.txt`, `structure-report.txt`, `build-report.txt`, `api-smoke-report.txt`).
