package com.elias.common;

import com.elias.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简历模块 Schema 校验工具
 */
@Component
public class SchemaValidator {

    private final ObjectMapper objectMapper;
    // 多线程也能安全并发
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        initialize();
    }

    /**
     * Initialize all schemas from SectionSchema definitions.
     * Safe to call multiple times.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        registerSchema(SectionCode.BASIC, buildSchemaJson(SectionSchema.BASIC));
        registerSchema(SectionCode.JOB_INTENT, buildSchemaJson(SectionSchema.JOB_INTENT));
        registerSchema(SectionCode.SUMMARY, buildSchemaJson(SectionSchema.SUMMARY));
        registerSchema(SectionCode.EXPERIENCE, buildSchemaJson(SectionSchema.EXPERIENCE));
        registerSchema(SectionCode.EDUCATION, buildSchemaJson(SectionSchema.EDUCATION));
        registerSchema(SectionCode.SKILLS, buildSchemaJson(SectionSchema.SKILLS));
        registerSchema(SectionCode.SELF_EVALUATION, buildSchemaJson(SectionSchema.SELF_EVALUATION));
        registerSchema(SectionCode.PROJECTS, buildSchemaJson(SectionSchema.PROJECTS));
        registerSchema(SectionCode.CAMPUS, buildSchemaJson(SectionSchema.CAMPUS));
        registerSchema(SectionCode.CERTIFICATES, buildSchemaJson(SectionSchema.CERTIFICATES));
        registerSchema(SectionCode.INTERNSHIP, buildSchemaJson(SectionSchema.INTERNSHIP));
        registerSchema(SectionCode.LAC_CERTIFICATES, buildSchemaJson(SectionSchema.LAC_CERTIFICATES));
        registerSchema(SectionCode.CUSTOM, buildSchemaJson(SectionSchema.CUSTOM));
        initialized = true;
    }

    private String buildSchemaJson(Map<String, Object> sectionSchema) {
        try {
            return objectMapper.writeValueAsString(sectionSchema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize schema", e);
        }
    }

    /**
     * 校验简历内容是否符合对应模块的 Schema 规范
     *
     * @param sectionCode 模块代码
     * @param contentJson JSON 内容字符串
     * @throws BusinessException 校验失败时抛出
     */
    public void validate(String sectionCode, String contentJson) {
        if (sectionCode == null || contentJson == null) {
            throw BusinessException.business("Schema校验失败: sectionCode 或 contentJson 不能为空");
        }

        // CUSTOM 类型做宽松校验，只检查是否为合法 JSON
        if (SectionCode.CUSTOM.equals(sectionCode)) {
            validateAsJson(contentJson);
            return;
        }

        // 获取或构建 Schema
        JsonSchema schema = getSchema(sectionCode);
        if (schema == null) {
            throw BusinessException.business("Schema校验失败: 未找到模块 " + sectionCode + " 的 Schema 定义");
        }

        // 解析待校验的 JSON 内容
        JsonNode contentNode;
        try {
            contentNode = objectMapper.readTree(contentJson);
        } catch (Exception e) {
            throw BusinessException.business("Schema校验失败: JSON 解析失败 - " + e.getMessage());
        }

        // 执行校验
        Set<ValidationMessage> errors = schema.validate(contentNode);
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ValidationMessage msg : errors) {
                sb.append(msg.getMessage()).append("; ");
            }
            throw BusinessException.business("Schema校验失败: " + sb);
        }
    }

    /**
     * 宽松校验：只检查是否为合法 JSON
     */
    private void validateAsJson(String contentJson) {
        try {
            objectMapper.readTree(contentJson);
        } catch (Exception e) {
            throw BusinessException.business("Schema校验失败: 非法的 JSON 格式 - " + e.getMessage());
        }
    }

    /**
     * 获取指定模块的 Schema，未找到则返回 null
     */
    private JsonSchema getSchema(String sectionCode) {
        return schemaCache.get(sectionCode);
    }

    /**
     * 注册模块的 JSON Schema 字符串
     *
     * @param sectionCode 模块代码
     * @param schemaJson  JSON Schema 格式字符串
     */
    public void registerSchema(String sectionCode, String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaJson);
        schemaCache.put(sectionCode, schema);
    }

    /**
     * 清除所有已注册的 Schema
     */
    public void clearAllSchemas() {
        schemaCache.clear();
    }

    /**
     * 移除指定模块的 Schema
     */
    public void removeSchema(String sectionCode) {
        schemaCache.remove(sectionCode);
    }
}
