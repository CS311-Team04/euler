# Development Setup Guide

## 🚀 Quick Start

### 1. Clone and Build
```bash
git clone <repository-url>
cd euler
./gradlew assembleDebug
```

### 2. Microsoft Entra ID Authentication Setup

**If you get `INVALID_CERT_HASH` error when trying to sign in:**

#### Option A: Add Your SHA-1 to Firebase Console (Recommended)

1. **Get your SHA-1 fingerprint:**
   ```bash
   # Method 1: Using keytool
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   
   # Method 2: Using Gradle
   ./gradlew signingReport
   
   # Method 3: Using Android Studio
   # File > Project Structure > Modules > app > Signing
   # Add debug keystore: ~/.android/debug.keystore
   # Password: android
   ```

2. **Add SHA-1 to Firebase Console:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select project `euler-e8edb`
   - Go to Project Settings (⚙️)
   - Find Android app `com.android.sample`
   - Click "Add fingerprint"
   - Paste your SHA-1 fingerprint
   - Download new `google-services.json` and replace the existing one

#### Option B: Use Physical Device

If you have an Android phone:
1. Enable Developer Options
2. Enable USB Debugging
3. Connect phone via USB
4. Run app on physical device (has internet connection)

#### Option C: Fix Emulator Internet

If using Android emulator:
1. **Restart emulator** with internet access
2. **Check emulator settings** in AVD Manager
3. **Ensure "Network Speed" is set to "Full"**

### 3. Run the App

```bash
./gradlew installDebug
```

## 🔧 Troubleshooting

### Microsoft Sign-In Not Working

**Error: `INVALID_CERT_HASH`**
- Solution: Add your SHA-1 fingerprint to Firebase Console (see Option A above)

**Error: `Unable to resolve host`**
- Solution: Check internet connection (emulator or device)

**Error: `Unknown calling package`**
- Solution: Ensure `google-services.json` is in `app/` directory

### Firebase Configuration

- **Project ID**: `euler-e8edb`
- **Package Name**: `com.android.sample`
- **Authentication**: Microsoft Entra ID enabled

## 📝 Notes

- Each developer needs to add their own SHA-1 fingerprint to Firebase Console
- The app uses Firebase production services (not emulator)
- Microsoft Entra ID requires internet connection
- Debug keystore is unique per machine
