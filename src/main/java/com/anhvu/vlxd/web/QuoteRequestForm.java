package com.anhvu.vlxd.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuoteRequestForm {

    @NotBlank(message = "Vui lòng nhập tên khách hàng.")
    @Size(max = 120, message = "Tên khách hàng không được vượt quá 120 ký tự.")
    private String customerName = "";

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 20, message = "Số điện thoại không hợp lệ.")
    private String phone = "";

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String address = "";

    // Khong bat buoc: co email thi nhan duoc bao gia qua mail
    @Email(message = "Email chưa đúng định dạng.")
    @Size(max = 160, message = "Email không được vượt quá 160 ký tự.")
    private String email = "";

    @NotBlank(message = "Vui lòng nhập nội dung báo giá.")
    private String content = "";

    // Khach tu tick moi gui tin khuyen mai (Nghi dinh 91/2020)
    private boolean marketingConsent;
}
