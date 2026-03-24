<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AppSidebar from '@/components/AppSidebar.vue'

const route = useRoute()

const pageTitle = computed(() => route.meta.title || '业务页面')
const pageDescription = computed(() => route.meta.description || '')
</script>

<template>
  <el-container class="main-shell">
    <el-aside width="240px" class="main-shell__aside">
      <AppSidebar />
    </el-aside>

    <el-container class="main-shell__content">
      <div class="main-shell__topbar">
        <div>
          <h2 class="main-shell__title">{{ pageTitle }}</h2>
          <p v-if="pageDescription" class="main-shell__description">{{ pageDescription }}</p>
        </div>
      </div>

      <el-main class="main-shell__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.main-shell {
  min-height: 100vh;
  background: #f5f7fa;
}

.main-shell__aside {
  border-right: 1px solid #e4e7ed;
  background: #ffffff;
}

.main-shell__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.main-shell__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 0;
}

.main-shell__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.main-shell__description {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.main-shell__main {
  padding: 20px 24px 24px;
}

@media (max-width: 960px) {
  .main-shell {
    display: block;
  }

  .main-shell__aside {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid #e4e7ed;
  }

  .main-shell__topbar {
    padding: 16px 16px 0;
  }

  .main-shell__main {
    padding: 16px;
  }
}
</style>
