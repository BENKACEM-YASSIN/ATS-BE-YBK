package com.ats.optimizer.service;

import com.ats.optimizer.model.dto.ATSResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public List<String> enhanceText(String text, String type) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key is missing. Please configure your environment.");
        }

        String systemInstruction;
        String prompt;

        if ("job".equals(type)) {
            systemInstruction = "You are an expert CV writer. Rewrite the user's job description. Return a JSON array of 3 strings. 1) ATS Optimized (keyword heavy), 2) Professional & Concise, 3) Action-Oriented. Return ONLY the JSON array.";
            prompt = "Enhance the following job description: \"" + text + "\"";
        } else if ("summary".equals(type)) {
            systemInstruction = "You are an expert CV writer. Rewrite the professional summary. Return a JSON array of 3 strings. 1) ATS Optimized (keyword focused), 2) Engaging & Story-driven, 3) Executive/Professional. Return ONLY the JSON array.";
            prompt = "Enhance the following summary: \"" + text + "\"";
        } else {
            systemInstruction = "You are a career coach. Suggest professional skills based on the input. Return a JSON array with a single string containing a comma-separated list of skills.";
            prompt = "Suggest skills for: \"" + text + "\"";
        }

        String responseText = callGemini(prompt, systemInstruction, 0.7);
        try {
            JsonNode jsonNode = objectMapper.readTree(responseText);
            if (jsonNode.isArray()) {
                List<String> result = new ArrayList<>();
                jsonNode.forEach(node -> result.add(node.asText()));
                return result;
            } else {
                return Collections.singletonList(responseText); // Fallback
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON", e);
            return Collections.singletonList(responseText);
        }
    }

    public List<String> generateTailoredBullets(String draftText, String jobDescription) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key is missing.");
        }

        boolean hasJD = jobDescription != null && !jobDescription.trim().isEmpty();

        String systemInstruction = """
              You are an expert CV writer specializing in ATS optimization.
              
              Task: Convert the candidate's draft text into a comprehensive list of 10-15 high-impact, ATS-optimized bullet points.
              """ + (hasJD ? """
              Target Job Description: Use the provided Job Description to tailor the bullets.
              
              Rules for Generation:
              1. **Identify Weaknesses:** Find gaps between the draft text and the Job Description (missing keywords, soft skills, or technical requirements).
              2. **Bridge Gaps:** Rewrite the draft content to naturally incorporate these missing keywords where truthful.
              3. **Prioritize Relevance:** The first bullet point MUST be the most relevant to the Job Description.
              4. **Merge & Impress:** If multiple draft points are similar, merge them into a single, punchy bullet point.
              5. **Sort Order:** Sort from [Most Relevant/High Impact] to [Least Relevant].
              6. **Quantity:** Generate at least 10 distinct options.
              """ : """
              Rules for Generation:
              1. **Enhance Impact:** Transform simple tasks into achievement-oriented statements.
              2. **Action Verbs:** Start every bullet with a strong action verb (e.g., Orchestrated, Spearheaded, Engineered).
              3. **Metrics:** Include quantifiable results where possible (e.g., "Increased efficiency by 20%").
              4. **Clarity:** Keep bullets concise and professional.
              5. **Quantity:** Generate at least 10 distinct options.
              """) + """
              
              Output Format:
              Return a JSON array of strings only. Example: ["Led team of 5...", "Reduced latency by 20%..."].
            """;

        String prompt = "Draft Text: \"" + draftText + "\"" + (hasJD ? "\n\nTarget Job Description: \"" + jobDescription + "\"" : "");

        String responseText = callGemini(prompt, systemInstruction, 0.5);
        try {
            JsonNode jsonNode = objectMapper.readTree(responseText);
            if (jsonNode.isArray()) {
                List<String> result = new ArrayList<>();
                jsonNode.forEach(node -> result.add(node.asText()));
                return result;
            } else {
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            log.error("Bullet Generation Error:", e);
            throw new RuntimeException("Failed to generate bullets.");
        }
    }

    public JsonNode parseCV(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key is missing.");
        }

        String systemInstruction = """
              You are a data extraction expert. Your task is to extract CV information from the provided text and map it to a specific JSON structure.
              
              Return ONLY valid JSON. Do not include markdown formatting (like ```json).
              
              The JSON structure must match this exactly:
              {
                "personalInfo": {
                  "firstName": "string", "lastName": "string", "title": "string", "email": "string", "phone": "string", "address": "string", "city": "string", "postalCode": "string", "country": "string", "website": "string", "linkedin": "string", "aboutMe": "string (summary)"
                },
                "workExperience": [
                  { "id": "string (unique)", "title": "string", "employer": "string", "city": "string", "country": "string", "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD", "current": boolean, "description": "string (html/text)" }
                ],
                "education": [
                  { "id": "string (unique)", "degree": "string", "school": "string", "city": "string", "country": "string", "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD", "current": boolean, "description": "string" }
                ],
                "skills": {
                  "motherTongue": "string",
                  "otherLanguages": [{ "id": "string", "language": "string", "listening": "Level (A1-C2)", "reading": "Level", "spokenInteraction": "Level", "spokenProduction": "Level", "writing": "Level" }],
                  "digitalSkills": [{ "id": "string", "name": "Category Name", "skills": ["string"] }],
                  "softSkills": ["string"]
                }
              }
        
              For dates, use YYYY-MM-DD format. If only year is available, use YYYY-01-01. If current, set 'current' to true.
              Map language levels to A1, A2, B1, B2, C1, C2 if possible, otherwise default to B1.
              For digital skills, try to categorize them (e.g., Programming, Tools), if not sure put them in a category named "General".
            """;

        String prompt = "Parse this CV text into JSON: \n\n" + text;

        String responseText = callGemini(prompt, systemInstruction, 0.2);
        try {
            JsonNode parsed = objectMapper.readTree(responseText);
            
            // Add defaults for UI tracking (as per frontend logic)
            if (parsed.isObject()) {
                ObjectNode obj = (ObjectNode) parsed;
                if (!obj.has("theme")) obj.put("theme", "euro-classic");
                if (!obj.has("sectionOrder")) {
                    ArrayNode order = obj.putArray("sectionOrder");
                    order.add("aboutMe");
                    order.add("workExperience");
                    order.add("education");
                    order.add("skills");
                }
                if (!obj.has("customSections")) {
                    obj.putArray("customSections");
                }
            }
            return parsed;
        } catch (JsonProcessingException e) {
            log.error("Gemini Parse Error:", e);
            throw new RuntimeException("Failed to parse CV text.");
        }
    }

    public ATSResult analyzeATS(String cvText, String jobDescription) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key is missing.");
        }

        String systemInstruction = """
              You are an expert ATS (Applicant Tracking System) analyzer. 
              Compare the candidate's CV text against the Job Description.
              
              Return a strict JSON object with:
              1. "score": a number between 0 and 100 representing the match percentage. Be strict.
              2. "matchReasoning": a brief summary of why they scored this way.
              3. "missingKeywords": a list of important skills or keywords found in the JD but missing in the CV.
              4. "suggestions": 2-3 actionable tips to improve the CV for this specific role.
        
              Do not include markdown formatting.
            """;
        
        String prompt = "CV Text: \"" + cvText + "\"\n\nJob Description: \"" + jobDescription + "\"";

        try {
            String responseText = callGemini(prompt, systemInstruction, 0.3);
            return objectMapper.readValue(responseText, ATSResult.class);
        } catch (Exception e) {
            log.error("ATS Analysis Error:", e);
            // Return fallback as per frontend logic
            ATSResult fallback = new ATSResult();
            fallback.setScore(0);
            fallback.setMatchReasoning("Could not analyze at this time.");
            fallback.setMissingKeywords(new ArrayList<>());
            fallback.setSuggestions(Collections.singletonList("Try manual comparison."));
            return fallback;
        }
    }

    private String callGemini(String prompt, String systemInstruction, double temperature) {
        String trimmedApiKey = apiKey != null ? apiKey.trim() : "";
        String trimmedApiUrl = apiUrl != null ? apiUrl.trim() : "";

        if (trimmedApiKey.isEmpty() || trimmedApiUrl.isEmpty()) {
            throw new RuntimeException("API Key or URL is missing.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        
        // Contents
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(part));
        requestBody.put("contents", Collections.singletonList(content));

        // System Instruction
        Map<String, Object> sysPart = new HashMap<>();
        sysPart.put("text", systemInstruction);
        Map<String, Object> sysContent = new HashMap<>();
        sysContent.put("parts", Collections.singletonList(sysPart));
        requestBody.put("systemInstruction", sysContent);

        // Config
        Map<String, Object> config = new HashMap<>();
        config.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", config);

        String url = trimmedApiUrl + "?key=" + trimmedApiKey;

        try {
            String jsonResponse = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(jsonResponse);
            // Extract the text from the response structure: candidates[0].content.parts[0].text
            JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
            if (textNode.isMissingNode()) {
                throw new RuntimeException("Invalid response from Gemini");
            }
            return textNode.asText();

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to communicate with AI service: " + e.getMessage());
        }
    }
}
