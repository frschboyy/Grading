package com.example.gradingsystem.service;

import com.example.gradingsystem.model.Assignment;
import com.example.gradingsystem.model.DocumentSubmission;
import com.example.gradingsystem.repository.AssignmentRepository;
import com.example.gradingsystem.repository.DocumentSubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private DocumentSubmissionRepository documentSubmissionRepository;
    
    // Create a new assignment
    public Assignment createAssignment(Assignment assignment) {
        return assignmentRepository.save(assignment); // Save assignment to DB
    }
    
    //  Get upcoming Assignments
    public List<Assignment> getUpcomingAssignments(Long studentId) {
        // Fetch all assignments
        List<Assignment> allAssignments = assignmentRepository.findAll();

        // Fetch all submissions for the student
        List<DocumentSubmission> submissions = documentSubmissionRepository.findByStudentId(studentId);

        // Extract the assignment IDs that have been submitted
        List<Long> assignmentIds = submissions.stream()
            .map(submission -> submission.getAssignment().getId())
            .collect(Collectors.toList());

        // Filter out the assignments that have been submitted
        List<Assignment> upcomingAssignments = new ArrayList<>();
        for (Assignment assignment : allAssignments) {
            if (!assignmentIds.contains(assignment.getId())) {
                upcomingAssignments.add(assignment);
            }
        }

        return upcomingAssignments;
    }
    
    //  Get submitted assignments
    public List<Assignment> getSubmittedAssignments(Long studentId) {
        // Fetch all submissions for the student
        List<DocumentSubmission> submissions = documentSubmissionRepository.findByStudentId(studentId);

        // Extract the assignment IDs from submissions
        List<Long> assignmentIds = submissions.stream()
            .map(submission -> submission.getAssignment().getId())
            .collect(Collectors.toList());

        // Fetch the assignment details for each submission
        List<Assignment> submittedAssignments = new ArrayList<>();
        for (Long assignmentId : assignmentIds) {
            assignmentRepository.findById(assignmentId).ifPresent(submittedAssignments::add);
        }

        return submittedAssignments;
    }
}
