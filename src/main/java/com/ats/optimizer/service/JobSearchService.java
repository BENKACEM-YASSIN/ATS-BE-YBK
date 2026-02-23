package com.ats.optimizer.service;

import com.ats.optimizer.model.dto.JobPost;
import com.ats.optimizer.model.dto.JobSearchRequest;
import com.ats.optimizer.model.dto.JobSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class JobSearchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;
    private final ProfileService profileService;

    @Value("${jsearch.api.key:}")
    private String jsearchApiKey;

    @Value("${jsearch.api.url:https://jsearch.p.rapidapi.com/search}")
    private String jsearchApiUrl;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    public JobSearchResponse searchJobs(JobSearchRequest request, String cvProfileJson) {
        try {
            String location = request.getLocation();
            String jobTitle = request.getJobTitle();
            
            if (location == null || jobTitle == null) {
                JsonNode profile = profileService.getLatestProfile();
                if (profile != null && profile.has("personalInfo")) {
                    JsonNode personalInfo = profile.get("personalInfo");
                    if ((location == null || location.isEmpty()) && personalInfo.has("city")) {
                        location = personalInfo.get("city").asText();
                        if (personalInfo.has("country") && !personalInfo.get("country").asText().isEmpty()) {
                            location += ", " + personalInfo.get("country").asText();
                        }
                    }
                    if (jobTitle == null && personalInfo.has("title")) {
                        jobTitle = personalInfo.get("title").asText();
                    }
                }
            }

            if (location == null || location.isEmpty()) {
                location = "Remote";
            }
            if (jobTitle == null || jobTitle.isEmpty()) {
                jobTitle = "Software Developer";
            }

            List<JobPost> jobs = searchJobsFromAPI(jobTitle, location, request.getMaxResults());
            jobs = extractEmailsFromJobs(jobs);

            jobs = jobs.stream()
                .filter(job -> job.getEmail() != null && !job.getEmail().isEmpty())
                .limit(request.getMaxResults())
                .collect(Collectors.toList());

            if (cvProfileJson != null && !cvProfileJson.isEmpty()) {
                jobs = scoreJobs(jobs, cvProfileJson);
            }

            jobs.sort((a, b) -> {
                double scoreA = a.getMatchScore() != null ? a.getMatchScore() : 0;
                double scoreB = b.getMatchScore() != null ? b.getMatchScore() : 0;
                return Double.compare(scoreB, scoreA);
            });

            JobSearchResponse response = new JobSearchResponse();
            response.setJobs(jobs);
            response.setTotalFound(jobs.size());
            response.setSearchLocation(location);
            response.setSearchTitle(jobTitle);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error searching jobs", e);
            throw new RuntimeException("Failed to search jobs: " + e.getMessage());
        }
    }

    private List<JobPost> searchJobsFromAPI(String jobTitle, String location, int maxResults) {
        if (jsearchApiKey != null && !jsearchApiKey.isEmpty()) {
            try {
                return searchJSearchAPI(jobTitle, location, maxResults);
            } catch (Exception e) {
                log.warn("JSearch API failed, using fallback", e);
            }
        }

        return generateMockJobs(jobTitle, location, maxResults);
    }

    private List<JobPost> searchJSearchAPI(String jobTitle, String location, int maxResults) {
        try {
            String url = jsearchApiUrl + "?query=" + encode(jobTitle + " in " + location) 
                + "&page=1&num_pages=1&date_posted=day";
            
            String response = webClient.get()
                .uri(url)
                .header("X-RapidAPI-Key", jsearchApiKey)
                .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.isEmpty()) {
                log.warn("Empty response from JSearch API");
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            
            List<JobPost> jobs = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode jobNode : data) {
                    if (jobs.size() >= maxResults) break;
                    
                    JobPost job = new JobPost();
                    job.setId(jobNode.has("job_id") ? jobNode.path("job_id").asText() : "job_" + jobs.size());
                    job.setTitle(jobNode.has("job_title") ? jobNode.path("job_title").asText() : jobTitle);
                    job.setCompany(jobNode.has("employer_name") ? jobNode.path("employer_name").asText() : "Unknown");
                    String city = jobNode.has("job_city") ? jobNode.path("job_city").asText() : "";
                    String country = jobNode.has("job_country") ? jobNode.path("job_country").asText() : "";
                    job.setLocation(city + (country.isEmpty() ? "" : ", " + country));
                    job.setDescription(jobNode.has("job_description") ? jobNode.path("job_description").asText() : "");
                    job.setUrl(jobNode.has("job_apply_link") ? jobNode.path("job_apply_link").asText() : "");
                    job.setPostedDate(jobNode.has("job_posted_at_datetime_utc") ? jobNode.path("job_posted_at_datetime_utc").asText() : "");
                    job.setJobType(jobNode.has("job_employment_type") ? jobNode.path("job_employment_type").asText() : "");
                    job.setSalary(jobNode.has("job_max_salary") && !jobNode.path("job_max_salary").isNull() ? 
                        jobNode.path("job_max_salary").asText() : null);
                    
                    jobs.add(job);
                }
            }
            
            return jobs;
        } catch (Exception e) {
            log.error("Error calling JSearch API", e);
            throw new RuntimeException("Failed to search JSearch API: " + e.getMessage(), e);
        }
    }

    private List<JobPost> generateMockJobs(String jobTitle, String location, int maxResults) {
        List<JobPost> jobs = new ArrayList<>();
        String[] companies = {"TechCorp", "InnovateLabs", "Digital Solutions", "CloudTech", "DataSystems", 
                              "WebDev Inc", "CodeMasters", "FutureTech", "SmartApps", "DevOps Pro"};
        
        for (int i = 0; i < Math.min(maxResults, 24); i++) {
            JobPost job = new JobPost();
            job.setId("job_" + (i + 1));
            job.setTitle(jobTitle);
            job.setCompany(companies[i % companies.length]);
            job.setLocation(location);
            job.setDescription("We are looking for an experienced " + jobTitle + 
                " to join our team. Responsibilities include developing software solutions, " +
                "collaborating with cross-functional teams, and implementing best practices. " +
                "Please send your resume to careers@" + companies[i % companies.length].toLowerCase().replace(" ", "") + ".com");
            job.setUrl("https://example.com/jobs/" + (i + 1));
            job.setPostedDate(LocalDate.now().minusDays(i % 2).format(DateTimeFormatter.ISO_DATE));
            job.setJobType("Full-time");
            job.setSalary("$" + (50000 + i * 5000) + "-$" + (80000 + i * 5000));
            jobs.add(job);
        }
        
        return jobs;
    }

    private List<JobPost> extractEmailsFromJobs(List<JobPost> jobs) {
        for (JobPost job : jobs) {
            String email = extractEmailFromText(job.getDescription());
            if (email == null && job.getUrl() != null) {
                try {
                    email = extractEmailFromUrl(job.getUrl());
                } catch (Exception e) {
                    log.debug("Could not extract email from URL: " + job.getUrl(), e);
                }
            }
            job.setEmail(email);
        }
        return jobs;
    }

    private String extractEmailFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractEmailFromUrl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();
            
            String html = doc.html();
            Matcher matcher = EMAIL_PATTERN.matcher(html);
            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            log.debug("Error fetching URL for email extraction: " + url, e);
        }
        return null;
    }

    private List<JobPost> scoreJobs(List<JobPost> jobs, String cvProfileJson) {
        try {
            JsonNode cvData = objectMapper.readTree(cvProfileJson);

            StringBuilder cvText = new StringBuilder();
            if (cvData.has("personalInfo")) {
                JsonNode info = cvData.get("personalInfo");
                if (info.has("title")) cvText.append(info.get("title").asText()).append(" ");
                if (info.has("aboutMe")) cvText.append(info.get("aboutMe").asText()).append(" ");
            }
            if (cvData.has("workExperience")) {
                cvData.get("workExperience").forEach(exp -> {
                    if (exp.has("description")) cvText.append(exp.get("description").asText()).append(" ");
                });
            }
            if (cvData.has("skills")) {
                JsonNode skills = cvData.get("skills");
                if (skills.has("digitalSkills")) {
                    skills.get("digitalSkills").forEach(cat -> {
                        if (cat.has("skills")) {
                            cat.get("skills").forEach(skill -> cvText.append(skill.asText()).append(" "));
                        }
                    });
                }
            }
            
            String cvTextStr = cvText.toString();

            for (JobPost job : jobs) {
                try {
                    var atsResult = geminiService.analyzeATS(cvTextStr, job.getDescription());
                    job.setMatchScore((double) atsResult.getScore());
                    job.setMatchReasoning(atsResult.getMatchReasoning());
                    job.setRequiredSkills(atsResult.getMissingKeywords());
                } catch (Exception e) {
                    log.warn("Failed to score job: " + job.getId(), e);
                    job.setMatchScore(50.0);
                }
            }
            
        } catch (Exception e) {
            log.error("Error scoring jobs", e);
        }
        
        return jobs;
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
