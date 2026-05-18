import { formatDateTime } from './dateFormat.js'

const APP_LABELS = {
  IAM: 'IAM',
  BIDS: 'BIDS',
  EDM: 'EDM',
  APIG: 'APIG'
}

/**
 * 归一化列表行上的所属应用、最后操作人、最后操作时间字段。
 *
 * @param {Record<string, unknown>} row 原始行
 * @param {string} defaultApp 默认所属应用编码
 * @returns {Record<string, unknown>} 附加审计字段后的行
 */
export function mapAuditRow(row, defaultApp = 'IAM') {
  const appCode = row.appCode || row.applicationCode || row.app || defaultApp
  const lastOperator =
    row.lastOperator || row.modifiedBy || row.modifier || row.updatedBy || row.operator || '-'
  const rawTime = row.lastOperateTime ?? row.gmtModified ?? row.updatedAt ?? row.gmtCreate ?? ''
  return {
    ...row,
    appCode: APP_LABELS[String(appCode).toUpperCase()] || String(appCode),
    lastOperator,
    lastOperateTime: rawTime,
    lastOperateTimeText: formatDateTime(rawTime) || '-'
  }
}

/**
 * 批量映射审计字段。
 */
export function mapAuditRows(rows, defaultApp = 'IAM') {
  if (!Array.isArray(rows)) {
    return []
  }
  return rows.map((row) => mapAuditRow(row, defaultApp))
}
