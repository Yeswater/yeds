import { readSession } from './authStorage.js'

/** @returns {{ username: string, password: string }} */
export function sessionAuthOpts() {
  const s = readSession()
  if (!s) {
    throw new Error('未登录')
  }
  return s
}
