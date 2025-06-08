# 🎯 Strategic Savings App

Welcome to the **Strategic Savings App** – your all-in-one mobile solution for mastering personal finance! With a modern interface, robust offline/online sync, advanced filtering and analytics, and a feature set that goes far beyond the basics, our app helps you budget smarter, spend wiser, and reach your financial goals.

---

## 🗂️ Table of Contents

1. [🚀 Overview](#overview)  
2. [✨ Key Features](#key-features)
   - [Required Features](#required-features)
   - [Additional Features](#additional-features)
3. [📝 Usage Guide](#usage-guide)
4. [📦 Demo Account](#demo-account)
5. [🛠️ Part 2 Feedback Implementation](#part-2-feedback-implementation)
6. [⚙️ Requirements & Configuration](#requirements--configuration)
7. [🛠️ Tech Stack](#tech-stack)
8. [🗂️ Project Structure](#project-structure)
9. [⚡ Quick Start](#quick-start)
10. [📦 Releases & APK Download](#releases--apk-download)
11. [👥 Team Members](#team-members)
12. [🎥 Demo Video](#demo-video)

---

## 🚀 Overview

The **Strategic Savings App** is a next-generation budgeting platform built for Android that empowers users to take full control of their finances, wherever they are. Designed for both everyday spenders and ambitious savers, the app blends required academic features with professional polish, advanced analytics, and a host of quality-of-life enhancements.

**What makes Strategic Savings stand out?**
- **All-in-one Experience:** Track expenses and income, set budgets and goals, visualize data, and even gamify your financial journey.
- **Advanced Filtering:** Pinpoint trends with expense and income filtering by flexible date ranges and categories.
- **Real-time Cloud Sync:** Access your data seamlessly across devices with robust online and offline support.
- **Intuitive UI:** Dark mode, modern charts, and easy navigation make personal finance simple and even fun.

---

## ✨ Key Features

### 🛡️ Required Features

Our app **meets and exceeds** all core requirements for a modern budgeting tool:

#### 🔑 Authentication & Security
- **User Registration & Login:** Secure Firebase Authentication (email/password, with optional biometric login) ensures your data is safe.
- **Automatic Session Management:** Stay logged in and resume where you left off.

#### 📂 Categories & Transactions
- **Custom Expense Categories:** Create custom categories with personalised icons (e.g., Groceries, Entertainment, Transport).
- **Transaction Entry:** Add expenses and income with amount, date picker, description, and category selection.
- **Advanced Filtering:** Instantly filter and view transactions by any custom date range (across all transaction lists).

#### 📸 Receipts & Attachments
- **Photo Attachments:** Attach a receipt to any expense.
- **Direct Camera Integration:** Take a photo on-the-spot **or** select from gallery (see [Part 2 Feedback Implementation](#part-2-feedback-implementation)).
- **Receipt Viewer:** Tap any expense in history to instantly view its receipt image.

#### 🎯 Budgets & Insights
- **Monthly Budget Goal:** Set, edit, and track a total monthly spending cap.
- **Category Limits:** Fine-tune your budget with specific limits per category.
- **Expense Summaries:** Instantly view totals spent per category for any time period.
- **Data Persistence:** All financial data is securely stored locally (RoomDB) and/or in the cloud (Firestore).

#### 📊 Analytics & Dashboards
- **Visual Reports:** Interactive line and bar charts (powered by MPAndroidChart) show spending breakdowns and trends.
- **Date-range Graphs:** All analytics and charts can be filtered by any user-selectable period.
- **Home Progress Dashboard:** Monitor your budget performance for the current month; categories over budget are visually flagged.

#### 🏆 Gamification & Multi-device Sync
- **Rewards & Badges:** Earn badges and streaks for consistent expense logging.
- **Cloud Sync:** All data is stored online (Firestore & Supabase), allowing seamless access across multiple devices.

---

### 🚀 Additional Features

#### 🔒 Biometric Login
- **Fingerprint/Face Unlock:** Speed up login and boost security with device biometrics.

#### 💸 Currency Converter
- **Real-time Conversion:** Convert currencies instantly with up-to-date rates via a live API [Currency Freaks](#https://currencyfreaks.com/#documentation).

#### 🌙 Dark Mode
- **Automatic Theming:** Enjoy a modern dark interface, with all charts and dialogs adapting for readability and style.

#### 🎯 Financial Goals
- **Goal Tracking:** Define savings targets (e.g., “Save for Vacation”), track progress, and visualize your journey.

#### ⏰ Reminders & Notifications
- **Expense Reminders:** Get notified to log expenses to build good budgeting habits.

#### 📆 Advanced Monthly Limits
- **Per-category Budgeting:** Set monthly caps for individual spending categories for granular control.

---

## 📝 Usage Guide

**Getting started and making the most of the app:**

### 1. **Sign Up / Log In**
   - Register with email/password (or use biometric if set up).
   - Your profile and all financial data are secured via Firebase Auth.

### 2. **Create Categories**
   - Head to the **Categories** tab and add your own expense categories.
   - Each category can have a custom icon (from camera or gallery).

### 3. **Add a Transaction**
   - Use the quick actions button on the main dashboard.
   - Choose **Expense** or **Income** or **Savings**.
   - Enter the amount, select category, add a description, and pick a date.
   - **Attach a receipt:** Option to take a new photo or select from your gallery.
   - Tap **Save** — your balance and graphs update instantly.

### 4. **Set Your Budget**
   - Set using the Budget card when you register. 
   - Go to **Budget Settings**.
   - Set your monthly total budget and individual limits per category.

### 5. **Set and Track Goals**
   - Access the **Goals** tab.
   - Create a saving goal with a target amount and deadline.
   - Track your progress visually; get rewarded for consistency.

### 6. **Analyze Spending**
   - Use the **Graphs** section in each screen for interactive charts:
     - **Bar/Line Charts:** View trends over time.
   - Filter any chart or list by date range.

### 7. **View Receipts**
   - Tap on any transaction in **History** to view details and receipt images.

### 8. **Sync & Multi-device**
   - Your data is backed up and synced via Firestore and Supabase.
   - Switch devices at any time—just log in!

### 9. **Switch Themes**
   - Toggle **Dark Mode** in settings for nighttime comfort.

### 10. **Currency Converter**
   - Access from Settings to convert currencies in real-time.

---

## 📦 Demo Account

**Want to explore the app instantly?**

> **Demo Account Credentials**
> - **Email:** `strategicsavings@gmail.com`
> - **Password:** `Piggy.1`

- Log in with this account to access preloaded data: expenses, receipts, goals, and graphs—all set up to showcase every feature.
- All demo data can be synced to your device for offline exploring.
- Perfect for presentations or quick testing—*no need to tediously enter data!*

---

## 🛠️ Part 2 Feedback Implementation

### **Feedback:**  
> *"Allow users to take a picture of a receipt."*  
>  
> *Score: 7/10 – Feature worked for uploads from gallery, but lacked direct camera integration.*

### **Solution:**

We enhanced the receipt feature with a **user-friendly dialog**:
- When attaching a receipt, users can now:
  - **Take a photo using the device camera** (real-time).
  - **Select an existing image** from their gallery.
- Both methods upload the image to **Supabase Storage** and instantly link it to the transaction.
- The image is viewable in transaction details, fully meeting the acceptance criteria and improving the user experience.

**Implementation Highlights:**
- Uses Android’s `ActivityResultContracts` for both camera and gallery.
- Receipt images are securely uploaded to cloud storage and linked in the local database.
- Seamless UI: The selection dialog appears wherever an image can be attached.

---

## ⚙️ Requirements & Configuration

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: 11 or higher
- **Android SDK**: Platform 31 (Android 12)

---

## 🛠️ Tech Stack

| Widget                             | Purpose                            |
|-------------------------------------|------------------------------------|
| 🟦 `Kotlin`                         | Primary language                   |
| 🟪 `Android XML`                    | UI layouts & resources             |
| 🔐 `Firebase Auth`                  | User authentication                |
| ☁️ `Firebase Firestore`             | Cloud data storage                 |
| 📦 `RoomDB`                         | Local SQLite persistence           |
| 🖼️ `Supabase Storage`               | Receipt image storage              |
| 📊 `MPAndroidChart`                 | Charts & data visualization        |

---

## 🗂️ Project Structure

```text
PROG7313-POE/
├── .github/             # CI workflows
├── app/                 
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/   # Activities, Adapters, Models, Utils
│   │   │   └── res/    # Layouts, drawables, strings
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/              # Gradle wrapper files
├── build.gradle.kts     # Project-level build script
├── settings.gradle.kts
└── README.md            # ← You are here!
```

---

## ⚡ Quick Start

1. **Clone the repo**
   ```bash
   git clone https://github.com/Strategic-Savings-Bureau/PROG7313-POE.git
   cd PROG7313-POE
   ```

2. **Open in Android Studio**
   - Choose “Open an existing project” and select this folder.

3. **Build & Run**
   - From Android Studio: Click Run ▶️
   - Or via CLI:
     ```bash
     ./gradlew clean assembleDebug
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```

---

## 📦 Releases & APK Download

- Find the latest APKs on the [Releases Page](https://github.com/Strategic-Savings-Bureau/PROG7313-POE/releases).
- Download and install on your device to get started immediately!

---

## 👥 Team Members

| Name               | Student ID |
| ------------------ | ---------- |
| Blaise de Gier     | ST10249838 |
| Sashveer Ramjathan | ST10361554 |
| Shravan Ramjathan  | ST10247982 |

---

## 🎥 Demo Video

Check out a walkthrough of the Strategic Savings App here:  
[Demo Video: https://youtu.be/Kt6-eqFk7GM](https://youtu.be/Kt6-eqFk7GM)

---

> **Ready to take control of your savings?**  
> Download, log in, and start your journey with Strategic Savings today!

---

<p align="center">
  <sub>
    Built with ❤️ by the Strategic Savings Bureau team.
  </sub>
</p>
