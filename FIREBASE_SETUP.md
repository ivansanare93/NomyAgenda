# Firebase Setup Instructions

To use Firebase features (cloud sync, authentication), you need to:

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com) and create a new project.
2. In the project, add an **Android app** with package name `com.nomyagenda.app`.
3. Download the `google-services.json` file and place it in the `app/` directory of this project.
4. In the Firebase console, enable **Email/Password** authentication:
   - Go to **Authentication → Sign-in method → Email/Password → Enable**.
5. Enable **Cloud Firestore**:
   - Go to **Firestore Database → Create database**.
   - Choose **Start in production mode** (or test mode during development).
   - Add the following security rules to restrict access to each user's own data:
     ```
     rules_version = '2';
     service cloud.firestore {
       match /databases/{database}/documents {
         match /users/{userId}/entries/{entryId} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
       }
     }
     ```

> **Note:** `google-services.json` is listed in `.gitignore` and must **not** be committed to version control.
