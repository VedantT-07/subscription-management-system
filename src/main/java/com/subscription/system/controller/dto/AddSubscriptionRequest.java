package com.subscription.system.controller.dto;

import com.subscription.system.models.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddSubscriptionRequest {
    private String email;
    private String serviceName;
    private String planType;
    private String startDate;
    private double amount;
    private Category category;
}
