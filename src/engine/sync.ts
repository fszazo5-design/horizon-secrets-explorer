import type { SaveData } from './save'

export type SyncStatus = 'offline' | 'syncing' | 'synced' | 'deferred'
export type SyncResult = { status: SyncStatus; at?: number }

/**
 * The frontend is deployable without a backend. If VITE_CLOUDFLARE_SYNC_URL is configured at
 * build time, the same local record is mirrored to a Worker endpoint whenever connectivity exists.
 * The endpoint is intentionally optional so GitHub Pages and Capacitor both work offline unchanged.
 */
function endpoint(): string | undefined {
  const value = (import.meta.env.VITE_CLOUDFLARE_SYNC_URL as string | undefined)?.trim()
  return value || undefined
}

function playerId(): string {
  const key = 'horizon-secrets.player-id'
  try {
    const current = localStorage.getItem(key)
    if (current) return current
    const next = globalThis.crypto?.randomUUID?.() || `guest-${Date.now()}-${Math.random().toString(36).slice(2)}`
    localStorage.setItem(key, next)
    return next
  } catch {
    return 'guest-session'
  }
}

export async function syncProgress(save: SaveData): Promise<SyncResult> {
  const url = endpoint()
  if (!url || !navigator.onLine) return { status: 'offline' }
  window.dispatchEvent(new CustomEvent('horizon:sync-status', { detail: 'syncing' satisfies SyncStatus }))
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'content-type': 'application/json', 'x-player-id': playerId() },
      body: JSON.stringify({ game: 'horizon-secrets', version: 1, progress: save }),
      keepalive: true,
    })
    if (!response.ok) throw new Error(`sync ${response.status}`)
    const at = Date.now()
    window.dispatchEvent(new CustomEvent('horizon:sync-status', { detail: 'synced' satisfies SyncStatus }))
    return { status: 'synced', at }
  } catch {
    window.dispatchEvent(new CustomEvent('horizon:sync-status', { detail: 'deferred' satisfies SyncStatus }))
    return { status: 'deferred' }
  }
}
