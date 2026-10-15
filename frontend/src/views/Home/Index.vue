<template>
  <div>
    <el-card shadow="never">
      <div class="flex items-center">
        <el-avatar :src="avatar" :size="70" class="mr-16px">
          <img src="@/assets/imgs/avatar.gif" alt="" />
        </el-avatar>
        <div>
          <div class="text-20px">
            {{ t('workplace.welcome') }} {{ username }}
          </div>
          <div class="mt-10px text-14px text-gray-500">
            智能工时系统
          </div>
        </div>
      </div>
    </el-card>

    <el-row v-if="visibleModules.length || visibleShortcuts.length" class="mt-8px" :gutter="8">
      <el-col v-if="visibleModules.length" :xl="16" :lg="16" :md="24" :sm="24" :xs="24" class="mb-8px">
        <el-card shadow="never">
          <template #header>
            <span>工作台</span>
          </template>
          <el-row :gutter="12">
            <el-col
              v-for="item in visibleModules"
              :key="item.name"
              :xl="8"
              :lg="8"
              :md="8"
              :sm="12"
              :xs="24"
            >
              <el-card shadow="hover" class="mb-8px cursor-pointer" @click="go(item.url)">
                <div class="flex items-center">
                  <Icon :icon="item.icon" :size="22" class="mr-8px" :style="{ color: item.color }" />
                  <span class="text-16px">{{ item.name }}</span>
                </div>
                <div class="mt-8px text-12px text-gray-400">{{ item.desc }}</div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col v-if="visibleShortcuts.length" :xl="8" :lg="8" :md="24" :sm="24" :xs="24" class="mb-8px">
        <el-card shadow="never">
          <template #header>
            <span>快捷入口</span>
          </template>
          <el-row>
            <el-col v-for="item in visibleShortcuts" :key="item.name" :span="12" class="mb-8px">
              <el-link type="default" :underline="false" @click="go(item.url)">
                <Icon :icon="item.icon" class="mr-8px" :style="{ color: item.color }" />
                {{ item.name }}
              </el-link>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { findRouteByPath } from '@/layout/components/Menu/src/menuRoute'
import { useRouter } from 'vue-router'

defineOptions({ name: 'Index' })

interface HomeEntry {
  name: string
  icon: string
  url: string
  color: string
  desc?: string
  /** 固定入口，不按菜单权限过滤 */
  always?: boolean
}

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const avatar = userStore.getUser.avatar
const username = userStore.getUser.nickname

const modules: HomeEntry[] = [
  { name: '用户管理', desc: '维护账号、部门与岗位', icon: 'ep:user', url: '/system/user', color: '#409EFF' },
  { name: '角色权限', desc: '配置角色与菜单权限', icon: 'ep:lock', url: '/system/role', color: '#67C23A' },
  { name: '菜单管理', desc: '管理系统菜单与路由', icon: 'ep:menu', url: '/system/menu', color: '#E6A23C' },
  { name: '基础设施', desc: '文件、配置与定时任务', icon: 'ep:setting', url: '/infra/job', color: '#909399' }
]

const shortcuts: HomeEntry[] = [
  { name: '个人中心', icon: 'ep:user', url: '/user/profile', color: '#409EFF', always: true },
  { name: '字典管理', icon: 'ep:collection', url: '/system/dict', color: '#67C23A' },
  { name: '操作日志', icon: 'ep:document', url: '/system/log/operate-log', color: '#E6A23C' },
  { name: '代码生成', icon: 'ep:cpu', url: '/infra/codegen', color: '#909399' }
]

const canVisit = (item: HomeEntry) =>
  !!item.always || !!findRouteByPath(permissionStore.getRouters, item.url)

const visibleModules = computed(() => modules.filter(canVisit))
const visibleShortcuts = computed(() => shortcuts.filter(canVisit))

const go = (url: string) => {
  router.push(url)
}
</script>
