# Brilliant Dialer Bridge

**Brilliant Dialer Bridge** connects [Brilliant Connect](https://play.google.com/store/apps/details?id=com.brilliant.connect.com.bd) (by InterCloud Limited, Bangladesh) directly to Android's native Telecom framework.

It registers a dedicated `PhoneAccount` (`CAPABILITY_CALL_PROVIDER`) so that **"Brilliant Connect"** appears inside your default phone dialer (Google Phone and others) alongside your physical SIMs (SIM 1, SIM 2, Brilliant Connect). When selected, the bridge automatically hands off the dialed number to Brilliant Connect and utilizes an automated AccessibilityService to press the call button and select the **"Brilliant-OUT"** call option.

---

## Architecture Overview

```
 [Google Phone / Default Dialer]
                │
         (Selects Account)
                ▼
   [Telecom: PhoneAccount] ────────► [BridgeConnectionService]
                                             │ (Inspects & Disconnects locally)
                                             ▼
                                     [HandoffLauncher]
                                             │ (Direct Activity Launch / Overlay)
                                             ▼
                                  [Brilliant Connect Keypad]
                                             │
                                             ▼
                               [AutoSelectController]
                                             │ (Validates digits on Keypad)
                                             ▼
                              [Clicks Call Button (dialler_button)]
                                             │
                                             ▼
                            [Selects "Brilliant-OUT" Sheet Option]
                                             │
                                             ▼
                                  [Outbound VoIP Call Placed]
```

---

## 1. Setup Walkthrough (Initial Configuration)

Android requires explicit user consent for Telecom Calling Accounts, Accessibility Services, and Overlay permissions. On first launch, open the **Setup Checklist** in the app and complete the 8 steps:

1. **Phone & Calling Permissions (`READ_PHONE_STATE`, `CALL_PHONE`)**
   - Tap **Grant Permissions** and approve the system prompts.
2. **Notifications (`POST_NOTIFICATIONS`)**
   - Tap **Allow Notifications** to enable high-priority call launch alerts and confirmation prompts.
3. **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**
   - Tap **Grant Overlay Access** and toggle ON for **Brilliant Dialer Bridge**.
   - *Why:* On Android 10+, background activity starts are blocked. This permission allows instant handoff when you dial from an external dialer.
4. **Full-Screen Call Prompts (`USE_FULL_SCREEN_INTENT`)**
   - On Android 14+, allow full-screen intent access so call prompts appear over the lock screen.
5. **Battery Saver Exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**
   - Exempt the app from battery optimizations so background Telecom connections are not killed.
6. **Brilliant Connect Target App**
   - Ensure `com.brilliant.connect.com.bd` is installed on your device.
7. **Accessibility Automation Service**
   - Tap **Open Accessibility Settings**.
   - Select **Brilliant Dialer Auto-Select** and toggle it **ON**.
   - *Android 13+ Note:* If the accessibility toggle is grayed out, tap the three dots in App Info -> **Allow restricted settings**, then return and turn it on.
8. **Enable Calling Account in System Settings**
   - Tap **Open Calling Accounts**.
   - Under **Calling accounts**, toggle ON **Brilliant Connect**.

---

## 2. How to Test

### In-App Test
1. Open **Brilliant Dialer Bridge** and navigate to **Test Bridge**.
2. Enter a test telephone number (e.g., `01712345678`).
3. Tap **Test Handoff Only** to verify that Brilliant Connect opens to the Keypad with the number pre-filled.
4. Tap **Test Full Auto-Call** to execute the complete sequence: Keypad opens -> digits verified -> green call button pressed -> "Brilliant-OUT" option selected.

### Real World Call from Google Phone
1. Open the phone's native dialer (e.g., **Google Phone**).
2. Dial any mobile or landline number you own.
3. In the calling account popup, choose **Brilliant Connect** (alongside SIM 1 and SIM 2).
4. Watch Brilliant Connect open and automatically place the call through **Brilliant-OUT**.

---

## 3. Safety & Privacy Safeguards

- **Emergency Number Protection:** Emergency numbers (`999`, `112`, `911`, `100`, `101`, `102`, `108`, `000`, `110`, `119`) are **strictly blocked from VoIP interception**. If dialed via the bridge, the call is rejected immediately and an alert directs the user to use their cellular SIM carrier.
- **USSD & Short Code Filtering:** MMI/USSD codes (`*123#`, `*121#`, etc.) and codes under 3 digits cannot be routed over VoIP and are rejected with a helpful notification.
- **Strict Number Verification:** The accessibility controller reads the text in the Brilliant Connect keypad (`screen_tab_dialer_editText_number`) and compares the last 10 digits against the dialed number. If there is a mismatch, the automation **aborts immediately** to prevent calling the wrong number.
- **Privacy Masking:** Full phone numbers are never stored in plain text or logged to Logcat. The middle digits are masked (e.g. `017****5678`).
- **Paid Call Warning:** Brilliant-OUT is a paid VoIP call service. The app provides a one-time acknowledgement dialog and an optional **Confirm before calling** toggle in Settings.

---

## 4. Troubleshooting

| Issue | Cause & Solution |
|---|---|
| **"Brilliant Connect" does not appear in dialer account picker** | **1.** Confirm that you enabled the account under *Settings -> Calling Accounts -> Brilliant Connect*.<br>**2.** Certain OEM dialers (Samsung One UI, Xiaomi HyperOS, Oppo ColorOS) hide third-party calling accounts. Workaround: install **Phone by Google** from Play Store, or enable **Route ALL Outgoing Calls via Brilliant** in Bridge Settings. |
| **Brilliant Connect does not open when dialed** | Display over other apps (`SYSTEM_ALERT_WINDOW`) is disabled. Grant this permission in app checklist or system settings to permit background activity launching. |
| **Service does not press Call button or Brilliant-OUT** | **1.** Verify that the Accessibility Service is active in *Settings -> Accessibility*.<br>**2.** Ensure the dialed digits match the keypad.<br>**3.** Check the **Diagnostics** screen in the app to view real-time state machine logs and failure reasons. |
| **Accessibility toggle is grayed out on Android 13+** | Sideloaded apps are subject to Android 13+ restricted settings. Go to *Settings -> Apps -> Brilliant Dialer Bridge*, tap the three dots in the top right corner, tap **Allow restricted settings**, and authenticate. |

---

## 5. Known Limitations

1. **OEM Dialer Variations:** Standard Android implementations (Google Pixel, Motorola, Nothing, Sony, ASUS) list third-party `PhoneAccount` entries directly in their calling account selection popup. Heavily skinned Android forks (Samsung One UI, MIUI/HyperOS) may restrict third-party accounts; use Google Phone or the Call Redirection fallback on those devices.
2. **Brilliant Connect UI Updates:** Automation relies on Brilliant Connect's view IDs (`com.brilliant.connect.com.bd:id/dialler_button`, `com.brilliant.connect.com.bd:id/call_type_list`, and the label `"Brilliant-OUT"`). If InterCloud changes these resource IDs in an update, the Diagnostics screen will pinpoint the exact step that halted.
3. **Personal Sideload Usage:** Google Play Developer Program policies restrict AccessibilityService use for non-accessibility automation. This app is designed for direct personal installation via APK.
4. **Platform Compatibility:** iOS does not offer an open Telecom CallProvider or Accessibility gesture dispatch API equivalent to Android; this functionality is exclusive to Android.
