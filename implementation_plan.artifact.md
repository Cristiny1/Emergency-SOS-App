# Implementation Plan - Final "Smart City SOS" Completion

Final phase to reach 100% completion of all proposed features, including high-end intelligence and media capabilities.

## User Review Required

> [!IMPORTANT]
> **Video Evidence**: Capturing video will require the `CAMERA` permission and will increase the report size. I will implement a compressed recording to save bandwidth.
>
> **AI Voice Output**: The app will use the system's default Text-to-Speech (TTS) engine. Users can mute this at any time using the on-screen volume controls.

## Proposed Changes

### 🤖 1. AI Voice Assistant (Full I/O)
- **Voice Output (TTS)**: Integrate `android.speech.tts.TextToSpeech` into [ChatbotActivity.java](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/java/com/example/emergency_sos_app/ChatbotActivity.java). The AI will now "read out" its safety guidance automatically.
- **Toggle**: Add a small speaker icon to the chat UI to enable/disable auto-reading.

### ⚠️ 2. Danger Reporting: Video Evidence
- **Video Capture**: Implement a `VideoPickerLauncher` in [CitizenReportActivity.java](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/java/com/example/emergency_sos_app/CitizenReportActivity.java) using `ActivityResultContracts.CaptureVideo`.
- **UI Update**: Add a "Record Video" button and a small video preview thumbnail.

### 🎙️ 3. Danger Reporting: Voice Description
- **Real Audio Capture**: Replace the placeholder in [CitizenReportActivity.java](file:///C:/Users/ASUS/AndroidStudioProjects/EmergencySOSApp/app/src/main/java/com/example/emergency_sos_app/CitizenReportActivity.java) with actual `MediaRecorder` logic to save a voice memo of the danger.

### 🛡️ 4. Low-Battery Emergency Mode
- **Energy Saver**: Implement a `BatteryReceiver`. If the battery level falls below 15% during an active SOS, the app will:
    - Reduce GPS update frequency.
    - Dim the map intensity.
    - Disable the radar animation to conserve power for the final rescue broadcast.

### 🎨 5. Final UI/UX Consistency & Polish
- **Animations**: Add "Entrance" animations to Dashboard cards using `LayoutTransition`.
- **Iconography**: Final sweep to ensure all icons have appropriate `contentDescription` for accessibility.

## Verification Plan

### Manual Verification
1. **TTS Test**: Ask the AI "What should I do in a fire?". Verify the phone's speaker reads out the instructions.
2. **Video Test**: Record a 5-second video in the Report screen. Verify it shows in the preview and "hasVideo" is true in the payload.
3. **Battery Test (Simulation)**: Use emulator controls to set battery to 10% during SOS. Verify the "Power Saving Active" toast appears and animations stop.
4. **End-to-End**: Run a full cycle (Report -> SOS -> Chat -> Finish) to ensure zero crashes and perfect speed.
