# 100 ms - Android Sample Application - Getting Started

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

## Quick start

```bash
git clone https://github.com/100mslive/hmsvideo-android.git
gradlew assembleDebug (or Run it from Android Studio)
```

## Pre requisites

- Android Studio 2.0 or higher
- Support for Android API level 21 or higher
- Support for Java 8
- The sample application uses build tool version `30.0.1` and latest NDK version 

## Supported Devices

The Android SDK supports Android API level 21 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures.

## Library Dependency

This step won't be necessary. There are two library files that needs to be imported inside "libs" folder of the sample application. Follow the below steps to add both libraries into your project.

```jsx
1. Open your project in Android Studio
2. Open File -> Project structure -> Go to modules
3. Click on + sign and create a new module
4. Select import .jar/.aar file and import the brytecam-sdk-0.8.3.aar lib
5. Click Finish

Now do the same steps for brytecam-lib-1.0.3 lib as well
```

 And all the transitive dependencies are added in the sample application's app level gradle file.
