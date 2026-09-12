package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.ProductImage;
import com.anhvu.vlxd.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    public static final String DB_IMAGE_PREFIX = "/images/db/";
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_WIDTH = 900;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final ProductImageRepository productImageRepository;

    public boolean isUsable(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    /** Luu (ghi de) anh cho san pham; tra ve imagePath de gan vao Product. Anh duoc thu nho ve toi da 900px. */
    public String store(Long productId, MultipartFile file) throws IOException {
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IOException("Ảnh vượt quá 5MB.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(contentType)) {
            throw new IOException("Chỉ nhận ảnh JPG, PNG hoặc WEBP.");
        }
        byte[] original = file.getBytes();
        byte[] data = original;
        String storedType = contentType;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            if (image != null && image.getWidth() > MAX_WIDTH) {
                int height = (int) Math.round(image.getHeight() * (MAX_WIDTH / (double) image.getWidth()));
                BufferedImage resized = new BufferedImage(MAX_WIDTH, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, MAX_WIDTH, height, null);
                g.dispose();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(resized, "jpeg", out);
                data = out.toByteArray();
                storedType = "image/jpeg";
            }
        } catch (IOException | RuntimeException ignored) {
            // khong thu nho duoc (vd webp) -> giu nguyen file goc
        }
        productImageRepository.save(ProductImage.builder()
                .productId(productId)
                .contentType(storedType)
                .data(data)
                .updatedAt(LocalDateTime.now())
                .build());
        return DB_IMAGE_PREFIX + productId;
    }

    public Optional<ProductImage> find(Long productId) {
        return productImageRepository.findById(productId);
    }

    public void delete(Long productId) {
        productImageRepository.deleteById(productId);
    }
}
