package com.anhvu.vlxd.web;

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

    @NotBlank(message = "Vui lòng nhập nội dung báo giá.")
    private String content = "";
}
