# Implementation Plan - Apple-style Medical ID Profile

This plan completely redesigns the Profile screen to match the clean, high-utility **Apple Medical ID** aesthetic shown in the reference image. The focus is on scannability, high-contrast text, and a utility-first layout.

## Proposed Changes

### [Visuals & Theme]

#### [MODIFY] [colors.xml](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/values/colors.xml)
- Add Apple-style colors:
    - `medical_id_red`: `#FF3B30`
    - `ios_system_bg`: `#F2F2F7`
    - `ios_label_grey`: `#8E8E93`

#### [NEW] [ic_medical_star.xml](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/drawable/ic_medical_star.xml)
- A red 6-pointed star icon (Star of Life style) for the header.

### [Profile Screen Redesign]

#### [MODIFY] [activity_profile.xml](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/res/layout/activity_profile.xml)
- **Background**: Set the overall background to `ios_system_bg`.
- **Custom Toolbar**: A clean white header with the `ic_medical_star`, the text "Medical ID", and a red "Edit" button.
- **Header Card**:
    - A rounded white container.
    - Large, bold Name (e.g., "Alex Johnson").
    - Subtext for Age (e.g., "28 years old").
    - A circular profile image positioned on the right side of the card.
- **Information Sections**:
    - Each section (Medications, Allergies, Emergency Contacts, Conditions) will have:
        - A small grey label above.
        - A rounded white card container for the content.
        - High-contrast black text for the actual data.
- **Footer**: A long-form explanatory text at the bottom about how this data is shared during an SOS.

#### [MODIFY] [ProfileActivity.java](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/java/com/example/emergency_sos_app/ProfileActivity.java)
- **Data Model Expansion**: Support new fields from the reference:
    - `age` (calculated from birthdate or simple string).
    - `medications`.
    - `conditions`.
- **View Binding**: Update the code to populate these new fields.
- **Simplified Edit Flow**: Switch to a more traditional "Edit Screen" or maintain the inline toggle but with the new styling.

## Verification Plan

### Manual Verification
1. **Visual Accuracy**: Compare the final UI with the provided reference image.
2. **Scannability**: Ensure the user's name and critical medical info (Allergies, Medications) are the most prominent elements.
3. **Data Persistence**: Test saving "Conditions" and "Medications" to ensure they show up correctly in the new layout.
4. **Navigation**: Ensure the "Edit" button correctly toggles the input fields or launches the edit state.
