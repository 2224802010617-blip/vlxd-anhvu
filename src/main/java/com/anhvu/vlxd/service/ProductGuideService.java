package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Bai huong dan ky thuat theo nhom hang cho trang chi tiet san pham:
 * gioi thieu, cach chon, dinh muc tham khao, cau hoi thuong gap.
 * Noi dung la kien thuc chung cua nganh VLXD, khong bia so lieu rieng cua cua hang.
 */
@Service
public class ProductGuideService {

    public record Norm(String item, String value) {}

    public record Faq(String question, String answer) {}

    public record Guide(String title, List<String> intro, List<String> tips, List<Norm> norms, List<Faq> faqs) {}

    public Guide forProduct(Product product) {
        String category = product.getCategory() == null ? "" : product.getCategory().getName().toLowerCase(Locale.ROOT);
        String name = product.getName();
        if (category.contains("gạch")) return brick(name);
        if (category.contains("xi măng")) return cement(name);
        if (category.contains("cát")) return sand(name);
        if (category.contains("đá")) return stone(name);
        if (category.contains("thép")) return steel(name);
        if (category.contains("dịch vụ")) return service(name);
        return generic(name);
    }

    private Guide brick(String name) {
        return new Guide(
                "Chọn và tính gạch cho công trình",
                List.of(
                        name + " thuộc nhóm gạch xây tại bãi Anh Vũ. Gạch nung (gạch ống, gạch thẻ, gạch tuynel) chịu lực tốt, bám vữa chắc, hợp cho tường nhà ở dân dụng. Gạch block xi măng nhẹ hơn, kích thước lớn nên xây nhanh, hợp cho tường rào, nhà xưởng, hạng mục phụ.",
                        "Gạch ống 8x8x18 là loại dùng nhiều nhất cho tường 10 và tường 20. Gạch thẻ đặc dùng ở chân tường, bậc thềm, chỗ cần chịu lực và chống thấm. Khi mua nên lấy dư 3 đến 5 phần trăm cho hao hụt khi cắt và vận chuyển."
                ),
                List.of(
                        "Gõ hai viên vào nhau nghe tiếng đanh là gạch nung đủ lửa; tiếng đục là gạch non, dễ vỡ và hút nước nhiều.",
                        "Chọn lô gạch đều màu, cạnh thẳng, không cong vênh để mạch vữa đều và tô ít tốn vữa.",
                        "Tưới ẩm gạch trước khi xây khoảng 30 phút để gạch không hút hết nước của vữa.",
                        "Xe ba gác chở khoảng 400 đến 500 viên gạch ống một chuyến, xe tải nhỏ chở 2.000 đến 3.000 viên. Cho biết đường vào công trình để bãi xếp xe phù hợp."
                ),
                List.of(
                        new Norm("Tường 10 (dày 100 mm), gạch ống 8x8x18", "khoảng 55 đến 60 viên/m²"),
                        new Norm("Tường 20 (dày 200 mm), gạch ống 8x8x18", "khoảng 110 đến 120 viên/m²"),
                        new Norm("Tường gạch thẻ 4x8x18 xây dày 100 mm", "khoảng 110 viên/m²"),
                        new Norm("Tường gạch block 19x19x39", "khoảng 12,5 viên/m²"),
                        new Norm("Hao hụt nên tính thêm", "3 đến 5 phần trăm")
                ),
                List.of(
                        new Faq("Mua vài trăm viên gạch có giao không?", "Có. Đơn nhỏ bãi giao bằng xe ba gác, vào được hẻm rộng từ 2 mét. Phí vận chuyển báo theo địa chỉ khi bạn gọi hoặc đặt trên web."),
                        new Faq("Gạch giao có đủ số không?", "Gạch xếp theo lớp trên xe, đếm được từng lớp khi xuống hàng. Bạn kiểm cùng tài xế trước khi ký nhận."),
                        new Faq("Gạch vỡ trong lúc chở tính sao?", "Vỡ do vận chuyển bãi bù ngay trong chuyến sau. Vỡ do cắt, thi công thì nằm trong phần hao hụt bạn đã tính dư.")
                )
        );
    }

