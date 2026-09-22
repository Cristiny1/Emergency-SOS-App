# Implementation Plan - Fix Signup Step 1 Crash

Resolve the crash occurring when entering the Signup flow by restoring missing view components in the Step 1 layout.

## User Review Required

> [!IMPORTANT]
> **Crash Analysis**: The app is crashing because `SignupStep1Fragment.java` attempts to set an `OnClickListener` on `btnChangePhoto`, but this button (and the entire photo picker UI) is missing from `fragment_signup_step1.xml`.
>
> **Missing UI**: I will restore the profile image circle and the camera action button to the layout.

## Proposed Changes

### 👤 1. Restore Photo Picker UI
- **[MODIFY] [fragment_signup_step1.xml](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/layout/fragment_signup_step1.xml)**:
    - Add `CircleImageView` (ivSignupProfile) inside the Photo Section `FrameLayout`.
    - Add `MaterialCardView` (btnChangePhoto) with the camera icon.
    - Restore the background halo for a premium feel.

### 🧪 2. Verification
- Rebuild the app and verify that clicking "Get Started" now opens the Signup screen without closing the app.

## Verification Plan

### Manual Verification
1. **Launch App**: Open the app and wait for the Splash screen to finish.
2. **Navigate**: Click "Get Started" on the Welcome screen.
3. **Verify**:
    - App stays open.
    - Signup Step 1 is displayed.
    - Profile photo placeholder and camera button are visible.
