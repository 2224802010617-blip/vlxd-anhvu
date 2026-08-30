package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.entity.QuoteRequest;
import com.anhvu.vlxd.repository.CategoryRepository;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.repository.QuoteRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/admin", produces = "text/html;charset=UTF-8")
    public String dashboard(Model model, Authentication authentication) throws Exception {
        List<Product> products = productRepository.findAll();
        List<CustomerOrder> orders = customerOrderRepository.findAllByOrderByCreatedAtDesc();
        List<QuoteRequest> quotes = quoteRequestRepository.findAllByOrderByCreatedAtDesc();
        List<CustomerOrder> allOrders = customerOrderRepository.findAll();
        List<QuoteRequest> allQuotes = quoteRequestRepository.findAll();

        long lowStock = products.stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() < 300)
                .count();
        double totalUnitsSold = allOrders.stream()
                .map(order -> order.getQuantity() == null ? 0d : order.getQuantity().doubleValue())
                .mapToDouble(Double::doubleValue)
                .sum();

        Map<String, Double> salesByProduct = allOrders.stream()
                .collect(Collectors.groupingBy(CustomerOrder::getProductName, LinkedHashMap::new,
                        Collectors.summingDouble(order -> order.getQuantity() == null ? 0d : order.getQuantity().doubleValue())));

        Map<String, Integer> stockByCategory = products.stream()
                .collect(Collectors.groupingBy(product -> product.getCategory().getName(), LinkedHashMap::new,
                        Collectors.summingInt(product -> product.getStockQuantity() == null ? 0 : product.getStockQuantity())));

        model.addAttribute("productCount", products.size());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("quoteCount", quoteRequestRepository.count());
        model.addAttribute("orderCount", customerOrderRepository.count());
        model.addAttribute("pendingOrders", allOrders.stream().filter(order -> "NEW".equalsIgnoreCase(order.getStatus())).count());
        model.addAttribute("pendingQuotes", allQuotes.stream().filter(quote -> "NEW".equalsIgnoreCase(quote.getStatus())).count());
        model.addAttribute("totalUnitsSold", totalUnitsSold);
        model.addAttribute("quotes", quotes);
        model.addAttribute("orders", orders);
        model.addAttribute("products", products);
        model.addAttribute("adminEmail", adminEmail(authentication));
        model.addAttribute("orderStatuses", List.of("NEW", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELED"));
        model.addAttribute("quoteStatuses", List.of("NEW", "CONTACTED", "QUOTED", "CLOSED"));
        model.addAttribute("salesLabelsJson", objectMapper.writeValueAsString(salesByProduct.keySet()));
        model.addAttribute("salesDataJson", objectMapper.writeValueAsString(salesByProduct.values()));
        model.addAttribute("stockLabelsJson", objectMapper.writeValueAsString(stockByCategory.keySet()));
        model.addAttribute("stockDataJson", objectMapper.writeValueAsString(stockByCategory.values()));
        return "admin/dashboard";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status) {
        customerOrderRepository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            customerOrderRepository.save(order);
        });
        return "redirect:/admin#orders";
    }

    @PostMapping("/admin/quotes/{id}/status")
    public String updateQuoteStatus(@PathVariable Long id,
                                    @RequestParam String status) {
        quoteRequestRepository.findById(id).ifPresent(quote -> {
            quote.setStatus(status);
            quoteRequestRepository.save(quote);
        });
        return "redirect:/admin#quotes";
    }

    @PostMapping("/admin/products/{id}/inventory")
    public String updateInventory(@PathVariable Long id,
                                  @RequestParam Integer stockQuantity,
                                  @RequestParam BigDecimal price,
                                  @RequestParam(required = false) Boolean active,
                                  RedirectAttributes redirectAttributes) {
        if (stockQuantity == null || stockQuantity < 0 || price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("adminError", "Tồn kho và giá phải lớn hơn hoặc bằng 0.");
            return "redirect:/admin#inventory";
        }

        productRepository.findById(id).ifPresent(product -> {
            product.setStockQuantity(stockQuantity);
            product.setPrice(price);
            product.setActive(Boolean.TRUE.equals(active));
            productRepository.save(product);
        });
        redirectAttributes.addFlashAttribute("adminSuccess", "Đã cập nhật kho hàng.");
        return "redirect:/admin#inventory";
    }

    private String adminEmail(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return authentication.getName();
    }
}
