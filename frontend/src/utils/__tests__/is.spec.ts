import { describe, expect, it } from 'vitest'
import { isEmpty, isEmptyVal, isImgPath, isNumber, isObject, isUrl } from '@/utils/is'

describe('utils/is', () => {
  it('isEmpty 判断空值', () => {
    expect(isEmpty('')).toBe(true)
    expect(isEmpty([])).toBe(true)
    expect(isEmpty({})).toBe(true)
    expect(isEmpty(new Map())).toBe(true)
    expect(isEmpty(new Set())).toBe(true)
    expect(isEmpty(null)).toBe(true)
    expect(isEmpty(undefined)).toBe(true)

    expect(isEmpty('a')).toBe(false)
    expect(isEmpty([1])).toBe(false)
    expect(isEmpty({ a: 1 })).toBe(false)
    // 数字 0 不算空值
    expect(isEmpty(0)).toBe(false)
  })

  it('isEmptyVal 只把空串、null、undefined 当作空', () => {
    expect(isEmptyVal('')).toBe(true)
    expect(isEmptyVal(null)).toBe(true)
    expect(isEmptyVal(undefined)).toBe(true)

    expect(isEmptyVal(0)).toBe(false)
    expect(isEmptyVal([])).toBe(false)
  })

  it('isUrl 区分外链和站内路由', () => {
    expect(isUrl('https://example.com/a')).toBe(true)
    expect(isUrl('http://127.0.0.1:48080/admin-api')).toBe(true)
    expect(isUrl('www.example.com')).toBe(true)

    expect(isUrl('/system/user')).toBe(false)
    expect(isUrl('system/user')).toBe(false)
  })

  it('isImgPath 按后缀识别图片链接', () => {
    expect(isImgPath('https://example.com/a.png')).toBe(true)
    expect(isImgPath('https://example.com/a.jpeg')).toBe(true)

    expect(isImgPath('https://example.com/a.txt')).toBe(false)
  })

  it('isNumber 与 isObject 只匹配对应类型', () => {
    expect(isNumber(1)).toBe(true)
    expect(isNumber('1')).toBe(false)

    expect(isObject({})).toBe(true)
    expect(isObject([])).toBe(false)
    expect(isObject(null)).toBe(false)
  })
})
