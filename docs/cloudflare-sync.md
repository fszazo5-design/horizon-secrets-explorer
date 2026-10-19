# مزامنة Cloudflare Workers / KV

الإصدار الحالي أوفلاين أولاً: يحفظ التقدم محلياً ويعمل دون اتصال. عند تجهيز Worker، يضبط النشر متغير البناء `VITE_CLOUDFLARE_SYNC_URL` إلى عنوان `POST`، وسيبدأ `src/engine/sync.ts` إرسال سجل تقدم محدود إلى الجسم التالي:

```json
{
  "game": "horizon-secrets",
  "version": 1,
  "progress": { "unlockedLevels": 2, "completedLevels": [true, true, false, false] }
}
```

يرسل العميل أيضاً رأس `x-player-id` مجهولاً ومستمراً محلياً. يخزن Worker السجل في Cloudflare KV تحت مفتاح منفصل لكل لاعب، مثل `player:<player-id>`. KV مناسب لنسخة أولية خفيفة للحفظ والمزامنة، لكنه ليس قاعدة SQL ولا يوفر استعلامات Leaderboard مركبة أو معاملات ذرية مثل D1. يمكن إبقاء لوحة الأبطال محلية في هذه النسخة أو نقلها لاحقاً إلى D1/خدمة مخصصة.

يحتاج توكن Cloudflare المستخدم في GitHub إلى:

```text
Account → Workers Scripts → Edit
Account → Workers KV Storage → Edit
Account → Account Settings → Read
```

لا تُضمَّن أسرار Cloudflare في الواجهة أو في GitHub Pages. عند غياب المتغير أو انقطاع الشبكة تبقى اللعبة محلية وتظهر حالة المزامنة المؤجلة دون تعطيل اللعب.
