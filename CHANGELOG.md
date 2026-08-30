# CHANGELOG - VLXD ANH VŨ

## [1.0.1] - 2026-08-23 - Production Ready Fixes

### ✅ Added
- **Thông tin công ty**
  - SĐT: 0866785645 (hiển thị trong business info & footer)
  - Zalo: https://zalo.me/84866785645 (link trong business info & floating button)
  - SĐT có link `tel:` để gọi trực tiếp trên mobile

### 🔐 Security
- **CSRF Protection**
  - Bật CSRF cho Admin panel (`/admin/**`)
  - Tắt CSRF cho public endpoints (tránh session conflicts)
  - Fix: lỗi "Cannot create session after response committed"
  - Tất cả forms đã dùng `th:action` để Thymeleaf tự thêm CSRF token

### 🐛 Fixed
- Session management conflict với large pages (index.html 88KB)
- CSRF token generation timing issue

### 📝 Documentation
- Thêm `START_PRODUCTION.md` - hướng dẫn khởi động chi tiết
- Thêm `CHANGELOG.md` - lịch sử thay đổi
- Cập nhật `AUDIT_REPORT.md` - báo cáo audit đầy đủ

### 🔧 Configuration
- `application.properties`: thêm default values cho phone & zalo
- `SecurityConfig.java`: CSRF chỉ bắt buộc cho admin routes
- `index.html`: thêm phone & zalo display

---

## [1.0.0] - 2026-08-22 - Initial Release

### Features
- ✅ Trang chủ với catalog 40 sản phẩm
- ✅ Đặt hàng online với VietQR
- ✅ Yêu cầu báo giá
- ✅ Calculator tính vật liệu
- ✅ Đăng nhập/đăng ký (Email + Google OAuth)
- ✅ Quên mật khẩu với OTP qua email
- ✅ Admin dashboard với charts
- ✅ Trang lỗi 404 branded
- ✅ Responsive design

### Tech Stack
- Java 17
- Spring Boot 3.3.2
- Spring Security 6.3.1
- Thymeleaf
- MySQL 8.4
- Bootstrap 5.3.3

### Database
- 3 users (1 admin, 2 users)
- 40 products active
- 6 categories
- 13 orders
- 1 quote request

---

## Known Issues

### v1.0.1
- None

### v1.0.0
- ❌ CSRF disabled (FIXED in 1.0.1)
- ❌ Missing phone & zalo info (FIXED in 1.0.1)
- ❌ Admin password chưa set (cần set qua env var)

---

## Upcoming

### v1.1.0 (Future)
- [ ] Product image upload
- [ ] Order tracking cho customer
- [ ] Email notification khi order status thay đổi
- [ ] Export orders/quotes to Excel
- [ ] Advanced inventory management

### v1.2.0 (Future)
- [ ] Multiple payment gateways
- [ ] Customer dashboard
- [ ] Invoice generation
- [ ] SMS notification (Twilio/Vonage)

---

**Maintained by:** Development Team  
**Last updated:** 2026-08-23
