package com.example.gradingsystem.repository;

import com.example.gradingsystem.model.DocumentSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentSubmissionRepository extends JpaRepository<DocumentSubmission, Long> {
    DocumentSubmission findByStudentNameAndAssignmentId(String studentName, String fileName);
    
    List<DocumentSubmission> findByAssignmentId(Long assignmentId);
    
    List<DocumentSubmission> findByStudentId(Long studentId);
}