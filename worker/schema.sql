CREATE TABLE IF NOT EXISTS player_progress (
  player_id TEXT PRIMARY KEY,
  progress_json TEXT NOT NULL,
  best_score INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_player_progress_score
  ON player_progress(best_score DESC, updated_at ASC);
