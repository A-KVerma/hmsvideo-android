package live.hms.video;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.brytecam.lib.HMSStreamRequestHandler;
import com.brytecam.lib.payload.HMSPayloadData;
import com.brytecam.lib.payload.HMSPublishStream;
import com.brytecam.lib.payload.HMSStreamInfo;
import com.brytecam.lib.webrtc.HMSRTCMediaStream;
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints;
import com.brytecam.lib.webrtc.HMSStream;
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils;
import com.instabug.library.InstabugTrackingDelegate;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import live.hms.video.network.NetworkInterface;
import live.hms.video.network.NetworkReceiver;
import live.hms.video.network.NetworkUtil;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoActivity extends AppCompatActivity implements HMSEventListener, NetworkInterface {

    private String TAG = "HMSVideoActivity";
    private HMSPeer peer ;
    private HMSRoom hmsRoom;
    //Create client configuration
    private HMSClientConfig config ;
    //Create a 100ms client
    private HMSClient hmsClient;
    private HMSRTCMediaStreamConstraints localMediaConstraints;
    private String roomname = null, username = null, authToken = null, servername = null, bitRate = null, env = null;
    private boolean isPublished = false, isJoined = false;
    private HMSRTCMediaStream localMediaStream = null;
    private VideoTrack localVideoTrack = null;
    private AudioTrack localAudioTrack = null;

    private int TOTAL_REMOTE_PEERS = 7;
    private SurfaceViewRenderer[] remoteSurfaceViewRenderers = new SurfaceViewRenderer[TOTAL_REMOTE_PEERS];
    private TextView[] remoteTextViews = new TextView[TOTAL_REMOTE_PEERS];
    private VideoTrack[] remoteVideoTracks = new VideoTrack[TOTAL_REMOTE_PEERS];
    private AudioTrack[] remoteAudioTracks = new AudioTrack[TOTAL_REMOTE_PEERS];
    private String[] remoteUserIds = new String[TOTAL_REMOTE_PEERS];
    private Boolean[] isRemoteCellFree = new Boolean[TOTAL_REMOTE_PEERS];



    boolean isCameraToggled = false;
    boolean isAudioEnabled = true;
    boolean isFrontCameraEnabled = true;
    //private Boolean[] isCellFreeHolder = {true, true, true, true, true, true};
    private int cell =0;
    //private String[] userIdHolder = new String[6];

    // Control buttons for limited UI
    private ImageButton disconnectButton;
    private ImageButton cameraSwitchButton;
    private ImageButton toggleMuteButton;


    private SurfaceViewRenderer localSurfaceViewRenderer;
    private TextView localPeerTextView;

    private static final int RC_CALL = 111;
    private boolean isNetworkAvailable = true;
    private boolean shouldReconnect = false;
    private boolean isRetrying = false;

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

    private static BroadcastReceiver networkReceiver = null;
    private int delay = 0, retryCount = 0, maxRetryCount = 40;
    private Handler reconnectionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
        } else {
            final Window window = getWindow();
            getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

        if(getIntent()!=null)
        {
            servername = getIntent().getStringExtra("server");
            roomname = getIntent().getStringExtra("room");
            username = getIntent().getStringExtra("user");
            authToken = getIntent().getStringExtra("auth_token");
            bitRate = getIntent().getStringExtra("bitrate");
            env = getIntent().getStringExtra("env");
        }

        //Internet connectivity receiver
        networkReceiver = new NetworkReceiver(this);
        broadcastIntent();


        start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        InstabugTrackingDelegate.notifyActivityGotTouchEvent(ev, this);
        return super.dispatchTouchEvent(ev);
    }

    public void broadcastIntent() {
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            if(networkReceiver!=null)
                unregisterReceiver(networkReceiver);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
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

    @Override
    public void onNetworkStatusChange(String status) {
        Log.v(TAG, "Network status change: " + status+" Should reconnect: "+shouldReconnect);
        if (status.isEmpty() || status.equals("No internet is available") || status.equals("No Internet Connection")) {

        }
        else
        {
           // if(shouldReconnect)
             //   handleReconnect();
        }
    }



    @AfterPermissionGranted(RC_CALL)
    void start()
    {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms) && initNetworkCheck()) {
            initPreferences();
            initHMSClient();
            initializeSurfaceViews();
            initToggleMenu();
        } else {
            EasyPermissions.requestPermissions(this, "Need User permissions to proceed", RC_CALL, perms);
        }

    }

    private boolean initNetworkCheck()
    {
       String networkStatus = NetworkUtil.getConnectivityStatusString(getApplicationContext());
       Log.v(TAG, networkStatus);
       if(networkStatus.isEmpty()||networkStatus.equals("No internet is available")||networkStatus.equals("No Internet Connection")) {
            return false;
       }
       return true;
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
                        localVideoTrack.addSink(localSurfaceViewRenderer);
                    }
                    else
                    {
                        localVideoTrack.setEnabled(false);
                        localVideoTrack.removeSink(localSurfaceViewRenderer);
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
                    if(hmsClient!=null)
                        hmsClient.setBitrate(Integer.valueOf(DEFAULT_VIDEO_BITRATE)*1000);
                }

                if(key.equals("video_framerate")){
                    DEFAULT_VIDEO_FRAMERATE = hmsSharedPreferences.getString(key, "30");
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

        if(peer.getRoomId()==null)

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
        AudioManager audioManager =(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if(localAudioTrack!=null)
        {
            if(localAudioTrack.enabled())
            {
                isAudioEnabled = false;
                localAudioTrack.setEnabled(false);
                if(audioManager!=null) {
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                }
            }
            else
            {
                isAudioEnabled = true;
                localAudioTrack.setEnabled(true);
                if(audioManager!=null) {
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                }
            }
            if(audioManager!=null)
                audioManager.setSpeakerphoneOn(isAudioEnabled);
        }
    }



    private void initializeSurfaceViews() {

        if(HMSWebRTCEglUtils.getRootEglBaseContext()==null)
            HMSWebRTCEglUtils.getRootEglBase();

        localPeerTextView = (TextView) findViewById(R.id.firstpeer_textview);
        remoteTextViews[0] = (TextView) findViewById(R.id.secondpeer_textview);
        remoteTextViews[1] = (TextView) findViewById(R.id.thirdpeer_textview);
        remoteTextViews[2] = (TextView) findViewById(R.id.fourthpeer_textview);
        remoteTextViews[3] = (TextView) findViewById(R.id.fifthpeer_textview);
        remoteTextViews[4] = (TextView) findViewById(R.id.sixthpeer_textview);
        remoteTextViews[5] = (TextView) findViewById(R.id.seventhpeer_textview);
        remoteTextViews[6] = (TextView) findViewById(R.id.eighthpeer_textview);


        //Init all the remote views are free
        for(int i=0; i<TOTAL_REMOTE_PEERS; i++)
        {
            isRemoteCellFree[i] = true;
        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    //Setting local peer name
                    localPeerTextView.setText(username);

                    //Init view
                    localSurfaceViewRenderer = (SurfaceViewRenderer) findViewById(R.id.surface_view1);
                    remoteSurfaceViewRenderers[0] = (SurfaceViewRenderer) findViewById(R.id.surface_view2);
                    remoteSurfaceViewRenderers[1] = (SurfaceViewRenderer) findViewById(R.id.surface_view3);
                    remoteSurfaceViewRenderers[2] = (SurfaceViewRenderer) findViewById(R.id.surface_view4);
                    remoteSurfaceViewRenderers[3] = (SurfaceViewRenderer) findViewById(R.id.surface_view5);
                    remoteSurfaceViewRenderers[4] = (SurfaceViewRenderer) findViewById(R.id.surface_view6);
                    remoteSurfaceViewRenderers[5] = (SurfaceViewRenderer) findViewById(R.id.surface_view7);
                    remoteSurfaceViewRenderers[6] = (SurfaceViewRenderer) findViewById(R.id.surface_view8);

                    localSurfaceViewRenderer.init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                    localSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    localSurfaceViewRenderer.setEnableHardwareScaler(true);
                    localSurfaceViewRenderer.setMirror(true);


                    for(int i=0; i< TOTAL_REMOTE_PEERS; i++){
                        remoteSurfaceViewRenderers[i].init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                        remoteSurfaceViewRenderers[i].setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                        remoteSurfaceViewRenderers[i].setEnableHardwareScaler(true);
                    }

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
                if(localSurfaceViewRenderer ==null)
                {
                    initializeSurfaceViews();
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
                        localSurfaceViewRenderer.setVisibility(View.VISIBLE);
                        localVideoTrack.addSink(localSurfaceViewRenderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                if(!isPublished) {
                    hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSStreamRequestHandler() {
                        @Override
                        public void onSuccess(HMSPublishStream data) {
                            Log.v(TAG, "publish success "+data.getMid());
                            isPublished = true;
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

    public void handleReconnect()
    {
        Log.v(TAG, "handleReconnect");
        initHMSClient();
    }

    int getDelay(int n)  {
        int maxDelay = 300000; // 5 minutes
        int delay = (int)Math.pow(2.0, Double.valueOf(n)) * 1000;
        int jitter = (int)(Math.random() * (1000 - 1 + 1) + 1);
        return Math.min(delay + jitter, maxDelay);
    }



    public void disconnect()
    {
        try {
            HMSStream.stopCapturers();

        isJoined = false;
        isPublished = false;

        //Clearing all the views
        clearLocalView();
        clearRemoteViews();

        //Clean up the local streams
        if(localVideoTrack!=null)
            localVideoTrack.dispose();

        if(localAudioTrack!=null)
            localAudioTrack.dispose();

        if(localMediaStream!=null)
            localMediaStream = null;

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
        catch(Exception e){e.printStackTrace();}

    }


    void setTracks(MediaStream data, int position, String name)
    {

        if(data.videoTracks.size()>0) {
            remoteVideoTracks[position] = data.videoTracks.get(0);
            remoteVideoTracks[position].setEnabled(true);
        }
        if(data.audioTracks.size()>0)
        {
            remoteAudioTracks[position] = data.audioTracks.get(0);
            remoteAudioTracks[position].setEnabled(true);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(name!=null)
                {
                    if(data.videoTracks.size()>0 && data.audioTracks.size()>0)
                        remoteTextViews[position].setText(name);
                }

                if(position == 0)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view2);
                if(position == 1)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view3);
                if(position == 2)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view4);
                if(position == 3)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view5);
                if(position == 4)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view6);
                if(position == 5)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view7);
                if(position == 6)
                    remoteSurfaceViewRenderers[position] = (SurfaceViewRenderer) findViewById(R.id.surface_view8);

                remoteSurfaceViewRenderers[position].setVisibility(View.VISIBLE);

                if(data.videoTracks.size()>0) {
                    if (remoteSurfaceViewRenderers[position] == null) {
                        remoteSurfaceViewRenderers[position].init(HMSWebRTCEglUtils.getRootEglBaseContext(), null);
                        remoteSurfaceViewRenderers[position].setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                        remoteSurfaceViewRenderers[position - 1].setEnableHardwareScaler(true);
                    }
                    remoteSurfaceViewRenderers[position].setVisibility(View.VISIBLE);
                    remoteVideoTracks[position].addSink(remoteSurfaceViewRenderers[position]);
                }

            }
        });

    }


    @Override
    public void onConnect() {
        shouldReconnect = false;
        retryCount = 0;
        Log.v(TAG, "Connect success");
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
        shouldReconnect = true;
        isJoined = false;
        isPublished = false;
        //Clearing all the views
        clearLocalView();
        clearRemoteViews();

        //Clean up the local streams
        localMediaStream = null;
        localAudioTrack = null;
        localVideoTrack = null;

        if (shouldReconnect) {
            if (retryCount > maxRetryCount) {
                Log.v(TAG, "Still disconnected");
            }

            Log.v(TAG, "Connection retry:: delay:" + delay + " retry count: " + retryCount);
            delay = getDelay(retryCount);
            retryCount += 1;

            Handler reconnectionHandler = new Handler(Looper.getMainLooper());
            reconnectionHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleReconnect();
                }
            }, delay);
        }


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
                if(pos == -1)
                    Log.v(TAG, "No more UI space for additional users but you can hear the audio");
                else {
                    remoteUserIds[pos] = hmsStreamInfo.getUid();
                    isRemoteCellFree[pos] = false;

                    Log.v(TAG, "On subscribe success");
                    Log.v(TAG, "position: " + pos);
                    Log.v(TAG, "user id: " + hmsStreamInfo.getUid());

                    setTracks(data, pos, hmsStreamInfo.getUserName());
                    printAllCelldata();
                }


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
        for(int i = 0;i<TOTAL_REMOTE_PEERS;i++)
        {
            try {
                if (remoteUserIds[i]!=null && isRemoteCellFree[i].equals(hmsStreamInfo.getUid())) {
                    isRemoteCellFree[i] = true;
                    remoteUserIds[i] = null;

                    hmsClient.unsubscribe(hmsStreamInfo, hmsRoom, new HMSRequestHandler() {

                        @Override
                        public void onSuccess(String data) {
                            Log.v(TAG, "unsubscribed successfully");
                        }

                        @Override
                        public void onFailure(long error, String errorReason) {
                            Log.v(TAG, "unsubscribed failure");

                        }
                    });

                }
            }
            catch(Exception e)
            {
                Log.v(TAG, "Exception :"+e.getMessage());
            }
        }


        clearCellData();





    }

    @Override
    public void onBroadcast(HMSPayloadData hmsPayloadData) {

    }


    int getFreePosition()
    {
        //int pos = 0;
        for(int i=0; i<TOTAL_REMOTE_PEERS; i++)
        {
            Log.v(TAG, "printing free pos:"+i);
            if(isRemoteCellFree[i] == true)
                return i;
        }
        return -1;
    }


    void printAllCelldata()
    {
        for(int i=0;i<TOTAL_REMOTE_PEERS;i++)
        {
            Log.v(TAG, "Pos: "+i+" status: "+isRemoteCellFree[i]+"  uid:"+remoteUserIds[i]);
        }
    }

    void clearCellData()
    {
        printAllCelldata();
        runOnUiThread(new Runnable() {
        @Override
        public void run() {
            try {
                for (cell = 0; cell < TOTAL_REMOTE_PEERS; cell++) {
                    Log.v(TAG, "Loop"+ cell+" "+isRemoteCellFree[cell]);
                    if (!isRemoteCellFree[cell]) {
                        if (remoteSurfaceViewRenderers[cell] != null){
                            remoteSurfaceViewRenderers[cell].clearImage();
                        }

                        Log.v(TAG, "Clear cell data "+ cell+" "+isRemoteCellFree[cell]);
                        if (remoteSurfaceViewRenderers[cell] != null)
                            remoteSurfaceViewRenderers[cell].setVisibility(View.INVISIBLE);
                        remoteSurfaceViewRenderers[cell] = null;
                        remoteUserIds[cell] = null;
                        isRemoteCellFree[cell] = true;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            }
        });

    }


    void clearLocalView()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(localSurfaceViewRenderer!=null) {
                    localSurfaceViewRenderer.setVisibility(View.INVISIBLE);
                }
                if(localSurfaceViewRenderer!=null)
                    localSurfaceViewRenderer.clearImage();
                localSurfaceViewRenderer = null;

            }
        });

    }




    void clearRemoteViews()
    {
        printAllCelldata();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (cell = 0; cell < TOTAL_REMOTE_PEERS; cell++) {
                        if (!isRemoteCellFree[cell]) {
                            Log.v(TAG, "Clear remote views: "+cell+"  "+isRemoteCellFree[cell]);
                            if(remoteSurfaceViewRenderers[cell]!=null) {
                                remoteSurfaceViewRenderers[cell].setVisibility(View.INVISIBLE);
                            }
                            remoteUserIds[cell] = null;
                            isRemoteCellFree[cell] = true;
                            if (remoteSurfaceViewRenderers[cell] != null){
                                remoteSurfaceViewRenderers[cell].clearImage();
                            }
                            remoteSurfaceViewRenderers[cell] = null;
                        }
                    }
                }
                catch (Exception e){e.printStackTrace();}
            }
        });


    }


}