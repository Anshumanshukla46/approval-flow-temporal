package com.temporal.learning.activities;

public class PaymentActivitiesImpl implements PaymentActivities {
    @Override
    public void processPayment(String orderId) {
        System.out.println("Processed payment for order: " + orderId);
    }
}
