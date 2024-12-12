package com.example.gradingsystem.controller;

import com.example.gradingsystem.DTO.AssignmentDTO;
import com.example.gradingsystem.model.Assignment;
import com.example.gradingsystem.model.DocumentSubmission;
import com.example.gradingsystem.repository.AssignmentRepository;
import com.example.gradingsystem.repository.DocumentSubmissionRepository;
import com.example.gradingsystem.service.AssignmentService;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;
    private final DocumentSubmissionRepository submissionRepository;

    @Autowired
    public AssignmentController(AssignmentRepository assignmentRepository, AssignmentService assignmentService, DocumentSubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.submissionRepository = submissionRepository;
    }

        //  Fetch unsubmitted assignments
    @GetMapping("/upcoming")
    public List<Assignment> getUpcomingAssignments(@RequestParam Long studentId) {
        return assignmentService.getUpcomingAssignments(studentId);
    }
    
    //  Fetch submitted assignments
    @GetMapping("/submitted")
    public List<Assignment> getSubmittedAssignments(@RequestParam Long studentId) {
        return assignmentService.getSubmittedAssignments(studentId);
    }

    // Endpoint to create a new assignment
    @PostMapping
    public ResponseEntity<String> createAssignment(
            @RequestParam("assignmentName") String assignmentName,
            @RequestParam("dueDate") String dueDate,
            @RequestParam("description") String description) {

        // Validate fields
        if (assignmentName == null || dueDate == null || description == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Convert String dueDate to Date or LocalDate if necessary
        LocalDateTime dueDateTime = LocalDateTime.parse(dueDate); // Assuming 'dueDate' is passed as an ISO format string

        // Create the Assignment object
        Assignment assignment = new Assignment();
        assignment.setTitle(assignmentName);
        assignment.setDescription(description);
        assignment.setDueDate(dueDateTime);

        // Save assignment using service
        Assignment createdAssignment = assignmentService.createAssignment(assignment);

        // Return the created assignment with HTTP 201 status
        return ResponseEntity.status(HttpStatus.CREATED).body("Assignment '" + createdAssignment.getTitle() + "' added successfully");
    }

    @PostMapping("/saveAssignmentDetails")
    public String saveAssignmentDetails(@RequestBody AssignmentDTO details, HttpSession session) {
        
        System.out.println(details); 
        System.out.println(details.getId());
        System.out.println(details.getTitle());        
        System.out.println(details.getDescription());   
        System.out.println(details.getDueDate());

        // Save data
        session.setAttribute("id", details.getId());
        session.setAttribute("title", details.getTitle());
        session.setAttribute("description", details.getDescription());
        session.setAttribute("dueDate", details.getDueDate());
        System.out.println("Added to session");

        return "redirect:/submit-page"; 
    }
}
