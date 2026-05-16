<template>
  <section class="add-block">
    <h2 class="section-title">添加模块</h2>
    <div class="blocks-grid">
      <button
        v-for="block in blocks"
        :key="block.id"
        class="block-card"
        :class="{ added: isAdded(block.id) }"
        :disabled="isAdded(block.id)"
        @click="handleAdd(block.id)"
      >
        <span class="block-icon">{{ block.icon }}</span>
        <div class="block-info">
          <span class="block-name">{{ block.name }}</span>
          <span class="block-desc">{{ block.desc }}</span>
        </div>
        <span v-if="isAdded(block.id)" class="block-check">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="8" fill="#22c55e"/>
            <path d="M4.5 8.5L7 11L11.5 5.5" stroke="#fff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
      </button>

      <!-- Custom Block Card -->
      <button
        class="block-card custom-card"
        :class="{ expanded: showCustomForm }"
        @click="toggleCustomForm"
      >
        <span class="block-icon">+</span>
        <div class="block-info">
          <span class="block-name">自定义模块</span>
          <span class="block-desc">创建自定义模块</span>
        </div>
      </button>

      <!-- Custom Block Inline Form -->
      <div v-if="showCustomForm" class="custom-form">
        <div class="form-header">
          <span class="form-title">创建自定义模块</span>
          <button class="close-form-btn" @click.stop="showCustomForm = false">×</button>
        </div>

        <div class="form-body">
          <div class="field">
            <label class="field-label">模块名称 <span class="required">*</span></label>
            <input
              class="field-input"
              type="text"
              v-model="customName"
              placeholder="请输入模块名称"
            />
          </div>

          <div class="field">
            <label class="field-label">内容类型</label>
            <div class="radio-group">
              <label class="radio-item">
                <input type="radio" v-model="customSchemaType" value="TEXT" />
                <span>文本 (TEXT)</span>
              </label>
              <label class="radio-item">
                <input type="radio" v-model="customSchemaType" value="LIST" />
                <span>列表 (LIST)</span>
              </label>
              <label class="radio-item">
                <input type="radio" v-model="customSchemaType" value="TAGS" />
                <span>标签 (TAGS)</span>
              </label>
            </div>
          </div>

          <!-- TEXT: text area -->
          <div v-if="customSchemaType === 'TEXT'" class="field">
            <label class="field-label">内容</label>
            <textarea
              class="field-input field-textarea"
              v-model="customTextContent"
              placeholder="请输入文本内容..."
              rows="4"
            ></textarea>
          </div>

          <!-- LIST: simple item form -->
          <div v-if="customSchemaType === 'LIST'" class="field">
            <label class="field-label">条目内容</label>
            <textarea
              class="field-input field-textarea"
              v-model="customListContent"
              placeholder="每行一条，例如：&#10;项目一描述&#10;项目二描述&#10;项目三描述"
              rows="4"
            ></textarea>
            <span class="field-hint">每行作为一条列表项</span>
          </div>

          <!-- TAGS: comma-separated -->
          <div v-if="customSchemaType === 'TAGS'" class="field">
            <label class="field-label">标签</label>
            <input
              class="field-input"
              type="text"
              v-model="customTagsContent"
              placeholder="例如：Vue, React, TypeScript"
            />
            <span class="field-hint">用逗号分隔多个标签</span>
          </div>

          <button
            class="submit-btn"
            :disabled="!customName.trim()"
            @click.stop="submitCustomBlock"
          >
            添加模块
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['add-block'])

// Custom block form state
const showCustomForm = ref(false)
const customName = ref('')
const customSchemaType = ref('TEXT')
const customTextContent = ref('')
const customListContent = ref('')
const customTagsContent = ref('')

function toggleCustomForm() {
  showCustomForm.value = !showCustomForm.value
  if (!showCustomForm.value) {
    resetCustomForm()
  }
}

function resetCustomForm() {
  customName.value = ''
  customSchemaType.value = 'TEXT'
  customTextContent.value = ''
  customListContent.value = ''
  customTagsContent.value = ''
}

function submitCustomBlock() {
  if (!customName.value.trim()) return

  const sectionCode = 'CUSTOM_' + Date.now()
  let contentJson = {}

  if (customSchemaType.value === 'TEXT') {
    contentJson = { text: customTextContent.value }
  } else if (customSchemaType.value === 'LIST') {
    // Split by newlines into items array
    const items = customListContent.value
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(text => ({ text }))
    contentJson = { items }
  } else if (customSchemaType.value === 'TAGS') {
    // Split by commas into items array
    const items = customTagsContent.value
      .split(',')
      .map(tag => tag.trim())
      .filter(tag => tag.length > 0)
    contentJson = { items }
  }

  emit('add-block', {
    sectionType: 'CUSTOM',
    sectionCode,
    sectionTitle: customName.value.trim(),
    schemaType: customSchemaType.value,
    contentJson,
  })

  showCustomForm.value = false
  resetCustomForm()
}

