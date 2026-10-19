export interface Env {
  DB: D1Database
  ALLOWED_ORIGIN?: string
}

type Progress = {
  unlockedLevels?: number
  completedLevels?: boolean[]
  leaderboard?: Array<{ name?: string; score?: number; seconds?: number; at?: number; level?: number }>
}

const cors = (env: Env) => ({
  'access-control-allow-origin': env.ALLOWED_ORIGIN || '*',
  'access-control-allow-methods': 'POST, OPTIONS',
  'access-control-allow-headers': 'content-type, x-player-id',
  'cache-control': 'no-store',
})
const json = (data: unknown, env: Env, status = 200) => new Response(JSON.stringify(data), { status, headers: { ...cors(env), 'content-type': 'application/json; charset=utf-8' } })

function sanitizeProgress(value: unknown): Progress {
  const raw = value && typeof value === 'object' ? value as Progress : {}
  return {
    unlockedLevels: Math.max(1, Math.min(4, Math.floor(Number(raw.unlockedLevels) || 1))),
    completedLevels: Array.from({ length: 4 }, (_, i) => Array.isArray(raw.completedLevels) && raw.completedLevels[i] === true),
    leaderboard: Array.isArray(raw.leaderboard) ? raw.leaderboard.slice(0, 10).map(entry => ({
      name: String(entry?.name || 'مستكشف').slice(0, 16),
      score: Math.max(0, Math.min(9999999, Math.floor(Number(entry?.score) || 0))),
      seconds: Math.max(0, Math.min(99999, Number(entry?.seconds) || 0)),
      level: Math.max(0, Math.min(3, Math.floor(Number(entry?.level) || 0))),
    })) : [],
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors(env) })
    const url = new URL(request.url)
    if (url.pathname === '/health') return json({ ok: true, service: 'horizon-secrets-sync' }, env)
    if (url.pathname !== '/sync' || request.method !== 'POST') return json({ error: 'not_found' }, env, 404)
    const playerId = (request.headers.get('x-player-id') || '').trim().slice(0, 128)
    if (!playerId) return json({ error: 'missing_player_id' }, env, 400)
    let body: { game?: string; version?: number; progress?: unknown }
    try { body = await request.json() as typeof body } catch { return json({ error: 'invalid_json' }, env, 400) }
    if (body.game !== 'horizon-secrets' || body.version !== 1) return json({ error: 'unsupported_payload' }, env, 422)
    const progress = sanitizeProgress(body.progress)
    const bestScore = Math.max(0, ...(progress.leaderboard || []).map(entry => Number(entry.score) || 0))
    await env.DB.prepare(`INSERT INTO player_progress(player_id, progress_json, best_score, updated_at)
      VALUES (?1, ?2, ?3, ?4)
      ON CONFLICT(player_id) DO UPDATE SET progress_json=excluded.progress_json, best_score=MAX(player_progress.best_score, excluded.best_score), updated_at=excluded.updated_at`)
      .bind(playerId, JSON.stringify(progress), bestScore, Date.now()).run()
    return json({ ok: true, savedAt: Date.now() }, env)
  },
}
