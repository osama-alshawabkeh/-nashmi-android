# حالة مشروع Nashmi Android

## تم رفعه
- مشروع Server وClient: ملفات Gradle وManifest وواجهات Kotlin والخدمات وBootReceiver.
- Workflow للبناء الآلي على GitHub Actions.
- توثيق المعمارية والبناء والحالة الأمنية.

## ما لم يتم إثباته بعد
- لم يتم تأكيد نجاح Gradle build أو إنتاج APK من GitHub Actions في هذه الجلسة.
- لم يتم اختبار الاتصال الفعلي بين هاتفين أو على TECNO Android 16.
- لم يتم إجراء اختبارات reconnect تحت تبديل الشبكات أو reboot أو اختبارات ضغط.

## مخاطر وقيود معروفة
- TCP خام دون TLS أو certificate validation.
- سر مشترك بين العملاء؛ لا توجد هوية مستقلة وإلغاء جهاز.
- لا توجد heartbeat/health monitoring مكتملة من طرف الخادم.
- لا توجد HTTP host routing أو reverse proxy layer.
- قيود البطارية وForeground Service على Android وواجهات الشركات المصنعة قد تمنع التشغيل المستمر.

**لا تعرض منافذ هذا الإصدار للعامة ولا تمرر بيانات حساسة.** يجب إكمال hardening واختبار أمني قبل أي نشر حقيقي.
