# VLXD Anh Vu

Spring Boot 3 + Thymeleaf + MySQL website for Anh Vu construction materials.

## Tech
- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Security
- Thymeleaf
- MySQL
- Maven

## Run Local
1. Create database `vlxd_anhvu`.
2. Set database info in `src/main/resources/application.properties`.
3. Run the app with Maven or `start.ps1`.

## Google Login
1. Create an OAuth 2.0 Client in Google Cloud Console.
2. Add this redirect URI:
   `http://localhost:8095/login/oauth2/code/google`
3. Set these env vars:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
4. Keep the app on port `8095`, or update `SERVER_PORT` to match the redirect URI.
5. If credentials are missing, the login page will show a warning.

Run with a downloaded Google OAuth client JSON file:

```powershell
.\start-google.ps1 -CredentialsFile "D:\path\to\client_secret.json"
```

The credentials are loaded into the app process only and are not written to the project.

To also send password reset codes through Gmail, enable two-step verification for the
sender account, create a Gmail App Password, and run:

```powershell
.\start-google.ps1 `
  -CredentialsFile "D:\path\to\client_secret.json" `
  -ConfigureMail `
  -MailUsername "sender@gmail.com" `
  -Restart
```

The script asks for the Gmail App Password in a hidden prompt. Reset codes expire after
10 minutes and are invalidated after 5 incorrect attempts.

## Production Handover

Before handover, set these values on the server or pass them through `start-google.ps1`:

- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `ADMIN_PASSWORD`
- `APP_BUSINESS_PHONE`
- `APP_ZALO_URL`, for example `https://zalo.me/849xxxxxxxx`
- `SERVER_PORT`

Example:

```powershell
.\start-google.ps1 `
  -CredentialsFile "D:\path\to\client_secret.json" `
  -ConfigureMail `
  -MailUsername "sender@gmail.com" `
  -BusinessPhone "09xxxxxxxx" `
  -ZaloUrl "https://zalo.me/849xxxxxxxx" `
  -ConfigureAdminPassword `
  -Restart
```

For a real domain, add the production callback URL in Google Cloud Console:

```text
https://your-domain.com/login/oauth2/code/google
```

## Notes
- The app also supports local email/password login.
- Password reset currently uses a web reset-token flow.
- Admin pages manage orders, quotes, products, and inventory fields.
- A branded error page is available for 404/500 pages.
