# Euler
Unifying EPFL's Digital Ecosystem Through Conversational AI.

Euler helps students navigate EPFL's digital services through natural language. Instead of switching between Moodle, IS-Academia, library catalogs, and Ed Discussion, students ask questions and Euler executes actions automatically across campus services.

---

## Design (Figma) :

The project design is available on Figma:

 **[Team Project – Figma](https://www.figma.com/design/FBupCDf8gAEmdhbehg3hdd/M1?node-id=0-1&t=pKoTTtTJcKkT7AOU-1)** 
## Setup & Installation

### Prerequisites
- Android Studio Jellyfish or newer  
- Java 17  
- Firebase project configured (Firestore + Authentication)  
- Node.js (for backend functions, optional)
- GitHub account with access to the repository

## Setup & Installation

### Prerequisites
- Android Studio Jellyfish or newer  
- Java 17  
- Firebase project configured (Firestore + Authentication)  
- Node.js (for backend functions, optional)  
- GitHub account with access to the repository  

### Steps
- Clone the repository  
    ```bash
    git clone https://github.com/CS311-Team04/euler.git
    ```
- Open the project in Android Studio  
- Add your `google-services.json` file in the `app/` directory  
- Sync Gradle and build the app  
    ```bash
    ./gradlew assembleDebug
    ```
- Run the emulator or install the generated APK


## Project Structure
```text
euler/
├── app/
│   ├── src/main/java/com/android/sample/
│   │   ├── auth/          # Microsoft Entra ID, Firebase Auth
│   │   ├── home/          # Drawer, Top bar, Settings, Navigation
│   │   ├── chat/          # RAG assistant integration (in progress)
│   │   └── viewmodel/     # Shared ViewModels
│   └── res/               # Layouts, strings, and drawable resources
├── backend/               # Firebase functions (RAG backend)
└── build.gradle.kts        # Gradle configuration
```

---
## Architecture
- Follows the MVVM (Model–View–ViewModel) design pattern:

  - Model: Firebase and RAG data repositories

  - View: Jetpack Compose UI components

  - ViewModel: State management between UI and data layer

  - Full architecture details available on the Wiki.

---
 ## Continuous Integration (CI/CD)
- CI is handled through GitHub Actions:

   - Builds and lints Kotlin code (./gradlew build)

   - Runs ktfmt formatting checks

   - Publishes M1 APK as a downloadable artifact

   - Performs static analysis via SonarCloud
