package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.service.ProductService;
import com.anhvu.vlxd.web.CustomerOrderForm;
import com.anhvu.vlxd.web.QuoteRequestForm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Value("${app.business.company-name}")
    private String companyName;
    @Value("${app.business.bank-account}")
    private String bankAccount;
    @Value("${app.business.bank-name}")
    private String bankName;
    @Value("${app.business.email}")
    private String email;
    @Value("${app.business.phone:}")
    private String phone;
    @Value("${app.business.zalo-url:}")
    private String zaloUrl;

    @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
    public String index(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false, defaultValue = "relevance") String sort,
                        Model model) {
        // Tim kiem / loc danh muc deu chuyen sang trang san pham rieng
        if ((keyword != null && !keyword.isBlank()) || (category != null && !category.isBlank())) {
            StringBuilder target = new StringBuilder("/san-pham?sort=").append(sort == null ? "relevance" : sort);
            if (keyword != null && !keyword.isBlank()) {
                target.append("&keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            }
            if (category != null && !category.isBlank()) {
                target.append("&category=").append(URLEncoder.encode(category, StandardCharsets.UTF_8));
            }
            return "redirect:" + target;
        }
        List<Product> products = sortProducts(productService.searchProducts(null, null), "relevance");
        addCommonAttributes(model);
        model.addAttribute("products", products);
        model.addAttribute("featuredProducts", products.stream().limit(8).toList());
        return "index";
    }

    @GetMapping(value = "/san-pham", produces = "text/html;charset=UTF-8")
    public String productsPage(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false, defaultValue = "relevance") String sort,
                               Model model) {
        List<Product> products = sortProducts(productService.searchProducts(keyword, category), sort);
        addCommonAttributes(model);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("sort", sort == null ? "relevance" : sort);
        model.addAttribute("smartHints", buildSmartHints(keyword, category));
        return "products";
    }

    @GetMapping(value = "/gioi-thieu", produces = "text/html;charset=UTF-8")
    public String aboutPage(Model model) {
        addCommonAttributes(model);
        return "about";
    }

    @GetMapping(value = "/dat-hang", produces = "text/html;charset=UTF-8")
    public String orderPage(@RequestParam(required = false) String product,
                            @RequestParam(defaultValue = "false") boolean orderSuccess,
                            Model model) {
        addCommonAttributes(model);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("selectedProduct", product == null ? "" : product);
        model.addAttribute("orderForm", prefillOrderForm(product));
        model.addAttribute("quoteForm", new QuoteRequestForm());
        model.addAttribute("orderSuccess", orderSuccess);
        return "order";
    }

    @GetMapping(value = "/san-pham/{id}", produces = "text/html;charset=UTF-8")
    public String productDetail(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        Product product = productService.getActiveProductById(id);
        if (product == null) {
            return "redirect:/san-pham";
        }
        addCommonAttributes(model);
        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", productService.getRelatedProducts(product, 4));
        return "product-detail";
    }

    @PostMapping(value = "/calculate", produces = "text/html;charset=UTF-8")
    public String calculate(@RequestParam BigDecimal areaM2,
                            @RequestParam BigDecimal consumptionPerM2,
                            Model model) {
        addCommonAttributes(model);
        List<Product> allProducts = productService.getAllActiveProducts();
        model.addAttribute("products", allProducts);
        model.addAttribute("calculatedQuantity", productService.calculateMaterialQuantity(areaM2, consumptionPerM2));
        model.addAttribute("areaM2", areaM2);
        model.addAttribute("consumptionPerM2", consumptionPerM2);
        model.addAttribute("keyword", "");
        model.addAttribute("category", "");
        model.addAttribute("sort", "relevance");
        model.addAttribute("quoteSuccess", false);
        model.addAttribute("orderSuccess", false);
        return "quote";
    }

    private List<Product> sortProducts(List<Product> products, String sort) {
        Comparator<Product> relevance = Comparator
                .comparing((Product product) -> product.getStockQuantity() == null ? Integer.MAX_VALUE : product.getStockQuantity())
                .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

        Comparator<Product> comparator = switch (sort == null ? "" : sort.toLowerCase(Locale.ROOT)) {
            case "price-asc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "price-desc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "stock-desc" -> Comparator.comparing(Product::getStockQuantity, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "name-asc" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            default -> relevance;
        };

        return products.stream()
                .filter(Objects::nonNull)
                .sorted(comparator)
                .toList();
    }

    private List<Product> buildSmartRecommendations(List<Product> products) {
        List<Product> picks = new ArrayList<>();
        String[] priorityCategories = {"Gạch", "Xi măng", "Cát xây dựng", "Đá xây dựng", "Thép", "Dịch vụ"};
        for (String category : priorityCategories) {
            products.stream()
                    .filter(product -> product.getCategory() != null && category.equalsIgnoreCase(product.getCategory().getName()))
                    .findFirst()
                    .ifPresent(picks::add);
        }

        if (picks.size() < 6) {
            products.stream()
                    .filter(product -> !picks.contains(product))
                    .limit(6 - picks.size())
                    .forEach(picks::add);
        }

        return picks.stream().limit(6).toList();
    }

    private List<String> buildSmartHints(String keyword, String category) {
        String text = ((keyword == null ? "" : keyword) + " " + (category == null ? "" : category)).toLowerCase(Locale.ROOT);
        List<String> hints = new ArrayList<>();

        if (text.contains("san") || text.contains("mat bang")) {
            hints.add("San lấp: xem San lấp mặt bằng, Cát xây dựng và Đá 0x4 xanh.");
        }
        if (text.contains("mong") || text.contains("ham") || text.contains("nha")) {
            hints.add("Móng/sàn: gợi ý Thép cây D10, Đá 1x2 đen và Xi măng PCB40.");
        }
        if (text.contains("xay to") || text.contains("tuong") || text.contains("gach")) {
            hints.add("Xây tô: nên ưu tiên Gạch ống 8x8x18, Cát xây dựng và Xi măng PCB40.");
        }
        if (text.contains("be tong")) {
            hints.add("Bê tông: cân nhắc Đá 1x2, Cát bê tông vàng và Thép cây gân.");
        }
        if (text.contains("thep")) {
            hints.add("Thép: có thép cây, thép hình và thép hộp cho nhiều loại công trình.");
        }

        if (hints.isEmpty()) {
            hints.add("Dùng bộ lọc để chốt nhanh nhóm hàng, giá và tồn kho.");
            hints.add("Bấm Tính vật liệu để ra khối lượng sơ bộ trước khi đặt hàng.");
        }

        return hints;
    }

    private CustomerOrderForm prefillOrderForm(String selectedProduct) {
        CustomerOrderForm form = new CustomerOrderForm();
        if (selectedProduct != null) {
            form.setProductName(selectedProduct);
        }
        return form;
    }

    private void addCommonAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String currentEmail = "";
        if (authenticated && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            currentEmail = oidcUser.getEmail();
        } else if (authenticated) {
            currentEmail = authentication.getName();
        }

        model.addAttribute("companyName", companyName);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankName", bankName);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone == null ? "" : phone.trim());
        model.addAttribute("zalo", zaloUrl == null ? "" : zaloUrl.trim());
        model.addAttribute("zaloReady", zaloUrl != null && !zaloUrl.isBlank());
        model.addAttribute("authenticated", authenticated);
        model.addAttribute("currentEmail", currentEmail);
    }
}
