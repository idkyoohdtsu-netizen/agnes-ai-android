# 🤖 Agnes AI — Android App

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-orange" />
  <img src="https://img.shields.io/badge/Target%20SDK-34-orange" />
  <img src="https://img.shields.io/badge/Build-Gradle%208.4-lightgrey?logo=gradle" />
</p>

> **Agnes AI** is a powerful Android AI chat assistant app — similar to Claude or ChatGPT — with built-in file management, third-party AI token configuration, and GitHub as persistent memory storage.

---

## ✨ Features

### 💬 AI Chat
- Beautiful dark-themed chat interface with animated message bubbles
- Real-time typing indicator with smooth dot animation
- Support for multi-line messages
- File attachment support in conversations
- Conversation history with auto-scroll

### 📁 File Manager
- Browse files stored in your GitHub repository
- Upload files directly from your device
- Delete files with confirmation dialog
- File type icons (code, images, documents, JSON)
- Human-readable file sizes

### ⚙️ Settings & Token Management
- **Agnes AI API Key** — connect to `https://apihub.agnes-ai.com/v1` or any OpenAI-compatible endpoint
- **Custom Base URL** — point to any AI API provider
- **Model Selector** — choose from GPT-4o, Claude, Gemini, Llama, and more
- **GitHub Integration** — personal access token + repo configuration
- Secure password fields with visibility toggle
- All settings persisted locally via DataStore

### 🧠 GitHub Memory
- Store conversation history in a GitHub repository
- Files and memories persist across sessions
- Full GitHub API integration for file CRUD operations

---

## 🖼️ Screenshots

> _Screenshots coming soon after first release build_

| Chat Screen | File Manager | Settings |
|:-----------:|:------------:|:--------:|
| Coming soon | Coming soon  | Coming soon |

---

## 🏗️ Architecture

```
app/src/main/java/com/agnesai/android/
├── MainActivity.kt                    # Entry point
├── AgnesAiApp.kt                      # Root composable with navigation
├── ui/
│   ├── theme/
│   │   ├── Color.kt                   # Deep purple/indigo color palette
│   │   ├── Type.kt                    # Typography definitions
│   │   └── Theme.kt                   # MaterialTheme dark configuration
│   ├── navigation/
│   │   ├── Screen.kt                  # Sealed class route definitions
│   │   └── NavGraph.kt                # Navigation host
│   ├── components/
│   │   ├── BottomNavBar.kt            # Bottom navigation bar
│   │   ├── MessageBubble.kt           # Chat message UI component
│   │   └── FileAttachmentCard.kt      # File attachment preview
│   └── screens/
│       ├── chat/
│       │   ├── ChatScreen.kt          # Full chat UI with input bar
│       │   └── ChatViewModel.kt       # Chat state management
│       ├── files/
│       │   ├── FilesScreen.kt         # File manager UI
│       │   └── FilesViewModel.kt      # Files state management
│       └── settings/
│           ├── SettingsScreen.kt      # Settings UI with dropdowns
│           └── SettingsViewModel.kt   # Settings state management
└── data/
    ├── model/
    │   ├── Message.kt                 # Chat message data class
    │   ├── FileItem.kt                # File metadata data class
    │   ├── TokenConfig.kt             # AI API configuration
    │   └── GitHubConfig.kt            # GitHub configuration
    ├── local/
    │   └── PreferencesManager.kt      # DataStore preferences
    └── repository/
        ├── AiRepository.kt            # AI API interface
        └── GitHubRepository.kt        # GitHub API interface
```

---

## 🛠️ Tech Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 1.9.22 | Language |
| Jetpack Compose BOM | 2024.02.00 | UI framework |
| Material3 | BOM | Design system |
| Navigation Compose | 2.7.6 | Screen navigation |
| Retrofit | 2.9.0 | HTTP client |
| OkHttp | 4.12.0 | Networking |
| Gson | 2.10.1 | JSON parsing |
| Coroutines | 1.7.3 | Async operations |
| ViewModel Compose | 2.7.0 | MVVM architecture |
| DataStore Preferences | 1.0.0 | Local storage |
| Coil Compose | 2.5.0 | Image loading |
| Accompanist Permissions | 0.32.0 | Runtime permissions |

---

## 🚀 Building the App

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK (API 26+)

### Clone & Build
```bash
git clone https://github.com/idkyoohdtsu-netizen/agnes-ai-android.git
cd agnes-ai-android
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### CI/CD
Every push to `main` triggers a GitHub Actions build that produces a downloadable APK artifact. Check the **Actions** tab for build status and artifact downloads.

---

## ⚙️ Configuration

1. **Launch the app**
2. Navigate to the **Settings** tab (gear icon)
3. Enter your **Agnes AI API Key** from [apihub.agnes-ai.com](https://apihub.agnes-ai.com)
4. (Optional) Set a custom **Base URL** for other OpenAI-compatible providers
5. Select your preferred **AI Model**
6. Configure **GitHub** credentials for memory storage
7. Tap **Save Settings**

---

## 🎨 Design

- **Theme**: Dark mode only with deep purple/indigo accent palette
- **Primary**: `#9B6FDE` (Accent Purple)
- **Secondary**: `#7986CB` (Accent Indigo)
- **Background**: `#0D0D1A` (Near black)
- **Surface**: `#1A1A2E` (Dark navy)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">Built with ❤️ using Kotlin & Jetpack Compose</p>