const blocks = [
  {
    id: 'BASIC',
    icon: '👤',
    name: '基本信息',
    desc: '姓名、职位、联系方式',
    sectionCode: 'BASIC',
    sectionTitle: '基本信息',
    schemaType: 'TEXT',
    contentJson: {
      name: '张三',
      title: 'Java 后端开发工程师',
      email: 'zhangsan@example.com',
      phone: '138-0013-8000',
      location: '北京市朝阳区',
      wechat: 'zhangsan_dev',
      github: 'github.com/zhangsan',
      website: 'zhangsan.com',
      avatar: '',
    },
  },
  {
    id: 'JOB_INTENT',
    icon: '💼',
    name: '求职意向',
    desc: '期望职位、城市、薪资',
    sectionCode: 'JOB_INTENT',
    sectionTitle: '求职意向',
    schemaType: 'TEXT',
    contentJson: {
      desiredPosition: 'Java 后端开发工程师',
      desiredCity: '北京',
      salaryRange: '25K-35K',
      employmentType: '全职',
      jobStatus: '随时可入职',
    },
  },
  {
    id: 'EDUCATION',
    icon: '🎓',
    name: '教育背景',
    desc: '学校、学历、专业',
    sectionCode: 'EDUCATION',
    sectionTitle: '教育背景',
    schemaType: 'LIST',
    contentJson: {
      items: [
        { school: '北京理工大学', degree: '硕士', major: '计算机科学与技术', gpa: '', startDate: '2019.09', endDate: '2022.06', description: '' },
        { school: '天津大学', degree: '本科', major: '软件工程', gpa: '', startDate: '2015.09', endDate: '2019.06', description: '' },
      ],
    },
  },
  {
    id: 'EXPERIENCE',
    icon: '💻',
    name: '工作经验',
    desc: '工作经历与成就',
    sectionCode: 'EXPERIENCE',
    sectionTitle: '工作经验',
    schemaType: 'LIST',
    contentJson: {
      items: [
        { company: '字节跳动', position: '后端开发工程师', startDate: '2022.07', endDate: '', current: true, description: '负责抖音评论服务设计与开发，优化缓存架构并推动 Spring Boot + gRPC 框架落地。' },
        { company: '阿里巴巴', position: 'Java 开发实习生', startDate: '2021.03', endDate: '2021.09', current: false, description: '参与电商订单系统开发，完成订单履约流程模块设计与实现。' },
      ],
    },
  },
  {
    id: 'CAMPUS',
    icon: '🏫',
    name: '校园经历',
    desc: '社团、志愿者、竞赛',
    sectionCode: 'CAMPUS',
    sectionTitle: '校园经历',
    schemaType: 'LIST',
    contentJson: {
      items: [
        { organization: '校ACM竞赛团队', role: '队长', startDate: '2017.09', endDate: '2019.06', description: '带领团队获省级 ACM 程序设计竞赛银奖，并组织校内算法竞赛。' },
        { organization: '计算机学院学生会', role: '副主席', startDate: '2016.09', endDate: '2018.06', description: '策划并执行学院科技文化节，参与人数达 2000+。' },
      ],
    },
  },
  {
    id: 'SKILLS',
    icon: '⚡',
    name: '技能特长',
    desc: '专业技能与工具',
    sectionCode: 'SKILLS',
    sectionTitle: '技能特长',
    schemaType: 'TAGS',
    contentJson: {
      items: [{ name: 'Java', proficiency: '精通' }, { name: 'Spring Boot', proficiency: '掌握' }, { name: 'MySQL', proficiency: '掌握' }, { name: 'Redis', proficiency: '熟悉' }, { name: 'Kafka', proficiency: '熟悉' }, { name: 'Docker', proficiency: '掌握' }],
    },
  },
  {
    id: 'CERTIFICATES',
    icon: '🏅',
    name: '荣誉证书',
    desc: '获奖、证书、资质',
    sectionCode: 'CERTIFICATES',
    sectionTitle: '荣誉证书',
    schemaType: 'LIST',
    contentJson: {
      items: [
        { name: 'Oracle Certified Professional, Java SE 8 Programmer', level: '其他', date: '', issuer: 'Oracle', description: '' },
        { name: 'ACM-ICPC 省级银奖', level: '二等奖', date: '', issuer: '', description: '' },
        { name: '校级优秀毕业生', level: '其他', date: '', issuer: '学校', description: '' },
      ],
    },
  },
  {
    id: 'SELF_EVALUATION',
    icon: '✍️',
    name: '自我评价',
    desc: '个人总结与优势',
    sectionCode: 'SELF_EVALUATION',
    sectionTitle: '自我评价',
    schemaType: 'TEXT',
    contentJson: {
      text: '拥有扎实的 Java 基础和良好的面向对象设计能力，熟悉微服务架构设计。具备独立负责业务模块的能力，善于与产品、测试团队高效沟通。热爱技术，持续关注新技术发展，具有较强的学习能力和问题解决能力。',
    },
  },
  {
    id: 'PROJECTS',
    icon: '🚀',
    name: '项目经历',
    desc: '项目经历与成果',
    sectionCode: 'PROJECTS',
    sectionTitle: '项目经历',
    schemaType: 'LIST',
    contentJson: {
      items: [
        { name: '分布式任务调度平台', role: '后端开发', startDate: '2023.03', endDate: '2023.08', description: '采用 XXL-JOB 二次开发，支持集群部署与任务分片，并实现可视化任务配置与失败告警。', techStack: 'Java, Spring Boot, XXL-JOB, MySQL, Redis' },
        { name: '实时数据同步中间件', role: '后端开发', startDate: '2022.10', endDate: '2023.02', description: '基于 Canal + Flink 实现 MySQL 到 Elasticsearch 实时数据同步，支持全量与增量同步。', techStack: 'Java, Flink, Canal, Elasticsearch, MySQL' },
      ],
    },
  },
  {
    id: 'INTERNSHIP',
    icon: '🌱',
    name: '实习经历',
    desc: '实习经历与收获',
    sectionCode: 'INTERNSHIP',
    sectionTitle: '实习经历',
    schemaType: 'LIST',
    contentJson: {
      items: [
        { company: '阿里巴巴', position: 'Java 开发实习生', startDate: '2021.03', endDate: '2021.09', description: '参与电商订单系统开发，完成订单履约流程模块设计与实现。' },
      ],
    },
  },
]

