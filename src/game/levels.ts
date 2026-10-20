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
    name: 'سهول التوأم',
    kicker: '01 · قرية الشروق',
    description: 'انطلق بين المروج والبيوت الصغيرة تحت شمسَي الكوكب واجمع شظايا النور.',
    accent: '#62e6ff',
    accentSoft: 'rgba(98,230,255,.16)',
    glyph: '◈',
    puzzle: 'توازن الضوء',
  },
  {
    id: 1,
    name: 'غابة القمرين',
    kicker: '02 · طريق السُدُم',
    description: 'اتبع انعكاسات القمرين بين الأشجار والبيوت، واقفز عبر الممر المخفي.',
    accent: '#ba8cff',
    accentSoft: 'rgba(186,140,255,.16)',
    glyph: '◐',
    puzzle: 'كشف الممر الخفي',
  },
  {
    id: 2,
    name: 'وادي الينابيع',
    kicker: '03 · الحقول الخضراء',
    description: 'أعد توزيع الجسور فوق الينابيع واجعل كل منطقة تحمل نصيبها من الماء.',
    accent: '#55f0b3',
    accentSoft: 'rgba(85,240,179,.16)',
    glyph: '≈',
    puzzle: 'موازنة الموارد',
  },
  {
    id: 3,
    name: 'مرآة الجبل',
    kicker: '04 · الأفق المأهول',
    description: 'ميّز الإشارة الحقيقية من الوهم ثم افتح بوابة الجبل فوق القرية القديمة.',
    accent: '#ffca6b',
    accentSoft: 'rgba(255,202,107,.16)',
    glyph: '✦',
    puzzle: 'تمييز الوهم',
  },
]
