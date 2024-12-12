package com.example.gradingsystem.controller;

import com.example.gradingsystem.model.*;
import com.example.gradingsystem.repository.*;
import com.example.gradingsystem.service.GradingService;
import com.example.gradingsystem.service.PlagiarismService;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    @Autowired
    private PlagiarismService plagiarismService;
    
    @Autowired
    private GradingService gradingService;

    @Autowired
    private DocumentSubmissionRepository documentSubmissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, String>> evaluateSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") Long studentId,
            @RequestParam("assignmentId") Long assignmentId){
//            @RequestParam(value = "rubricBased", defaultValue = "false") boolean rubricBased) {

        try {
            // Extract text based on file type
            String extractedText = extractText(file);
            
            // Check for duplicate submissions
            List<DocumentSubmission> existingSubmissions = documentSubmissionRepository.findByAssignmentId(assignmentId);
            for (DocumentSubmission submission : existingSubmissions) {
                if (Arrays.equals(submission.getExtractedText(), extractedText.getBytes(StandardCharsets.UTF_8))) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Duplicate submission detected for this assignment."));
                }
            }
            
            // Check for plagiarism
            double plagiarismScore = 0;
            List<DocumentSubmission> allSubmissions = documentSubmissionRepository.findAll();
            for (DocumentSubmission submission : allSubmissions) {
                // Calculate TF-IDF similarity using Cosine Similarity
                plagiarismScore = plagiarismService.calculateTFIDFSimilarity(
                        new String(submission.getExtractedText(), StandardCharsets.UTF_8),
                        extractedText
                );
                if (plagiarismScore >= 0.3) { // threshold: 30%
                    break;
                }
            }

            int percentage;
            if (plagiarismScore < 0.3){
                // Parse questions and answers
                Map<String, String> answerToQuestion = gradingService.parseQuestionsAndAnswers(extractedText);

                Map<String, String> evaluationResults;
                Optional<byte[]> rubric = assignmentRepository.findRubricById(assignmentId);
                // Check if rubric exists
                if (rubric.isEmpty()) {
                    // Evaluate without rubric
                    evaluationResults = gradingService.evaluateAnswersWithoutRubric(answerToQuestion);
                } else {
                    String rubricText = new String(rubric.get(), StandardCharsets.UTF_8);

                    // Evaluate based on rubric
                    evaluationResults = gradingService.evaluateAnswersWithRubric(answerToQuestion, rubricText);
                }
                
                // Calculate student's mark for assignment
                Map<String, Integer> scores = gradingService.calculateAggregateScore(evaluationResults);
                int totalScore = scores.get("totalScore");
                int totalMaxScore = scores.get("totalMaxScore");
                double aggregateScore = (double) totalScore / totalMaxScore;
                percentage = (int) aggregateScore*100;
            }
            else {
                percentage = 0;
            }
            
            //  Retrieve the student
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            
            // Retrieve the assignment
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found"));
            
            // Save document submission
            DocumentSubmission submission = DocumentSubmission.builder()
                    .student(student)
                    .assignment(assignment)
                    .extractedText(extractedText.getBytes())
                    .score(percentage)
                    .build();
            documentSubmissionRepository.save(submission);

            if (plagiarismScore < 0.3){
                return ResponseEntity.ok(Map.of("message", "Submission Processed", "score", String.valueOf(percentage)));
            }
            else{
                return ResponseEntity.ok(Map.of("message", "Submission Processed: Plagiarism Detected!", "Similarity Score", String.valueOf(plagiarismScore)));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String extractText(MultipartFile file) throws Exception {
        String extractedText;
        
        if (file == null || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("File or filename is null");
        }
        
        if (file.getOriginalFilename().endsWith(".pdf")) {
            extractedText = gradingService.extractTextFromPDF(file.getInputStream());
        } else if (file.getOriginalFilename().endsWith(".docx")) {
            extractedText = gradingService.extractTextFromWord(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }
        
        return extractedText;
    }
}