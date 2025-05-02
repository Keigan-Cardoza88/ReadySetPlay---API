**ReadySetPlay: A Performance Companion**

---

# Project Overview

ReadySetPlay is a complete performance companion app designed to enhance live performances with instant access to thousands of songs, dynamic features like autoscroll and transposition, and custom setlist management.

This project includes both:
- A backend server (FastAPI) that serves the song data.
- An Android frontend app (Kotlin on Android Studio Meerkat, SDK 35) that interacts with the API.

**Note:** This project is private and not open source. It is intended strictly for personal use. You may test it out for individual purposes, but any form of commercialization or public redistribution is strictly prohibited. All copyright rights remain reserved.

---

# Key Features

- **Song Library:**
  Access a database of over 5400 songs complete with chords and lyrics.

- **Transposition:**
  Easily transpose any song to fit your vocal range or instrument tuning.

- **Setlist Management:**
  - Save your favorite songs into setlists as `.txt` files.
  - Load setlists instantly whenever required.

- **Advanced Autoscroll:**
  - Multiple autoscroll speed settings to match different song tempos.
  - Hands-free experience while performing.

- **AI-Assisted Autoscroll:**
  - TarsosDSP technology is used to analyze the audio input.
  - Detects notes and matches them with chord patterns.
  - Enables real-time, intelligent autoscrolling based on musical progression.

---

# How to Run

This project consists of two main parts: the backend API and the Android application.

## 1. Backend Setup (API Server)

- Open the `API` folder in Visual Studio Code (VS Code).
- Inside, you should find two files:
  - `api.py`
  - `requirements.txt`

- Install the required Python packages:

  ```bash
  pip install -r requirements.txt
  ```

- Navigate to the directory where `api.py` is located in your terminal. You must be inside the correct folder or else the server will not run.

- Run the FastAPI server using the following command:

  ```bash
  uvicorn api:app --reload --host 0.0.0.0 --port 8000
  ```

- This will start a local server accessible on your device’s IP address (e.g., `http://192.168.1.2:8000`).

## 2. Android App Setup

- Open all other project files (excluding `api.py` and `requirements.txt`) in Android Studio Meerkat (SDK 35).

- Locate the Retrofit client file in the Android project and update the API base URL:

  ```kotlin
  private const val BASE_URL = "http://IPAddress:8000"
  ```

- Replace `IPAddress` with the local IP address of the device hosting the FastAPI server.

- Build and run the project by launching `MainActivity` or running the `app` module.

## 3. Notes

- Ensure that both the Android device and the machine hosting the API server are connected to the same Wi-Fi network.
- If running on a real Android device, ensure that network permissions are properly set.

