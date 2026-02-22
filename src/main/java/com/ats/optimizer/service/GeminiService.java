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

import java.util.regex.Pattern;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "as", "at", "be", "by", "in", "is", "it", "of", "on", "or", "to",
            "the", "and", "for", "with", "from", "that", "this", "into", "your", "you", "are", "our", "their",
            "have", "has", "had", "will", "can", "must", "able", "using", "used", "use", "work", "works",
            "role", "team", "teams", "within", "across", "including", "develop", "developed", "development",
            "experience", "years", "year", "strong", "good", "high", "level", "required", "preferred",
            "responsible", "responsibilities", "knowledge", "skills", "skill", "candidate", "position", "job"
    ));

    private static final Set<String> ACTION_VERBS = new HashSet<>(Arrays.asList(
            "led", "built", "designed", "implemented", "optimized", "delivered", "developed", "created",
            "launched", "improved", "managed", "reduced", "increased", "automated", "scaled", "engineered",
            "spearheaded", "drove", "orchestrated", "integrated", "maintained"
    ));

    private static final Pattern NON_KEYWORD_CHARS = Pattern.compile("[^a-z0-9+# ]");

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
              3. **Prioritize Relevance:** Order bullets from highest ATS impact to lowest based on the Job Description.
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
              Do not add numbering, markdown, or prefixes like "- ".
            """;

        String prompt = "Draft Text: \"" + draftText + "\"" + (hasJD ? "\n\nTarget Job Description: \"" + jobDescription + "\"" : "");

        String responseText = callGemini(prompt, systemInstruction, 0.5);
        try {
            JsonNode jsonNode = objectMapper.readTree(responseText);
            if (jsonNode.isArray()) {
                List<String> result = new ArrayList<>();
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        result.add(node.asText());
                    } else if (node.isObject()) {
                        if (node.has("bullet")) {
                            result.add(node.get("bullet").asText());
                        } else if (node.has("text")) {
                            result.add(node.get("text").asText());
                        }
                    }
                }
                return rankBulletsByAtsImpact(result, jobDescription);
            } else {
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            log.error("Bullet Generation Error:", e);
            throw new RuntimeException("Failed to generate bullets.");
        }
    }

    private List<String> rankBulletsByAtsImpact(List<String> rawBullets, String jobDescription) {
        if (rawBullets == null || rawBullets.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> cleanedBullets = cleanAndDedupeBullets(rawBullets);
        if (cleanedBullets.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> keywordWeights = extractKeywordWeights(jobDescription);
        List<BulletScore> scored = new ArrayList<>();

        for (int i = 0; i < cleanedBullets.size(); i++) {
            String bullet = cleanedBullets.get(i);
            double score = keywordWeights.isEmpty()
                    ? scoreBulletWithoutJobDescription(bullet)
                    : scoreBulletAgainstJobDescription(bullet, keywordWeights);
            scored.add(new BulletScore(bullet, score, i));
        }

        scored.sort(Comparator
                .comparingDouble(BulletScore::score).reversed()
                .thenComparingInt(BulletScore::order));

        List<String> ranked = new ArrayList<>();
        for (BulletScore bulletScore : scored) {
            ranked.add(bulletScore.bullet());
        }
        return ranked;
    }

    private List<String> cleanAndDedupeBullets(List<String> bullets) {
        Set<String> seen = new HashSet<>();
        List<String> cleaned = new ArrayList<>();

        for (String bullet : bullets) {
            String normalized = sanitizeBullet(bullet);
            if (normalized.isEmpty()) {
                continue;
            }

            String dedupeKey = normalized.toLowerCase(Locale.ROOT);
            if (seen.add(dedupeKey)) {
                cleaned.add(normalized);
            }
        }

        return cleaned;
    }

    private String sanitizeBullet(String bullet) {
        if (bullet == null) {
            return "";
        }

        String normalized = bullet.trim();
        normalized = normalized.replaceAll("^[\\-\\*\\d\\.\\)\\s]+", "");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.trim();
    }

    private Map<String, Integer> extractKeywordWeights(String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokenize(jobDescription)) {
            if (token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }
            frequencies.merge(token, 1, Integer::sum);
        }

        if (frequencies.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> keywordWeights = new HashMap<>();
        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            keywordWeights.put(entry.getKey(), Math.min(entry.getValue(), 3) + 1);
        }
        return keywordWeights;
    }

    private double scoreBulletAgainstJobDescription(String bullet, Map<String, Integer> keywordWeights) {
        List<String> tokens = tokenize(bullet);
        if (tokens.isEmpty()) {
            return 0;
        }

        Set<String> uniqueTokens = new HashSet<>(tokens);
        double keywordScore = 0;
        int matchedKeywords = 0;

        for (Map.Entry<String, Integer> entry : keywordWeights.entrySet()) {
            if (uniqueTokens.contains(entry.getKey())) {
                keywordScore += entry.getValue();
                matchedKeywords++;
            }
        }

        double metricsBonus = containsMetric(bullet) ? 1.2 : 0.0;
        double actionVerbBonus = startsWithActionVerb(tokens) ? 0.8 : 0.0;
        double coverageBonus = matchedKeywords == 0
                ? 0.0
                : Math.min(1.5, ((double) matchedKeywords / Math.max(5, keywordWeights.size())) * 4.0);

        return keywordScore + coverageBonus + metricsBonus + actionVerbBonus;
    }

    private double scoreBulletWithoutJobDescription(String bullet) {
        List<String> tokens = tokenize(bullet);
        if (tokens.isEmpty()) {
            return 0;
        }

        double score = 0;
        if (startsWithActionVerb(tokens)) {
            score += 1.0;
        }
        if (containsMetric(bullet)) {
            score += 1.0;
        }

        int wordCount = tokens.size();
        if (wordCount >= 8 && wordCount <= 30) {
            score += 0.6;
        } else if (wordCount > 40) {
            score -= 0.3;
        }

        return score;
    }

    private boolean startsWithActionVerb(List<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        return ACTION_VERBS.contains(tokens.get(0));
    }

    private boolean containsMetric(String text) {
        return text != null && text.matches(".*\\d+.*");
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = NON_KEYWORD_CHARS.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        String[] rawTokens = normalized.trim().split("\\s+");

        List<String> tokens = new ArrayList<>();
        for (String raw : rawTokens) {
            String token = raw.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private record BulletScore(String bullet, double score, int order) {}

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
