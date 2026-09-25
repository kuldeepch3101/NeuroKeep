# 📚 LearnKeep – Smart Knowledge Tracking Android App

LearnKeep is a modern Android application built using **Java**, **Room Database**, and a **Node.js + MongoDB cloud backend** that helps users capture, organize, and review their learning efficiently.

The app allows users to store topics, attach files, track confidence levels, schedule review reminders, and manage learning resources in a clean and structured way.

The project contains **two parts**:

• Android Application
• Node.js Backend API (MongoDB + JWT Authentication)

#Live Test<h4>https://appetize.io/app/b_e2m23fih73t6n43yqgfk37u37q</h4>
---

# 🚀 Features

## 📌 Topic Management

* Add, edit, and delete topics
* Real-time search functionality
* Clean and minimal UI
* Topic icons auto-selected using tags

---

## 📎 Attachments Support

* 📷 Capture images using Camera
* 🖼 Select images from Gallery
* 📄 Attach PDF and other files
* Auto-delete files when removed
* Fullscreen image preview
* Physical file cleanup when topic is deleted

---

## 🎯 Confidence Tracking

* Select confidence level from **1–10**
* Color-coded circular selector *(Red → Green)*
* Stored per topic
* Used to schedule **review reminders**

---

## ⏰ Smart Study Reminder

Topics are automatically scheduled for revision based on confidence level.

| Confidence | Review Time |
| ---------- | ----------- |
| 1–3        | 1 day       |
| 4–6        | 3 days      |
| 7–8        | 7 days      |
| 9–10       | 14 days     |

Notifications remind users when a topic should be revised.

---

## 📊 Study Statistics

Stats dashboard provides learning insights:

* Study Streak
* Retention Rate
* Confidence Trend Graph
* Strong Subjects
* Weak Subjects
* Review History

---

## 🏷 Tag System

* Single tag per topic
* Tag-based automatic icon selection
* Editable tag support

---

## 📺 YouTube Learning Links

* Add multiple YouTube links per topic
* Clickable links inside topic details
* Open videos directly from the app

---

## 🔎 Smart Search

* Real-time filtering
* Search by title or tag

---

## 🌐 Cloud Authentication

Users can create accounts and login securely.

Features:

* User Signup
* User Login
* MongoDB Atlas cloud database
* JWT secure authentication
* Local session storage

---

## 💾 Hybrid Storage System

| Storage           | Purpose              |
| ----------------- | -------------------- |
| Room Database     | Store topics locally |
| Internal Storage  | Store attachments    |
| MongoDB Atlas     | User authentication  |
| SharedPreferences | Login session        |

---

# 🛠 Tech Stack

### 📱 Android

* Java
* XML UI
* Material Design Components
* Room Database
* RecyclerView
* Retrofit API
* Notification Manager

### 🌐 Backend

* Node.js
* Express.js
* MongoDB Atlas
* JWT Authentication
* dotenv environment variables

---

# 📂 Project Structure

```
LearnKeep
│
├── app/                    # Android application
│
├── learnkeep-server/       # Node.js backend
│   │
│   ├── models
│   │     └── User.js
│   │
│   ├── auth.js
│   ├── server.js
│   ├── package.json
│   └── package-lock.json
│
├── .gitignore
└── README.md
```

---

# 📥 How To Clone & Run This Project

These steps allow **any beginner to run the project on another device.**

---

# 1️⃣ Install Required Software

Install the following tools first.

### Android Development

Download and install:

**Android Studio**

https://developer.android.com/studio

Make sure the following are installed inside Android Studio:

* Android SDK
* Android Emulator (optional)

---

### Node.js Backend

Install Node.js:

https://nodejs.org

Check installation:

```bash
node -v
npm -v
```

---

### Git

Install Git:

https://git-scm.com/downloads

Verify installation:

```bash
git --version
```

---

# 2️⃣ Clone the Repository

Open terminal or command prompt.

```bash
git clone https://github.com/Jiteshck/LearnKeep
```

Move inside project folder:

```bash
cd LearnKeep
```

---

# 3️⃣ Setup Backend Server

Go to the server folder:

```bash
cd learnkeep-server
```

Install required packages:

```bash
npm install
```

This installs dependencies such as:

* Express
* Mongoose
* JWT
* dotenv
* CORS

---

# 4️⃣ Create Environment File (.env)

Inside **learnkeep-server** create a file named:

```
.env
```

Example:

```
MONGO_URI=your_mongodb_connection_string
JWT_SECRET=learnkeep_secret_key
PORT=3000
```

⚠ IMPORTANT
This file is not included in GitHub for security reasons.

---

# 5️⃣ Setup MongoDB Database

1. Create account on:

https://cloud.mongodb.com

2. Create a **MongoDB Atlas Cluster**

3. Click:

```
Connect → Drivers
```

4. Copy the connection string and paste into `.env`

Example:

```
mongodb+srv://username:password@cluster0.mongodb.net/learnkeep
```

---

# 6️⃣ Allow Network Access

In MongoDB Atlas:

```
Network Access → Add IP Address
```

Add:

```
0.0.0.0/0
```

This allows connections from any device.

---

# 7️⃣ Start Backend Server

Inside **learnkeep-server** run:

```bash
node server.js
```

You should see:

```
MongoDB Connected
Server running on port 3000
```

---

# 8️⃣ Configure Android API URL

Inside Android project update the API URL.

File:

```
ApiClient.java
```

Change base URL to your local IP.

Example:

```
http://192.168.1.5:3000/
```

Find your IP using:

```
ipconfig
```

Both **phone and computer must be on the same WiFi network**.

---

# 9️⃣ Run Android Application

Open Android Studio.

Click:

```
Open → LearnKeep
```

Wait for **Gradle Sync** to complete.

Connect:

• Android Phone
OR
• Android Emulator

Press:

▶ Run App

---

# 🔐 Security Notes

Sensitive data is protected using:

```
.gitignore
```

The following files are NOT uploaded to GitHub:

```
.env
node_modules
build files
keystore files
```

Environment variables store:

* MongoDB credentials
* JWT secret
* server port

---

# 🧪 Database Information

### Local Storage

Room Database stores:

* Topics
* Notes
* Tags
* Confidence levels

### Cloud Storage

MongoDB stores:

* User accounts
* Login credentials

---

# 🔮 Future Improvements

* Cloud sync for topics
* Multi-device topic access
* AI topic summarization
* Study quiz generation
* Profile avatars
* Leaderboard for study streaks

---

# 👨‍💻 Author

**Jitesh Choudhary**

Android Developer | Java | Room | Backend APIs | UI/UX

GitHub:
https://github.com/Jiteshck
