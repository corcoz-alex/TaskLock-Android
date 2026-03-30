#  TaskLock

> 🚧 **Note:** TaskLock is currently in active development. Core features are functional, but new features are constantly added.

TaskLock is a productivity and task management system with a twist: it forces you to actually get out of bed. Instead of just tapping a button to snooze an alarm, users must physically get up and scan a specific household object (a toothbrush, laptop, shoes, or a bag for now) using their phone's camera to prove they are awake.

This project pairs a native Android frontend with a Python FastAPI backend.

## AI-Powered Wake Up
The standout feature is the physical verification system. It uses a custom TensorFlow Lite model running locally on the device via Google ML Kit.
* It intercepts the morning alarm using Android's `AlarmManager` and `Foreground Services` to bypass deep sleep (Doze) and the lock screen.
* It fires up `CameraX` to scan the environment in real-time.
* The alarm will not dismiss until the on-device ML model confidently recognizes the required object (e.g., your toothbrush in the bathroom) for a sustained number of frames.

## Architecture & Engineering Highlights

* **Strict Separation of Concerns:** The Android app strictly follows the **MVVM (Model-View-ViewModel)** architecture, keeping UI logic entirely separate from business and data layers.

* **Bulletproof Session Management:** No lazy token storage. Auth tokens are locked in the Android Keystore using `EncryptedSharedPreferences` (AES256). Network calls are wrapped in an `OkHttp Interceptor` that automatically catches 401 Unauthorized errors, silently negotiates a new JWT refresh token with the server, and retries the failed request without the user ever noticing.
* **Clean Backend Design:** The FastAPI backend utilizes the **Repository Pattern**. Database queries (SQLAlchemy) are completely abstracted away from the API routing layer.
* **Automated DevOps:** CI/CD is fully configured. Merging to the `main` branch triggers a GitHub Actions workflow that automatically SSHs into a DigitalOcean droplet, pulls the latest code, runs Alembic database migrations, and restarts the system daemon.

## Tech Stack

### Frontend (Android)
* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Camera & ML:** CameraX, Google ML Kit (Custom TFLite Models)
* **Background Work:** AlarmManager, Foreground Services, WakeLocks
* **Networking:** Retrofit, OkHttp
* **Security:** AndroidX Security Crypto (MasterKey)

### Backend (REST API)
* **Framework:** Python / FastAPI
* **Database & ORM:** PostgreSQL / SQLAlchemy
* **Migrations:** Alembic
* **Security:** Bcrypt (Passlib), PyJWT (OAuth2 Password Bearer)
* **Hosting:** DigitalOcean VPS + Uvicorn

##  Repositories
* **Backend API:** [TaskLock Backend](https://github.com/corcoz-alex/TaskLock-Backend)
* **Android Client:** *(This repository)*

## Download & Test

The FastAPI backend is currently live and hosted on DigitalOcean. You do not need to build the Android project from source to evaluate the UI, the real-time network synchronization, or the physical ML object verification.

1. **Download the App:** [Download TaskLock-v1.0.apk here](https://github.com/corcoz-alex/TaskLock-Android/blob/main/app/release/TaskLock-v1.0.apk)
2. **Install on your Android device:** You may need to prompt your device to allow "Install from unknown sources."
3. **Log in:** Bypass the registration flow using this dedicated test account:
   * **Email:** `testacc@test.com`
   * **Password:** `test123123`

*Tip: To quickly test the core feature, log in, tap the settings icon on the bottom left of the screen and press the **Test Alarm (15s)** button. An alarm with the default laptop object will be scheduled for 15 seconds into the future.*

## Backend Local Setup

Want to set up the API locally?

1. **Clone the repo:**
   ```bash
   git clone https://github.com/corcoz-alex/TaskLock-Backend.git
   cd tasklock-backend
   ```
2. **Create a virtual environment and install dependencies**
    ```bash
   python -m venv .venv
    source .venv/bin/activate  # Windows: .venv\Scripts\activate
    pip install -r requirements.txt
   ```
3. **Set up the database**
    ```bash
   alembic upgrade head
   ```
4. **Run the server**
    ```bash
   uvicorn app.main:app --reload
   ```
