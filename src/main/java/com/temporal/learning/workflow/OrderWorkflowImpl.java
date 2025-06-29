package com.temporal.learning.workflow;

import com.temporal.learning.activities.PaymentActivities;
import com.temporal.learning.activities.ShippingActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;


// event history is big word in this temporal
/*
    ✅ 1. What is a Java dynamic proxy?
    ✅ 2. Replay Behavior & Determinism

        ✅ What is “Replay”?
        Temporal doesn’t save RAM/memory. It saves what happened (like a journal 📓).

        On restart/resume:

        Temporal goes through that journal step-by-step.

        Re-executes the workflow from the beginning (but doesn’t redo external calls like payments etc. — it reads past results).

        That’s Replay.

        So your workflow logic must be deterministic — it should behave the same way every time on replay.

        ✅ What is “Yield control”?
        It means the worker lets go of system resources — no CPU, no thread, no memory usage.
        Temporal saves the current progress and parks the workflow.
        This makes it highly scalable, unlike Thread.sleep() which blocks a thread.


 */

public class OrderWorkflowImpl implements OrderWorkflow{
    private boolean isApproved = false;
    private boolean isRejected = false;
    private String approverId;

    // List of approvers
    private final List<String> allowedApprovers = Arrays.asList("me", "myself", "i");

    @Override
    public void startOrder(String orderId) {

        // Logging in temporal
        /*
            🔁 Temporal Workflows can replay their execution
            Temporal workflows are deterministic. That means:
                - Every step is recorded in event history.
                - If a workflow resumes (after crash, failover, or restart), Temporal replays the event history step-by-step.


        ------

            Now imagine your workflow crashes and is replayed.

            With Workflow.getLogger():
            ✔️ Logs only during original run
            ❌ Skips logging during replay

            With regular logger:
            ❌ Logs again during replay — confusing logs and possibly doubling output
         */
        Workflow.getLogger(this.getClass())
                .info("Order {} started. Waiting for approval or rejection.", orderId);


        // Wait until either approve or reject signal sets the state
        Workflow.await(() -> isApproved || isRejected);

        if (isApproved) {

            /*
                Each activity must finish within 30 seconds, or it will be considered failed by Temporal
                how:
                Once the method completes successfully, the worker:
                - Sends a "completed" event to the Temporal server.
                - This gets logged in the event history of the workflow.

                Temporal then:
                - Resumes the workflow from where it left off (e.g., runs prepareShipment() next).
             */
            ActivityOptions options = ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build();


            /*
                Temporal uses the stub to:
                - Send the activity task to a worker.
                - Wait for the result (asynchronously under the hood).
             */
            PaymentActivities payment = Workflow.newActivityStub(PaymentActivities.class, options);
            payment.processPayment(orderId);

            ShippingActivities shipping = Workflow.newActivityStub(ShippingActivities.class, options);
            shipping.prepareShipment(orderId);

            Workflow.getLogger(this.getClass()).info("Order {} approved by approver {}.", orderId, approverId);

        } else {
            Workflow.getLogger(this.getClass()).info("Order {} was rejected by approver {}.", orderId, approverId);
        }
    }

    @Override
    public void approveOrder(String approverId) {
        if (allowedApprovers.contains(approverId)) {
            this.isApproved = true;
            this.approverId = approverId;
        } else {
            Workflow.getLogger(this.getClass()).info("Unauthorized approver {} attempted to approve.", approverId);
        }
    }

    @Override
    public void rejectOrder(String approverId) {
        if (allowedApprovers.contains(approverId)) {
            this.isRejected = true;
            this.approverId = approverId;
        } else {
            Workflow.getLogger(this.getClass()).info("Unauthorized approver {} attempted to reject.", approverId);
        }
    }
}
