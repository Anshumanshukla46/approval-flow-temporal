package com.temporal.learning.workflow;

import com.temporal.learning.OrderStatus;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {

    @WorkflowMethod // this annotation tells Temporal which method is the entry point (main method) for the workflow
    OrderStatus startOrder(String orderId);

    /** Signal to approve the order. */
    @SignalMethod // (name = "approve")
    void approveOrder(String approverId);

    /** Signal to reject the order. */
    @SignalMethod
    void rejectOrder(String approverId);
}