    private Guide cement(String name) {
        return new Guide(
                "Chọn xi măng đúng việc, bảo quản đúng cách",
                List.of(
                        name + " nằm trong nhóm xi măng bao 50 kg tại bãi Anh Vũ. Xi măng PCB40 là loại phổ thông cho móng, cột, dầm, sàn và xây tô nhà ở. Xi măng trắng dùng cho chà ron, ốp lát, trang trí và chỗ cần bề mặt sáng màu. Xi măng, vữa chống cháy dùng cho hạng mục có yêu cầu phòng cháy.",
                        "Xi măng có chứng chỉ chất lượng của nhà máy (CO/CQ) đi kèm lô hàng, bạn có thể yêu cầu khi nhận. Bao mới sản xuất trong 60 ngày cho cường độ tốt nhất; để lâu, hút ẩm sẽ vón cục và giảm mác."
                ),
                List.of(
                        "Kết cấu chịu lực (móng, cột, dầm, sàn) dùng PCB40. Xây tô có thể dùng PCB30 hoặc PCB40 tùy thiết kế.",
                        "Kê bao cách nền ít nhất 30 cm, phủ bạt, không xếp quá 10 bao một chồng để bao dưới không bị nén vỡ.",
                        "Trộn đúng tỷ lệ theo mác thiết kế; thêm nước quá nhiều làm bê tông, vữa yếu và dễ nứt.",
                        "Nếu công trình kéo dài, đặt hàng theo từng đợt thay vì trữ nhiều, bãi giao lại trong ngày."
                ),
                List.of(
                        new Norm("1 m³ vữa xây mác 75", "khoảng 6 đến 7 bao"),
                        new Norm("Tô tường dày 15 mm", "khoảng 0,15 đến 0,2 bao/m²"),
                        new Norm("1 m³ bê tông mác 200", "khoảng 6 đến 7 bao"),
                        new Norm("1 m³ bê tông mác 250", "khoảng 7 đến 8 bao"),
                        new Norm("Lát gạch nền, lớp lót 20 mm", "khoảng 0,25 bao/m²")
                ),
                List.of(
                        new Faq("Xi măng có chứng chỉ CO/CQ không?", "Có. Bãi giữ chứng chỉ theo lô nhập từ nhà máy, gửi bản chụp qua Zalo hoặc kèm phiếu giao khi bạn yêu cầu."),
                        new Faq("Mua lẻ vài bao được không?", "Được. Vài bao xi măng bãi vẫn giao bằng xe ba gác quanh Bình Long, phí vận chuyển báo trước khi giao."),
                        new Faq("Giá xi măng có thay đổi không?", "Giá theo giá nhà máy, có thể đổi theo tháng. Giá trên trang là giá tại bãi ngày cập nhật; gọi để chốt giá cho đơn lớn.")
                )
        );
    }

    private Guide sand(String name) {
        return new Guide(
                "Chọn cát đúng hạng mục",
                List.of(
                        name + " thuộc nhóm cát xây dựng tại bãi Anh Vũ, bán theo mét khối, giao bằng xe ben hoặc xe ba gác. Cát xây và cát tô là cát hạt nhỏ, ít sạn, dễ trộn vữa và cho mặt tô mịn. Cát bê tông hạt lớn, sạch bùn, dùng trộn bê tông móng, sàn, dầm.",
                        "Cát sạch, ít tạp chất giúp vữa và bê tông đạt mác; cát lẫn nhiều bùn sét làm giảm cường độ và dễ nứt bề mặt. Khối lượng cát tính theo thùng xe, bạn có thể đo cùng tài xế khi nhận."
                ),
                List.of(
                        "Nắm một nắm cát bóp nhẹ rồi mở tay: cát rơi hết là cát sạch; dính thành nắm là nhiều bùn.",
                        "Tô tường dùng cát mịn đã sàng để mặt tô láng và đỡ tốn xi măng.",
                        "Đổ bê tông dùng cát hạt lớn (cát bê tông), không dùng cát mịn vì tốn xi măng và bê tông yếu.",
                        "Đổ cát lên tấm bạt hoặc nền sạch, tránh lẫn đất và rác khi xúc."
                ),
                List.of(
                        new Norm("1 m³ vữa xây, tô", "khoảng 1,1 m³ cát"),
                        new Norm("1 m³ bê tông mác 200 đến 250", "khoảng 0,45 đến 0,5 m³ cát"),
                        new Norm("Xây tường 10, gạch ống", "khoảng 0,02 m³ cát/m²"),
                        new Norm("Tô tường dày 15 mm", "khoảng 0,017 m³ cát/m²"),
                        new Norm("Một chuyến xe ba gác", "khoảng 1 đến 1,5 m³")
                ),
                List.of(
                        new Faq("Đặt nửa khối cát được không?", "Được. Đơn dưới 1 m³ bãi giao bằng xe ba gác hoặc đóng bao, phù hợp sửa nhà nhỏ."),
                        new Faq("Đo khối cát thế nào cho đúng?", "Đo dài, rộng, cao thùng xe rồi nhân lại, bạn đo cùng tài xế trước khi đổ. Cát ướt sẽ nặng hơn nhưng khối không đổi."),
                        new Faq("Cát xây và cát tô khác gì?", "Cát tô mịn hơn, đã sàng bỏ sạn để mặt tô láng. Cát xây hạt nhỏ đến vừa, dùng cho vữa xây gạch.")
                )
        );
    }

