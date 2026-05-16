/**
 * 将 ISO 时间格式化为当前时区的 yyyy-MM-dd HH:mm:ss。
 */
export function formatDateTime(value) {
  if (value == null || value === '') {
    return ''
  }
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/**
 * 导出文件名用时间戳：yyyyMMddHHmmss（当前时区）。
 */
export function formatExportTimestamp(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

/**
 * 从 Content-Disposition 解析下载文件名。
 */
export function parseContentDispositionFileName(disposition, fallback) {
  if (!disposition) {
    return fallback
  }
  const utf8Match = disposition.match(/filename\*=(?:UTF-8''|utf-8'')([^;\s]+)/i)
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1].replace(/^"|"$/g, ''))
    } catch {
      /* 使用 fallback */
    }
  }
  const quoted = disposition.match(/filename="([^"]+)"/i)
  if (quoted) {
    const name = quoted[1]
    if (!name.startsWith('=?')) {
      return name
    }
  }
  const plain = disposition.match(/filename=([^;\s]+)/i)
  if (plain) {
    const name = plain[1].replace(/^"|"$/g, '')
    if (!name.startsWith('=?')) {
      return name
    }
  }
  return fallback
}

export function buildSyncExportFileName(modelCode) {
  return `${modelCode}_${formatExportTimestamp()}.xlsx`
}
