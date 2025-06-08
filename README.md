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
9. [🚦 GitHub Actions & Workflow Automation](#github-actions--workflow-automation)
10. [⚡ Quick Start](#quick-start)
11. [📦 Releases & APK Download](#releases--apk-download)
12. [👥 Team Members](#team-members)
13. [🎥 Demo Video](#demo-video)

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

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Sign%20up%20and%20login.jpg" alt="Sign up and login screen" width="320"/>
  <br>
  <em>Register a new account here</em>
</p>

---

### 2. **Create Categories**
   - Head to the **Categories** tab and add your own expense categories.
   - Each category can have a custom icon (from camera or gallery).

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Add%20a%20category.jpg" alt="Add a category screen" width="320"/>
  <br>
  <em>Create a category to add expenses to</em>
</p>

---

### 3. **Add a Transaction**
   - Use the quick actions button on the main dashboard.
   - Choose **Expense** or **Income** or **Savings**.
   - Enter the amount, select category, add a description, and pick a date.
   - **Attach a receipt:** Option to take a new photo or select from your gallery.
   - Tap **Save** — your balance and graphs update instantly.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Add%20a%20transaction.jpg" alt="Add a transaction screen" width="320"/>
  <br>
  <em>Use the quick actions on the top to add income, expenses or savings</em>
</p>

---

### 4. **Set Your Budget**
   - Set using the Budget card when you register. 
   - Go to **Budget Settings**.
   - Set your monthly total budget and individual limits per category.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Set%20your%20budget.jpg" alt="Set your budget screen" width="320"/>
  <br>
  <em>You can edit your budget from the budget settings option in the settings menu</em>
</p>

---

### 5. **Set and Track Goals**
   - Access the **Goals** tab.
   - Create a saving goal with a target amount and deadline.
   - Track your progress visually; get rewarded for consistency.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Set%20and%20track%20goals.jpg" alt="Set and track goals screen" width="320"/>
  <br>
  <em>You can create personalised goals, which you can save towards</em>
</p>

---

### 6. **Analyze Spending**
   - Use the **Graphs** section in each screen for interactive charts:
     - **Bar/Line Charts:** View trends over time.
   - Filter any chart or list by date range.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Analyse%20Spending.jpg" alt="Analyze spending graphs" width="320"/>
  <br>
  <em>Graphs like this throughout the app can be used to visually track your budget</em>
</p>

---

### 7. **View Receipts**
   - Tap on any transaction in **History** to view details and receipt images.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//View%20receipts.jpg" alt="View receipts screen" width="320"/>
  <br>
  <em>In the transaction lists, just click on one to view attachments</em>
</p>

---

### 8. **Sync & Multi-device**
   - Your data is backed up and synced via Firestore and Supabase.
   - Switch devices at any time—just log in!

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Sync%20and%20multi%20device.jpg" alt="Sync and multi-device screen" width="320"/>
  <br>
  <em>Sync your data in the settings page and then just log in on your new device</em>
</p>

---

### 9. **Switch Themes**
   - Toggle **Dark Mode** in settings for nighttime comfort.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Switch%20Themes.jpg" alt="Switch themes screen" width="320"/>
  <br>
  <em>Use the toggle in the settings menu to switch themes easily</em>
</p>

---

### 10. **Currency Converter**
   - Access from Settings to convert currencies in real-time.

<p align="center">
  <img src="https://bxpptnwmvrqqvdwpzucp.supabase.co/storage/v1/object/public/app-pics//Currency%20Converter.jpg" alt="Currency Converter screen" width="320"/>
  <br>
  <em>Use the built in currency converter in the settings menu, to do conversions on the go</em>
</p>

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

## 🚦 GitHub Actions & Workflow Automation

Modern app development demands reliability, consistency, and speed. For the Strategic Savings App, we leveraged **GitHub Actions** and workflow automation to ensure every code change is automatically built, tested, and validated before merging into the main codebase.

### Why GitHub Actions Matter for Our Project

- **Continuous Integration (CI):**
  - Every push and pull request triggers our CI workflow.
  - The app is built and tested automatically, catching bugs before they reach production.
  - Ensures code quality stays high, even as the team grows or changes.

- **Automated Testing:**
  - Unit tests and lint checks run headlessly on every new commit.
  - This prevents regressions and saves manual QA time.

- **Build Automation:**
  - Android builds are generated in clean, reproducible environments.
  - Ensures every team member and reviewer sees the same results.

- **Seamless Collaboration:**
  - Workflows create status checks and pass/fail badges on pull requests.
  - Developers get instant feedback, helping us move fast without breaking things.

### How We Utilize GitHub Actions

- **Workflow Location:**  
  All our workflows live under `.github/workflows/` in the repo.
- **CI Pipeline:**  
  - **Build:** Checks out code, sets up the correct JDK and Android SDK, and builds the APK.
  - **Test:** Runs all unit and instrumentation tests.
  - **Lint:** Ensures code style and best practices are followed.

**Result:**  
Thanks to GitHub Actions, every commit is automatically validated, builds remain reproducible, and our team can confidently ship high-quality code at speed—without ever worrying about "it works on my machine" problems.

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
| Blaise Mikka de Gier     | ST10249838 |
| Sashveer Lakhan Ramjathan | ST10361554 |
| Shravan Ramjathan  | ST10247982 |

---

## 🎥 Demo Video

Check out a walkthrough of the Strategic Savings App here:  
[https://youtu.be/76AumXWomrA](https://youtu.be/76AumXWomrA)

---

> **Ready to take control of your savings?**  
> Download, log in, and start your journey with Strategic Savings today!

---

<p align="center">
  <sub>
    Built with ❤️ by the Strategic Savings Bureau team.
  </sub>
</p>
