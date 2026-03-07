package com.subscription.system.repositories;

import com.subscription.system.models.Subscription;
import com.subscription.system.models.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByRenewalDateBetween(LocalDate start, LocalDate end);
}
