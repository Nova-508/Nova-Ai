# NOVA – hands-free Android voice assistant

Say a wake word, then a command ("Nova, open WhatsApp and message Sam I'll be late"). NOVA listens in the background, sends the command to Claude, and carries out the steps (open apps, draft/send messages, search, call, tap, type).

## Build the APK on GitHub
1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab, pick **Build APK**, click **Run workflow** (it also runs on every push to `main`).
3. When it finishes, open the run and download the **NOVA-debug-apk** artifact. Unzip it and install `app-debug.apk` on your phone (allow "install unknown apps").

## First-time setup in the app
1. Paste your Anthropic API key. Open **Settings** to set your wake word, language and voice (male, female, robot, Jarvis, or custom pitch/speed and installed voice packs).
2. Tap steps 1–4 to grant permissions, enable the accessibility service, allow overlay, and disable battery optimization.
3. Tap **Save & start NOVA**.

## Notes
- Android only. Your API key is stored on the phone; for anything beyond personal use, route calls through your own server.
- Accessibility automation matches on-screen text, so it can break when apps change their layout. Banking and some other apps block it.
- Wake-word detection uses Android's speech recognizer, so it needs internet and may beep on some phones. A dedicated on-device engine (e.g. Porcupine) is a good upgrade.
