# 100 ms - Android Sample Application - Getting Started

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

## Pre requisites

- Android Studio 2.0 or higher
- Support for Android API level 21 or higher
- Support for Java 8
- The sample application uses build tool version `30.0.1` and latest NDK version 

## Supported Devices

The Android SDK supports Android API level 21 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures.


## Quick start to run the sample application

```bash
git clone https://github.com/100mslive/hmsvideo-android.git
gradlew assembleDebug (or Run it from Android Studio)
```
![Git Pull](img/git.png?raw=true "Git Pull")


## Install SDK & NDK

The sample application should run without any issues after you have the latest SDK and NDK. You don't need to add any other additional steps. 

The sample app uses build tool version 30. 

![SDK version](img/sdkmanager.png?raw=true "SDK Version")

The sample app uses NDK. You can download from the `SDK Tools` tab from your SDK manager. 

![NDK version](img/ndk.png?raw=true "NDK Version")

Once you have both SDK and NDK, you can just run the application. Incase if the app prompts you to select the proper NDK version, follow the below steps. 

Open the `Project Structure` from the `File` menu. (File → Project Structure)

![NDK location](img/projectstr.png?raw=true "NDK location")

And select the NDK path where you have your NDK version downloaded. By default, it will be inside your SDK directory path.

![NDK path](img/ndkselect.png?raw=true "NDK path")

# Run the application using an Emulator

Using an Emulator This guide will walk you through setting up and emulator that is compatible with the 100ms Video SDK.

![Create VD](img/createvd.png?raw=true "Create VD")

Pick the Virtual device that you would like to create.

![Pick VD](img/vdsize.png?raw=true "Pick VD")

Pick a system image for the virtual device. The sample app uses `API version 30`.

![System image](img/systemimage.png?raw=true "System image")

Now, we are good to go to run the application. 

Once we are good to go with the emulator, `Run` the application from Android Studio.

![run](img/runapp.png?raw=true "run app")

In the launch screen, here we need to mention three inputs. 
```
1. Endpoint URL 
2. Room Name
3. User Name
```
And then click `Connect`.

On the first time of launch, user will be prompted with permissions. Then you are good to go to run the application. To verify quickly, connect from a browser on the same end point URL with `https` prefix and join the room. (wss://conf.brytecam.com/ws -> https://conf.brytecam.com)

Start the video conversation!

![app](img/app.png?raw=true "app")


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

