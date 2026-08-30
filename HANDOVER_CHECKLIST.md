# Handover Checklist - VLXD Anh Vu

## Required Client Information

- Real Zalo number or official Zalo OA URL.
- Production domain name.
- Final sender Gmail account for password reset emails.
- Final admin email and admin password policy.
- Final product price, stock, description, and image list.

## Server Configuration

- `SERVER_PORT`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `ADMIN_PASSWORD`
- `APP_BUSINESS_PHONE`
- `APP_ZALO_URL`

## Google Cloud Console

- Add local callback when testing: `http://localhost:8095/login/oauth2/code/google`
- Add production callback before launch: `https://your-domain.com/login/oauth2/code/google`
- Publish/verify OAuth consent screen if Google requires it for external users.

## Acceptance Test

- Open home page, product filter, product search, and responsive mobile view.
- Register with email/password.
- Login with email/password.
- Register with Google.
- Login with Google using an already registered account.
- Confirm a new Google email cannot login before registration.
- Request password reset email and complete reset.
- Submit an order request.
- Submit a quote request.
- Login as admin and verify admin menu appears.
- Confirm non-admin account cannot access `/admin`.
- Update order/quote status in admin.
- Update product price and stock in admin.
- Visit an invalid URL and confirm the branded error page appears.

## Notes For Customer

- Keep Gmail App Password and Google client secret private.
- Do not share admin password through screenshots or chat.
- If the app is moved to a new domain, update Google OAuth redirect URI.
