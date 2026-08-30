package com.anhvu.vlxd.config;

import com.anhvu.vlxd.entity.Category;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.repository.CategoryRepository;
import com.anhvu.vlxd.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String STEEL_IMAGE = "/images/products/thep-cay-d10.jpg";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        fixSteelImages();

        if (productRepository.count() >= 34 && !hasBrokenSampleData() && !hasMissingImages() && !hasOldMaterialImages()) {
            return;
        }

        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category gach = categoryRepository.save(Category.builder().name("G\u1EA1ch").build());
        Category xiMang = categoryRepository.save(Category.builder().name("Xi m\u0103ng").build());
        Category cat = categoryRepository.save(Category.builder().name("C\u00E1t x\u00E2y d\u1EF1ng").build());
        Category da = categoryRepository.save(Category.builder().name("\u0110\u00E1 x\u00E2y d\u1EF1ng").build());
        Category thep = categoryRepository.save(Category.builder().name("Th\u00E9p").build());
        Category dichVu = categoryRepository.save(Category.builder().name("D\u1ECBch v\u1EE5").build());

        List<Product> seedProducts = List.of(
                product("G\u1EA1ch \u1ED1ng 8x8x18", "G\u1EA1ch x\u00E2y t\u01B0\u1EDDng ph\u1ED5 bi\u1EBFn cho c\u00F4ng tr\u00ECnh d\u00E2n d\u1EE5ng.", "1200", 50000, "vi\u00EAn", "/images/materials/brick.jpg", "55", gach),
                product("G\u1EA1ch Tuynel", "G\u1EA1ch nung d\u00F9ng cho t\u01B0\u1EDDng bao, t\u01B0\u1EDDng ng\u0103n v\u00E0 h\u1EA1ng m\u1EE5c x\u00E2y th\u00F4.", "1300", 42000, "vi\u00EAn", "/images/materials/brick.jpg", "55", gach),
                product("G\u1EA1ch block", "G\u1EA1ch block ch\u1ECBu l\u1EF1c, ph\u00F9 h\u1EE3p s\u00E2n b\u00E3i, t\u01B0\u1EDDng r\u00E0o v\u00E0 c\u00F4ng tr\u00ECnh ph\u1EE5.", "6500", 18000, "vi\u00EAn", "/images/materials/brick.jpg", "12", gach),

                product("C\u00E1t T\u00E2y Ninh", "C\u00E1t h\u1EA1t v\u00E0ng, ph\u00F9 h\u1EE3p san l\u1EA5p v\u00E0 tr\u1ED9n b\u00EA t\u00F4ng theo nhu c\u1EA7u.", "430000", 260, "m3", "/images/materials/sand.jpg", "0.04", cat),
                product("C\u00E1t v\u00E0ng mi", "C\u00E1t v\u00E0ng mi cho x\u00E2y t\u00F4, c\u00E1n n\u1EC1n v\u00E0 h\u1EA1ng m\u1EE5c ho\u00E0n thi\u1EC7n.", "300000", 320, "m3", "/images/materials/sand.jpg", "0.04", cat),
                product("C\u00E1t nghi\u1EC1n 2.8", "C\u00E1t nh\u00E2n t\u1EA1o nghi\u1EC1n 2.8, k\u00EDch c\u1EE1 \u0111\u1ED3ng \u0111\u1EC1u cho thi c\u00F4ng.", "220000", 260, "m3", "/images/materials/sand.jpg", "0.04", cat),
                product("C\u00E1t b\u00EA t\u00F4ng v\u00E0ng", "C\u00E1t v\u00E0ng h\u1EA1t s\u1EA1ch d\u00F9ng tr\u1ED9n b\u00EA t\u00F4ng v\u00E0 h\u1EA1ng m\u1EE5c ch\u1ECBu l\u1EF1c.", "350000", 230, "m3", "/images/materials/sand.jpg", "0.04", cat),
                product("C\u00E1t x\u00E2y d\u1EF1ng", "C\u00E1t x\u00E2y t\u00F4 th\u00F4ng d\u1EE5ng, giao theo xe cho c\u00F4ng tr\u00ECnh.", "200000", 420, "m3", "/images/materials/sand.jpg", "0.04", cat),
                product("C\u00E1t b\u00EA t\u00F4ng h\u1EA1t l\u1EDBn", "C\u00E1t h\u1EA1t l\u1EDBn cho tr\u1ED9n b\u00EA t\u00F4ng, n\u1EC1n m\u00F3ng v\u00E0 c\u1EA5u ki\u1EC7n.", "350000", 180, "m3", "/images/materials/sand.jpg", "0.04", cat),
                product("C\u00E1t tr\u1EAFng", "C\u00E1t tr\u1EAFng cho trang tr\u00ED, l\u00F3t n\u1EC1n, c\u00F4ng tr\u00ECnh \u0111\u1EB7c th\u00F9.", "500000", 120, "m3", "/images/materials/sand.jpg", "0.04", cat),

                product("\u0110\u00E1 1x1", "\u0110\u00E1 xanh 1x1 cho b\u00EA t\u00F4ng, s\u00E0n n\u1EC1n v\u00E0 h\u1EA1ng m\u1EE5c d\u00E2n d\u1EE5ng.", "300000", 180, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 ch\u1EBB", "\u0110\u00E1 ch\u1EBB d\u00F9ng x\u00E2y m\u00F3ng, k\u00E8, b\u00F3 v\u1EC9a v\u00E0 c\u00F4ng tr\u00ECnh ph\u1EE5.", "158000", 260, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 5x7 xanh", "\u0110\u00E1 5x7 cho m\u00F3ng, \u0111\u01B0\u1EDDng n\u1ED9i b\u1ED9 v\u00E0 h\u1EA1ng m\u1EE5c san l\u1EA5p.", "380000", 210, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 1x2 \u0111en", "\u0110\u00E1 1x2 \u0111en ph\u1EE5c v\u1EE5 tr\u1ED9n b\u00EA t\u00F4ng v\u00E0 c\u00E1c h\u1EA1ng m\u1EE5c ch\u1ECBu l\u1EF1c.", "270000", 250, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 xanh Bi\u00EAn H\u00F2a", "\u0110\u00E1 xanh Bi\u00EAn H\u00F2a, h\u1EA1t ch\u1EAFc, ph\u00F9 h\u1EE3p c\u00F4ng tr\u00ECnh quy m\u00F4 l\u1EDBn.", "400000", 200, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 0x4 xanh", "\u0110\u00E1 0x4 xanh cho san n\u1EC1n, l\u00F3t m\u00F3ng v\u00E0 l\u00E0m \u0111\u01B0\u1EDDng.", "360000", 260, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 mi s\u00E0ng", "\u0110\u00E1 mi s\u00E0ng d\u00F9ng l\u00F3t n\u1EC1n, tr\u1ED9n v\u1EEFa v\u00E0 c\u00E1c h\u1EA1ng m\u1EE5c nh\u1ECF.", "230000", 300, "m3", "/images/materials/warehouse.jpg", "0.08", da),
                product("\u0110\u00E1 4x6 \u0111en", "\u0110\u00E1 4x6 \u0111en cho m\u00F3ng, \u0111\u01B0\u1EDDng v\u00E0 h\u1EA1ng m\u1EE5c c\u1EA7n \u0111\u1ED9 ch\u1ECBu t\u1EA3i.", "280000", 190, "m3", "/images/materials/warehouse.jpg", "0.08", da),

                product("Th\u00E9p c\u00E2y D10", "Th\u00E9p x\u00E2y d\u1EF1ng cho d\u1EA7m, c\u1ED9t, s\u00E0n.", "16500", 8000, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p tr\u00F2n tr\u01A1n", "Th\u00E9p tr\u00F2n tr\u01A1n d\u00F9ng gia c\u00F4ng, bu\u1ED9c, khung ph\u1EE5 v\u00E0 k\u1EBFt c\u1EA5u nh\u1EB9.", "16000", 9000, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p c\u00E2y g\u00E2n", "Th\u00E9p c\u00E2y g\u00E2n cho k\u1EBFt c\u1EA5u b\u00EA t\u00F4ng c\u1ED1t th\u00E9p.", "16500", 10000, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p h\u1ED9p oval", "Th\u00E9p h\u1ED9p oval cho c\u01A1 kh\u00ED, m\u00E1i che, lan can v\u00E0 trang tr\u00ED.", "16500", 5200, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p h\u00ECnh I300", "Th\u00E9p h\u00ECnh I300 cho nh\u00E0 x\u01B0\u1EDFng, k\u1EBFt c\u1EA5u th\u00E9p v\u00E0 d\u1EA7m ch\u1ECBu l\u1EF1c.", "18500", 2400, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p h\u00ECnh U200", "Th\u00E9p h\u00ECnh U200 cho khung, gi\u1EB1ng, x\u00E0 g\u1ED3 v\u00E0 k\u1EBFt c\u1EA5u ph\u1EE5.", "18000", 2800, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p h\u00ECnh I250", "Th\u00E9p h\u00ECnh I250 cho c\u00F4ng tr\u00ECnh c\u01A1 kh\u00ED v\u00E0 nh\u00E0 ti\u1EC1n ch\u1EBF.", "19000", 2300, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p h\u00ECnh H150", "Th\u00E9p h\u00ECnh H150 cho c\u1ED9t, d\u1EA7m, khung ch\u1ECBu l\u1EF1c.", "19500", 2100, "kg", "/images/materials/steel.jpg", "3.20", thep),
                product("Th\u00E9p h\u00ECnh I100", "Th\u00E9p h\u00ECnh I100 cho h\u1EA1ng m\u1EE5c v\u1EEBa v\u00E0 nh\u1ECF.", "17500", 3100, "kg", "/images/materials/steel.jpg", "3.20", thep),

                product("Xi m\u0103ng PCB40", "Xi m\u0103ng ch\u1EA5t l\u01B0\u1EE3ng cao cho k\u1EBFt c\u1EA5u d\u00E2n d\u1EE5ng.", "85000", 1200, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng tr\u1EAFng Th\u00E1i B\u00ECnh", "Xi m\u0103ng tr\u1EAFng cho trang tr\u00ED, ron g\u1EA1ch, t\u01B0\u1EDDng m\u1EF9 thu\u1EADt.", "170000", 450, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng tr\u1EAFng AALBORG WHITE", "Xi m\u0103ng tr\u1EAFng AALBORG WHITE d\u00F9ng cho h\u1EA1ng m\u1EE5c ho\u00E0n thi\u1EC7n cao c\u1EA5p.", "170000", 360, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng tr\u1EAFng SCG PCW 50.i", "Xi m\u0103ng tr\u1EAFng SCG PCW 50.i, m\u00E0u s\u00E1ng, \u0111\u1ED9 m\u1ECB n t\u1ED1t.", "155000", 380, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("V\u1EEFa ch\u1ED1ng ch\u00E1y", "V\u1EEFa ch\u1ED1ng ch\u00E1y cho khe k\u1EF9 thu\u1EADt, c\u1EEDa ch\u1ED1ng ch\u00E1y v\u00E0 h\u1EA1ng m\u1EE5c an to\u00E0n.", "280000", 180, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng ch\u1ED1ng ch\u00E1y", "Xi m\u0103ng chuy\u00EAn d\u1EE5ng cho h\u1EA1ng m\u1EE5c y\u00EAu c\u1EA7u kh\u00E1ng nhi\u1EC7t.", "160000", 220, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng Nghi S\u01A1n Premium", "Xi m\u0103ng Nghi S\u01A1n Premium cho x\u00E2y t\u00F4 v\u00E0 k\u1EBFt c\u1EA5u th\u00F4ng d\u1EE5ng.", "83000", 1100, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng Ho\u00E0ng Th\u1EA1ch", "Xi m\u0103ng Ho\u00E0ng Th\u1EA1ch ch\u00EDnh h\u00E3ng, ph\u00F9 h\u1EE3p x\u00E2y t\u00F4 d\u00E2n d\u1EE5ng.", "70000", 900, "bao", "/images/materials/cement.jpg", "0.35", xiMang),
                product("Xi m\u0103ng TOP", "Xi m\u0103ng TOP \u0111a d\u1EE5ng cho c\u00F4ng tr\u00ECnh d\u00E2n d\u1EE5ng v\u00E0 s\u1EEDa ch\u1EEFa.", "70000", 850, "bao", "/images/materials/cement.jpg", "0.35", xiMang),

                product("San l\u1EA5p m\u1EB7t b\u1EB1ng", "D\u1ECBch v\u1EE5 san l\u1EA5p, chu\u1EA9n b\u1ECB n\u1EC1n, ph\u1EE5c v\u1EE5 c\u00F4ng tr\u00ECnh d\u00E2n d\u1EE5ng.", "0", 1, "b\u00E1o gi\u00E1", "/images/materials/warehouse.jpg", "1", dichVu),
                product("Cho thu\u00EA gi\u00E0n gi\u00E1o", "Cho thu\u00EA gi\u00E0n gi\u00E1o thi c\u00F4ng theo ng\u00E0y, theo th\u00E1ng ho\u1EB7c theo c\u00F4ng tr\u00ECnh.", "0", 1, "b\u00E1o gi\u00E1", "/images/materials/steel.jpg", "1", dichVu),
                product("\u0110\u00E0o m\u00F3ng, \u0111\u00E0o h\u1EA7m, \u0111\u00E0o ao", "D\u1ECBch v\u1EE5 \u0111\u00E0o m\u00F3ng, \u0111\u00E0o h\u1EA7m, \u0111\u00E0o ao b\u1EB1ng m\u00E1y c\u00F4ng tr\u00ECnh.", "0", 1, "b\u00E1o gi\u00E1", "/images/materials/hero-construction.jpg", "1", dichVu),
                product("C\u00E1t \u0111\u00E1 \u0111\u00F3ng bao", "C\u00E1t \u0111\u00E1 \u0111\u00F3ng bao g\u1ECDn, ph\u00F9 h\u1EE3p s\u1EEDa ch\u1EEFa nh\u1ECF v\u00E0 c\u00F4ng tr\u00ECnh h\u1EB9p.", "0", 1, "b\u00E1o gi\u00E1", "/images/materials/sand.jpg", "1", dichVu)
        );

        productRepository.saveAll(seedProducts);
    }

    private boolean hasBrokenSampleData() {
        return productRepository.findAll().stream()
                .anyMatch(product -> isBroken(product.getName()) || isBroken(product.getDescription()))
                || categoryRepository.findAll().stream()
                .anyMatch(category -> isBroken(category.getName()));
    }

    private boolean hasMissingImages() {
        return productRepository.findAll().stream()
                .anyMatch(product -> product.getImagePath() == null || product.getImagePath().isBlank());
    }

    private boolean hasOldMaterialImages() {
        return productRepository.findAll().stream()
                .anyMatch(product -> product.getImagePath() != null
                        && product.getImagePath().startsWith("/images/materials/")
                        && isSteelName(product.getName()));
    }

    private boolean isBroken(String value) {
        return value != null
                && (value.contains("\u00C3")
                || value.contains("\u00C2")
                || value.contains("\u00C4")
                || value.contains("\u00C5")
                || value.contains("\u00BA")
                || value.contains("\u00BB")
                || value.contains("\u00A1")
                || value.contains("\u00AD")
                || value.contains("\u00B1"));
    }

    private Product product(String name,
                            String description,
                            String price,
                            Integer stockQuantity,
                            String unit,
                            String imagePath,
                            String consumptionPerM2,
                            Category category) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stockQuantity(stockQuantity)
                .unit(unit)
                .imagePath(productImagePath(name))
                .consumptionPerM2(new BigDecimal(consumptionPerM2))
                .category(category)
                .active(true)
                .build();
    }

    private String productImagePath(String name) {
        String normalizedName = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd');
        if ("thep-cay-d10".equals(normalizedName)) {
            return STEEL_IMAGE;
        }

        String slug = normalizedName
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return "/images/products/" + slug + ".jpg";
    }

    private void fixSteelImages() {
        List<Product> products = productRepository.findAll();
        boolean changed = false;
        for (Product product : products) {
            if (product.getName() == null) {
                continue;
            }
            String desiredImage = productImagePath(product.getName());
            if (!desiredImage.equals(product.getImagePath())) {
                product.setImagePath(desiredImage);
                changed = true;
            }
        }
        if (changed) {
            productRepository.saveAll(products);
        }
    }

    private boolean isSteelName(String name) {
        if (name == null) {
            return false;
        }
        String normalizedName = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd');
        return normalizedName.startsWith("thep");
    }
}
