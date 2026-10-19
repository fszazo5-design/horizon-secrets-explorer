# النشر على GitHub Pages وتحويل أندرويد

## GitHub Pages

الواجهة مبنية بقاعدة Vite نسبية (`base: './'`) وتنتج `dist/`، لذلك لا تحتاج إلى تعديل اسم المستودع. شغّل:

```bash
pnpm install --frozen-lockfile
pnpm build:github
```

ثم ارفع محتويات `dist/` إلى مصدر GitHub Pages أو استخدم Workflow المشروع. لا تضع أسرار Cloudflare في المستودع؛ مرّر `VITE_CLOUDFLARE_SYNC_URL` كمتغير بناء عام فقط إذا كان Worker مصمماً لقبول هذا الطلب.

## Capacitor

بعد تثبيت أدوات Capacitor في بيئة التطوير:

```bash
pnpm add @capacitor/core
pnpm add -D @capacitor/cli
pnpm add @capacitor/android
pnpm build:github
pnpm exec cap add android
pnpm exec cap sync android
pnpm exec cap open android
```

ملف `capacitor.config.ts` يوجه التطبيق إلى `dist/` ويحافظ على مسارات الأصول النسبية. التخزين المحلي وService Worker يدعمان العمل أوفلاين؛ مزامنة الحفظ تبقى اختيارية عبر `VITE_CLOUDFLARE_SYNC_URL`.
