package com.elias.resume.validation;

/** 简历段落 schema 的注册与内容校验契约。 */
public interface SchemaValidator {

    void initialize();

    void validate(String sectionCode, String contentJson);

    void registerSchema(String sectionCode, String schemaJson);

    void clearAllSchemas();

    void removeSchema(String sectionCode);
}
