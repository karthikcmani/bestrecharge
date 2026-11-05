# Development Guide

## Prerequisites
- **JDK**: OpenJDK 17 or higher
- **Android SDK**: API Level 35, Build Tools 35.0.0+
- **Gradle**: 8.11.1
- **Python**: 3.10+ (tested with Python 3.13)
- **Database**: PostgreSQL (Production) or SQLite (Development)

---

## 1. Backend Setup

```bash
cd backend
python -m venv venv
# Windows:
.\venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
python seed_data.py
uvicorn app.main:app --reload --port 8000
```

### Running Backend Tests
```bash
cd backend
pytest
```

---

## 2. Android Setup

```bash
cd android
# Build debug APK:
./gradlew assembleDebug

# Run unit tests:
./gradlew testDebugUnitTest
```

### Generated APK Location
```
android/app/build/outputs/apk/debug/app-debug.apk
```