    private Guide stone(String name) {
        return new Guide(
                "Chọn cỡ đá theo hạng mục",
                List.of(
                        name + " thuộc nhóm đá xây dựng tại bãi Anh Vũ, bán theo mét khối, giao bằng xe ben. Đá 1x2 là cỡ chuẩn cho bê tông móng, cột, dầm, sàn. Đá 0x4 và đá mi dùng lót nền, làm lớp đệm, san lấp mặt bằng. Đá 4x6 và đá chẻ dùng cho móng đá hộc, kè, bờ bao.",
                        "Đá sạch, ít bụi bám giúp bê tông kết dính tốt. Trước khi đổ bê tông lớn nên tưới rửa đá nếu đá quá bụi."
                ),
                List.of(
                        "Bê tông kết cấu dùng đá 1x2; bê tông lót móng có thể dùng đá 4x6 để tiết kiệm.",
                        "Nền nhà, sân, đường nội bộ: rải đá 0x4 hoặc đá mi rồi lu lèn trước khi đổ bê tông.",
                        "Đá xanh có cường độ cao hơn đá đen, hợp cho công trình chịu lực lớn.",
                        "Đổ đá chỗ trống, cách xa cửa và cống để tránh chảy tràn khi mưa."
                ),
                List.of(
                        new Norm("1 m³ bê tông mác 200 đến 250", "khoảng 0,85 đến 0,9 m³ đá 1x2"),
                        new Norm("Lớp đệm nền dày 10 cm, đá 0x4", "khoảng 0,1 m³/m²"),
                        new Norm("Lớp đệm nền dày 10 cm, đá mi", "khoảng 0,1 m³/m²"),
                        new Norm("Móng đá hộc, đá chẻ", "khoảng 1,2 m³ đá cho 1 m³ móng"),
                        new Norm("Một chuyến xe ben nhỏ", "khoảng 3 đến 5 m³")
                ),
                List.of(
                        new Faq("Đá 1x2 và đá 1x1 khác nhau chỗ nào?", "Đá 1x1 nhỏ hơn, dùng cho bê tông mỏng, cấu kiện nhỏ hoặc trộn với đá 1x2. Bê tông kết cấu thông thường dùng đá 1x2."),
                        new Faq("Đường nhỏ xe ben có vào được không?", "Đường rộng từ 3 mét xe ben nhỏ vào được. Hẻm nhỏ hơn bãi chuyển bằng xe ba gác hoặc đá đóng bao, báo phí trước."),
                        new Faq("Đá có lẫn nhiều bụi không?", "Đá mi và 0x4 vốn có bụi đá, đó là bình thường. Đá 1x2, 4x6 sàng sạch; nếu cần đá rửa bạn báo khi đặt.")
                )
        );
    }

