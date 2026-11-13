# MenuTest

Opens the hidden test / service / engineering menus of Android phones directly, without the dialer and without typing codes.

* **Menus** – curated catalog per brand (Samsung, Xiaomi, Huawei, OnePlus/Oppo/Realme, Vivo, Motorola, Sony, Pixel, Nokia, LG, ASUS, HTC, Transsion, MediaTek, Qualcomm, AOSP, classic codes, system screens). Your own brand is detected and shown first.
* **Discover** – scans the installed packages for engineering/factory/test activities and opens the ones the system allows.
* **Settings** – English / Français / system language, vibration on/off.

Each entry is opened with the first method that works on the device: explicit activity → secret-code broadcast → system screen. If none works, the app says so and offers to copy the code or open the dialer.

## Build
Push to GitHub: the workflow in `.github/workflows/build.yml` builds a signed release APK (artifact `MenuTest-apk`).
Local: `gradle assembleRelease` (AGP 8.5, Kotlin 1.9.24, JDK 17).

`release.jks` (password `menutest`) is included so every build is signed with the same key and updates install over each other.
