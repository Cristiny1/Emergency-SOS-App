# Restrict Language Support to English (En) and Khmer (KH)

Ensure the application only supports English and Khmer languages by removing other localizations and refining the selection UI.

## User Review Required

> [!WARNING]
> This action will permanently remove Arabic (`values-ar`) and Spanish (`values-es`) localization files from the project.

## Proposed Changes

### [Component Name] Localization Cleanup

#### [DELETE] [values-ar](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/values-ar)
- Remove all Arabic translation files.

#### [DELETE] [values-es](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/values-es)
- Remove all Spanish translation files.

### [Component Name] UI & Labeling

#### [MODIFY] [strings.xml](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/values/strings.xml)
- Update the language selection title to reflect the available choices: "English (En) / Khmer (KH)".

#### [MODIFY] [values-km/strings.xml](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/values-km/strings.xml)
- Synchronize labels with the English version.

### [Component Name] Logic Verification

#### [MODIFY] [LanguageHelper.java](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/java/com/example/emergency_sos_app/LanguageHelper.java)
- Confirm that only `en` and `km` are handled. (Already looks clean but will verify).

#### [MODIFY] [LanguagePickerDialog.java](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/java/com/example/emergency_sos_app/LanguagePickerDialog.java)
- Confirm that only English and Khmer options are inflated from `dialog_language_picker.xml`.

## Verification Plan

### Automated Tests
- Build verification via `./gradlew assembleDebug`.

### Manual Verification
- **Language Dialog**: Open the language picker and verify only "English" and "Khmer" options are visible.
- **Persistence**: Switch between English and Khmer and ensure the app reloads correctly in both.
- **Cleanup**: Verify that `res/` no longer contains `values-ar` or `values-es` directories.
