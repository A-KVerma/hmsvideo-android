# 100 ms - Android Sample Application 

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

## Pre requisites

- Android Studio 2.0 or higher
- Support for Android API level 21 or higher
- Support for Java 8
- The sample application uses build tool version `30.0.1`

## Supported Devices

The Android SDK supports Android API level 21 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures.

## Quick start to run the sample application

- Clone this repository

  ```bash
  git clone https://github.com/100mslive/hmsvideo-android.git
  ```

- Host your token generation service [following this guide](https://100ms.gitbook.io/100ms/helpers/runkit)
- Create `app/gradle.properties`

  ```bash
  cp app/gradle.properties.example app/gradle.properties
  ```

- Put your endpoint URL as `TOKEN_ENDPOINT` in `app/gradle.properties`

# Run the application

## Run using Emulator

Follow the official guide at [developers.android.com](https://developer.android.com/studio/run/emulator) to download and deploying app in a emulator.

## Run on Device (recommended)

Follow the official guide at [developers.android.com](https://developer.android.com/studio/run/device) to setup your mobile device for development.

## Layout

In the launch screen, here we need to mention three inputs.

```
1. Endpoint URL
2. Room ID
3. User Name
```

NOTE: Use the exact Room Id as obtained from the [`create-room` API](https://100ms.gitbook.io/100ms/server-side/create-room)

And then click `Connect`.

On the first time of launch, user will be prompted with permissions. Then you are good to go to run the application. 

Start the video conversation!

### Create a Room

Follow the create-room guide in [100ms GitBook](https://100ms.gitbook.io/100ms/server-side/create-room).

![app](img/app.png?raw=true "app")

# 100ms SDK Documentation

Refer the [Getting Started - Android](https://100ms.gitbook.io/100ms/client-side/android) guide in 100ms Gitbook.
