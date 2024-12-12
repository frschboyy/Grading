package com.example.gradingsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GradingService {
    private final String apiKey;

    public GradingService(String apiKey) {
        this.apiKey = apiKey;
    }

    // Extract text from a PDF file
    public String extractTextFromPDF(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            // Normalize text: Convert text to lowercase and remove extra spaces
            String text = textStripper.getText(document);
            String normalizedText = text.toLowerCase().replaceAll("\\s+", " ").trim();
            return normalizedText;
        }
    }

    // Extract text from a Word document
    public String extractTextFromWord(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            // Normalize text: Convert text to lowercase and remove extra spaces
            String normalizedText = text.toString().toLowerCase().replaceAll("\\s+", " ").trim();
            return normalizedText;
        }
    }

    // Parse questions and answers from extracted text
    public Map<String, String> parseQuestionsAndAnswers(String documentText) {
        Map<String, String> qaPairs = new LinkedHashMap<>();
        String regEx = "Question:\\s*(.*?)\\s*Answer:\\s*(.*?)(?=Question:|$)";
        Pattern pattern = Pattern.compile(regEx, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(documentText);

        while (matcher.find()) {
            String question = matcher.group(1).trim();
            String answer = matcher.group(2).trim();
            qaPairs.put(question, answer);
        }
        return qaPairs;
    }

    // Evaluate answers without a rubric
    public Map<String, String> evaluateAnswersWithoutRubric(Map<String, String> qaPairs) {
        Map<String, String> results = new LinkedHashMap<>();

        qaPairs.forEach((question, answer) -> {
            String prompt = """
                            Evaluate the following answer to the question and provide a score out of 10 with feedback.
                            Question: %s
                            Answer: %s
                            """.formatted(question, answer);

            try {
                String evaluation = callAIAPI(prompt);
                results.put(question, evaluation);
            } catch (Exception e) {
                results.put(question, "Error evaluating answer: " + e.getMessage());
            }
        });
        return results;
    }
    
    // Evaluate answers with a rubric
    public Map<String, String> evaluateAnswersWithRubric(Map<String, String> qaPairs, String rubricText) {
        Map<String, String> results = new LinkedHashMap<>();
        Map<String, String> rubricQA = parseQuestionsAndAnswers(rubricText);

        qaPairs.forEach((question, studentAnswer) -> {
            String rubricAnswer = rubricQA.getOrDefault(question, ""); // Use rubric answer if available
            String prompt = """
                            Compare the following student answer to the teacher's ideal answer and provide a score out of 10 with feedback.
                            Question: %s
                            Rubric's Answer: %s
                            Student's Answer: %s
                            """.formatted(question, rubricAnswer, studentAnswer);

            try {
                String evaluation = callAIAPI(prompt);
                results.put(question, evaluation);
            } catch (Exception e) {
                results.put(question, "Error evaluating answer: " + e.getMessage());
            }
        });
        return results;
    }

    // Call the OpenAI API for evaluation
    private String callAIAPI(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.openai.com/v1/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                                "model": "text-davinci-003",
                                "prompt": "%s",
                                "max_tokens": 100,
                                "temperature": 0
                            }
                            """.formatted(prompt)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return parseAIResponse(response.body());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("AI API call failed");
        }
    }
    
    private String parseAIResponse(String responseBody) {
        try {
            // Use a JSON parsing library
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choicesNode = rootNode.get("choices");

            if (choicesNode.isArray() && choicesNode.size() > 0) {
                return choicesNode.get(0).get("text").asText().trim();
            } else {
                throw new RuntimeException("Invalid response from AI API");
            }
        } catch (JsonProcessingException | RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing AI response");
        }
    }
    
    //  Add scores together
    public Map<String, Integer> calculateAggregateScore(Map<String, String> evaluationResults) {
        int totalScore = 0;
        int totalMaxScore = 0;

        // Loop through the evaluation results
        for (String evaluation : evaluationResults.values()) {
            int[] scores = extractScores(evaluation); // Extract both score and max score
            totalScore += scores[0];   // Add student score
            totalMaxScore += scores[1]; // Add max score
        }

        // Return both the total score and the max score
        return Map.of("totalScore", totalScore, "totalMaxScore", totalMaxScore);
    }

    //  Extract both student score and max score from each question's evaluation
    public int[] extractScores(String evaluation) {
        String regEx = "Score:\\s*(\\d+)/(\\d+)"; // Extract both the student score and max score
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(evaluation);

        if (matcher.find()) {
            int studentScore = Integer.parseInt(matcher.group(1));
            int maxScore = Integer.parseInt(matcher.group(2));
            return new int[]{studentScore, maxScore};
        }
        return new int[]{0, 10};  // Default to 0/10 if no score is found
    }
}