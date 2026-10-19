package com.anhvu.vlxd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Render ban free cho web "ngu" sau 15 phut khong co truy cap, khach vao phai doi ~50 giay.
 * Lich chay GitHub Actions (keep-alive.yml) bi GitHub tri hoan hang gio nen khong du;
 * web tu goi dia chi cong khai cua minh moi 10 phut de luon thuc.
 * Chi chay khi co RENDER_EXTERNAL_URL (Render tu cap) hoac KEEP_ALIVE_URL.
 */
@Slf4j
@Component
public class KeepAliveScheduler {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    @Value("${KEEP_ALIVE_URL:${RENDER_EXTERNAL_URL:}}")
    private String baseUrl;

    @Scheduled(initialDelay = 60_000, fixedDelay = 600_000)
    public void ping() {
        if (baseUrl == null || baseUrl.isBlank()) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/favicon.ico"))
                    .timeout(Duration.ofSeconds(30)).GET().build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            log.debug("Keep-alive ping loi: {}", ex.getMessage());
        }
    }
}
