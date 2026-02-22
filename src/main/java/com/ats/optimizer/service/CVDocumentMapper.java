package com.ats.optimizer.service;

import com.ats.optimizer.entity.CVCustomSectionEntity;
import com.ats.optimizer.entity.CVDocument;
import com.ats.optimizer.entity.CVEducationEntity;
import com.ats.optimizer.entity.CVPersonalInfoEntity;
import com.ats.optimizer.entity.CVSkillsEntity;
import com.ats.optimizer.entity.CVWorkExperienceEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CVDocumentMapper {

    private final ObjectMapper objectMapper;

    public CVDocument fromJson(JsonNode cvData) {
        CVDocument document = new CVDocument();

        JsonNode root = objectNode(cvData);
        document.setTheme(text(root, "theme"));
        document.setSectionOrderJson(writeJson(arrayNode(root.get("sectionOrder")), "[]"));
        document.setSectionTitlesJson(writeJson(objectNode(root.get("sectionTitles")), "{}"));
        document.setSectionColumnsJson(writeJson(objectNode(root.get("sectionColumns")), "{}"));

        JsonNode personalInfoNode = objectNode(root.get("personalInfo"));
        CVPersonalInfoEntity personalInfo = new CVPersonalInfoEntity();
        personalInfo.setFirstName(text(personalInfoNode, "firstName"));
        personalInfo.setLastName(text(personalInfoNode, "lastName"));
        personalInfo.setTitle(text(personalInfoNode, "title"));
        personalInfo.setEmail(text(personalInfoNode, "email"));
        personalInfo.setPhone(text(personalInfoNode, "phone"));
        personalInfo.setAddress(text(personalInfoNode, "address"));
        personalInfo.setCity(text(personalInfoNode, "city"));
        personalInfo.setPostalCode(text(personalInfoNode, "postalCode"));
        personalInfo.setCountry(text(personalInfoNode, "country"));
        personalInfo.setWebsite(text(personalInfoNode, "website"));
        personalInfo.setLinkedin(text(personalInfoNode, "linkedin"));
        personalInfo.setAboutMe(text(personalInfoNode, "aboutMe"));
        document.setPersonalInfo(personalInfo);

        List<CVWorkExperienceEntity> workItems = new ArrayList<>();
        ArrayNode workArray = arrayNode(root.get("workExperience"));
        for (int i = 0; i < workArray.size(); i++) {
            JsonNode node = objectNode(workArray.get(i));
            CVWorkExperienceEntity item = new CVWorkExperienceEntity();
            item.setItemId(text(node, "id"));
            item.setSortOrder(i);
            item.setTitle(text(node, "title"));
            item.setEmployer(text(node, "employer"));
            item.setCity(text(node, "city"));
            item.setCountry(text(node, "country"));
            item.setStartDate(text(node, "startDate"));
            item.setEndDate(text(node, "endDate"));
            item.setCurrentRole(bool(node, "current"));
            item.setDescription(text(node, "description"));
            workItems.add(item);
        }
        document.setWorkExperiences(workItems);

        List<CVEducationEntity> educationItems = new ArrayList<>();
        ArrayNode educationArray = arrayNode(root.get("education"));
        for (int i = 0; i < educationArray.size(); i++) {
            JsonNode node = objectNode(educationArray.get(i));
            CVEducationEntity item = new CVEducationEntity();
            item.setItemId(text(node, "id"));
            item.setSortOrder(i);
            item.setDegree(text(node, "degree"));
            item.setSchool(text(node, "school"));
            item.setCity(text(node, "city"));
            item.setCountry(text(node, "country"));
            item.setStartDate(text(node, "startDate"));
            item.setEndDate(text(node, "endDate"));
            item.setCurrentEducation(bool(node, "current"));
            item.setDescription(text(node, "description"));
            educationItems.add(item);
        }
        document.setEducations(educationItems);

        JsonNode skillsNode = objectNode(root.get("skills"));
        CVSkillsEntity skills = new CVSkillsEntity();
        skills.setMotherTongue(text(skillsNode, "motherTongue"));
        skills.setOtherLanguagesJson(writeJson(arrayNode(skillsNode.get("otherLanguages")), "[]"));
        skills.setDigitalSkillsJson(writeJson(arrayNode(skillsNode.get("digitalSkills")), "[]"));
        skills.setSoftSkillsJson(writeJson(arrayNode(skillsNode.get("softSkills")), "[]"));
        document.setSkills(skills);

        List<CVCustomSectionEntity> customSections = new ArrayList<>();
        ArrayNode customSectionsArray = arrayNode(root.get("customSections"));
        for (int i = 0; i < customSectionsArray.size(); i++) {
            JsonNode node = objectNode(customSectionsArray.get(i));
            CVCustomSectionEntity item = new CVCustomSectionEntity();
            item.setSectionId(text(node, "id"));
            item.setSortOrder(i);
            item.setTitle(text(node, "title"));
            item.setType(text(node, "type"));
            item.setItemsJson(writeJson(arrayNode(node.get("items")), "[]"));
            customSections.add(item);
        }
        document.setCustomSections(customSections);

        return document;
    }

    public ObjectNode toJson(CVDocument document) {
        ObjectNode root = objectMapper.createObjectNode();
        if (document == null) {
            return root;
        }

        root.put("theme", nonNull(document.getTheme()));

        ArrayNode sectionOrder = parseArray(document.getSectionOrderJson());
        if (sectionOrder.isEmpty()) {
            sectionOrder.add("aboutMe");
            sectionOrder.add("workExperience");
            sectionOrder.add("education");
            sectionOrder.add("skills");
        }
        root.set("sectionOrder", sectionOrder);

        root.set("sectionTitles", parseObject(document.getSectionTitlesJson()));
        root.set("sectionColumns", parseObject(document.getSectionColumnsJson()));

        CVPersonalInfoEntity info = document.getPersonalInfo();
        ObjectNode personalInfo = objectMapper.createObjectNode();
        personalInfo.put("firstName", value(info != null ? info.getFirstName() : null));
        personalInfo.put("lastName", value(info != null ? info.getLastName() : null));
        personalInfo.put("title", value(info != null ? info.getTitle() : null));
        personalInfo.put("email", value(info != null ? info.getEmail() : null));
        personalInfo.put("phone", value(info != null ? info.getPhone() : null));
        personalInfo.put("address", value(info != null ? info.getAddress() : null));
        personalInfo.put("city", value(info != null ? info.getCity() : null));
        personalInfo.put("postalCode", value(info != null ? info.getPostalCode() : null));
        personalInfo.put("country", value(info != null ? info.getCountry() : null));
        personalInfo.put("website", value(info != null ? info.getWebsite() : null));
        personalInfo.put("linkedin", value(info != null ? info.getLinkedin() : null));
        personalInfo.put("aboutMe", value(info != null ? info.getAboutMe() : null));
        root.set("personalInfo", personalInfo);

        ArrayNode workExperience = objectMapper.createArrayNode();
        for (CVWorkExperienceEntity item : nonNullList(document.getWorkExperiences())) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", value(item.getItemId(), item.getId()));
            node.put("title", value(item.getTitle()));
            node.put("employer", value(item.getEmployer()));
            node.put("city", value(item.getCity()));
            node.put("country", value(item.getCountry()));
            node.put("startDate", value(item.getStartDate()));
            node.put("endDate", value(item.getEndDate()));
            node.put("current", Boolean.TRUE.equals(item.getCurrentRole()));
            node.put("description", value(item.getDescription()));
            workExperience.add(node);
        }
        root.set("workExperience", workExperience);

        ArrayNode education = objectMapper.createArrayNode();
        for (CVEducationEntity item : nonNullList(document.getEducations())) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", value(item.getItemId(), item.getId()));
            node.put("degree", value(item.getDegree()));
            node.put("school", value(item.getSchool()));
            node.put("city", value(item.getCity()));
            node.put("country", value(item.getCountry()));
            node.put("startDate", value(item.getStartDate()));
            node.put("endDate", value(item.getEndDate()));
            node.put("current", Boolean.TRUE.equals(item.getCurrentEducation()));
            node.put("description", value(item.getDescription()));
            education.add(node);
        }
        root.set("education", education);

        CVSkillsEntity skillEntity = document.getSkills();
        ObjectNode skills = objectMapper.createObjectNode();
        skills.put("motherTongue", value(skillEntity != null ? skillEntity.getMotherTongue() : null));
        skills.set("otherLanguages", parseArray(skillEntity != null ? skillEntity.getOtherLanguagesJson() : null));
        skills.set("digitalSkills", parseArray(skillEntity != null ? skillEntity.getDigitalSkillsJson() : null));
        skills.set("softSkills", parseArray(skillEntity != null ? skillEntity.getSoftSkillsJson() : null));
        root.set("skills", skills);

        ArrayNode customSections = objectMapper.createArrayNode();
        for (CVCustomSectionEntity item : nonNullList(document.getCustomSections())) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", value(item.getSectionId(), item.getId()));
            node.put("title", value(item.getTitle()));
            node.put("type", value(item.getType(), "custom"));
            node.set("items", parseArray(item.getItemsJson()));
            customSections.add(node);
        }
        root.set("customSections", customSections);

        return root;
    }

    public JsonNode parseRawJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(JsonNode node, String fallback) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull() || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private Boolean bool(JsonNode node, String field) {
        if (node == null || node.isNull() || !node.has(field) || node.get(field).isNull()) return Boolean.FALSE;
        return node.get(field).asBoolean(false);
    }

    private JsonNode objectNode(JsonNode node) {
        return node != null && node.isObject() ? node : objectMapper.createObjectNode();
    }

    private ArrayNode arrayNode(JsonNode node) {
        return node != null && node.isArray() ? (ArrayNode) node : objectMapper.createArrayNode();
    }

    private ArrayNode parseArray(String rawJson) {
        JsonNode parsed = parseRawJson(rawJson);
        return parsed != null && parsed.isArray() ? (ArrayNode) parsed : objectMapper.createArrayNode();
    }

    private ObjectNode parseObject(String rawJson) {
        JsonNode parsed = parseRawJson(rawJson);
        return parsed != null && parsed.isObject() ? (ObjectNode) parsed : objectMapper.createObjectNode();
    }

    private String value(String input) {
        return input == null ? "" : input;
    }

    private String value(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }

    private String value(String input, Long fallbackId) {
        if (input != null && !input.isBlank()) return input;
        return fallbackId == null ? "" : String.valueOf(fallbackId);
    }

    private String nonNull(String value) {
        return value == null || value.isBlank() ? "euro-classic" : value;
    }

    private <T> List<T> nonNullList(List<T> list) {
        return list == null ? List.of() : list;
    }
}

