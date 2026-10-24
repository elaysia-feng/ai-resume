<template>
  <section class="add-block">
    <header class="add-block__head">
      <div>
        <h2 class="section-title">添加区块</h2>
        <p class="add-block__hint">选择要加入简历的内容类型，已添加的区块会标记为不可重复添加。</p>
      </div>
      <input
        v-model="keyword"
        type="search"
        class="ui-input add-block__search"
        placeholder="搜索区块，例如「教育」"
        aria-label="搜索区块"
      />
    </header>

    <div v-if="showCustomForm" class="custom-form">
      <div class="custom-form__head">
        <span class="custom-form__title">创建自定义区块</span>
        <button type="button" class="ui-btn ui-btn--sm ui-btn--ghost" @click.stop="toggleCustomForm">收起</button>
      </div>

      <div class="custom-form__body">
        <div class="field">
          <label class="ui-label" for="custom-block-name">区块名称 <span class="required">*</span></label>
          <input
            id="custom-block-name"
            v-model="customName"
            class="ui-input"
            type="text"
            placeholder="例如：开源项目"
          />
        </div>

        <div class="field">
          <span class="ui-label">内容类型</span>
          <div class="radio-group">
            <label class="radio-item">
              <input type="radio" v-model="customSchemaType" value="TEXT" />
              <span>文本（TEXT）</span>
            </label>
            <label class="radio-item">
              <input type="radio" v-model="customSchemaType" value="LIST" />
              <span>列表（LIST）</span>
            </label>
            <label class="radio-item">
              <input type="radio" v-model="customSchemaType" value="TAGS" />
              <span>标签（TAGS）</span>
            </label>
          </div>
        </div>

        <div v-if="customSchemaType === 'TEXT'" class="field">
          <label class="ui-label" for="custom-block-text">内容</label>
          <textarea
            id="custom-block-text"
            v-model="customTextContent"
            class="ui-input custom-form__textarea"
            placeholder="请输入文本内容…"
            rows="4"
          ></textarea>
        </div>

        <div v-if="customSchemaType === 'LIST'" class="field">
          <label class="ui-label" for="custom-block-list">条目内容</label>
          <textarea
            id="custom-block-list"
            v-model="customListContent"
            class="ui-input custom-form__textarea"
            placeholder="每行一条，例如：&#10;项目一描述&#10;项目二描述&#10;项目三描述"
            rows="4"
          ></textarea>
          <span class="field-hint">每行作为一条列表项</span>
        </div>

        <div v-if="customSchemaType === 'TAGS'" class="field">
          <label class="ui-label" for="custom-block-tags">标签</label>
          <input
            id="custom-block-tags"
            v-model="customTagsContent"
            class="ui-input"
            type="text"
            placeholder="例如：Vue, React, TypeScript"
          />
          <span class="field-hint">用逗号分隔多个标签</span>
        </div>

        <button
          type="button"
          class="ui-btn ui-btn--sm ui-btn--primary custom-form__submit"
          :disabled="!customName.trim()"
          @click.stop="submitCustomBlock"
        >
          添加区块
        </button>
      </div>
    </div>

    <template v-for="group in visibleGroups" :key="group.key">
      <section class="add-block__group">
        <h3 class="add-block__group-title">{{ group.label }}</h3>
        <div class="blocks-grid">
          <button
            v-for="block in group.blocks"
            :key="block.id"
            type="button"
            class="block-card"
            :class="{ 'is-added': isAdded(block.id) }"
            :disabled="isAdded(block.id)"
            @click="handleAdd(block.id)"
          >
            <span class="block-icon" aria-hidden="true" v-html="block.icon"></span>
            <span class="block-info">
              <span class="block-name">{{ block.name }}</span>
              <span class="block-desc">{{ block.desc }}</span>
            </span>
            <span v-if="isAdded(block.id)" class="ui-badge ui-badge--success block-check">已添加</span>
          </button>
        </div>
      </section>
    </template>

    <div v-if="!visibleGroups.length" class="ui-empty add-block__empty">
      <div class="ui-empty__title">没有匹配的区块</div>
      <div class="ui-empty__hint">换个关键词试试，或使用下方的「自定义区块」自由创建。</div>
    </div>

    <button
      type="button"
      class="block-card block-card--custom"
      :class="{ 'is-expanded': showCustomForm }"
      @click="toggleCustomForm"
    >
      <span class="block-icon" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        </svg>
      </span>
      <span class="block-info">
        <span class="block-name">自定义区块</span>
        <span class="block-desc">按自己的结构创建内容，如开源项目、获奖情况</span>
      </span>
    </button>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'

