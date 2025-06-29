Starting with temporal (will attach complete things later)

- started with docker compose file
- docker exec container-id tctl namespace list
TO GET LIST OF NAMESPACES 


How code is written:
1. Application.java               ✅ Start here
2. PaymentActivities.java         ✅ Write interfaces
3. PaymentActivitiesImpl.java     ✅ Implement logic
4. ShippingActivities.java
5. ShippingActivitiesImpl.java

[//]: # (6. NotificationActivities.java)

[//]: # (7. NotificationActivitiesImpl.java)
8. OrderWorkflow.java             ✅ Write workflow interface
9. OrderWorkflowImpl.java         ✅ Implement workflow logic
10. TemporalConfig.java           ✅ Configure Temporal now
11. WorkflowService.java          ✅ Write service layer
12. OrderController.java          ✅ Final step: Expose REST APIs


--------------------

    entrypoint:
      - temporal
      - server
      - start-dev


    ✅ 1. temporal
        This is the command-line interface (CLI) binary provided by Temporal.
        
        Think of it like:
           git for Git CLI
           kubectl for Kubernetes
           temporal is the CLI for Temporal
        
        👉 This binary is available inside the Docker image you're using:
        temporalio/server:1.24.0
        
        You can run other commands with it too:
            temporal workflow list
            temporal namespace list
            temporal operator namespace describe --namespace default

        ✅ 2. server
        This tells the CLI:
            “I want to start the Temporal Server — not run an admin command.”
        
        Temporal CLI supports multiple command families. For example:
            temporal operator — Admin-level commands
            temporal workflow — Workflow-level actions
            temporal server — Start the Temporal backend server
        
        When you say temporal server, you are entering the "server" context of the CLI.


        ✅ 3. start-dev
            This is a special subcommand used only for local development.
        
        It does several things automatically, all-in-one:
        
        What it does	
            🧩 Starts all internal services ->	Temporal has 4 services: Frontend, History, Matching, and Worker Service
            🧪 Uses SQLite ->	Instead of needing a real MySQL/Postgres/Cassandra database
            🔁 Initializes everything ->	No need to manually create namespaces, DB schemas, etc.
            🌱 Registers a default namespace -> So your workflows can start immediately under "default"
            
        💡 You don’t use start-dev in production. You would use temporal server start with full configuration instead.


Flow and use of this:
        entrypoint:
        - temporal
          - server
          - start-dev
          - --namespace=default # Namespace => environment (e.g., dev, prod), Because many applications or microservices might be using the same Temporal server.
          - --db-filename=/tmp/sqlite/db
          - --ip=0.0.0.0
          ports:
          - "7233:7233" # gRPC port — your Java Spring app uses this to connect to Temporal
          - "8233:8233" # web UI
          volumes:
          - ./sqlite_db:/tmp/sqlite


1️⃣ You hit POST /orders/create in your Spring controller
The process begins when your REST API (e.g., /orders/create) is triggered by a user or service. At this point, no Temporal interaction has happened yet — just a regular controller call in your Spring Boot app.

2️⃣ Java app creates a workflow stub using newWorkflowStub(...)
Inside the controller (or a service it calls), you create a stub for the workflow using client.newWorkflowStub(...). This creates a Java proxy object for the workflow interface. This is the first point where your application talks to the Temporal Server using gRPC on port 7233.

3️⃣ WorkflowClient.start(...) is called to start the workflow
Instead of calling a normal method, this uses the proxy to package the method name, arguments, and metadata and sends it as a gRPC request to the Temporal Server. Temporal saves this event into its SQLite database, tracking that a workflow has started.

4️⃣ Temporal assigns the workflow to a task queue (OrderTaskQueue)
The Temporal Server now places a new task into the internal task queue that matches the one you configured (e.g., "OrderTaskQueue"). This is how it tracks that some worker needs to pick up the task.

5️⃣ Your worker polls the Temporal Server asking for work
In your Spring app, a worker has been started and is continuously polling the Temporal Server (via gRPC again) asking: “Do you have anything for me in OrderTaskQueue?” Temporal responds when there is work.

6️⃣ Worker receives task and starts executing the workflow logic
Once it gets the task, the worker begins executing the workflow method, e.g., startOrder(...) in OrderWorkflowImpl. This is just normal Java code now being run by your worker.

7️⃣ Workflow hits Workflow.await(...) and yields control
Inside your workflow, when it reaches Workflow.await(...) (for example, waiting for a signal), it does not block a thread. Instead, the current state is persisted by the Temporal Server into its SQLite DB. The worker then "yields control" — i.e., stops execution and frees up the thread.

8️⃣ You hit POST /orders/approve to send a signal
Later, another REST API (/orders/approve) is triggered. It uses client.newUntypedWorkflowStub(orderId) and then calls .signal(...). This creates another gRPC call to the Temporal Server with signal data like approverId.

9️⃣ Temporal logs the signal and resumes the paused workflow
The Temporal Server receives the signal, records it in the event history, and realizes that the workflow can now proceed. It tells the worker to resume the workflow. The worker replays the event history (i.e., re-runs prior steps to rebuild state) and then continues past the await() logic.