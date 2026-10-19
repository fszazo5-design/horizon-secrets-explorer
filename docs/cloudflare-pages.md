# نشر اللعبة على Cloudflare Pages

استخدم إعدادات المشروع التالية:

- **Root directory:** اتركه فارغاً أو استخدم جذر المستودع.
- **Build command:** `pnpm run build`
- **Build output directory:** `dist`
- **Node.js:** 20 أو أحدث.
- **Package manager:** pnpm 10.18.0.

أمر `pnpm run build` لا يعتمد على Manus Storage؛ الأصول المستخدمة في اللعبة موجودة داخل `public/` وتُنسخ مباشرة إلى `dist/`. لذلك لا تستخدم `scripts/manus-game-assets/restore.mjs` في Cloudflare Pages.

للمزامنة الأونلاين، ضع متغير البيئة أثناء البناء:

```text
VITE_CLOUDFLARE_SYNC_URL=https://YOUR-WORKER.workers.dev/sync
```

المتغير عام داخل JavaScript لأنه رابط API فقط. لا تضع مفاتيح سرية أو مفاتيح توقيع Android في متغيرات واجهة Cloudflare Pages.
