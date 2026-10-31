<template>
  <div
    class="template-document-preview"
    :class="{ 'template-document-preview--large': large }"
    aria-hidden="true"
  >
    <span class="template-document-preview__sample">示例数据</span>
    <div class="template-document-preview__viewport">
      <div class="template-document-preview__page">
        <ResumeTemplate
          :template="templateId"
          :sections="PREVIEW_SECTIONS"
          :body-font-size="10"
          :locale="templateId === 'international-cv' ? 'en' : 'zh'"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import ResumeTemplate from './ResumeTemplate.vue';

defineProps({
  templateId: { type: String, required: true },
  large: { type: Boolean, default: false },
});

const PREVIEW_SECTIONS = [
  {
    id: 101,
    sectionType: 'SYSTEM',
    sectionCode: 'BASIC',
    sectionTitle: '基本信息',
    schemaType: 'TEXT',
    contentJson: {
      name: '林知远',
      title: '后端工程师 · 3 年经验',
      email: 'lin.zhiyuan@example.com',
      phone: '138 0000 1234',
      location: '杭州',
      github: 'github.com/linzy',
      website: 'linzy.dev',
    },
  },
  {
    id: 102,
    sectionType: 'SYSTEM',
    sectionCode: 'JOB_INTENT',
    sectionTitle: '求职意向',
    schemaType: 'TEXT',
    contentJson: { desiredPosition: '后端工程师', desiredCity: '杭州', employmentType: '全职' },
  },
  {
    id: 103,
    sectionType: 'SYSTEM',
    sectionCode: 'SUMMARY',
    sectionTitle: '个人简介',
    schemaType: 'TEXT',
    contentJson: { text: '3 年 Java 服务端开发经验，聚焦高并发接口、数据建模与稳定性治理。' },
  },
  {
    id: 104,
    sectionType: 'SYSTEM',
    sectionCode: 'EDUCATION',
    sectionTitle: '教育背景',
    schemaType: 'LIST',
    contentJson: {
      items: [{
        school: '浙江大学',
        degree: '硕士',
        major: '计算机科学与技术',
        dateRange: '2022.09 - 2025.06',
      }],
    },
  },
  {
    id: 105,
    sectionType: 'SYSTEM',
    sectionCode: 'EXPERIENCE',
    sectionTitle: '工作经历',
    schemaType: 'LIST',
    contentJson: {
      items: [{
        company: '澄知科技',
        position: '后端研发工程师',
        dateRange: '2025.07 - 至今',
        bullets: [
          '拆分结算服务边界，接口 P95 延迟降低 28%。',
          '补齐幂等与告警链路，减少重复订单处理。',
        ],
      }],
    },
  },
  {
    id: 106,
    sectionType: 'SYSTEM',
    sectionCode: 'PROJECTS',
    sectionTitle: '项目经历',
    schemaType: 'LIST',
    contentJson: {
      items: [{
        name: '云舟协作平台',
        role: '服务端负责人',
        dateRange: '2024.03 - 2024.11',
        techStack: 'Java, Spring Boot, Redis, Kafka',
        bullets: [
          '设计异步任务与重试机制，支撑高峰期稳定处理。',
          '将核心查询响应时间缩短 35%，并补充指标看板。',
        ],
      }],
    },
  },
  {
    id: 107,
    sectionType: 'SYSTEM',
    sectionCode: 'SKILLS',
    sectionTitle: '专业技能',
    schemaType: 'TAGS',
    contentJson: { items: ['Java', 'Spring Boot', 'MySQL', 'Redis', 'Kafka'] },
  },
  {
    id: 108,
    sectionType: 'SYSTEM',
    sectionCode: 'CERTIFICATES',
    sectionTitle: '荣誉证书',
    schemaType: 'LIST',
    contentJson: { items: [{ name: '校级优秀研究生' }] },
  },
];
</script>

<style scoped>
.template-document-preview {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--ui-neutral-100);
}

.template-document-preview__sample {
  position: absolute;
  z-index: 2;
  top: 8px;
  right: 8px;
  padding: 3px 7px;
  border: 1px solid var(--ui-grid-line-strong);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-secondary);
  font-size: 10px;
  line-height: 1.3;
}

.template-document-preview__viewport {
  display: flex;
  width: 100%;
  height: 100%;
  justify-content: center;
  overflow: hidden;
}

.template-document-preview__page {
  width: 794px;
  min-width: 794px;
  transform: scale(0.265);
  transform-origin: top center;
}

.template-document-preview--large .template-document-preview__page {
  transform: scale(0.41);
}
</style>
