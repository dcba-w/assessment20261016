<template>
  <div
    :class="prefixCls"
    class="relative h-[100%]"
  >
    <div class="relative mx-auto h-full flex">
      <div
        :class="`${prefixCls}__left flex-1 relative p-30px lt-xl:hidden overflow-x-hidden overflow-y-auto`"
      >
        <!-- 左上角的 logo + 系统标题 -->
        <div class="relative flex items-center text-white">
          <img alt="" class="mr-10px h-48px w-48px" src="@/assets/imgs/logo.png" />
          <span class="text-20px font-bold">{{ underlineToHump(appStore.getTitle) }}</span>
        </div>
        <!-- 左边欢迎语 -->
        <div class="h-[calc(100%-60px)] flex items-center justify-center">
          <TransitionGroup
            appear
            enter-active-class="animate__animated animate__bounceInLeft"
            tag="div"
          >
            <div
              key="2"
              class="text-22px font-semibold tracking-[6px] text-white/90 drop-shadow-[0_2px_8px_rgba(0,0,0,0.45)]"
            >
              {{ t('login.welcome') }}
            </div>
            <div
              key="3"
              class="mt-18px text-56px font-black tracking-[8px] text-white drop-shadow-[0_0_22px_rgba(56,189,248,0.85)]"
            >
              {{ t('login.message') }}
            </div>
            <div key="4" class="mt-18px h-3px w-72px rounded-full bg-cyan-300/90"></div>
          </TransitionGroup>
        </div>
      </div>
      <div class="relative flex-1 p-30px lt-sm:p-10px overflow-x-hidden overflow-y-auto">
        <!-- 右上角的主题、语言选择 -->
        <div
          class="flex items-center justify-between text-white at-2xl:justify-end at-xl:justify-end"
        >
          <div class="flex items-center at-2xl:hidden at-xl:hidden">
            <img alt="" class="mr-10px h-48px w-48px" src="@/assets/imgs/logo.png" />
            <span class="text-20px font-bold">{{ underlineToHump(appStore.getTitle) }}</span>
          </div>
          <div class="flex items-center justify-end space-x-10px h-48px">
            <ThemeSwitch />
            <LocaleDropdown class="dark:text-white lt-xl:text-white" />
          </div>
        </div>
        <!-- 右边的登录界面 -->
        <Transition appear enter-active-class="animate__animated animate__bounceInRight">
          <div
            class="m-auto h-[calc(100%-60px)] w-[100%] flex items-center at-2xl:max-w-500px at-lg:max-w-500px at-md:max-w-500px at-xl:max-w-500px"
          >
            <!-- 账号登录 -->
            <LoginForm class="m-auto h-auto p-20px rounded-3xl bg-white/92 shadow-xl" />
            <!-- 手机登录 -->
            <MobileForm class="m-auto h-auto p-20px rounded-3xl bg-white/92 shadow-xl" />
            <!-- 二维码登录 -->
            <QrCodeForm class="m-auto h-auto p-20px rounded-3xl bg-white/92 shadow-xl" />
            <!-- 注册
            <RegisterForm class="m-auto h-auto p-20px rounded-3xl bg-white/92 shadow-xl" />
            -->
            <!-- 三方登录 -->
            <SSOLoginVue class="m-auto h-auto p-20px rounded-3xl bg-white/92 shadow-xl" />
            <!-- 忘记密码 -->
            <ForgetPasswordForm class="m-auto h-auto p-20px rounded-3xl bg-white/92 shadow-xl" />
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { underlineToHump } from '@/utils'

import { useDesign } from '@/hooks/web/useDesign'
import { useAppStore } from '@/store/modules/app'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'

import {
  LoginForm,
  MobileForm,
  QrCodeForm,
  // RegisterForm,
  SSOLoginVue,
  ForgetPasswordForm
} from './components'

defineOptions({ name: 'Login' })

const { t } = useI18n()
const appStore = useAppStore()
const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-login;

.#{$prefix-cls} {
  overflow: auto;
  background-image: url('@/assets/imgs/login-bg.png');
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;

  &__left {
    background: transparent;
  }
}
</style>

<style lang="scss">
.dark .login-form {
  .el-divider__text {
    background-color: var(--login-bg-color);
  }

  .el-card {
    background-color: var(--login-bg-color);
  }
}
</style>
