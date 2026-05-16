package com.airesumeforge.common;

import com.airesumeforge.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {

    private SchemaValidator validator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        validator = new SchemaValidator(objectMapper);
        validator.initialize();
    }

    @Test
    void testValidExperience() {
        String validExperience = """
            {
                "company": "ByteDance",
                "position": "Senior Engineer",
                "dateRange": "2020.09 - 2024.03",
                "highlights": ["Built distributed systems", "Led team of 5"]
            }
            """;
        assertDoesNotThrow(() -> validator.validate(SectionCode.EXPERIENCE, validExperience));
    }

    @Test
    void testInvalidExperienceMissingRequired() {
        String invalidExperience = """
            {
                "company": "ByteDance",
                "position": "Senior Engineer"
            }
            """;
        BusinessException ex = assertThrows(BusinessException.class,
            () -> validator.validate(SectionCode.EXPERIENCE, invalidExperience));
        assertTrue(ex.getMessage().contains("Schema校验失败"));
    }

    @Test
    void testValidBasic() {
        String validBasic = """
            {
                "name": "Zhang San",
                "title": "Software Engineer",
                "email": "zhangsan@example.com",
                "phone": "13800138000"
            }
            """;
        assertDoesNotThrow(() -> validator.validate(SectionCode.BASIC, validBasic));
    }

    @Test
    void testValidCustom() {
        String customJson = """
            {"anything": "goes", "nested": {"data": [1, 2, 3]}}
            """;
        assertDoesNotThrow(() -> validator.validate(SectionCode.CUSTOM, customJson));
    }

    @Test
    void testInvalidJson() {
        String invalidJson = "{ invalid json }";
        BusinessException ex = assertThrows(BusinessException.class,
            () -> validator.validate(SectionCode.BASIC, invalidJson));
        assertTrue(ex.getMessage().contains("JSON 解析失败"));
    }

    @Test
    void testAllSectionTypes() {
        assertDoesNotThrow(() -> validator.validate(SectionCode.BASIC,
            "{\"name\":\"Test\",\"title\":\"Engineer\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.JOB_INTENT,
            "{\"targetPosition\":\"Engineer\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.SUMMARY,
            "{\"summary\":\"A summary\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.EDUCATION,
            "{\"school\":\"MIT\",\"degree\":\"MS\",\"major\":\"CS\",\"dateRange\":\"2020-2024\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.SKILLS,
            "{\"skills\":[\"Java\",\"Python\"]}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.SELF_EVALUATION,
            "{\"evaluation\":\"Good\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.PROJECTS,
            "{\"name\":\"Proj\",\"role\":\"Lead\",\"dateRange\":\"2023\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.CAMPUS,
            "{\"organization\":\"Club\",\"role\":\"President\",\"dateRange\":\"2021\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.CERTIFICATES,
            "{\"name\":\"PMP\",\"issuer\":\"PMI\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.INTERNSHIP,
            "{\"company\":\"Co\",\"position\":\"Intern\",\"dateRange\":\"2023\",\"highlights\":[]}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.LAC_CERTIFICATES,
            "{\"name\":\"CET-6\"}"));
        assertDoesNotThrow(() -> validator.validate(SectionCode.CUSTOM,
            "{}"));
    }
}