const emit = defineEmits(['add-block'])

// 自定义区块表单状态
const showCustomForm = ref(false)
const customName = ref('')
const customSchemaType = ref('TEXT')
const customTextContent = ref('')
const customListContent = ref('')
const customTagsContent = ref('')
const keyword = ref('')

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
    // 每行拆成一条列表项
    const items = customListContent.value
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(text => ({ text }))
    contentJson = { items }
  } else if (customSchemaType.value === 'TAGS') {
    // 逗号分隔成标签项
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

const ICONS = {
  person: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="3.4" stroke="currentColor" stroke-width="1.8"/><path d="M5 20c0-3.4 3.1-5.6 7-5.6s7 2.2 7 5.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  target: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.8"/><circle cx="12" cy="12" r="3.4" stroke="currentColor" stroke-width="1.8"/></svg>',
  cap: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M3.5 9L12 5l8.5 4L12 13 3.5 9z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M7 11v4.4c0 .9 2.2 2 5 2s5-1.1 5-2V11" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  case: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><rect x="3" y="7" width="18" height="13" rx="2" stroke="currentColor" stroke-width="1.8"/><path d="M9 7V5.6A1.6 1.6 0 0 1 10.6 4h2.8A1.6 1.6 0 0 1 15 5.6V7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  school: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><rect x="4" y="3.5" width="16" height="17" rx="1.5" stroke="currentColor" stroke-width="1.8"/><path d="M8.5 8h2M13.5 8h2M8.5 12h2M13.5 12h2M10 20.5V16h4v4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  bolt: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M13 2.5L5 13.5h6l-1 8 8-11h-6l1-8z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  award: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="9" r="5" stroke="currentColor" stroke-width="1.8"/><path d="M8.6 13.4L7 21l5-2.6L17 21l-1.6-7.6" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  pen: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M4 20l4.2-1 9.6-9.6-3.2-3.2L5 15.8 4 20z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M13.8 6.2l3.2 3.2" stroke="currentColor" stroke-width="1.8"/></svg>',
  rocket: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 3c2.8 2.1 4.2 5 4.2 8.3L12 15.6l-4.2-4.3C7.8 8 9.2 5.1 12 3z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><circle cx="12" cy="9.2" r="1.7" stroke="currentColor" stroke-width="1.8"/><path d="M7.6 16.4L5 21l4.6-1.4" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  sprout: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 21v-7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M12 14C12 11 9.5 9 6 9c0 3 2.5 5 6 5zM12 14c0-3 2.5-5 6-5 0 3-2.5 5-6 5z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
}

const blocks = [
  {
    id: 'BASIC',
    group: 'base',
    icon: ICONS.person,
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
    group: 'base',
    icon: ICONS.target,
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
    group: 'story',
    icon: ICONS.cap,
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
    group: 'story',
    icon: ICONS.case,
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
    id: 'INTERNSHIP',
    group: 'story',
    icon: ICONS.sprout,
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
  {
    id: 'PROJECTS',
    group: 'story',
    icon: ICONS.rocket,
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
    id: 'CAMPUS',
    group: 'story',
    icon: ICONS.school,
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
    group: 'proof',
    icon: ICONS.bolt,
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
    group: 'proof',
    icon: ICONS.award,
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
    group: 'proof',
    icon: ICONS.pen,
    name: '自我评价',
    desc: '个人总结与优势',
    sectionCode: 'SELF_EVALUATION',
    sectionTitle: '自我评价',
    schemaType: 'TEXT',
    contentJson: {
      text: '拥有扎实的 Java 基础和良好的面向对象设计能力，熟悉微服务架构设计。具备独立负责业务模块的能力，善于与产品、测试团队高效沟通。热爱技术，持续关注新技术发展，具有较强的学习能力和问题解决能力。',
    },
  },
]

const groupLabels = {
  base: '基本信息',
  story: '经历与背景',
  proof: '能力与证明',
}

const groupOrder = ['base', 'story', 'proof']

// isAdded is called by the template before the store prop is available,
// so we expose sections via a prop passed from the parent
const props = defineProps({
  addedSectionCodes: {
    type: Array,
    default: () => []
  }
})

const visibleGroups = computed(() => {
  const search = keyword.value.trim().toLowerCase()

  return groupOrder
    .map((key) => {
      const matched = blocks.filter((block) => {
        if (block.group !== key) return false
        if (!search) return true
        return [block.name, block.desc, block.sectionCode]
          .join(' ')
          .toLowerCase()
          .includes(search)
      })

      if (!matched.length) return null
      return { key, label: groupLabels[key], blocks: matched }
    })
    .filter(Boolean)
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
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-5);
  padding: var(--ui-space-4);
}

.add-block__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ui-space-4);
  flex-wrap: wrap;
}

