# أسرار الأفق — Android Native 3D

هذا مجلد تطبيق Android أصلي مستقل داخل مستودع اللعبة، مكتوب بـ Kotlin ويستخدم LibGDX لرسم عالم 3D خفيف، Room للحفظ المحلي، Ktor للتواصل، وWorkManager لإعادة المزامنة عند عودة الإنترنت. نسخة الويب الأصلية في جذر المستودع محفوظة ولا تُحذف.

## فتح وبناء المشروع

افتح مجلد `android-native/` في Android Studio Hedgehog أو أحدث، واترك Android Studio ينشئ Gradle Wrapper الحقيقي إن لم يكن موجوداً. ثبّت Android SDK 35 وJDK 17، ثم:

```bash
./gradlew assembleDebug
./gradlew bundleRelease
```

ينتج APK تجريبي في `app/build/outputs/apk/debug/` وAAB للإصدار في `app/build/outputs/bundle/release/`.

## التوقيع

أنشئ ملف keystore خارج Git، ثم أضف خصائص التوقيع إلى `~/.gradle/gradle.properties` أو إلى إعدادات release المحلية. لا تضع كلمات المرور في المستودع:

```properties
HORIZON_STORE_FILE=/secure/path/horizon-upload.jks
HORIZON_STORE_PASSWORD=...
HORIZON_KEY_ALIAS=horizon
HORIZON_KEY_PASSWORD=...
```

بعد ربطها في `app/build.gradle.kts` نفّذ `bundleRelease`. ملف `gradlew` الحالي إرشادي لأن بيئة Sandbox لا تحتوي Android SDK/Gradle؛ Android Studio ينشئ wrapper الرسمي عند فتح المشروع.

## المزامنة

ضع رابط Worker نفسه المستخدم في مجلد `worker/` داخل SharedPreferences باسم `sync_url`. التطبيق يعمل أوفلاين كلياً من دون الرابط، ويضع طلبات المزامنة في WorkManager عند توفر الاتصال.
