package com.airesumeforge.resume.entity;

/**
 * 内容Schema类型枚举
 * <pre>
 * TEXT — 单文本，用于个人简介、自我评价等单段落内容
 * LIST — 列表，用于工作经历、教育背景等可增删条目的内容
 * TAGS — 标签组，用于技能特长等扁平标签
 * </pre>
 */
public enum SchemaType {

    /**
     * 单文本，contentJson = {text: "..."}
     */
    TEXT,

    /**
     * 列表，contentJson = {items: [{...}, {...}]}
     */
    LIST,

    /**
     * 标签组，contentJson = {items: [{name, proficiency}, ...]}
     */
    TAGS
}
