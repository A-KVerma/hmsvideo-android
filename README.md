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

On the first time of launch, user will be prompted with permissions. Then you are good to go to run the application. To verify quickly, connect from a browser on the same end point URL with `https` prefix and join the room. (wss://prod-in.100ms.live/ws -> https://prod-in.100ms.live)

Start the video conversation!

![app](img/app.png?raw=true "app")

# Quick start into 100ms basics
---

## Core building block concepts


- **Room** - A room represents a real-time audio, data, video and/or screenshare session, the basic building block of the Brytecam Video SDK
- **Stream** - A stream represents real-time audio, video and data media streams that are shared to a room
- **Peer/Participant** - A peer represents all participants connected to a room (other than the local participant)
- **Publish** - A local participant can share its audio, video and data tracks by "publishing" its tracks to the room
- **Subscribe** - A local participant can stream any peer's audio, video and data tracks by "subscribing" to their tracks
- **Broadcast** - A local participant can send any message/data to all peers in the room

## Create and instantiate HMSClient (100ms Client)

This will instantiate an `HMSClient` object

```java
//Create a 100ms peer
hmspeer = new HMSPeer(username, authToken);
//Create a room
hmsRoom = new HMSRoom(roomname);

//Create client configuration
config = new HMSClientConfig(servername);

//Create a 100ms client
hmsClient = new HMSClient(this, getApplicationContext(), hmspeer, config);

hmsClient.setLogLevel(HMSLogger.LogLevel.LOG_DEBUG);
```

## Connect

After instantiating `HMSClient`, connect to 100ms' server

```java
//The client will connect to the WebSocket channel provided through the config
hmsClient.connect();
```

## Join a room

```java
//Pass the unique id for the room here as a String
hmsClient.join(roomid, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
    		//data returns roomid
		Log.v("HMSClient onJoinSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onJoinFailure", errorReason);
	}
});
```

## Setup listeners

After joining, immediately add listeners to listen to peers joining, new streams being added to the room

```java
HMSEventListener listener = new HMSEventListener()
{
    @Override
    public void onConnect() {
       //When the peer connects to the room
    }
    @Override
    public void onDisconnect() {
      //when the peer disconnected from the room   
    }
    @Override
    public void onPeerJoin(HMSPeer peer) {
			//call actions related to a new peer addition 
    }
    @Override
    public void onPeerLeave(HMSPeer peer) {
			//call actions related to a peer removal   
    }
    @Override
    public void onStreamAdd(HMSPeer peer, HMSStreamInfo mediaInfo) {
			//call actions related to a stream addtion
			//call subscribe    
    }
    @Override
    public void onStreamRemove(HMSPeer peer, HMSStreamInfo mediaInfo) {
			//call actions related to a stream removal
			//call unsubscribe    
    }
    @Override
    public void onBroadcast(HMSPayload payload) {
			//call actions related to a broadcast
    }
};
```

## Switch Camera

```java
//Toggle between front and rear camera. Make sure you have initialized 
//hmsclient before calling this
hmsClient.switchCamera();
```

## Mute/unmute local video/audio

Get the local audio/video tracks from **HMSStream.getUserMedia()** method.

---

```java
// To mute a local video track
localVideoTrack = mediaStream.getStream().videoTracks.get(0);
localVideoTrack.setEnabled(false);

// To mute a local audio track
localAudioTrack = mediaStream.getStream().audioTracks.get(0);
localAudioTrack.setEnabled(false);
```

## Create and Get local camera/mic streams

```java
//Set all the media constraints here.
//You can disable video/audio publishing by changing the settings from the settings activity
//Do it before joining the room
localMediaConstraints = new HMSRTCMediaStreamConstraints(DEFAULT_PUBLISH_AUDIO, DEFAULT_PUBLISH_VIDEO);
localMediaConstraints.setVideoCodec(DEFAULT_CODEC);
localMediaConstraints.setVideoFrameRate(Integer.valueOf(DEFAULT_VIDEO_FRAMERATE));
localMediaConstraints.setVideoResolution(DEFAULT_VIDEO_RESOLUTION);
localMediaConstraints.setVideoMaxBitRate(Integer.valueOf(DEFAULT_VIDEO_BITRATE));
if(frontCamEnabled){
    isFrontCameraEnabled = true;
    localMediaConstraints.setCameraFacing(FRONT_FACING_CAMERA);
}
else {
    isFrontCameraEnabled = false;
    localMediaConstraints.setCameraFacing(REAR_FACING_CAMERA);
}

hmsClient.getUserMedia(this, localMediaConstraints, new HMSStream.GetUserMediaListener() {

    @Override
    public void onSuccess(HMSRTCMediaStream mediaStream) {				//Receive the local media stream
     
				 localStream = mediaStream;
				//Expose Media stream APIs to developers
				// process the stream
    }

    @Override
    public void onFailure(String errorReason) {
			Log.v("HMSClient onLeaveFailure", errorReason);
    }
});
```

Please use the following settings for video that looks good in postcard-sized videos - codec:`VP8`, bitrate `256`, framerate `25`. We will extend this in the future to add more options including front/back camera

## Display local stream

Once `mediaStream` has been received, get the video and audio tracks from the stream object.

Call the `VideoTrack` `addsink` method with `SurfaceviewRenderer`.

```java
//The following code is a sample. Developers can make use of the stream object 
//in their own way of rendering
if(mediaStream.getStream().videoTracks.size()>0) {
    localVideoTrack = mediaStream.getStream().videoTracks.get(0);
    localVideoTrack.setEnabled(true);
}
if(mediaStream.getStream().audioTracks.size()>0) {
    localAudioTrack = mediaStream.getStream().audioTracks.get(0);
    localAudioTrack.setEnabled(true);
}

runOnUiThread(() -> {
    try {
        firstSVrenderer.setVisibility(View.VISIBLE);
        localVideoTrack.addSink(firstSVrenderer);
    } catch (Exception e) {
        e.printStackTrace();
    }
});
```

## Publish

A local participant can share her audio, video and data tracks by "publishing" its tracks to the room

```java

hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSStreamRequestHandler() {
@Override
public void onSuccess(HMSPublishStream data) {
    Log.v(TAG, "publish success "+data.getMid());
}

@Override
public void onFailure(long error, String errorReason) {
    Log.v(TAG, "publish failure");
}
});
```


## Subscribe

This method "subscribes" to a peer's stream. This should ideally be called in the `onStreamAdd` listener

```java
hmsClient.subscribe(streamInfo, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onSubscribeSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onSubscribeFailure", errorReason);
	}
});
```

## Broadcast

This method broadcasts a payload to all participants

```java
hmsClient.broadcast(payload, room, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onBroadcastSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onBroadcastFailure", errorReason);
	}
});
```

## Unpublish local stream

```java
hmsClient.unpublish(stream, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onPublishSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onPublishFailure", errorReason);
	}
});
```

## Unsubscribe to a peer's stream

```java
hmsClient.unsubscribe(stream, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onUnSubscribeSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onUnSubscribeFailure", errorReason);
	}
});
```

## Disconnect client

```java
//The client will disconnect from the WebSocket channel provided
hmsClient.disconnect();
```


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

Once you have added dependency libraries, you can run the application. Incase if you face any issues, reach out to us.



