package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.service.ProductService;
import com.anhvu.vlxd.web.CustomerOrderForm;
import com.anhvu.vlxd.web.QuoteRequestForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CustomerOrderController {

    private static final String COMPANY_NAME = "C\u00D4NG TY TNHH MTV TM DV XD ANH V\u0168";
    private static final String BANK_ACCOUNT = "1991777790";
    private static final String BANK_NAME = "Ng\u00E2n h\u00E0ng Qu\u00E2n \u0111\u1ED9i - Chi nh\u00E1nh B\u00ECnh Long";
    private static final String EMAIL = "xaydungvantaidaphuongthucanhvu@gmail.com";
    private static final BigDecimal ONLINE_PAYMENT_LIMIT = new BigDecimal("100000000");

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @PostMapping("/order-request")
    public String store(@Valid @ModelAttribute("orderForm") CustomerOrderForm form,
                        BindingResult bindingResult,
                        String shippingOptions,
                        Model model) {
        List<Product> activeProducts = productRepository.findByActiveTrueOrderByCreatedAtDesc();

        // Doi chieu tung dong hang voi danh muc san pham dang ban
        List<Product> resolvedProducts = new ArrayList<>();
        List<CustomerOrderForm.OrderItem> items = form.getItems() == null ? List.of() : form.getItems();
        for (int i = 0; i < items.size(); i++) {
            CustomerOrderForm.OrderItem item = items.get(i);
            String name = item.getProductName() == null ? "" : item.getProductName().trim();
            Optional<Product> match = activeProducts.stream()
                    .filter(product -> product.getName() != null && product.getName().equalsIgnoreCase(name))
                    .findFirst();
            if (match.isEmpty()) {
                if (!name.isBlank()) {
                    bindingResult.rejectValue("items[" + i + "].productName", "product.not_found",
                            "S\u1EA3n ph\u1EA9m kh\u00F4ng h\u1EE3p l\u1EC7.");
                }
                resolvedProducts.add(null);
            } else {
                resolvedProducts.add(match.get());
            }
        }

        if (bindingResult.hasErrors()) {
            addOrderPageAttributes(model, form, false);
            model.addAttribute("smartHints", List.of("Vui l\u00F2ng ki\u1EC3m tra l\u1EA1i c\u00E1c tr\u01B0\u1EDDng c\u00F2n thi\u1EBFu ho\u1EB7c ch\u01B0a \u0111\u00FAng."));
            model.addAttribute("smartRecommendations", productService.getAllActiveProducts().stream().limit(6).toList());
            return "order";
        }

        // Process shipping notes
        String shippingNote = "";
        if ("xe-tai".equals(shippingOptions)) shippingNote = "[Giao xe t\u1EA3i \u0111\u01B0\u1EDDng l\u1EDBn] ";
        else if ("xe-bagac".equals(shippingOptions)) shippingNote = "[Giao h\u1EBBm nh\u1ECF xe ba g\u00E1c] ";
        else if ("can-boc-vac".equals(shippingOptions)) shippingNote = "[C\u1EA7n thu\u00EA b\u1ED1c v\u00E1c] ";

        String finalNote = form.getNote() == null ? "" : form.getNote().trim();
        finalNote = shippingNote + finalNote;

        // Luu moi mat hang thanh mot dong don (admin va tra cuu hien tai deu theo dong)
        List<CustomerOrder> savedOrders = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Product product = resolvedProducts.get(i);
            BigDecimal quantity = items.get(i).getQuantity().setScale(2, RoundingMode.UP);
            BigDecimal lineTotal = product.getPrice().multiply(quantity).setScale(0, RoundingMode.UP);
            savedOrders.add(customerOrderRepository.save(CustomerOrder.builder()
                    .customerName(form.getCustomerName().trim())
                    .phone(form.getPhone().trim())
                    .address(form.getAddress().trim())
                    .productName(product.getName())
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .totalAmount(lineTotal)
                    .paymentMethod(form.getPaymentMethod().trim())
                    .note(finalNote)
                    .status("NEW")
                    .build()));
        }

        addOrderPageAttributes(model, prefillNextOrderForm(), true);
        addPaymentResult(model, savedOrders, resolvedProducts);
        return "order";
    }

    private CustomerOrderForm prefillNextOrderForm() {
        CustomerOrderForm nextForm = new CustomerOrderForm();
        nextForm.setPaymentMethod("BANK_TRANSFER");
        return nextForm;
    }

    private void addOrderPageAttributes(Model model, CustomerOrderForm form, boolean orderSuccess) {
        addCommonAttributes(model);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("selectedProduct",
                form.getItems() == null || form.getItems().isEmpty() ? "" : form.getItems().get(0).getProductName());
        model.addAttribute("orderForm", form);
        model.addAttribute("quoteForm", new QuoteRequestForm());
        model.addAttribute("orderSuccess", orderSuccess);
        model.addAttribute("quoteSuccess", false);
    }

    private void addPaymentResult(Model model, List<CustomerOrder> orders, List<Product> products) {
        CustomerOrder first = orders.get(0);
        BigDecimal totalAmount = orders.stream()
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean bankTransfer = "BANK_TRANSFER".equalsIgnoreCase(first.getPaymentMethod());
        boolean pricedOrder = totalAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean onlinePaymentAllowed = pricedOrder && totalAmount.compareTo(ONLINE_PAYMENT_LIMIT) < 0;
        String paymentCode = "ANHVU-DH" + first.getId()
                + (orders.size() > 1 ? "x" + orders.size() : "")
                + "-" + onlyDigits(first.getPhone());
        String paymentContent = orders.size() > 1
                ? paymentCode + " " + orders.size() + " mat hang"
                : paymentCode + " " + first.getProductName();

        // Danh sach dong hang cho bang tom tat thanh toan
        List<Map<String, Object>> lines = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            CustomerOrder order = orders.get(i);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("name", order.getProductName());
            line.put("quantity", order.getQuantity());
            line.put("unit", products.get(i).getUnit());
            line.put("unitPriceDisplay", money(order.getUnitPrice()));
            line.put("lineTotalDisplay", money(order.getTotalAmount()));
            lines.add(line);
        }

        model.addAttribute("orderedLines", lines);
        model.addAttribute("orderedLineCount", orders.size());
        model.addAttribute("orderTotal", totalAmount);
        model.addAttribute("orderTotalDisplay", money(totalAmount));
        model.addAttribute("paymentCode", paymentCode);
        model.addAttribute("paymentContent", paymentContent);
        model.addAttribute("bankTransferSelected", bankTransfer);
        model.addAttribute("onlinePaymentAllowed", onlinePaymentAllowed);
        model.addAttribute("priceNeedsConfirmation", !pricedOrder);
        model.addAttribute("storePaymentRequired", !pricedOrder || totalAmount.compareTo(ONLINE_PAYMENT_LIMIT) >= 0);

        if (bankTransfer && onlinePaymentAllowed) {
            model.addAttribute("paymentQrUrl", buildVietQrUrl(totalAmount, paymentContent));
        }
    }

    private String buildVietQrUrl(BigDecimal totalAmount, String paymentContent) {
        String amount = totalAmount.setScale(0, RoundingMode.UP).toPlainString();
        String accountName = encode("CONG TY TNHH MTV TM DV XD ANH VU");
        String addInfo = encode(paymentContent);
        return "https://img.vietqr.io/image/MB-" + BANK_ACCOUNT + "-compact2.png"
                + "?amount=" + amount
                + "&addInfo=" + addInfo
                + "&accountName=" + accountName;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }

    private String money(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value) + " VN\u0110";
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

        model.addAttribute("companyName", COMPANY_NAME);
        model.addAttribute("bankAccount", BANK_ACCOUNT);
        model.addAttribute("bankName", BANK_NAME);
        model.addAttribute("email", EMAIL);
        model.addAttribute("authenticated", authenticated);
        model.addAttribute("currentEmail", currentEmail);
    }
}