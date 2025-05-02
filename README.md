# 🎯 Strategic Savings App

Welcome to the Strategic Savings App, your go-to Android budgeting solution! Manage your finances by tracking income and expenses, setting savings goals, and visualising trends.

---

## 📋 Table of Contents

1. [🚀 Overview](#overview)  
2. [✨ Key Features](#key-features)  
3. [⚙️ Requirements & Configuration](#requirements-configuration)  
4. [🛠 Tech Stack](#tech-stack)  
5. [🗂️ Project Structure](#project-structure)  
6. [⚡ Quick Start](#quick-start)  
7. [💡 Usage Guide](#usage-guide)  
8. [📦 Releases & APK Download](#releases--apk-download)  
9. [👥 Team Members](#team-members)  
10. [🎥 Demo Video](#demo-video)    

---

## 🚀 Overview

The Strategic Savings App is a **mobile budgeting** application built for PROG7313-POE. It empowers users to:

- **Track** income & expenses in real time 🧾  
- **Set** and monitor personalised savings goals 🎯  
- **Visualise** spending patterns with interactive charts 📊  
- **Attach** receipts and notes for every transaction 📸  
- **Work offline** and **sync** when you’re back online 🔒  

---

## ✨ Key Features

- **Income & Expense Manager**  
  • Create, edit, and delete transactions  
  • Assign to categories (Food, Bills, Entertainment, etc.)  
  • Add notes and attach receipt images  

- **Savings Goals**  
  • Define target amounts & deadlines  
  • Track progress with percentage indicators  

- **Interactive Reports**  
  • MPAndroidChart-powered line, bar, and pie charts  
  • Filter by date ranges and categories  

- **Offline Persistence & Sync**  
  • Local caching with RoomDB  
  • Firebase Firestore for cloud backup  

- **Secure Authentication**  
  • Firebase Auth (Email & Password)  
  • Automatic session management  

---

## ⚙️ Requirements & Configuration
- Android Studio: Arctic Fox (2020.3.1) or later
- JDK: Version 11 or higher
- Android SDK: Platform 31 (Android 12)

---

## 🛠 Tech Stack

| Widget                                    | Purpose                              |
|-------------------------------------------|--------------------------------------|
| 🟦 `Kotlin`                               | Primary language                     |
| 🟪 `Android XML`                          | UI layouts & resources               |
| 🔐 `Firebase Auth`                        | User authentication                  |
| ☁️ `Firebase Firestore`                   | Cloud data storage                   |
| 📦 `RoomDB`                               | Local SQLite persistence             |
| 🖼️ `Supabase Storage`                     | Receipt image storage                |
| 📈 `MPAndroidChart`                       | Charts & data visualization          |

---

## 🗂️ Project Structure

```text
PROG7313-POE/
├─ .github/          # CI workflows
├─ app/              
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/   # Activities, Adapters, Models, Utils
│  │  │  └─ res/    # Layouts, drawables, strings
│  │  └─ AndroidManifest.xml
│  ├─ build.gradle.kts
├─ gradle/           # Gradle wrapper files
├─ build.gradle.kts  # Project-level build script
├─ settings.gradle.kts
└─ README.md         # ← You are here!
```
---

## ⚡ Quick Start

1. Clone the repo
```bash
git clone https://github.com/Strategic-Savings-Bureau/PROG7313-POE.git
cd PROG7313-POE
```

2. Open in Android Studio
  • Choose “Open an existing project” and select this folder.

3. Build & Run  
  • From Android Studio: click Run ▶️  
  • Or via CLI:  
    ```bash
    ./gradlew clean assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```

---

## 💡 Usage Guide

1. Sign Up / Log In  
  • Create an account or log in with your credentials.

2. Add a Transaction  
  • Tap the ➕ button  
  • Select Income or Expense  
  • Enter amount, category, notes, and optionally attach a receipt  
  • Save to update your balance  

3. Set a Savings Goal  
  • Go to the Goals tab  
  • Tap “Create Goal”  
  • Define your target amount and deadline  

4. View Reports  
  • Open the Reports section  
  • Toggle between Pie, Bar, and Line charts  
  • Filter by date or category for deeper insights  

---

## 📦 Releases & APK Download

Head to the Releases page on GitHub to grab the latest stable version:
https://github.com/Strategic-Savings-Bureau/PROG7313-POE/releases 

Under each release, download the APK asset (e.g., 'app-debug.apk') to install on your device.

---

## 👥 Team Members

| Name               | Student ID |
| ------------------ | ---------- |
| Blaise de Gier     | ST10249838 |
| Sashveer Ramjathan | ST10361554 |
| Shravan Ramjathan  | ST10247982 |
| Uvaan Covenden     | ST10022006 |

---

## 🎥 Demo Video

Check out a walkthrough of the Strategic Savings App here:  
Demo Video: https://youtu.be/Kt6-eqFk7GM

---
