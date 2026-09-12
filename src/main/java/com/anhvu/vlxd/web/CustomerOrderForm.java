package com.anhvu.vlxd.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CustomerOrderForm {

    @NotBlank(message = "Vui lòng nhập tên khách hàng.")
    @Size(max = 120, message = "Tên khách hàng không được vượt quá 120 ký tự.")
    private String customerName = "";

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 20, message = "Số điện thoại không hợp lệ.")
    private String phone = "";

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng.")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String address = "";

    // Khong bat buoc: co email thi gui xac nhan don cho khach
    @Email(message = "Email chưa đúng định dạng.")
    @Size(max = 160, message = "Email không được vượt quá 160 ký tự.")
    private String email = "";

    // Mot don co the gom nhieu mat hang (cat + xi mang + da... trong cung mot lan giao)
    @Valid
    @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm.")
    @Size(max = 10, message = "Mỗi đơn tối đa 10 mặt hàng, vui lòng tách đơn hoặc gọi hotline.")
    private List<OrderItem> items = new ArrayList<>(List.of(new OrderItem()));

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán.")
    @Size(max = 40, message = "Phương thức thanh toán không hợp lệ.")
    private String paymentMethod = "";

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự.")
    private String note = "";

    // Khach tu tick moi gui tin khuyen mai (Nghi dinh 91/2020)
    private boolean marketingConsent;

    @Getter
    @Setter
    public static class OrderItem {

        @NotBlank(message = "Vui lòng chọn sản phẩm.")
        @Size(max = 180, message = "Tên sản phẩm không hợp lệ.")
        private String productName = "";

        @NotNull(message = "Vui lòng nhập số lượng.")
        @DecimalMin(value = "0.01", message = "Số lượng phải lớn hơn 0.")
        private BigDecimal quantity;
    }
}
