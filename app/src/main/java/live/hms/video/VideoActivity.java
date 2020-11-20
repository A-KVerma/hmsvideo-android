package live.hms.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.brytecam.lib.webrtc.HMSStream;
import com.brytecam.lib.webrtc.HMSRTCMediaStream;
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints;
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils;

import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.Arrays;
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
    HMSRTCMediaStreamConstraints localMediaConstraints;
    private String roomname = null, username = null, authToken = null, servername = null, bitRate = null, env = null;
    private boolean isPublished = false;
    private HMSRTCMediaStream localMediaStream = null;
    VideoTrack localVideoTrack = null;
    AudioTrack localAudioTrack = null;
    VideoTrack secondSVVideoTrack= null;
    AudioTrack secondSVAudioTrack = null;
    VideoTrack thirdSVVideoTrack= null;
    AudioTrack thirdSVAudioTrack = null;
    VideoTrack fourthSVVideoTrack= null;
    AudioTrack fourthSVAudioTrack = null;
    VideoTrack fifthSVVideoTrack= null;
    AudioTrack fifthSVAudioTrack = null;
    VideoTrack sixthSVVideoTrack= null;
    AudioTrack sixthSVAudioTrack = null;
    boolean isCameraToggled = false;
    boolean isAudioEnabled = true;
    boolean isFrontCameraEnabled = true;
    Boolean[] isCellFreeHolder = {true, true, true, true, true, true};

    String[] userIdHolder = new String[6];

    // Control buttons for limited UI
    private ImageButton disconnectButton;
    private ImageButton cameraSwitchButton;
    private ImageButton toggleMuteButton;


    private SurfaceViewRenderer firstSVrenderer, secondSVrenderer, thirdSVrenderer, fourthSVrenderer, fifthSVrenderer, sixthSVrenderer;
    private TextView firstPeerTextView, secondPeerTextView, thirdPeerTextView, fourthPeerTextView, fifthPeerTextView, sixthPeerTextView;

    private static final int RC_CALL = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
            initToggleMenu();
            getUserMedia(isFrontCameraEnabled, isAudioEnabled, isCameraToggled);
        } else {
            EasyPermissions.requestPermissions(this, "Need User permissions to proceed", RC_CALL, perms);
        }

    }

    private void init()
    {
        //Create a 100ms peer
        peer = new HMSPeer(username, authToken);

        //Create a room
        hmsRoom = new HMSRoom(roomname);

        //For debugging purpose. Remove it later
        if (env.equals("conf"))
            peer.setRoomId(hmsRoom.getRoomId());

        //Create client configuration
        config = new HMSClientConfig(servername);

        //Create a 100ms client
        hmsClient = new HMSClient(this, getApplicationContext(), peer, config);

        hmsClient.setLogLevel(HMSLogger.LogLevel.LOG_DEBUG);

        hmsClient.connect();
    }

    void initToggleMenu()
    {


        disconnectButton = findViewById(R.id.button_call_disconnect);
        cameraSwitchButton = findViewById(R.id.button_call_switch_camera);
        toggleMuteButton = findViewById(R.id.button_call_toggle_mic);

        // Add buttons click events.
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });

        cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //firstSVrenderer.release();
                //firstSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view1);
                isCameraToggled = true;

                HMSStream.stopCapturers();
                if(isFrontCameraEnabled)
                    getUserMedia(false, isAudioEnabled, isCameraToggled);
                else
                    getUserMedia(true, isAudioEnabled, isCameraToggled);
            }
        });

        toggleMuteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleMic();
                toggleMuteButton.setAlpha(isAudioEnabled ? 1.0f : 0.3f);
            }
        });

    }


    public boolean toggleMic() {

        if(localAudioTrack!=null)
        {
            if(localAudioTrack.enabled())
            {
                isAudioEnabled = false;
                localAudioTrack.setEnabled(false);
            }
            else
            {
                isAudioEnabled = true;
                localAudioTrack.setEnabled(true);
            }
        }


        return isAudioEnabled;
    }



    private void initializeSurfaceViews() {

        if(HMSWebRTCEglUtils.getRootEglBaseContext()==null)
            HMSWebRTCEglUtils.getRootEglBase();

        firstPeerTextView = (TextView) findViewById(R.id.firstpeer_textview);
        secondPeerTextView = (TextView) findViewById(R.id.secondpeer_textview);
        thirdPeerTextView = (TextView) findViewById(R.id.thirdpeer_textview);
        fourthPeerTextView = (TextView) findViewById(R.id.fourthpeer_textview);
        fifthPeerTextView = (TextView) findViewById(R.id.fifthpeer_textview);
        sixthPeerTextView = (TextView) findViewById(R.id.sixthpeer_textview);

        //Setting local peer name
        firstPeerTextView.setText(username);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Init view
                    firstSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view1);
                    firstSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    firstSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
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
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        });


    }


    public void getUserMedia(boolean frontCamEnabled, boolean audioEnabled, boolean cameraToggle)
    {

        if(cameraToggle)
        {
            hmsClient.unpublish(localMediaStream, hmsRoom, new HMSRequestHandler() {
                @Override
                public void onSuccess(String data) {
                    Log.v(TAG, "unpusblish success: "+data);
                }

                @Override
                public void onFailure(long error, String errorReason) {
                    Log.v(TAG, "unpusblish failure"+errorReason);
                }
            });
        }


        Log.v("getUserMedia", "yes");
        Map<String, Object> video = new HashMap<>();
        video.put("resolution", "qvga");
        video.put("width", 320);
        video.put("height", 240);
        video.put("framerate", 15);
        if(frontCamEnabled) {
            isFrontCameraEnabled = true;
            //video.put("facing", "front");
            video.put("facingMode", "user");
        }
        else {
            isFrontCameraEnabled = false;
            video.put("facingMode", "environment");
            //video.put("facing", "environment");
        }
        video.put("codec", "vp8");
        video.put("bitrate", "128");

        Map<String, Object> audio = new HashMap<>();
        if(audioEnabled)
            audio.put("volume", Double.valueOf("100.0"));
        else
            audio.put("volume", Double.valueOf("0.0"));

        //currentCameraPosition = "user";
        localMediaConstraints = new HMSRTCMediaStreamConstraints(audio, video);



        HMSStream.getUserMedia(this, localMediaConstraints, new HMSStream.GetUserMediaListener() {

            @Override
            public void onSuccess(HMSRTCMediaStream mediaStream) {

                Log.v(TAG, "getusermedia success");

                localMediaStream = mediaStream;

                if(firstSVrenderer ==null)
                {
                    firstSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    firstSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    firstSVrenderer.setEnableHardwareScaler(true);
                    firstSVrenderer.setMirror(true);
                }


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


                if(cameraToggle)
                {
                    hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSRequestHandler() {
                        @Override
                        public void onSuccess(String data) {
                            Log.v("publish success", "yes");
                            isPublished = true;
                            isCellFreeHolder[0] = false;
                            userIdHolder[0] = peer.getPeerId();
                            printAllCelldata();
                        }

                        @Override
                        public void onFailure(long error, String errorReason) {
                            Log.v("publish failure", "yes");
                        }
                    });
                }


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

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    public void disconnect()
    {
        localVideoTrack = null;
        localAudioTrack = null;
        localMediaStream = null;

        if(firstSVrenderer!=null) {
            firstSVrenderer.release();
            firstSVrenderer.clearImage();
        }
        firstSVrenderer = null;
        if(secondSVrenderer!=null){
            secondSVrenderer.release();
            secondSVrenderer.clearImage();
        }
        secondSVrenderer = null;
        if(thirdSVrenderer!=null) {
            thirdSVrenderer.release();
            thirdSVrenderer.clearImage();
        }
        thirdSVrenderer = null;
        if(fourthSVrenderer!=null) {
            fourthSVrenderer.release();
            fourthSVrenderer.clearImage();
        }
        fourthSVrenderer = null;
        if(fifthSVrenderer!=null) {
            fifthSVrenderer.release();
            fifthSVrenderer.clearImage();
        }
        fifthSVrenderer = null;
        if(sixthSVrenderer!=null) {
            sixthSVrenderer.release();
            sixthSVrenderer.clearImage();
        }
        sixthSVrenderer = null;

        hmsClient.unpublish(localMediaStream, hmsRoom, new HMSRequestHandler() {
            @Override
            public void onSuccess(String data) {
                Log.v(TAG, "unpublish success: "+data);
            }

            @Override
            public void onFailure(long error, String errorReason) {
                Log.v(TAG, "unpublish failure"+errorReason);
            }
        });



    }


    void setTracks(MediaStream data, int position, String name)
    {
        //totalRemoteUsers++;
        switch (position) {
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


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(name!=null)
                            secondPeerTextView.setText(name);

                        Log.v(TAG, "Second Sv rendered");
                        secondSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view2);
                        secondSVrenderer.setVisibility(View.VISIBLE);

                        if (secondSVrenderer == null) {
                            secondSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                            secondSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                            secondSVrenderer.setEnableHardwareScaler(true);
                            secondSVrenderer.setMirror(true);
                        }

                        secondSVVideoTrack.addSink(secondSVrenderer);

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


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(name!=null)
                            thirdPeerTextView.setText(name);

                        thirdSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view3);
                        thirdSVrenderer.setVisibility(View.VISIBLE);

                        if(thirdSVrenderer==null) {
                            thirdSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                            thirdSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                            thirdSVrenderer.setEnableHardwareScaler(true);
                            thirdSVrenderer.setMirror(true);
                        }
                        thirdSVVideoTrack.addSink(thirdSVrenderer);

                    }
                });

                break;
            case 3:

                if(data.videoTracks.size()>0) {
                    fourthSVVideoTrack = data.videoTracks.get(0);
                    fourthSVVideoTrack.setEnabled(true);
                }

                if(data.audioTracks.size()>0) {
                    fourthSVAudioTrack = data.audioTracks.get(0);
                    fourthSVAudioTrack.setEnabled(true);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(name!=null)
                            fourthPeerTextView.setText(name);

                        fourthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view4);
                        fourthSVrenderer.setVisibility(View.VISIBLE);
                        if(fourthSVrenderer==null) {
                            fourthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                            fourthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                            fourthSVrenderer.setEnableHardwareScaler(true);
                            fourthSVrenderer.setMirror(true);

                        }
                        fourthSVVideoTrack.addSink(fourthSVrenderer);

                    }
                });

                break;
            case 4:

                if(data.videoTracks.size()>0) {
                    fifthSVVideoTrack = data.videoTracks.get(0);
                    fifthSVVideoTrack.setEnabled(true);
                }
                if(data.audioTracks.size()>0) {
                    fifthSVAudioTrack = data.audioTracks.get(0);
                    fifthSVAudioTrack.setEnabled(true);
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(name!=null)
                            fifthPeerTextView.setText(name);

                        fifthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view5);
                        fifthSVrenderer.setVisibility(View.VISIBLE);
                        if(fifthSVrenderer == null) {
                            fifthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                            fifthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                            fifthSVrenderer.setEnableHardwareScaler(true);
                            fifthSVrenderer.setMirror(true);

                        }
                        fifthSVVideoTrack.addSink(fifthSVrenderer);

                    }
                });

                break;
            case 5:

                if(data.videoTracks.size()>0) {
                    sixthSVVideoTrack = data.videoTracks.get(0);
                    sixthSVVideoTrack.setEnabled(true);
                }
                if(data.audioTracks.size()>0) {
                    sixthSVAudioTrack = data.audioTracks.get(0);
                    sixthSVAudioTrack.setEnabled(true);
                }
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      if(name!=null)
                                          sixthPeerTextView.setText(name);

                                      sixthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view6);
                                      sixthSVrenderer.setVisibility(View.VISIBLE);
                                      if(sixthSVrenderer == null) {
                                          sixthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                                          sixthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                          sixthSVrenderer.setEnableHardwareScaler(true);
                                          sixthSVrenderer.setMirror(true);

                                      }
                                      sixthSVVideoTrack.addSink(sixthSVrenderer);

                                  }
                              }
                );

                break;
            default:
                break;
        }

    }









    @Override
    public void onConnect() {
        Log.v(TAG, "onconnect");

        hmsClient.join(new HMSRequestHandler() {
            @Override
            public void onSuccess(String data) {
                Log.v("Join success","yes");

                if(!isPublished) {

                    hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSRequestHandler() {
                        @Override
                        public void onSuccess(String data) {
                            Log.v("publish success", "yes");
                            isPublished = true;
                            isCellFreeHolder[0] = false;
                            userIdHolder[0] = peer.getPeerId();
                            printAllCelldata();
                        }

                        @Override
                        public void onFailure(long error, String errorReason) {
                            Log.v("publish failure", "yes");
                        }
                    });

                }


            }

            @Override
            public void onFailure(long error, String errorReason) {
                Log.v("Join failure", "yes");
            }
        });


    }

    @Override
    public void onDisconnect() {
        Log.v(TAG, "ondisconnected");



    }

    @Override
    public void onPeerJoin(HMSPeer hmsPeer) {
        Log.v(TAG, "App peer join event"+hmsPeer.getUid());
    }

    @Override
    public void onPeerLeave(HMSPeer hmsPeer) {
        Log.v(TAG, "App peer leave event"+hmsPeer.getUid());
        Log.v(TAG, "On peer leave");
        // printAllCelldata();
    }



    @Override
    public void onStreamAdd(HMSPeer hmsPeer, HMSStreamInfo hmsStreamInfo) {
        Log.v(TAG, "App stream add  event"+hmsPeer.getUid());

        // this code will be executed after 2 seconds
        hmsClient.subscribe(hmsStreamInfo, hmsRoom, new HMSMediaRequestHandler() {
            @Override
            public void onSuccess(MediaStream data) {

                //printAllCelldata();
                int pos = getFreePosition();
                Log.v(TAG, "Current free positions: "+pos);
                if(pos<6) {
                    userIdHolder[pos] = hmsStreamInfo.getUid();
                    isCellFreeHolder[pos] = false;

                    Log.v(TAG, "Onsubsribesuccess");
                    Log.v(TAG, "positiin: " + pos);
                    Log.v(TAG, "user id: " + hmsStreamInfo.getUid());

                    setTracks(data, pos, hmsStreamInfo.getUserName());
                    printAllCelldata();
                }
                if(pos == 7)
                    Toast.makeText(getApplicationContext(), "No space for new users", Toast.LENGTH_SHORT ).show();

            }

            @Override
            public void onFailure(long error, String errorReason) {
                Log.v("HMSClient", "Onsubsuccess");
            }
        });





    }

    @Override
    public void onStreamRemove(HMSStreamInfo hmsStreamInfo) {
        Log.v(TAG,  "onstream remove:"+hmsStreamInfo.getUid() );
        printAllCelldata();
        for(int i = 1;i<6;i++)
        {
            try {
                if (userIdHolder[i]!=null && userIdHolder[i].equals(hmsStreamInfo.getUid())) {
                    isCellFreeHolder[i] = true;
                    userIdHolder[i] = null;

                    hmsClient.unsubscribe(hmsStreamInfo, hmsRoom, new HMSRequestHandler() {

                        @Override
                        public void onSuccess(String data) {
                            Log.v(TAG, "unsubsribed successfully");
                        }

                        @Override
                        public void onFailure(long error, String errorReason) {
                            Log.v(TAG, "unsubsribed failure");

                        }
                    });

                }
            }
            catch(Exception e)
            {
                Log.v(TAG, "Exception :"+e.getMessage());
            }
        }
        Log.v(TAG,  "Clear cell data" );

        clearCellData();
        Log.v(TAG,  "print cell data" );

        printAllCelldata();


    }

    @Override
    public void onBroadcast(HMSPayloadData hmsPayloadData) {

    }


    int getFreePosition()
    {
        //int pos = 0;
        for(int i=1; i<=5; i++)
        {
            Log.v(TAG, "printing free pos:"+i);
            if(isCellFreeHolder[i] == true)
                return i;
        }
        return 7;
    }


    void printAllCelldata()
    {
        for(int i=0;i<6;i++)
        {
            Log.v(TAG, "Pos: "+i+" status: "+isCellFreeHolder[i]+"  uid:"+userIdHolder[i]);
        }
    }

    void clearCellData()
    {
        for(int i=0;i<6;i++){
            if(userIdHolder[i]==null)
            {
                switch(i) {
                    case 1:
                        //secondSVrenderer.release();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(secondSVrenderer!=null)
                                    secondSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });

                        secondSVrenderer.clearImage();
                        secondSVrenderer.clearAnimation();
                        //secondSVrenderer = null;
                        break;
                    case 2:
                        //thirdSVrenderer.release();


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(thirdSVrenderer!=null)
                                    thirdSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });


                        thirdSVrenderer.clearImage();
                        thirdSVrenderer.clearAnimation();
                        //thirdSVrenderer = null;
                        break;
                    case 3:

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(fourthSVrenderer!=null)
                                    fourthSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });

                        //fourthSVrenderer.setVisibility(View.INVISIBLE);
                        fourthSVrenderer.clearAnimation();
                        fourthSVrenderer.clearImage();
                        //fourthSVrenderer =null;
                        break;
                    case 4:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(fifthSVrenderer!=null)
                                    fifthSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });


                        fifthSVrenderer.clearImage();
                        fifthSVrenderer.clearAnimation();

                        break;
                    case 5:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(sixthSVrenderer!=null)
                                    sixthSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });


                        sixthSVrenderer.clearImage();
                        sixthSVrenderer.clearAnimation();

                        break;
                    default:
                        break;

                }
            }
        }
    }


}