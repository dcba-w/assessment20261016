import { mount } from '@vue/test-utils'
import { defineComponent, h, ref } from 'vue'
import { describe, expect, it } from 'vitest'

// 组件测试的最小示例：验证 jsdom + @vue/test-utils 环境可用，可作为业务组件测试的模版
// 注意使用 render 函数而非 template 字符串，因为 vue 默认解析到 runtime-only 构建，不含模板编译器
const Counter = defineComponent({
  setup() {
    const count = ref(0)
    return () => h('button', { onClick: () => count.value++ }, String(count.value))
  }
})

describe('组件测试环境', () => {
  it('能挂载组件并触发交互', async () => {
    const wrapper = mount(Counter)
    expect(wrapper.text()).toBe('0')

    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).toBe('1')
  })
})
