package com.airesumeforge.common;

/**
 * 简历模块代码常量
 */
public final class SectionCode {

    private SectionCode() {}

    /** 基本信息：姓名、职位、邮箱、电话、地址等（TEXT/扁平对象） */
    public static final String BASIC = "BASIC";

    /** 求职意向：期望职位、城市、薪资范围，工作状态（TEXT） */
    public static final String JOB_INTENT = "JOB_INTENT";

    /** 个人简介：一段话概述（TEXT） */
    public static final String SUMMARY = "SUMMARY";

    /** 工作经历：公司、职位，时间段、要点列表（LIST） */
    public static final String EXPERIENCE = "EXPERIENCE";

    /** 教育背景：学校、学历，专业，时间（LIST） */
    public static final String EDUCATION = "EDUCATION";

    /** 专业技能：标签形式的技能列表，如[Vue精通][Python熟悉]（TAGS） */
    public static final String SKILLS = "SKILLS";

    /** 自我评价：一段话自述（TEXT） */
    public static final String SELF_EVALUATION = "SELF_EVALUATION";

    /** 项目经历：项目名、角色，时间、描述、要点、技术栈（LIST） */
    public static final String PROJECTS = "PROJECTS";

    /** 校园经历：组织、角色，时间（LIST） */
    public static final String CAMPUS = "CAMPUS";

    /** 荣誉证书：有等级、发证机构的证书，如PMP、AWS（LIST） */
    public static final String CERTIFICATES = "CERTIFICATES";

    /** 实习经历：同EXPERIENCE结构（LIST） */
    public static final String INTERNSHIP = "INTERNSHIP";

    /** 技能证书：无等级的简单证书，如四六级、普通话（LIST） */
    public static final String LAC_CERTIFICATES = "LAC_CERTIFICATES";

    /** 自定义模块：用户自行添加（TEXT/LIST/TAGS皆可） */
    public static final String CUSTOM = "CUSTOM";



    /**
     * 系统内置模块代码集合（不允许用户自行创建）
     */
    public static final java.util.Set<String> SYSTEM_CODES = java.util.Set.of(
            BASIC, JOB_INTENT, SUMMARY, EXPERIENCE, EDUCATION,
            SKILLS, SELF_EVALUATION, PROJECTS, CAMPUS,
            CERTIFICATES, INTERNSHIP, LAC_CERTIFICATES
    );

    /**
     * 判断是否为系统内置模块
     */
    public static boolean isSystem(String code) {
        return SYSTEM_CODES.contains(code);
    }
}
