# Fairy

Fairy is a personal AI assistant Android app built with Kotlin and Jetpack Compose. The app is designed around a conversational interface powered by Google Gemini, with Groq (Llama) as an automatic fallback when the primary model hits its quota limit.

---

## Overview

Fairy acts as a personal assistant that can hold multi-turn conversations, read real-time device context (battery, connectivity, storage, time), and interact with system-level features like alarms, timers, and Do Not Disturb mode — all driven through natural language. Alongside the chat interface, the app includes a Notes module and a Todo list that can be populated by Fairy directly during a conversation.

---

## Features

| Feature | Status |
|---|---|
| AI Chat with Google Gemini | ✅ Implemented |
| Groq (Llama 3.3-70B) automatic fallback | ✅ Implemented |
| Voice input via foreground `SpeechRecognizer` service | ✅ Implemented |
| Text-to-speech response (TTS) in voice mode | ✅ Implemented |
| Image attachment in chat (sent as Base64 inline data) | ✅ Implemented |
| Persistent chat history (Room, auto-purged after 3 days) | ✅ Implemented |
| Long-term memory — AI can save & recall user facts | ✅ Implemented |
| Notes — create, edit, delete | ✅ Implemented |
| Todo list — created by Fairy during chat, toggle/delete | ✅ Implemented |
| Firebase Authentication (email/password login & register) | ✅ Implemented |
| Profile screen — view chat history & manage memories | ✅ Implemented |
| System actions via chat (set alarm, set timer, toggle DND) | ✅ Implemented |
| Real-time device context injected into system prompt | ✅ Implemented |
| Push notification reminder receiver | ✅ Implemented |

---

## Tech Stack

| Category | Library / Tool |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt (Dagger) |
| Navigation | Navigation Compose |
| Local Database | Room |
| Networking | Retrofit + OkHttp + Kotlin Serialization |
| AI — Primary | Google Gemini API (`gemini-flash-latest`) |
| AI — Fallback | Groq API (`llama-3.3-70b-versatile`) |
| Authentication | Firebase Auth (email/password) |
| Image Loading | Coil |
| Voice | Android `SpeechRecognizer`, `TextToSpeech` (foreground service) |
| Build | AGP 9.3.1, Kotlin 2.2.10, KSP, Version Catalog (`libs.versions.toml`) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

> **Note:** The `google-services` plugin is currently commented out in `app/build.gradle.kts`, meaning Firebase services require a valid `google-services.json` to be active.

---

## Architecture

The project follows **Clean Architecture** with three distinct layers:

```
com.kaganim.fairyai/
├── data/               # Data layer
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Retrofit API services (Gemini, Groq)
│   └── repository/     # Repository implementations
├── domain/             # Domain layer
│   ├── model/          # Domain models (ChatMessage, Note, Todo, User)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Use cases (Auth, Notes, AddTodo)
├── presentation/       # Presentation layer
│   ├── features/       # Feature screens + ViewModels
│   │   ├── auth/       # LoginScreen, RegisterScreen, AuthViewModel
│   │   ├── chat/       # ChatScreen, ChatViewModel, FairyVoiceService, DeviceManager, ReminderReceiver
│   │   ├── notes/      # NotesScreen, NoteDetailScreen, ViewModels
│   │   ├── todo/       # TodoScreen, TodoViewModel
│   │   └── profile/    # ProfileScreen, ProfileViewModel
│   ├── navigation/     # AppNavigation, BottomBar, Screen sealed class
│   ├── common/         # BaseViewModel
│   └── theme/          # App theme
└── di/                 # Hilt modules (AppModule, AuthModule, RepositoryModule)
```

All ViewModels extend a shared `BaseViewModel<S>` that exposes UI state as a `StateFlow`.

---

## Project Structure

```
Fairy AI/
├── app/
│   ├── schemas/                    # Room schema export (version history)
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/kaganim/fairyai/
│   │   └── res/
├── gradle/
│   └── libs.versions.toml          # Version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Current Status

This is an active personal project. The core chat, voice, notes, todo, and authentication flows are fully wired end-to-end. The Room database is at schema version 13, with manual migrations in place. Firebase integration is present in code but depends on a local `google-services.json` that is not tracked in the repository.

---

## Getting Started

### Requirements

- Android Studio Ladybug or later
- JDK 17
- Android device or emulator running API 26+
- A `local.properties` file at the project root (see [Development Notes](#development-notes))

### Steps

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd "Fairy AI"
   ```

2. **Add API keys** — Create or edit `local.properties` (already in `.gitignore`):
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   GROQ_API_KEY=your_groq_api_key_here
   ```

3. **Add Firebase config** — Place your `google-services.json` inside `app/`. Without this file, you must keep the `google-services` plugin commented out in `app/build.gradle.kts` (it already is by default).

4. **Open in Android Studio** — Open the project root folder.

5. **Gradle Sync** — Let Android Studio sync dependencies automatically, or run:
   ```bash
   ./gradlew build
   ```

6. **Run** — Select an emulator or connected device and press **Run** (▶) in Android Studio.

---

## Development Notes

- **API Keys** are injected at build time via `BuildConfig` fields sourced from `local.properties`. The app will compile without them, but the chat feature will not function.
- **Firebase** — `google-services.json` must be present in `app/` for Firebase Authentication to work. The plugin is currently commented out in `build.gradle.kts` to allow building without it.
- **Voice Mode** — The voice feature runs as a foreground service (`FairyVoiceService`) and requires `RECORD_AUDIO` and `FOREGROUND_SERVICE_MICROPHONE` permissions to be granted at runtime.
- **Room Migrations** — The database is at version 13. Manual migrations from versions 5–12 to 13 are defined in `AppDatabase.kt`.

---

## License

License has not been determined yet. This section can be updated when one is chosen.