// isAdded is called by the template before the store prop is available,
// so we expose sections via a prop passed from the parent
const props = defineProps({
  addedSectionCodes: {
    type: Array,
    default: () => []
  }
})

function isAdded(id) {
  return props.addedSectionCodes.includes(id)
}

function handleAdd(id) {
  const block = blocks.find(b => b.id === id)
  if (block) {
    emit('add-block', {
      sectionType: 'SYSTEM',
      sectionCode: block.sectionCode,
      sectionTitle: block.sectionTitle,
      schemaType: block.schemaType,
      contentJson: { ...block.contentJson },
    })
  }
}
</script>

<style scoped>
.add-block {
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #16a34a;
  margin: 0 0 20px;
}

.blocks-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.block-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease, background 0.2s ease;
}

.block-card:hover:not(.added) {
  border-color: #22c55e;
  background: #f0fdf4;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.12);
}

.block-card.added {
  opacity: 0.5;
  cursor: default;
}

.block-icon {
  font-size: 22px;
  flex-shrink: 0;
  width: 36px;
  text-align: center;
}

.block-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.block-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.block-desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.block-check {
  flex-shrink: 0;
}

/* ── Custom Block Card ── */
.custom-card {
  border: 2px dashed #22c55e;
  background: #f9fafb;
  color: #6b7280;
  font-weight: 600;
}

.custom-card .block-icon {
  font-size: 22px;
  font-weight: 700;
  color: #22c55e;
}

.custom-card .block-name {
  color: #16a34a;
}

.custom-card:hover:not(.added) {
  border-color: #22c55e;
  background: #f0fdf4;
}

.custom-card.expanded {
  border-color: #22c55e;
  border-style: solid;
  background: #f0fdf4;
}

/* ── Custom Form ── */
.custom-form {
  border: 1px solid #22c55e;
  border-radius: 10px;
  background: #ffffff;
  overflow: hidden;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #e5e7eb;
  background: #f0fdf4;
}

.form-title {
  font-size: 14px;
  font-weight: 700;
  color: #16a34a;
}

.close-form-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  transition: background 0.15s, color 0.15s;
}

.close-form-btn:hover {
  background: #dcfce7;
  color: #22c55e;
}

.form-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.required {
  color: #ef4444;
}

.field-input {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  background-color: #ffffff;
  color: #1a1a2e;
  font-family: inherit;
}

.field-input:focus {
  border-color: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

.field-input::placeholder {
  color: #9ca3af;
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.field-hint {
  font-size: 11px;
  color: #9ca3af;
}

/* Radio group */
.radio-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

.radio-item input[type="radio"] {
  accent-color: #22c55e;
}

/* Submit button */
.submit-btn {
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: #22c55e;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s, opacity 0.15s;
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.25);
}

.submit-btn:hover:not(:disabled) {
  background: #16a34a;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.35);
}

.submit-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
