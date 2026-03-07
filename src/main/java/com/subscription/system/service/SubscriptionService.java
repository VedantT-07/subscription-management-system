package com.subscription.system.service;

import com.subscription.system.models.*;
import com.subscription.system.repositories.SubscriptionRepo;
import com.subscription.system.repositories.UserRepo;
import org.jspecify.annotations.Nullable;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {
    private final SubscriptionRepo subscriptionRepo;
    private final UserRepo userRepo;
    public SubscriptionService(SubscriptionRepo subscriptionRepo, UserRepo userRepo)
    {
        this.userRepo = userRepo;
        this.subscriptionRepo = subscriptionRepo;
    }


    public Subscription addSubscription(User user, String serviceName, PlanType planType, LocalDate startDate, double amount, Category category)
    {
        boolean exists = subscriptionRepo.findByUserId(user.getId()).stream()
                .anyMatch(s -> s.getServiceName().equalsIgnoreCase(serviceName)
                        && s.getStatus() == SubscriptionStatus.ACTIVE);

        if (exists) {
            throw new IllegalStateException("Subscription already exists for this service");
        }

        LocalDate renewalDate = planType == PlanType.MONTHLY ? startDate.plusMonths(1): startDate.plusYears(1);

        Subscription sub = new Subscription();
        sub.setServiceName(serviceName);
        sub.setPlanType(planType);
        sub.setStartDate(startDate);
        sub.setRenewalDate(renewalDate);
        sub.setAmount(java.math.BigDecimal.valueOf(amount));
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCategory(category);
        sub.setUser(user);

        return subscriptionRepo.save(sub);
    }
    public void verifyUser(Long userId, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getId() != userId) {
            throw new IllegalStateException("Unauthorized access");
        }
    }
    public List<Subscription> getUserSubscriptionsByEmail(String email)
    {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException(
                        "User not found"
                ));
        updateExpiredStatuses(user.getId());
        return subscriptionRepo.findByUserId(user.getId());
    }

    public List<Subscription> getActiveSubscriptions(Long userId)
    {
        return subscriptionRepo.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }

    public List<Subscription> getExpiredSubscriptions(Long userId)
    {
        return subscriptionRepo.findByUserIdAndStatus(userId, SubscriptionStatus.EXPIRED);
    }

    public List<Subscription> getExpiringSoon(LocalDate from, LocalDate to)
    {
        return subscriptionRepo.findByRenewalDateBetween(from, to);
    }

    public void cancelSubscription(Long subscriptionId)
    {
        Subscription sub = subscriptionRepo.findById(subscriptionId).orElseThrow(()-> new IllegalArgumentException("Subscription Not Found"));

        sub.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepo.save(sub);
    }

    public Map<String, Object> getDashboardStats(Long userId) {
        updateExpiredStatuses(userId);

        List<Subscription> all = subscriptionRepo.findByUserId(userId);

        long active = all.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();

        long expired = all.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.EXPIRED)
                .count();

        LocalDate now = LocalDate.now();
        long expiringSoon = all.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(s -> !s.getRenewalDate().isBefore(now) &&
                                         !s.getRenewalDate().isAfter(now.plusDays(7)))
                .count();

        BigDecimal monthlySpend = all.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(s-> {
                    if (s.getPlanType() == PlanType.MONTHLY) {
                        return s.getAmount(); // full amount
                    } else if (s.getPlanType() == PlanType.YEARLY) {
                        return s.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP); // monthly equivalent
                    } else {
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap();
        stats.put("activeCount", active);
        stats.put("expiredCount", expired);
        stats.put("expiringSoonCount", expiringSoon);
        stats.put("monthlySpend", monthlySpend);

        return stats;
    }

    public List<Subscription> getExpiringSoonForUser(Long userId, int days)
    {
        updateExpiredStatuses(userId);

        LocalDate now = LocalDate.now();
        return subscriptionRepo.findByUserId(userId).stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(s -> !s.getRenewalDate().isBefore(now) &&
                                            !s.getRenewalDate().isAfter(now.plusDays(days)))
                .toList();
    }

    public void updateExpiredStatuses(Long userId) {
        LocalDate today = LocalDate.now();

        List<Subscription> subs = subscriptionRepo.findByUserId(userId);
        for (Subscription s : subs) {
            if (s.getStatus() == SubscriptionStatus.ACTIVE &&
                    s.getRenewalDate().isBefore(today)) {
                s.setStatus(SubscriptionStatus.EXPIRED);
            }
        }
        subscriptionRepo.saveAll(subs);
    }

    public List<Subscription> getRecentlyExpired(Long userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(days);

        return subscriptionRepo.findByUserIdAndStatus(userId, SubscriptionStatus.EXPIRED)
                .stream()
                .filter(s ->
                        !s.getRenewalDate().isAfter(today) &&
                                !s.getRenewalDate().isBefore(cutoff)
                )
                .toList();
    }

    public Optional<Subscription> getMostExpensiveActive(Long userId) {
        return subscriptionRepo.findByUserId(userId).stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .max(Comparator.comparing(Subscription::getAmount));
    }

    public Subscription renewSubscription(Long id)
    {
        Subscription sub = subscriptionRepo.findById(id).orElseThrow(()-> new RuntimeException("Subscription not found"));

        if(sub.getStatus() != SubscriptionStatus.EXPIRED)
        {
            throw new IllegalStateException("Only expired subscriptions can be renewed");
        }

        LocalDate newRenewal;
        if(sub.getPlanType() == PlanType.MONTHLY)
        {
            newRenewal = LocalDate.now().plusMonths(1);
        }
        else
        {
            newRenewal = LocalDate.now().plusYears(1);
        }

        sub.setRenewalDate(newRenewal);
        sub.setStatus(SubscriptionStatus.ACTIVE);

        return subscriptionRepo.save(sub);
    }

    public Map<Category, BigDecimal> getCategoryWiseMonthlySpend(Long userId)
    {
        List<Subscription> all = subscriptionRepo.findByUserId(userId);

        return all.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.groupingBy(
                        Subscription::getCategory,
                        Collectors.mapping(
                                s-> {
                                    if (s.getPlanType() == PlanType.YEARLY)
                                    {
                                        return s.getAmount()
                                                .divide(BigDecimal.valueOf(12),2, RoundingMode.HALF_UP);
                                    }
                                    else {
                                        return s.getAmount();
                                    }
                                },
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

    public List<Subscription> searchFilterSort(String email, String search, Category category, SubscriptionStatus status, String sort)
    {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException(("User not found")));

        List<Subscription> subs = subscriptionRepo.findByUserId(user.getId());

        //Search
        if(search != null && !search.isBlank())
        {
            subs = subs.stream()
                    .filter(s-> s.getServiceName().toLowerCase()
                            .contains(search.toLowerCase()))
                    .toList();
        }

        //Filter Category
        if(category!=null)
        {
            subs = subs.stream()
                    .filter(s-> s.getCategory() == category)
                    .toList();
        }

        //Filter Status
        if(status!=null)
        {
            subs = subs.stream()
                    .filter(s -> s.getStatus() == status)
                    .toList();
        }

        //Sort
        if("renewal".equalsIgnoreCase(sort))
        {
            subs = subs.stream()
                    .sorted(Comparator.comparing(Subscription::getRenewalDate))
                    .toList();
        }
        else if("price".equalsIgnoreCase(sort))
        {
            subs = subs.stream()
                    .sorted(Comparator.comparing(Subscription::getAmount).reversed())
                    .toList();
        }

        return subs;
    }
}