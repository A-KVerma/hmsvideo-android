package live.hms.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.brytecam.lib.HMSClient;
import com.brytecam.lib.HMSClientConfig;
import com.brytecam.lib.HMSEventListener;
import com.brytecam.lib.HMSLogger;
import com.brytecam.lib.HMSMediaRequestHandler;
import com.brytecam.lib.HMSPeer;
import com.brytecam.lib.HMSRequestHandler;
import com.brytecam.lib.HMSRoom;
import com.brytecam.lib.payload.HMSPayloadData;
import com.brytecam.lib.payload.HMSStreamInfo;

import com.brytecam.lib.webrtc.HMSRTCMediaStream;
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints;
import com.brytecam.lib.webrtc.HMSStream;
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils;

import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoActivity extends AppCompatActivity implements HMSEventListener {

    private String TAG = "HMSVideoActivity";
    HMSPeer peer ;
    HMSRoom hmsRoom;
    //Create client configuration
    HMSClientConfig config ;
    //Create a 100ms client
    HMSClient hmsClient;
    String currentCameraPosition;
    private EglBase rootEglBase;
    HMSRTCMediaStreamConstraints localMediaConstraints;
    private String roomname = null, username = null, authToken = null, servername = null, bitRate = null, env = null;
    private int totalRemoteUsers = 0;
    private boolean isPublished = false;
    private HMSRTCMediaStream localMediaStream = null;
    VideoTrack localVideoTrack = null;
    AudioTrack localAudioTrack = null;
    VideoTrack secondSVVideoTrack= null;
    AudioTrack secondSVAudioTrack = null;
    VideoTrack thirdSVVideoTrack= null;
    AudioTrack thirdSVAudioTrack = null;

    private SurfaceViewRenderer firstSVrenderer, secondSVrenderer;

    private SurfaceViewRenderer  thirdSVrenderer, fourthSVrenderer, fifthSVrenderer, sixthSVrenderer;

    private static final int RC_CALL = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        if(getIntent()!=null)
        {
            servername = getIntent().getStringExtra("server");
            roomname = getIntent().getStringExtra("room");
            username = getIntent().getStringExtra("user");
            authToken = getIntent().getStringExtra("auth_token");
            bitRate = getIntent().getStringExtra("bitrate");
            env = getIntent().getStringExtra("env");
        }


        start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //You can use your own implmentation to get user permissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CALL)
    void start()
    {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            init();
            initializeSurfaceViews();
            getUserMedia();
        } else {
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }

    }

    private void init()
    {
        //Create a 100ms peer
        peer = new HMSPeer(username, authToken);
        //Create a room
        hmsRoom = new HMSRoom(roomname);

        //For debugging purpose. Remove it later
        if(env.equals("conf"))
            peer.setRoomId(hmsRoom.getRoomId());

        //Create client configuration
        config = new HMSClientConfig(servername);

        //Create a 100ms client
        hmsClient = new HMSClient(this, getApplicationContext(), peer, config);

        hmsClient.setLogLevel(HMSLogger.LogLevel.LOG_DEBUG);

        hmsClient.connect();
    }

    private void initializeSurfaceViews() {
        //rootEglBase = EglBase.create();
        //Init view
        firstSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view1);
        firstSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
        firstSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        firstSVrenderer.setEnableHardwareScaler(true);
        firstSVrenderer.setMirror(true);


        secondSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view2);
        secondSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
        secondSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        secondSVrenderer.setEnableHardwareScaler(true);
        secondSVrenderer.setMirror(true);

        thirdSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view3);
        thirdSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
        thirdSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        thirdSVrenderer.setEnableHardwareScaler(true);
        thirdSVrenderer.setMirror(true);

        fourthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view4);
        fourthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
        fourthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        fourthSVrenderer.setEnableHardwareScaler(true);
        fourthSVrenderer.setMirror(true);


        fifthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view5);
        fifthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
        fifthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        fifthSVrenderer.setEnableHardwareScaler(true);
        fifthSVrenderer.setMirror(true);


        sixthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view6);
        sixthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
        sixthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        sixthSVrenderer.setEnableHardwareScaler(true);
        sixthSVrenderer.setMirror(true);

    }


    public void getUserMedia()
    {
        Log.v("getUserMedia", "yes");
        Map<String, Object> video = new HashMap<>();
        video.put("resolution", "qvga");
        video.put("width", 320);
        video.put("height", 240);
        video.put("framerate", 15);
        video.put("codec", "vp8");
        video.put("bitrate", "128");
        currentCameraPosition = "user";
        localMediaConstraints = new HMSRTCMediaStreamConstraints(true, video);

        HMSStream.getUserMedia(this, localMediaConstraints, new HMSStream.GetUserMediaListener() {

            @Override
            public void onSuccess(HMSRTCMediaStream mediaStream) {

                localMediaStream = mediaStream;
                //firstSVrenderer.setSrcObject(mediaStream);




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



                hmsClient.join(new HMSRequestHandler() {
                    @Override
                    public void onSuccess(String data) {
                        Log.v("Join success","yes");
                        hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSRequestHandler() {
                            @Override
                            public void onSuccess(String data) {
                                Log.v("publish success", "yes");
                                isPublished = true;
                            }

                            @Override
                            public void onFailure(long error, String errorReason) {
                                Log.v("publish failure", "yes");
                            }
                        });


                    }

                    @Override
                    public void onFailure(long error, String errorReason) {
                        Log.v("Join failure", "yes");
                    }
                });




            }

            @Override
            public void onFailure(long errorcode, String errorreason) {
                Log.v("getUserMedia failure", "yes");
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        disconnect();
        hmsClient.disconnect();
    }



    public void disconnect()
    {
        HMSStream.stopCapturers();
        firstSVrenderer = null;
        secondSVrenderer = null;
        thirdSVrenderer = null;
        fourthSVrenderer = null;
        fifthSVrenderer = null;
        sixthSVrenderer = null;
    }


    void setTracks(MediaStream data)
    {
        totalRemoteUsers++;

        switch (totalRemoteUsers) {
            case 1:
                if(data.videoTracks.size()>0) {
                    secondSVVideoTrack = data.videoTracks.get(0);
                    secondSVVideoTrack.setEnabled(true);
                }
                if(data.audioTracks.size()>0)
                {
                    secondSVAudioTrack = data.audioTracks.get(0);
                    secondSVAudioTrack.setEnabled(true);
                }

                runOnUiThread(() -> {
                    try {
                        secondSVrenderer.setVisibility(View.VISIBLE);
                        secondSVVideoTrack.addSink(secondSVrenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case 2:
                if(data.videoTracks.size()>0) {
                    thirdSVVideoTrack = data.videoTracks.get(0);
                    thirdSVVideoTrack.setEnabled(true);
                }
                if(data.audioTracks.size()>0) {
                    thirdSVAudioTrack = data.audioTracks.get(0);
                    thirdSVAudioTrack.setEnabled(true);
                }
                runOnUiThread(() -> {
                    try {
                        thirdSVrenderer.setVisibility(View.VISIBLE);
                        thirdSVVideoTrack.addSink(thirdSVrenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case 3:
                VideoTrack fourthSVVideoTrack = data.videoTracks.get(0);
                AudioTrack fourthSVAudioTrack = data.audioTracks.get(0);
                fourthSVVideoTrack.setEnabled(true);
                fourthSVAudioTrack.setEnabled(true);

                runOnUiThread(() -> {
                    try {
                        fourthSVrenderer.setVisibility(View.VISIBLE);
                        fourthSVVideoTrack.addSink(fourthSVrenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case 4:
                VideoTrack fifthSVVideoTrack = data.videoTracks.get(0);
                AudioTrack fifthSVAudioTrack = data.audioTracks.get(0);
                fifthSVVideoTrack.setEnabled(true);
                fifthSVAudioTrack.setEnabled(true);

                runOnUiThread(() -> {
                    try {
                        fifthSVrenderer.setVisibility(View.VISIBLE);
                        fifthSVVideoTrack.addSink(fifthSVrenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case 5:
                VideoTrack sixthSVVideoTrack = data.videoTracks.get(0);
                AudioTrack sixthSVAudioTrack = data.audioTracks.get(0);
                sixthSVVideoTrack.setEnabled(true);
                sixthSVAudioTrack.setEnabled(true);

                runOnUiThread(() -> {
                    try {
                        sixthSVrenderer.setVisibility(View.VISIBLE);
                        sixthSVVideoTrack.addSink(sixthSVrenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case 6:
                break;
            case 7:
            default:
                break;
        }

    }






    @Override
    public void onConnect() {
        Log.v(TAG, "onconnect");

    }

    @Override
    public void onDisconnect() {
        Log.v(TAG, "ondisconnected");



    }

    @Override
    public void onPeerJoin(HMSPeer hmsPeer) {

    }

    @Override
    public void onPeerLeave(HMSPeer hmsPeer) {

    }

    @Override
    public void onStreamAdd(HMSPeer hmsPeer, HMSStreamInfo hmsStreamInfo) {


        // this code will be executed after 2 seconds
        hmsClient.subscribe(hmsStreamInfo, hmsRoom, new HMSMediaRequestHandler() {
            @Override
            public void onSuccess(MediaStream data) {

                Log.v("HMSClient", "Onsubsribesuccess");
                setTracks(data);

            }

            @Override
            public void onFailure(long error, String errorReason) {
                Log.v("HMSClient", "Onsubsuccess");
            }
        });





    }

    @Override
    public void onStreamRemove(HMSStreamInfo hmsStreamInfo) {

        hmsClient.unsubscribe(hmsStreamInfo, hmsRoom, new HMSRequestHandler()
        {

            @Override
            public void onSuccess(String data) {

            }

            @Override
            public void onFailure(long error, String errorReason) {

            }
        });

    }

    @Override
    public void onBroadcast(HMSPayloadData hmsPayloadData) {

    }
}