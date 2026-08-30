package com.anhvu.vlxd.service;

import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    public String getResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Chào anh/chị, em có thể giúp gì cho anh/chị ạ?";
        }

        String lowerInput = message.toLowerCase().trim();

        if (lowerInput.contains("xin chào") || lowerInput.contains("hello") || lowerInput.contains("chào")) {
            return "Chào anh/chị! Chào mừng anh/chị đến với VLXD Anh Vũ. Em là trợ lý AI có thể tư vấn giá vật liệu xây dựng. Anh/chị cần hỏi gì ạ?";
        }

        if (lowerInput.contains("nhà ngói") || lowerInput.contains("cấp 4") || lowerInput.contains("xây nhà")) {
            return "Dạ, để xây nhà ngói hoặc nhà ở cơ bản, anh/chị thường sẽ cần các vật tư sau bên em đang cung cấp:\n" +
                   "- **Cát xây tô & Cát đổ bê tông** (làm móng, trát tường)\n" +
                   "- **Đá 1x2 & Đá 4x6** (đổ móng bê tông)\n" +
                   "- **Xi măng** (PCB40 đổ móng, PCB30 xây tô)\n" +
                   "- **Thép cây & Thép cuộn** (đổ cột trụ, móng)\n" +
                   "- **Gạch ống 8x8x18** (xây tường bao)\n\n" +
                   "Anh/chị cần em báo giá hay tính số lượng cụ thể cho mặt hàng nào không ạ?";
        }
        
        if (lowerInput.contains("giá") || lowerInput.contains("bao nhiêu")) {
            if (lowerInput.contains("cát")) {
                return "Dạ giá cát hiện tại bên em như sau (tham khảo):\n- Cát xây tô: 250.000đ/khối\n- Cát bê tông vàng: 350.000đ/khối\n- Cát san lấp: 180.000đ/khối.\n\n*Giá có thể thay đổi theo cự ly vận chuyển, anh/chị lấy số lượng bao nhiêu ạ?*";
            }
            if (lowerInput.contains("xi măng")) {
                return "Dạ xi măng bên em có các loại:\n- Xi măng Hà Tiên Đa Dụng: 90.000đ/bao\n- Xi măng Insee (Sao Mai): 92.000đ/bao\n- Xi măng Nghi Sơn: 88.000đ/bao.\n\n*Giá đã bao gồm VAT và vận chuyển tận chân công trình (áp dụng >100 bao).*";
            }
            if (lowerInput.contains("đá")) {
                return "Dạ giá đá xanh Biên Hòa:\n- Đá 1x2: 320.000đ/khối\n- Đá 4x6: 280.000đ/khối\n- Đá mi bụi: 250.000đ/khối.\n\n*Cần số lượng lớn anh/chị vui lòng để lại SĐT hoặc bấm 'Yêu cầu báo giá' nhé.*";
            }
            if (lowerInput.contains("gạch")) {
                return "Dạ gạch Tuynel Bình Dương chuẩn:\n- Gạch ống 4 lỗ (8x8x18): 1.100đ/viên\n- Gạch đinh 2 lỗ: 1.150đ/viên\n- Gạch block xi măng: 4.500đ/viên.\n\n*Bên em giao bằng xe cẩu và xe ba gác vào được tận hẻm nhỏ.*";
            }
            if (lowerInput.contains("thép") || lowerInput.contains("sắt")) {
                return "Dạ sắt thép Pomina, Miền Nam, Hòa Phát đang có giá từ 15.500đ - 16.500đ/kg tùy loại và thời điểm.\nDo sắt thép biến động giá hàng ngày, anh/chị cần quy cách nào (Phi 6, 8, hay 10...) để em báo giá chính xác nhất ạ?";
            }
            
            return "Dạ anh/chị cần hỏi giá cho vật tư nào cụ thể ạ (Cát, Đá, Xi Măng, Gạch, Thép)?";
        }

        if (lowerInput.contains("liên hệ") || lowerInput.contains("sđt") || lowerInput.contains("số điện thoại") || lowerInput.contains("địa chỉ")) {
            return "Dạ anh/chị có thể liên hệ trực tiếp qua số Hotline/Zalo: 0866785645 hoặc ghé cửa hàng tại: 617 Nguyễn Huệ, P. Bình Long, TP. Đồng Nai ạ.";
        }
        
        if (lowerInput.contains("vận chuyển") || lowerInput.contains("giao hàng") || lowerInput.contains("xe")) {
            return "Bên em có đội xe ben (1 khối - 15 khối), xe cẩu, và cả xe ba gác để phục vụ giao hàng vào tận hẻm nhỏ luôn ạ.";
        }

        return "Dạ em chưa hiểu rõ ý của anh/chị. Anh/chị có thể hỏi giá cụ thể (VD: 'Báo giá gạch', 'Giá xi măng') hoặc hỏi định mức (VD: 'Xây 100m2 cần bao nhiêu gạch').";
    }
}