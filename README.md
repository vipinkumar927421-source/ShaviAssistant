# Shavi — Personal AI Assistant (Android)

Made by Vipin. Powered by Google Gemini.

## 📱 Sirf phone se build karna (laptop ke bina)

Ye project ke andar `.github/workflows/build.yml` already add kiya hua hai — GitHub Actions cloud me APK build kar dega, aapko sirf phone chahiye.

1. **GitHub app ya browser** se ek free GitHub account banayein (agar nahi hai).
2. Phone pe **Termux** install karein ([F-Droid](https://f-droid.org/packages/com.termux/) se — Play Store wala purana version outdated hai).
3. Termux khol kar ye commands chalayein:
   ```
   pkg update && pkg install git -y
   ```
4. Is chat me di gayi `ShaviAssistant.zip` ko phone ki Downloads folder me save karein, phir Termux me:
   ```
   pkg install unzip -y
   cd storage/downloads   # (pehli baar 'termux-setup-storage' chalana padega)
   unzip ShaviAssistant.zip
   cd ShaviAssistant
   ```
5. GitHub par ek naya **empty repository** banayein (naam: `ShaviAssistant`), phir Termux me:
   ```
   git init
   git add .
   git commit -m "Shavi assistant"
   git branch -M main
   git remote add origin https://github.com/<aapka-username>/ShaviAssistant.git
   git push -u origin main
   ```
   (Push karte waqt GitHub username + Personal Access Token maangega — token GitHub.com → Settings → Developer settings → Personal access tokens se bana lein, password ki jagah wahi paste karein.)
6. GitHub.com par apne repo ke **"Actions"** tab me jayein — build automatically start ho jayegi (2-3 minute lagenge).
7. Build complete hone par usi Actions run ke niche **"Artifacts"** section me `shavi-debug-apk` milega — download karein.
8. Downloaded `.apk` file ko phone me tap karke install karein ("Install from unknown sources" allow karna padega).

Isके baad koi bhi code-change karne ke liye Termux me hi `nano` ya `vim` se files edit karke phir `git add . && git commit -m "update" && git push` karein — Actions apne aap naya APK bana degi.

## Kholne ka tarika (Android Studio — agar kabhi laptop mil jaye)

1. Android Studio (Koala ya newer) me `File → Open` → is `ShaviAssistant` folder ko select karein.
2. Gradle sync hone dein (internet chahiye — dependencies download hongi).
3. Ek Android 12+ device/emulator par Run karein.
4. App khulte hi mic + notification permission maangega — allow karein.
5. Settings screen me apni **Gemini API key** paste karke Save karein (Google AI Studio se free key milti hai: https://aistudio.google.com/apikey).
6. "Hey Shavi" bol kar activate karein, ya niche wale mic button se manually bolein.

## Kya real hai is scaffold me

- ✅ Gemini REST API call, proper error handling (invalid key / rate limit / network / server down — sab alag messages).
- ✅ API key `EncryptedSharedPreferences` (Android Keystore backed) me store hoti hai, kabhi UI/logs me wapas nahi dikhti.
- ✅ Real `SpeechRecognizer` + `TextToSpeech` integration, Hindi/English/Hinglish support.
- ✅ Real phone-control actions: app open karna, flashlight, volume, Wi-Fi/Bluetooth panel, call/SMS (sahi Android permissions ke saath).
- ✅ Foreground service (Android 12+ ke background-mic rule ke mutabik), Notification Listener aur Accessibility Service ke real scaffolds (dono explicit user opt-in maangte hain, jaisa Android require karta hai).

## Jo cheezein aapko khud extend karni hongi

1. **True always-on wake word (app band hone par bhi)** — Android SpeechRecognizer sirf app active hone tak kaam karta hai. Isके liye [Picovoice Porcupine](https://picovoice.ai/) jaisi low-power wake-word SDK integrate karni hogi (unki apni free-tier key chahiye hogi).
2. **Bespoke "cute young-girl" voice** — abhi system TTS me se best-matching female/high-pitch voice auto-select hoti hai. Zyada expressive/child-like awaaz ke liye ElevenLabs jaisi paid TTS API use kar sakte hain (Gemini se text milne ke baad us API ko call karke audio generate karwana hoga).
3. **App icon** — abhi ek placeholder vector icon hai; apna launcher icon Android Studio ke Image Asset tool se generate kar lein.
4. **WhatsApp/Instagram jaise apps ke andar specific actions** (jaise "send message to X on WhatsApp") — inke liye Accessibility Service me actual UI-automation logic likhni hogi (scaffold already Manifest + service class me maujood hai).

## Security notes

- API key kabhi bhi hardcode ya GitHub me commit na karein.
- `proguard-rules.pro` release build ke liye already configured hai.
