function resolveApiBaseUrl() {
  const configured = import.meta.env.VITE_BASE_URL as string
  if (typeof window === 'undefined') {
    return configured
  }
  const { protocol, hostname } = window.location
  if (hostname && hostname !== 'localhost' && hostname !== '127.0.0.1') {
    return `${protocol}//${hostname}:48080`
  }
  return configured
}

const config: {
  base_url: string
  result_code: number | string
  default_headers: AxiosHeaders
  request_timeout: number
} = {
  /**
   * api请求基础路径。通过局域网 IP 打开页面时，自动改打同一台机器的后端，避免还去请求同事本机的 localhost。
   */
  base_url: resolveApiBaseUrl() + import.meta.env.VITE_API_URL,
  /**
   * 接口成功返回状态码
   */
  result_code: 200,

  /**
   * 接口请求超时时间
   */
  request_timeout: 30000,

  /**
   * 默认接口请求类型
   * 可选值：application/x-www-form-urlencoded multipart/form-data
   */
  default_headers: 'application/json'
}

export { config }
