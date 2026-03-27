import { createApp } from 'vue'
import {
  ElAlert,
  ElAside,
  ElButton,
  ElCard,
  ElContainer,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElPopover,
  ElProgress,
  ElScrollbar,
  ElSelect,
  ElSkeleton,
  ElSpace,
  ElStatistic,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElLoadingDirective,
} from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import './styles.css'

const app = createApp(App)

;[
  ElAlert,
  ElAside,
  ElButton,
  ElCard,
  ElContainer,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElPopover,
  ElProgress,
  ElScrollbar,
  ElSelect,
  ElSkeleton,
  ElSpace,
  ElStatistic,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
].forEach((component) => {
  app.component(component.name, component)
})

app.directive('loading', ElLoadingDirective)
app.use(pinia)
app.use(router)

app.mount('#app')
