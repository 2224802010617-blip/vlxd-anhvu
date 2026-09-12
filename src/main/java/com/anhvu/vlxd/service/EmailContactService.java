package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.EmailContact;
import com.anhvu.vlxd.repository.EmailContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailContactService {

    private final EmailContactRepository repository;
    private final SecureRandom random = new SecureRandom();

    /** Ghi nhan email khach sau khi dat hang / gui bao gia. Khong bao gio ha consent da co ve false do khach quen tick. */
    @Transactional
    public void record(String email, String name, boolean consent, String source) {
        if (email == null || email.isBlank()) {
            return;
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        EmailContact contact = repository.findById(key).orElseGet(() -> EmailContact.builder()
                .email(key)
                .token(newToken())
                .createdAt(LocalDateTime.now())
                .source(source)
                .build());
        if (name != null && !name.isBlank()) {
            contact.setName(name.trim());
        }
        if (consent) {
            contact.setMarketingConsent(true);
            contact.setUnsubscribed(false);
        }
        contact.setUpdatedAt(LocalDateTime.now());
        repository.save(contact);
    }

    public List<EmailContact> marketingRecipients() {
        return repository.findByMarketingConsentTrueAndUnsubscribedFalseOrderByCreatedAtDesc();
    }

    public List<EmailContact> all() {
        return repository.findAll();
    }

    /** Khach bam link huy nhan trong email. */
    @Transactional
    public Optional<EmailContact> unsubscribe(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return repository.findByToken(token.trim()).map(contact -> {
            contact.setUnsubscribed(true);
            contact.setUpdatedAt(LocalDateTime.now());
            return repository.save(contact);
        });
    }

    private String newToken() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
