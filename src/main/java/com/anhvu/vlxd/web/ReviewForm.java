package com.anhvu.vlxd.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {

    @NotBlank(message = "Vui lòng nhập tên của bạn.")
    @Size(max = 120, message = "Tên không được vượt quá 120 ký tự.")
    private String customerName = "";

    @Size(max = 120, message = "Phần mô tả không được vượt quá 120 ký tự.")
    private String customerRole = "";

    @Min(value = 1, message = "Vui lòng chọn số sao.")
    @Max(value = 5, message = "Vui lòng chọn số sao.")
    private int rating = 5;

    @NotBlank(message = "Vui lòng viết vài dòng nhận xét.")
    @Size(min = 10, max = 1000, message = "Nhận xét từ 10 đến 1000 ký tự.")
    private String content = "";

    /** Bay spam: nguoi that khong thay o nay, bot dien vao thi bo qua. */
    private String website = "";
}
