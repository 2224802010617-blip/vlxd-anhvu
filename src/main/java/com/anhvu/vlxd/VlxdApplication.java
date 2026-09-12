package com.anhvu.vlxd;

import org.springframework.boot.SpringApplication;

import java.util.TimeZone;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VlxdApplication {

    public static void main(String[] args) {
        // Server Railway chay UTC: ep gio Viet Nam de gio don hang, "hom nay", thang nay dung
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(VlxdApplication.class, args);
    }
}
