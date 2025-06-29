package com.temporal.learning.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;


/*
    Here's the 6-step checklist to correctly use an Activity in Temporal (Java):

    1. ✅ Define an interface for the activity with required method(s).
    2. 🛠️ Implement the interface in a concrete class with your logic.
    3. 🧠 Call the activity stub using Workflow.newActivityStub(...) inside your workflow.
    4. 🧩 Register the activity implementation in your worker using worker.registerActivitiesImplementations(...).
    5. 🌱 Create a Spring bean for the activity implementation if using Spring.
    6. 🔁 Restart the worker app after adding or changing activities to make it effective.

 */
@ActivityInterface
public interface NotificationActivities {

    @ActivityMethod
    void notify(String orderId);
}
