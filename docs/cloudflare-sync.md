# مزامنة Cloudflare Workers / D1

الإصدار الحالي أوفلاين أولاً: يحفظ التقدم في `localStorage` ويعمل دون اتصال. عند تجهيز Worker متوافق، يضبط النشر متغير البناء `VITE_CLOUDFLARE_SYNC_URL` إلى عنوان `POST`، وسيبدأ `src/engine/sync.ts` إرسال سجل تقدم محدود إلى الجسم التالي:

```json
{
  "game": "horizon-secrets",
  "version": 1,
  "progress": { "unlockedLevels": 2, "completedLevels": [true, true, false, false] }
}
```

ينبغي على Worker التحقق من حجم الطلب، وعدم الثقة في النتائج المحلية، ثم تخزين سجل التقدم ولوحة الأبطال في D1 بعد إضافة هوية لاعب مناسبة. لا تُضمَّن أسرار Cloudflare في الواجهة أو في GitHub Pages. عند غياب المتغير أو انقطاع الشبكة تبقى اللعبة محلية وتُظهر حالة المزامنة المؤجلة دون تعطيل اللعب.
