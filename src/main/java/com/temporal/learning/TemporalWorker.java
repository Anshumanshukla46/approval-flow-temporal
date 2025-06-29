package com.temporal.learning;

import com.temporal.learning.activities.NotificationActivitiesImpl;
import com.temporal.learning.activities.PaymentActivitiesImpl;
import com.temporal.learning.activities.ShippingActivitiesImpl;
import com.temporal.learning.workflow.OrderWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/*
        [Your Spring App - Controller/Service]
                    |
                    | (via Temporal Client, using gRPC)
                    v
    [Temporal Server (localhost:7233)]
                         |
        --------------------------------------
        |         |         |       |        |
        Event History  Task Queue  Signals  State Store
                         |
          [Worker (polls the queue)]
                         |
            Runs workflow/activity logic
*/


/*
    WHICH BEAN IS CREATED FIRST AND WHICH ONE AT LAST:

    🔍 This is called: Method Parameter Injection
        In @Configuration classes, method parameters act like dependencies, and Spring resolves them from the ApplicationContext.

        So even without any call like temporalService() inside temporalClient(), Spring still knows the flow via:
            Method parameter types
            Return types
            The entire dependency graph it builds at startup
 */
@Configuration
public class TemporalWorker {

//    If WorkflowClient is the brain of your app for Temporal…
//    then WorkflowServiceStubs is the nervous system that sends the signals over the network.

//    It doesn’t contain business logic — it enables gRPC communication with the Temporal platform.

    /*
              Mainly for : WorkflowServiceStubs
              This creates a gRPC client in your app that can talk to the Temporal Server.

              WorkflowServiceStubs is Temporal's low-level SDK layer built over gRPC.
              It handles serialization, networking, retrying, and connection pooling.
              By default, this connects to the local Temporal server at localhost:7233.

              what it does:
                  - Creates & manages gRPC connection to Temporal
                  - Every communication to the Temporal Server

              🎯 Why needed:
              Because everything in Temporal happens via gRPC — and this is your first step in establishing that communication pipeline.

              Without it, you cannot:
                  - Start workflows
                  - Send signals
                  - Poll task queues
     */
    @Bean
    public WorkflowServiceStubs temporalService() {
        return WorkflowServiceStubs.newLocalServiceStubs();   // Connect to local Temporal service (gRPC at localhost:7233)

        /*
            This line does not create the Temporal Server.
         Instead, it:
            - Creates a gRPC client that connects to an already running Temporal Server (usually on localhost:7233).
            - Acts like a remote control to talk to the Temporal Server.

         */
    }


    /*
        The WorkflowServiceStubs represents the gRPC connection to the Temporal Server. When you do:

        Internally, this does:
            - Uses the gRPC channel from service to talk to Temporal Server
            - Sets up the low-level connection management
        Prepares the client to make RPCs for:
            - Starting workflows
            - Sending signals
            - Querying workflows

        ✅ Without passing WorkflowServiceStubs, the WorkflowClient wouldn’t be able to reach the Temporal server at all.

        WorkflowServiceStubs:
            - High-level API to start/signal/query workflows
            - Your app uses this to interact with Temporal
     */
    @Bean
    public WorkflowClient temporalClient(WorkflowServiceStubs service) {
        // Create the client; by default uses namespace "default"
        return WorkflowClient.newInstance(service);
    }


    /*
        🔧 But What is WorkerFactory?
            - WorkerFactory is like a factory for creating Workers that:
            - Poll task queues (like OrderTaskQueue)
            - Run workflow implementations (e.g., OrderWorkflowImpl)
            - Execute activities (e.g., PaymentActivitiesImpl, ShippingActivitiesImpl)
            - Without it, no worker would be polling tasks from Temporal.

        🧠 Why Does It Need WorkflowClient?
            - The worker needs to connect to the Temporal Server to fetch tasks.
            - workflowClient is what allows the WorkerFactory to get that connection info via WorkflowServiceStubs.
     */
    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        return WorkerFactory.newInstance(client);
    }


    @Bean
    public PaymentActivitiesImpl paymentActivities() {
        return new PaymentActivitiesImpl();
    }

    @Bean
    public ShippingActivitiesImpl shippingActivities() {
        return new ShippingActivitiesImpl();
    }

    @Bean
    public NotificationActivitiesImpl notificationActivities() {
        return new NotificationActivitiesImpl();
    }


    @Bean
    public Worker worker(WorkerFactory factory,
                         PaymentActivitiesImpl paymentActivities,
                         ShippingActivitiesImpl shippingActivities, NotificationActivitiesImpl notificationActivities) {

        Worker worker = factory.newWorker("OrderTaskQueue"); // Create a Worker listening on the "OrderTaskQueue"

        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class); // Register our Workflow implementation class

        worker.registerActivitiesImplementations(paymentActivities, shippingActivities, notificationActivities); // Register Activity implementation instances

        factory.start(); // Start polling in the background
        return worker;
    }

}
