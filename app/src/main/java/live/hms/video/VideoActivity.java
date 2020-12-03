package live.hms.video;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;


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
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

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
    private boolean isPublished = false, isJoined = false;
    private HMSRTCMediaStream localMediaStream = null;
    HMSRTCMediaStreamConstraints localMediaStreamConstraints = null;
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


    //Settings
    private SharedPreferences hmsSharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener hmsSharedPrefListener;
    private boolean DEFAULT_PUBLISH_VIDEO = true;
    private boolean DEFAULT_PUBLISH_AUDIO = true;
    private String DEFAULT_VIDEO_RESOLUTION = "640 x 480";
    private String DEFAULT_VIDEO_BITRATE = "256";
    private String DEFAULT_VIDEO_FRAMERATE = "30";
    private String DEFAULT_CODEC = "VP8";
    private String FRONT_FACING_CAMERA = "user";
    private String REAR_FACING_CAMERA = "environment";



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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bryte, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            //Do your stuff here
            Intent callIntent = new Intent(VideoActivity.this, SettingsActivity.class);
            callIntent.putExtra("from", "videoscreen");
            startActivity(callIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @AfterPermissionGranted(RC_CALL)
    void start()
    {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            initPreferences();
            initHMSClient();
            initializeSurfaceViews();
            initToggleMenu();

        } else {
            EasyPermissions.requestPermissions(this, "Need User permissions to proceed", RC_CALL, perms);
        }

    }

    private void initPreferences()
    {
        //Loads Shared preferences
        hmsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DEFAULT_PUBLISH_VIDEO = hmsSharedPreferences.getBoolean("publish_video", true);
        DEFAULT_PUBLISH_AUDIO = hmsSharedPreferences.getBoolean("publish_audio", true);
        DEFAULT_VIDEO_RESOLUTION = hmsSharedPreferences.getString("resolution", "640 x 480");
        DEFAULT_CODEC = hmsSharedPreferences.getString("codec", "VP8");
        DEFAULT_VIDEO_BITRATE = hmsSharedPreferences.getString("video_bitrate", "256");
        DEFAULT_VIDEO_FRAMERATE = hmsSharedPreferences.getString("video_framerate", "30");

        isAudioEnabled = DEFAULT_PUBLISH_AUDIO;


        //Setup a shared preference listener
        hmsSharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("publish_video") && localVideoTrack !=null) {
                    Log.v(TAG, "Boolean video changes: "+hmsSharedPreferences.getBoolean(key, true));
                    DEFAULT_PUBLISH_VIDEO = hmsSharedPreferences.getBoolean(key, true);

                    if(DEFAULT_PUBLISH_VIDEO ){
                        localVideoTrack.setEnabled(true);
                        localVideoTrack.addSink(firstSVrenderer);
                    }
                    else
                    {
                        localVideoTrack.setEnabled(false);
                        localVideoTrack.removeSink(firstSVrenderer);
                    }
                }

                if (key.equals("publish_audio") && localAudioTrack!=null) {
                    DEFAULT_PUBLISH_AUDIO = hmsSharedPreferences.getBoolean(key, true);
                    Log.v(TAG, "Boolean audio changes: "+hmsSharedPreferences.getBoolean(key, true));
                    if(DEFAULT_PUBLISH_AUDIO)
                        localAudioTrack.setEnabled(true);
                    else
                        localAudioTrack.setEnabled(false);
                }

                if (key.equals("resolution")) {
                    DEFAULT_VIDEO_RESOLUTION = hmsSharedPreferences.getString(key, "VGA (640 x 480)");
                    Log.v(TAG, "Resolution changes: "+ DEFAULT_VIDEO_RESOLUTION);
                    localMediaConstraints.setVideoResolution(DEFAULT_VIDEO_RESOLUTION);
                    hmsClient.applyResolution(localMediaConstraints);
                }

                if(key.equals("codec")){
                    DEFAULT_CODEC = hmsSharedPreferences.getString(key, "VP8");
                    Log.v(TAG, "Codec changes: "+ DEFAULT_CODEC);
                }

                if(key.equals("video_bitrate")){
                    DEFAULT_VIDEO_BITRATE = hmsSharedPreferences.getString(key, "512");
                    Log.v(TAG, "Bitrate changes: "+ DEFAULT_VIDEO_BITRATE);
                    hmsClient.setBitrate(Integer.valueOf(DEFAULT_VIDEO_BITRATE)*1000);
                }

                if(key.equals("video_framerate")){
                    DEFAULT_VIDEO_FRAMERATE = hmsSharedPreferences.getString(key, "30");
                    if(firstSVrenderer!=null)
                        firstSVrenderer.setFpsReduction(Float.parseFloat(DEFAULT_VIDEO_FRAMERATE));
                    Log.v(TAG, "Framerate changes: "+ DEFAULT_VIDEO_FRAMERATE);
                }
            };
        };
        hmsSharedPreferences.registerOnSharedPreferenceChangeListener(hmsSharedPrefListener);
    }


    private void initHMSClient()
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
                isCameraToggled = true;
                hmsClient.switchCamera();

            }
        });

        toggleMuteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleMic();
                toggleMuteButton.setAlpha(isAudioEnabled ? 1.0f : 0.2f);
            }
        });

    }


    public void toggleMic() {

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


                    thirdSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view3);
                    thirdSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    thirdSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    thirdSVrenderer.setEnableHardwareScaler(true);


                    fourthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view4);
                    fourthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    fourthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    fourthSVrenderer.setEnableHardwareScaler(true);



                    fifthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view5);
                    fifthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    fifthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    fifthSVrenderer.setEnableHardwareScaler(true);



                    sixthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view6);
                    sixthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    sixthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    sixthSVrenderer.setEnableHardwareScaler(true);

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
        localMediaConstraints = new HMSRTCMediaStreamConstraints(true, DEFAULT_PUBLISH_VIDEO);
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



        hmsClient.getUserMedia(this, localMediaConstraints, new HMSClient.GetUserMediaListener() {

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
                    if(DEFAULT_PUBLISH_VIDEO)
                        localVideoTrack.setEnabled(true);
                    else
                        localVideoTrack.setEnabled(false);
                }
                if(mediaStream.getStream().audioTracks.size()>0) {
                    localAudioTrack = mediaStream.getStream().audioTracks.get(0);
                    if(DEFAULT_PUBLISH_AUDIO)
                        localAudioTrack.setEnabled(true);
                    else
                        localAudioTrack.setEnabled(false);
                }

                runOnUiThread(() -> {
                    try {
                        firstSVrenderer.setVisibility(View.VISIBLE);
                        localVideoTrack.addSink(firstSVrenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                if(!isPublished) {
                    hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSRequestHandler() {
                        @Override
                        public void onSuccess(String data) {
                            Log.v(TAG, "publish success");
                            isPublished = true;
                            isCellFreeHolder[0] = false;
                            userIdHolder[0] = peer.getPeerId();
                            printAllCelldata();
                        }

                        @Override
                        public void onFailure(long error, String errorReason) {
                            Log.v(TAG, "publish failure");
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
        HMSStream.stopCapturers();
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

                Log.v(TAG, "data tracks: "+data.videoTracks.size()+" audiotracks: "+ data.audioTracks.size());
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
                        {
                            if(data.videoTracks.size()>0 && data.audioTracks.size()>0)
                                secondPeerTextView.setText("AV: "+name);
                            if(data.videoTracks.size()==0 && data.audioTracks.size()>0)
                                secondPeerTextView.setText("audio: "+name);
                            if(data.videoTracks.size()>0 && data.audioTracks.size()==0)
                                secondPeerTextView.setText("video: "+name);
                        }



                        secondSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view2);
                        secondSVrenderer.setVisibility(View.VISIBLE);

                        if(data.videoTracks.size()>0) {
                            if (secondSVrenderer == null) {
                                secondSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                                secondSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                secondSVrenderer.setEnableHardwareScaler(true);
                            }
                            secondSVVideoTrack.addSink(secondSVrenderer);
                        }

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
                        {
                            if(data.videoTracks.size()>0 && data.audioTracks.size()>0)
                                thirdPeerTextView.setText("AV: "+name);
                            if(data.videoTracks.size()==0 && data.audioTracks.size()>0)
                                thirdPeerTextView.setText("audio: "+name);
                            if(data.videoTracks.size()>0 && data.audioTracks.size()==0)
                                thirdPeerTextView.setText("video: "+name);
                        }

                        thirdSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view3);
                        thirdSVrenderer.setVisibility(View.VISIBLE);
                        if(data.videoTracks.size()>0) {
                            if (thirdSVrenderer == null) {
                                thirdSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                                thirdSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                thirdSVrenderer.setEnableHardwareScaler(true);
                            }
                            thirdSVVideoTrack.addSink(thirdSVrenderer);
                        }
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
                        {
                            if(data.videoTracks.size()>0 && data.audioTracks.size()>0)
                                fourthPeerTextView.setText("AV: "+name);
                            if(data.videoTracks.size()==0 && data.audioTracks.size()>0)
                                fourthPeerTextView.setText("audio: "+name);
                            if(data.videoTracks.size()>0 && data.audioTracks.size()==0)
                                fourthPeerTextView.setText("video: "+name);
                        }


                        fourthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view4);
                        fourthSVrenderer.setVisibility(View.VISIBLE);
                        if(data.videoTracks.size()>0) {
                            if (fourthSVrenderer == null) {
                                fourthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                                fourthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                fourthSVrenderer.setEnableHardwareScaler(true);

                            }
                            fourthSVVideoTrack.addSink(fourthSVrenderer);
                        }
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
                        {
                            if(data.videoTracks.size()>0 && data.audioTracks.size()>0)
                                fifthPeerTextView.setText("AV: "+name);
                            if(data.videoTracks.size()==0 && data.audioTracks.size()>0)
                                fifthPeerTextView.setText("audio: "+name);
                            if(data.videoTracks.size()>0 && data.audioTracks.size()==0)
                                fifthPeerTextView.setText("video: "+name);
                        }

                        fifthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view5);
                        fifthSVrenderer.setVisibility(View.VISIBLE);
                        if(data.videoTracks.size()>0) {
                            if (fifthSVrenderer == null) {
                                fifthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                                fifthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                fifthSVrenderer.setEnableHardwareScaler(true);

                            }
                            fifthSVVideoTrack.addSink(fifthSVrenderer);
                        }
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
                          {
                              if(data.videoTracks.size()>0 && data.audioTracks.size()>0)
                                  sixthPeerTextView.setText("AV: "+name);
                              if(data.videoTracks.size()==0 && data.audioTracks.size()>0)
                                  sixthPeerTextView.setText("audio: "+name);
                              if(data.videoTracks.size()>0 && data.audioTracks.size()==0)
                                  sixthPeerTextView.setText("video: "+name);
                          }

                          sixthSVrenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view6);
                          sixthSVrenderer.setVisibility(View.VISIBLE);
                          if(data.videoTracks.size()>0) {
                              if (sixthSVrenderer == null) {
                                  sixthSVrenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                                  sixthSVrenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                  sixthSVrenderer.setEnableHardwareScaler(true);

                              }
                              sixthSVVideoTrack.addSink(sixthSVrenderer);
                          }
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
        Log.v(TAG, "You should be able to see local camera feed once the network connection is established and the user is able to join the room");

        if(!isJoined)
        {
            hmsClient.join(new HMSRequestHandler() {
                @Override
                public void onSuccess(String data) {
                    isJoined = true;
                    Log.v(TAG, "join success");
                    getUserMedia(isFrontCameraEnabled, DEFAULT_PUBLISH_AUDIO, isCameraToggled);
                }

                @Override
                public void onFailure(long error, String errorReason) {
                    Log.v(TAG, "join failure");

                }
            });
        }
    }

    @Override
    public void onDisconnect(String errorMessage) {
        Log.v(TAG, "ondisconnected: "+ errorMessage);

        //Clean up the local streams
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

        //add your retry connection logic here.
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

                int pos = getFreePosition();
                Log.v(TAG, "Current free positions: "+pos);
                if(pos<6) {
                    userIdHolder[pos] = hmsStreamInfo.getUid();
                    isCellFreeHolder[pos] = false;

                    Log.v(TAG, "On subscribe success");
                    Log.v(TAG, "position: " + pos);
                    Log.v(TAG, "user id: " + hmsStreamInfo.getUid());

                    setTracks(data, pos, hmsStreamInfo.getUserName());
                    printAllCelldata();
                }
                if(pos == 7)
                    Log.v(TAG, "No more UI space for additional users but you can hear the audio");

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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(secondSVrenderer!=null)
                                    secondSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });

                        secondSVrenderer.clearImage();
                        secondSVrenderer.clearAnimation();

                        break;
                    case 2:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(thirdSVrenderer!=null)
                                    thirdSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });

                        thirdSVrenderer.clearImage();
                        thirdSVrenderer.clearAnimation();
                        break;
                    case 3:

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(fourthSVrenderer!=null)
                                    fourthSVrenderer.setVisibility(View.INVISIBLE);
                            }
                        });

                        fourthSVrenderer.clearAnimation();
                        fourthSVrenderer.clearImage();
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