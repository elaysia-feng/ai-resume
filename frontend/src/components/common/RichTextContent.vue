<template>
  <div class="rich-text-content" v-html="safeHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import { normalizeRichTextHtml } from '../../utils/richText.js'

const props = defineProps({
  html: {
    type: String,
    default: '',
  },
})

const safeHtml = computed(() => normalizeRichTextHtml(props.html))
</script>

<style scoped>
.rich-text-content {
  white-space: normal;
}

.rich-text-content :deep(p) {
  margin: 0 0 8px;
}

.rich-text-content :deep(p:last-child) {
  margin-bottom: 0;
}

.rich-text-content :deep(h1),
.rich-text-content :deep(h2),
.rich-text-content :deep(h3),
.rich-text-content :deep(h4),
.rich-text-content :deep(h5),
.rich-text-content :deep(h6) {
  margin: 0 0 8px;
  font-size: inherit;
  font-weight: 600;
  line-height: inherit;
}

.rich-text-content :deep(ul),
.rich-text-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.rich-text-content :deep(li) {
  margin-bottom: 4px;
}

.rich-text-content :deep(a) {
  color: inherit;
  text-decoration: underline;
}
</style>