.section-title {
  margin: 0;
  font-size: var(--ui-text-lg);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.add-block__hint {
  margin: var(--ui-space-1) 0 0;
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
}

.add-block__search {
  width: 220px;
  max-width: 100%;
}

.add-block__group {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.add-block__group-title {
  margin: 0;
  font-size: var(--ui-text-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ui-fg-muted);
}

.blocks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--ui-space-3);
}

.block-card {
  display: flex;
  align-items: center;
  gap: var(--ui-space-3);
  padding: var(--ui-space-3);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  color: var(--ui-fg-primary);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--ui-duration-fast) var(--ui-ease-out),
    background var(--ui-duration-fast) var(--ui-ease-out),
    box-shadow var(--ui-duration-fast) var(--ui-ease-out);
}

.block-card:hover:not(.is-added) {
  border-color: var(--ui-accent-border);
  background: var(--ui-accent-soft);
  box-shadow: var(--ui-shadow-sm);
}

.block-card:focus-visible {
  outline: none;
  box-shadow: var(--ui-ring);
}

.block-card.is-added {
  cursor: default;
  background: var(--ui-bg-sunken);
}

.block-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--ui-radius-md);
  background: var(--ui-bg-sunken);
  color: var(--ui-fg-secondary);
}

.block-card:hover:not(.is-added) .block-icon {
  background: var(--ui-bg-surface);
  color: var(--ui-fg-accent);
}

.block-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.block-name {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.block-desc {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-secondary);
}

.block-check {
  flex-shrink: 0;
}

.block-card--custom {
  border-style: dashed;
  border-color: var(--ui-border-strong);
  background: transparent;
}

.block-card--custom .block-icon {
  background: var(--ui-accent-soft);
  color: var(--ui-fg-accent);
}

.block-card--custom.is-expanded {
  border-color: var(--ui-accent-border);
  border-style: solid;
  background: var(--ui-accent-soft);
}

.add-block__empty {
  padding: var(--ui-space-8) var(--ui-space-4);
}

/* ── 自定义区块表单 ── */
.custom-form {
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  background: var(--ui-bg-surface);
  overflow: hidden;
}

.custom-form__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ui-space-2);
  padding: var(--ui-space-3) var(--ui-space-4);
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-bg-sunken);
}

.custom-form__title {
  font-size: var(--ui-text-sm);
  font-weight: 600;
  color: var(--ui-fg-primary);
}

.custom-form__body {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-4);
  padding: var(--ui-space-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--ui-space-2);
}

.field :deep(.ui-label) {
  margin-bottom: 0;
}

.required {
  color: var(--ui-danger);
}

.custom-form__textarea {
  resize: vertical;
  min-height: 88px;
  font-family: inherit;
}

.field-hint {
  font-size: var(--ui-text-xs);
  color: var(--ui-fg-muted);
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ui-space-4);
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: var(--ui-space-2);
  font-size: var(--ui-text-sm);
  color: var(--ui-fg-secondary);
  cursor: pointer;
}

.radio-item input[type='radio'] {
  accent-color: var(--ui-accent);
}

.custom-form__submit {
  align-self: flex-start;
}
</style>
