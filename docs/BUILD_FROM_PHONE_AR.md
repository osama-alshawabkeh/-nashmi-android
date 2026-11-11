# بناء التطبيقين من الهاتف فقط

هذه الطريقة تستخدم Termux لرفع المصدر إلى GitHub، ثم GitHub Actions لبناء APK في السحابة.

## المتطلبات
- Termux من F-Droid الرسمي: https://f-droid.org/packages/com.termux/
- ملف المصدر ZIP على الهاتف.
- أدوات git وgh وunzip.

## الأوامر
```bash
pkg update -y
pkg install -y git gh unzip
termux-setup-storage
mkdir -p ~/nashmi-build
cd ~/nashmi-build
unzip /sdcard/Download/nashmi-android-apk-v1-source.zip
cd nashmi-apk-v1
git init
git branch -M main
git add .
git commit -m "Initial Nashmi Android APK source"
gh auth login
```

سجّل الدخول عبر المتصفح ولا تشارك كلمة المرور أو رمز الدخول.

## البناء
بعد وجود المصدر وملف workflow في المستودع، افتح Actions وشغّل Build Nashmi APKs، ثم نزّل Artifacts الخاصة بـ Server وClient.

هذه عملية Debug للاختبار وليست Release موقعة. لا تفتح منافذ الخادم للعامة قبل إضافة TLS ومراجعة الأمان.