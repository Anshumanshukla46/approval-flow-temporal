package com.temporal.learning.controller;

import com.temporal.learning.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    @Autowired
    private final WorkflowService workflowService;

    public OrderController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam String orderId) {
        return workflowService.startOrderWorkflow(orderId);
    }

    @PostMapping("/approve")
    public String approveOrder(@RequestParam String orderId, @RequestParam String approverId) {
        return workflowService.sendApprovalSignal(orderId,approverId);
    }

    @PostMapping("/reject")
    public String rejectOrder(@RequestParam String orderId, @RequestParam String approverId) {
        return workflowService.sendRejectionSignal(orderId,approverId);
    }



}
