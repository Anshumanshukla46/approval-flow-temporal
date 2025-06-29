package com.temporal.learning.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ShippingActivities {
    @ActivityMethod
    void prepareShipment(String orderId);
}