export type LevelInfo = {
  id: number
  name: string
  kicker: string
  description: string
  accent: string
  accentSoft: string
  glyph: string
  puzzle: string
}

export const LEVELS: LevelInfo[] = [
  {
    id: 0,
    name: 'مدار الاتزان',
    kicker: '01 · نقطة الانطلاق',
    description: 'حرّك النور بين المنصات واجمع شظايا المدار قبل أن ينطفئ المسار.',
    accent: '#62e6ff',
    accentSoft: 'rgba(98,230,255,.16)',
    glyph: '◈',
    puzzle: 'توازن الضوء',
  },
  {
    id: 1,
    name: 'بصائر الظلال',
    kicker: '02 · بوابة الرؤية',
    description: 'اتبع انعكاسات النور، واقفز عبر الممر الذي لا يظهر إلا حين تقترب.',
    accent: '#ba8cff',
    accentSoft: 'rgba(186,140,255,.16)',
    glyph: '◐',
    puzzle: 'كشف الممر الخفي',
  },
  {
    id: 2,
    name: 'ميزان الماء',
    kicker: '03 · الحوض العائم',
    description: 'أعد توزيع الجسور المائية، واجعل كل منصة تحمل نصيبها من التدفق.',
    accent: '#55f0b3',
    accentSoft: 'rgba(85,240,179,.16)',
    glyph: '≈',
    puzzle: 'موازنة الموارد',
  },
  {
    id: 3,
    name: 'مرآة الحقيقة',
    kicker: '04 · الأفق الأخير',
    description: 'ميّز الإشارة الحقيقية من الوهم، ثم افتح بوابة الأفق بإيقاعك الخاص.',
    accent: '#ffca6b',
    accentSoft: 'rgba(255,202,107,.16)',
    glyph: '✦',
    puzzle: 'تمييز الوهم',
  },
]
