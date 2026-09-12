package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.EmailContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailContactRepository extends JpaRepository<EmailContact, String> {
    List<EmailContact> findByMarketingConsentTrueAndUnsubscribedFalseOrderByCreatedAtDesc();

    Optional<EmailContact> findByToken(String token);
}
