package com.subscription.system.controller;

import com.subscription.system.controller.dto.AddSubscriptionRequest;
import com.subscription.system.models.*;
import com.subscription.system.service.SubscriptionService;
import com.subscription.system.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins="*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    public SubscriptionController(SubscriptionService subscriptionService, UserService userService)
    {
        this.subscriptionService = subscriptionService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Subscription> add(@RequestBody AddSubscriptionRequest req)
    {
        User user = userService.findByEmail(req.getEmail());
        Subscription sub = subscriptionService.addSubscription(
                user,
                req.getServiceName(),
                PlanType.valueOf(req.getPlanType()),
                LocalDate.parse(req.getStartDate()),
                req.getAmount(),
                req.getCategory()
        );
        return ResponseEntity.ok(sub);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Subscription>> getMySubs(@RequestParam String email)
    {
        return ResponseEntity.ok((subscriptionService.getUserSubscriptionsByEmail(email)));
    }

    @GetMapping("/my/stats")
    public ResponseEntity<?> getStats(@RequestParam String email){
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(subscriptionService.getDashboardStats(user.getId()));
    }

    @GetMapping("/my/expiring")
    public ResponseEntity<List<Subscription>> getExpiringSoon(@RequestParam String email)
    {
        User user = userService.findByEmail(email);
        subscriptionService.updateExpiredStatuses(user.getId());
        return ResponseEntity.ok(subscriptionService.getExpiringSoonForUser(user.getId(), 7));
    }

    @GetMapping("/my/expired")
    public ResponseEntity<List<Subscription>> getExpired(@RequestParam String email)
    {
        User user = userService.findByEmail(email);
        subscriptionService.updateExpiredStatuses(user.getId());
        return ResponseEntity.ok(subscriptionService.getExpiredSubscriptions(user.getId()));
    }

    @GetMapping("/my/expired/recent")
    public ResponseEntity<List<Subscription>> getRecentlyExpired(@RequestParam String email) {
        User user = userService.findByEmail(email);
        subscriptionService.updateExpiredStatuses(user.getId()); // keep statuses fresh
        return ResponseEntity.ok(subscriptionService.getRecentlyExpired(user.getId(), 7));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id)
    {
        subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my/most-expensive")
    public ResponseEntity<?> getMostExpensive(@RequestParam String email) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(subscriptionService.getMostExpensiveActive(user.getId()).orElse(null));
    }

    @PutMapping("/{id}/renew")
    public ResponseEntity<Subscription> renew(@PathVariable Long id)
    {
        return ResponseEntity.ok(subscriptionService.renewSubscription(id));
    }

    @GetMapping("/my/category-spend")
    public ResponseEntity<Map<Category, BigDecimal>> getCategorySpend(@RequestParam String email) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(subscriptionService.getCategoryWiseMonthlySpend(user.getId()));
    }

    @GetMapping("/my/search")
    public ResponseEntity<List<Subscription>> searchFilterSort(@RequestParam String email, @RequestParam(required = false) String search, @RequestParam(required = false) Category category, @RequestParam(required = false)SubscriptionStatus status, @RequestParam(required = false) String sort){
        return ResponseEntity.ok(subscriptionService.searchFilterSort(email, search, category, status, sort));
    }
}