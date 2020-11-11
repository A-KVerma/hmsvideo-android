# 100 ms - Android Sample Application - Getting Started

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

## Quick start to run the sample application

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

## To run the sample application

The sample application should run without any issues after you have the latest SDK and NDK. You don't need to add any other additional steps. 

## To add it on your existing project

All the following libraries in your app level gradle file as dependencies. Incase if you are using any of the following libraries already in your application, use the same version as it is. Make sure `okhttp` and `webrtc` uses the same version as mentioned. 

```
implementation 'org.webrtc:google-webrtc:1.0.32006'
implementation 'com.squareup.okhttp3:okhttp:3.6.0'
implementation 'com.google.code.gson:gson:2.8.6'
implementation 'org.jetbrains:annotations:15.0'
implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.2.61'
implementation "org.jetbrains.kotlin:kotlin-reflect:1.3.10"
implementation 'com.alibaba:fastjson:1.1.70.android'
```

## Add Compile options

```
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

## Library Dependency

There are two library files that needs to be imported from the "libs" folder of the sample application. Follow the below steps to add both libraries into your project.

```jsx
1. Open your project in Android Studio
2. Open File -> Project structure -> Go to modules
3. Click on + sign and create a new module
4. Select import .jar/.aar file and import the brytecam-sdk-0.8.3.aar lib
5. Click Finish

Now do the same steps for brytecam-lib-1.0.3 lib as well
```

