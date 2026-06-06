package com.elias.common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * JSON Schema definitions for all 13 SectionCode types.
 * Each schema defines required fields and optional fields using Map&lt;String, Object&gt; structure.
 */
public final class SectionSchema {

    private SectionSchema() {}

    // Schema field keys
    public static final String TYPE = "type";
    public static final String PROPERTIES = "properties";
    public static final String REQUIRED = "required";
    public static final String ITEMS = "items";
    public static final String STRING = "string";
    public static final String ARRAY = "array";
    private static final String OBJECT = "object";

    // BASIC: { name: string, title: string, email?: string, phone?: string, location?: string, ... }
    public static final Map<String, Object> BASIC = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "name", Map.of(TYPE, STRING),
                    "title", Map.of(TYPE, STRING),
                    "email", Map.of(TYPE, STRING),
                    "phone", Map.of(TYPE, STRING),
                    "location", Map.of(TYPE, STRING),
                    "wechat", Map.of(TYPE, STRING),
                    "github", Map.of(TYPE, STRING),
                    "website", Map.of(TYPE, STRING),
                    "avatar", Map.of(TYPE, STRING)
            ),
            REQUIRED, List.of("name", "title")
    );

    // JOB_INTENT: { targetPosition: string, city?: string, salaryRange?: string, jobStatus?: string }
    public static final Map<String, Object> JOB_INTENT = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "targetPosition", Map.of(TYPE, STRING),
                    "city", Map.of(TYPE, STRING),
                    "salaryRange", Map.of(TYPE, STRING),
                    "jobStatus", Map.of(TYPE, STRING)
            ),
            REQUIRED, List.of("targetPosition")
    );

    // SUMMARY: { summary: string }
    public static final Map<String, Object> SUMMARY = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "summary", Map.of(TYPE, STRING)
            ),
            REQUIRED, List.of("summary")
    );

    // EXPERIENCE: { items: [{ company: string, position: string, dateRange: string, highlights: string[] }] }
    public static final Map<String, Object> EXPERIENCE = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "items", Map.of(
                            TYPE, ARRAY,
                            ITEMS, Map.of(
                                    TYPE, OBJECT,
                                    PROPERTIES, Map.of(
                                            "company", Map.of(TYPE, STRING),
                                            "position", Map.of(TYPE, STRING),
                                            "dateRange", Map.of(TYPE, STRING),
                                            "highlights", Map.of(TYPE, ARRAY, ITEMS, Map.of(TYPE, STRING))
                                    ),
                                    REQUIRED, List.of("company", "position", "dateRange", "highlights")
                            )
                    )
            ),
            REQUIRED, List.of("items")
    );

    // EDUCATION: { items: [{ school: string, degree: string, major: string, dateRange: string }] }
    public static final Map<String, Object> EDUCATION = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "items", Map.of(
                            TYPE, ARRAY,
                            ITEMS, Map.of(
                                    TYPE, OBJECT,
                                    PROPERTIES, Map.of(
                                            "school", Map.of(TYPE, STRING),
                                            "degree", Map.of(TYPE, STRING),
                                            "major", Map.of(TYPE, STRING),
                                            "dateRange", Map.of(TYPE, STRING)
                                    ),
                                    REQUIRED, List.of("school", "degree", "major", "dateRange")
                            )
                    )
            ),
            REQUIRED, List.of("items")
    );

    // SKILLS: { skills: string[] }
    public static final Map<String, Object> SKILLS = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "skills", Map.of(TYPE, ARRAY, ITEMS, Map.of(TYPE, STRING))
            ),
            REQUIRED, List.of("skills")
    );

    // SELF_EVALUATION: { evaluation: string }
    public static final Map<String, Object> SELF_EVALUATION = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "evaluation", Map.of(TYPE, STRING)
            ),
            REQUIRED, List.of("evaluation")
    );

    // PROJECTS: { items: [{ name: string, role: string, dateRange: string, description?: string, highlights?: string[], techStack?: string[] }] }
    public static final Map<String, Object> PROJECTS = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "items", Map.of(
                            TYPE, ARRAY,
                            ITEMS, Map.of(
                                    TYPE, OBJECT,
                                    PROPERTIES, Map.of(
                                            "name", Map.of(TYPE, STRING),
                                            "role", Map.of(TYPE, STRING),
                                            "dateRange", Map.of(TYPE, STRING),
                                            "description", Map.of(TYPE, STRING),
                                            "highlights", Map.of(TYPE, ARRAY, ITEMS, Map.of(TYPE, STRING)),
                                            "techStack", Map.of(TYPE, ARRAY, ITEMS, Map.of(TYPE, STRING))
                                    ),
                                    REQUIRED, List.of("name", "role", "dateRange")
                            )
                    )
            ),
            REQUIRED, List.of("items")
    );

    // CAMPUS: { items: [{ organization: string, role: string, dateRange: string, description?: string }] }
    public static final Map<String, Object> CAMPUS = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "items", Map.of(
                            TYPE, ARRAY,
                            ITEMS, Map.of(
                                    TYPE, OBJECT,
                                    PROPERTIES, Map.of(
                                            "organization", Map.of(TYPE, STRING),
                                            "role", Map.of(TYPE, STRING),
                                            "dateRange", Map.of(TYPE, STRING),
                                            "description", Map.of(TYPE, STRING)
                                    ),
                                    REQUIRED, List.of("organization", "role", "dateRange")
                            )
                    )
            ),
            REQUIRED, List.of("items")
    );

    // CERTIFICATES: { items: [{ name: string, issuer: string, level?: string, date?: string }] }
    public static final Map<String, Object> CERTIFICATES = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "items", Map.of(
                            TYPE, ARRAY,
                            ITEMS, Map.of(
                                    TYPE, OBJECT,
                                    PROPERTIES, Map.of(
                                            "name", Map.of(TYPE, STRING),
                                            "issuer", Map.of(TYPE, STRING),
                                            "level", Map.of(TYPE, STRING),
                                            "date", Map.of(TYPE, STRING)
                                    ),
                                    REQUIRED, List.of("name", "issuer")
                            )
                    )
            ),
            REQUIRED, List.of("items")
    );

    // INTERNSHIP: 同EXPERIENCE结构
    public static final Map<String, Object> INTERNSHIP = EXPERIENCE;

    // LAC_CERTIFICATES: { items: [{ name: string, score?: string, date?: string }] }
    public static final Map<String, Object> LAC_CERTIFICATES = Map.of(
            TYPE, OBJECT,
            PROPERTIES, Map.of(
                    "items", Map.of(
                            TYPE, ARRAY,
                            ITEMS, Map.of(
                                    TYPE, OBJECT,
                                    PROPERTIES, Map.of(
                                            "name", Map.of(TYPE, STRING),
                                            "score", Map.of(TYPE, STRING),
                                            "date", Map.of(TYPE, STRING)
                                    ),
                                    REQUIRED, List.of("name")
                            )
                    )
            ),
            REQUIRED, List.of("items")
    );

    // CUSTOM: 宽松，只校验是否为合法JSON
    public static final Map<String, Object> CUSTOM = Map.of(
            TYPE, OBJECT
    );

    /**
     * Get schema by section code.
     */
    public static Map<String, Object> getSchema(String sectionCode) {
        return switch (sectionCode) {
            case "BASIC" -> BASIC;
            case "JOB_INTENT" -> JOB_INTENT;
            case "SUMMARY" -> SUMMARY;
            case "EXPERIENCE" -> EXPERIENCE;
            case "EDUCATION" -> EDUCATION;
            case "SKILLS" -> SKILLS;
            case "SELF_EVALUATION" -> SELF_EVALUATION;
            case "PROJECTS" -> PROJECTS;
            case "CAMPUS" -> CAMPUS;
            case "CERTIFICATES" -> CERTIFICATES;
            case "INTERNSHIP" -> INTERNSHIP;
            case "LAC_CERTIFICATES" -> LAC_CERTIFICATES;
            case "CUSTOM" -> CUSTOM;
            default -> throw new IllegalArgumentException("Unknown section code: " + sectionCode);
        };
    }

    /**
     * Check if schema is CUSTOM (lenient validation only checks for valid JSON).
     */
    public static boolean isCustomSchema(String sectionCode) {
        return CUSTOM.equals(sectionCode);
    }
}
