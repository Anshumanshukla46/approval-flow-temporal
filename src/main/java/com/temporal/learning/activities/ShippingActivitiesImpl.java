package com.temporal.learning.activities;

public class ShippingActivitiesImpl implements ShippingActivities {
    @Override
    public void prepareShipment(String orderId) {
        System.out.println("Prepared shipment for order " + orderId);
    }
}
