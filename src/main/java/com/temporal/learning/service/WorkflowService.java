package com.temporal.learning.service;

import com.temporal.learning.workflow.OrderWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.stereotype.Service;

@Service
public class WorkflowService {
    private final WorkflowClient client;

    /*
        🧩 What is a Task Queue in Temporal?
        A Task Queue is a named communication channel between:

        ✅ Workflow Client (where you start workflows)

        ✅ Worker (where the workflow code and activity code run)

        You can think of it as a “pipeline” that connects requests to processing logic.

        🎯 Why do we need this?
        Because Temporal is decoupled. The person starting the workflow and the worker executing it can be:

        On different machines 💻🖥️

        In different services/microservices 🌐

        Scaled independently 📈

        The Task Queue ensures that messages (tasks) get delivered to the right worker, even in a distributed system.
     */
    private final String taskQueue = "OrderTaskQueue";

    public WorkflowService(WorkflowClient workflowClient) {
        this.client = workflowClient;
    }

    // Start a new workflow
    public String startOrderWorkflow(String orderId) {

        /*
            WorkflowOptions is like a set of instructions you give Temporal when you're about to start a new workflow.
            It tells Temporal things like:
            - What to name this workflow (workflowId)
            - Which worker should handle it (taskQueue)
            - How long it should be allowed to run (timeouts)
         */
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(orderId)
                .setTaskQueue(taskQueue)
                .build();


        /*
            WorkflowClient is the entry point for your application to communicate with Temporal Server.
            You use it to:
            - Start new workflows ✅
            - Send signals to workflows ✅
            - Query workflows ✅
            - Get their status ✅



HERE:
OrderWorkflow.class → This is the interface type

workflow → This is the proxy object ✅

🔸 Wait, but OrderWorkflow is just an interface, how can it have an object?
Yes — interfaces can’t be instantiated directly. But:

Temporal uses Java Dynamic Proxy API to create a proxy object at runtime that implements the interface.

So, workflow is a fake object that:

Implements the OrderWorkflow interface

Doesn’t run your logic directly

Instead, when you call a method like workflow.startOrder("order123"), it captures the call, and sends it as a request over the network to the Temporal server

This is a classic use of the Proxy Design Pattern.
         */
        OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, options); // It creates a stub (a proxy object) that represents workflow


        /*
            ✅ This is where the workflow actually starts running.
            This line:
            - Tells Temporal: “Okay, here’s the method to run (startOrder) and here’s the input (orderId). Go!”
            - Starts the workflow asynchronously on the worker
            - Returns a WorkflowExecution object with info like the workflow ID and run ID







        Here’s what happens under the hood:

        Temporal doesn't execute startOrder() immediately

        Instead, it:

        Collects:

        The workflow interface + method name (startOrder)

        The input (orderId)

        The workflow ID + task queue (from WorkflowOptions)

        Serializes this into a gRPC request

        Sends that request to the Temporal server 🛰️

        The Temporal server adds this workflow to a task queue

        A Temporal Worker (running somewhere else) polls that queue, picks up the workflow, and then calls your real OrderWorkflowImpl logic

        So the method call is just the trigger that starts the engine. The actual logic runs remotely.




        What is gRPC? (Simply)
        gRPC is:

        A high-performance, open-source remote procedure call (RPC) framework developed by Google.

        In short:

        It lets one program call methods of another program on a different machine — like they were local.

        It uses HTTP/2 under the hood, making it faster and more efficient than traditional REST.

        It uses Protocol Buffers (protobuf) for data — which are superfast and compact.

        Why does Temporal use gRPC?
        Because:

        Temporal needs fast and reliable communication between:

        Your client code

        Temporal server

        Workers

        gRPC is great for microservices and real-time systems

        It supports bidirectional streaming, timeouts, deadlines, and typed contracts — which are perfect for Temporal complex workflows
         */

        /*
            Use .start(...) when you don’t care about result immediately (fire-and-forget).

            Use .execute(...).get() when you need the return value (like for logging APPROVED/REJECTED).
         */

        WorkflowExecution execution = WorkflowClient.start(workflow::startOrder, orderId);

        return "Started workflow, ID=" + execution.getWorkflowId();
    }


    public String sendApprovalSignal(String orderId, String approverId) {
        WorkflowStub stub = client.newUntypedWorkflowStub(orderId); // Find the workflow that has the WorkflowId = orderId and give me a handle (stub) to it.
        stub.signal("approveOrder", approverId); // Because Temporal uses Java reflection + annotations to bind
        return "OrderId: "+orderId+" is approved by approver of id"+approverId;
    }


    public String sendRejectionSignal(String orderId, String approverId) {
        WorkflowStub stub = client.newUntypedWorkflowStub(orderId);
        stub.signal("rejectOrder", approverId);
        stub.cancel(); // now this will be canceled, not completed
        return "OrderId: "+orderId+" is rejected by approver of id: "+approverId;
    }

}