    private Guide steel(String name) {
        return new Guide(
                "Chọn thép và tính khối lượng",
                List.of(
                        name + " thuộc nhóm thép xây dựng tại bãi Anh Vũ, bán theo ki lô gam, có chứng chỉ chất lượng nhà máy theo lô. Thép cây gân (CB300, CB400) dùng cho móng, cột, dầm, sàn. Thép tròn trơn cỡ nhỏ dùng làm đai, thép chờ, buộc. Thép hình I, U, H dùng cho nhà xưởng, dầm đỡ, kết cấu tiền chế.",
                        "Khối lượng thép cây tính theo công thức: đường kính (mm) nhân đường kính chia 162, ra ki lô gam mỗi mét. Một cây thép dài 11,7 mét. Khi đặt hàng bạn có thể báo số cây, bãi quy ra ki lô gam để tính tiền."
                ),
                List.of(
                        "Thép gân dùng cho chịu lực chính; thép trơn D6, D8 dùng đai và thép phân bố.",
                        "Kiểm tra nhãn nhà máy dập trên thân cây thép và chứng chỉ CO/CQ đi kèm lô hàng.",
                        "Kê thép cách đất, che mưa; thép gỉ nhẹ bề mặt vẫn dùng được, gỉ sâu bong vảy thì không.",
                        "Tính thêm 2 đến 3 phần trăm hao hụt cho phần cắt, nối, uốn móc."
                ),
                List.of(
                        new Norm("Thép D6", "0,222 kg/m, cây 11,7 m khoảng 2,6 kg"),
                        new Norm("Thép D8", "0,395 kg/m, cây 11,7 m khoảng 4,6 kg"),
                        new Norm("Thép D10", "0,617 kg/m, cây 11,7 m khoảng 7,2 kg"),
                        new Norm("Thép D12", "0,888 kg/m, cây 11,7 m khoảng 10,4 kg"),
                        new Norm("Thép D16", "1,578 kg/m, cây 11,7 m khoảng 18,5 kg"),
                        new Norm("Thép D20", "2,466 kg/m, cây 11,7 m khoảng 28,9 kg")
                ),
                List.of(
                        new Faq("Giá thép tính theo cây hay theo kg?", "Bãi niêm yết theo ki lô gam. Bạn đặt theo cây thì bãi quy ra ki lô gam theo bảng trên để tính tiền, cân đối chiếu tại bãi."),
                        new Faq("Có cắt thép theo kích thước không?", "Cây thép giao nguyên 11,7 mét. Cần cắt ngắn để chở vào hẻm, bạn báo trước khi đặt."),
                        new Faq("Thép có chứng chỉ không?", "Có. Mỗi lô thép về bãi kèm chứng chỉ chất lượng của nhà máy, gửi bản chụp qua Zalo hoặc kèm phiếu giao.")
                )
        );
    }

    private Guide service(String name) {
        return new Guide(
                "Cách đặt dịch vụ và báo giá",
                List.of(
                        name + " là dịch vụ đi kèm vật liệu tại Anh Vũ, giá tính theo khối lượng, ca máy hoặc ngày thuê nên cần khảo sát trước khi báo giá.",
                        "Bạn gửi địa chỉ, diện tích hoặc khối lượng dự kiến và thời gian cần làm qua form báo giá hoặc Zalo. Bãi phản hồi trong ngày làm việc với đơn giá và phương án xe máy phù hợp đường vào."
                ),
                List.of(
                        "San lấp: cho biết diện tích, độ cao cần nâng và loại vật liệu san lấp muốn dùng.",
                        "Giàn giáo: báo số bộ, thời gian thuê và địa chỉ giao nhận.",
                        "Đào móng, đào hầm: gửi bản vẽ hoặc kích thước hố đào để chọn máy đúng cỡ."
                ),
                List.of(
                        new Norm("San lấp bằng đá mi, đá 0x4", "tính theo m³ đã lu lèn"),
                        new Norm("Thuê giàn giáo", "tính theo bộ và số ngày"),
                        new Norm("Đào móng, đào ao", "tính theo ca máy hoặc m³ đất")
                ),
                List.of(
                        new Faq("Khảo sát có tốn phí không?", "Khảo sát trong khu vực Bình Long không tính phí. Báo giá gửi sau khảo sát trong ngày làm việc."),
                        new Faq("Có kèm vật liệu không?", "Có. Dịch vụ san lấp thường kèm đá mi, đá 0x4, cát san lấp từ bãi nên giá trọn gói tốt hơn mua rời.")
                )
        );
    }

    private Guide generic(String name) {
        return new Guide(
                "Thông tin mua hàng",
                List.of(name + " có sẵn tại bãi Anh Vũ, giao tận công trình quanh Bình Long, Đồng Nai. Gọi hotline hoặc đặt trên web, bãi xác nhận lại số lượng và giờ giao."),
                List.of("Cho biết đường vào công trình để bãi xếp xe phù hợp.", "Kiểm hàng cùng tài xế khi nhận, thanh toán tiền mặt hoặc chuyển khoản theo mã đơn."),
                List.of(),
                List.of(new Faq("Giao hàng bao lâu?", "Đơn đặt trước 15 giờ thường giao trong ngày quanh Bình Long. Đơn lớn hẹn giờ theo xe."))
        );
    }
}
