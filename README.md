# BKD Scanner

Barcode Scanner by barKoder allows you to extract barcode information from camera video stream or image files. It is a completely free application developed for various uses be that in retail, logistics, warehousing, healthcare and any other industry where barcodes are implemented. The Barcode Scanner by barKoder app is essentially a demo of the capabilities of the barKoder barcode scanner SDK in terms of performance & features.
Integrating the barKoder Barcode Scanner SDK into your Enterprise or Consumer mobile app will instantly transform your user's smartphones & tablets into rugged barcode scanning devices without the need to procure & maintain expensive hardware devices with a short life span.

## Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)

## Installation

### Prerequisites

- Android Studio (latest version recommended)

### Steps

**Clone the repository**

   ```bash
   git clone https://github.com/barKoderSDK/demo-barkoder-android.git
   ```

## Configuration

### Add google-services.json

- Obtain your google-services.json file from the Firebase console.
- Add the google-services.json file to your project.

### Update config.xml

Navigate to BKDDemo/app/src/scanner/res/values/config.xml and update the following properties with your links and license key

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <string name="howToUseLink">https://docs.barkoder.com/en/how-to/demo-app-barKoder></string>
    <string name="learnMoreLink">https://barkoder.com></string>
    <string name="termsOfUseLink">https://barkoder.com/terms-of-use></string>
    <string name="testBarcodeLink">https://barkoder.com/register></string>
    <string name="privacyPolicyLink">https://barkoder.com/privacy-policy></string>
    <string name="barkoderLicenseKey">LICENSE_KEY></string>

</resources>
```

**Replace LICENSE_KEY with your actual Barkoder license key**

### Update app icon, colors, application-id and logo

1. Update logo and AppIcon
    - Navigate to BKDDemo/app/src/scanner/res.
    - Replace the existing app icons in mipmap and logos in drawable folder with the same names with your custom images.
2. Update brand and accent colors
    - Navigate to scanner/res/values/config.xml
    - Modify the brand and accent colors to match your brand's color scheme
3. Update application ID and Display Name
    -   Navigate to build.gradle(Module:app) and update the applicationId on scanner product flavor
    -   Go to the build.gradle(Module:app) and update the buildConfigField on scanner product flavor

## Usage

Open the project

1. Open the project in Android Studio:
2. Open Build -> Select Build Variants option in Android Studio
3. Choose the scanner build variant
    -   Select scannerDebug or scannerRelease variant
4. Build and run the project
    -   Select your target device or simulator

