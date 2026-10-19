# Horizon Secrets Sync Worker

هذا Worker اختياري؛ اللعبة لا تحتاجه كي تعمل أوفلاين. أنشئ قاعدة D1، ضع معرفها في `wrangler.toml`، طبّق `schema.sql` ثم انشر:

```bash
wrangler d1 create horizon-secrets
wrangler d1 execute horizon-secrets --remote --file=worker/schema.sql
wrangler deploy
```

بعد النشر، ابنِ الواجهة مع:

```bash
VITE_CLOUDFLARE_SYNC_URL=https://YOUR-WORKER.workers.dev/sync pnpm build:github
```

النسخة الحالية تستخدم `x-player-id` مجهولاً من العميل، وتتحقق من شكل الطلب وتحد حجمه ومحتواه قبل الحفظ. قبل الإنتاج أضف هوية أو جلسة موثوقة، وحدد `ALLOWED_ORIGIN` بدلاً من `*`، وأضف rate limiting وTurnstile أو آلية مكافحة الغش للوحة الأبطال.
