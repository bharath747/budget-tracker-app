# Budget Tracker Android

Native Android/Jetpack Compose implementation of the existing Budget Tracker PWA.

## Current features
- Expense and credit transactions
- Source management
- Dashboard balance/credit/expense totals
- Loans with principal remaining and interest calculation
- Loan payment data model
- Lending with principal remaining and interest calculation
- Lending payment data model
- Keyword filter configuration
- Offline-first Room database

## Build
Requires JDK 17 and Gradle 8.10.

`gradle assembleDebug`

APK:
`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds the debug APK on pushes to main and makes it available as an artifact.

## Data
The Android app stores its data locally in Room.