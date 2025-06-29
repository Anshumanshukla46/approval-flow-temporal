package com.temporal.learning.activities;

public class NotificationActivitiesImpl implements NotificationActivities {
    @Override
    public void notify(String orderId) {
        System.out.println("Notified for order of id: " + orderId);
    }
}
