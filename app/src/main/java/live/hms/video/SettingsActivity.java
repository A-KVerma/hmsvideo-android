package live.hms.video;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;


import com.instabug.library.InstabugTrackingDelegate;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerator;

import java.util.ArrayList;
import java.util.List;

import live.hms.video.R;

public class SettingsActivity extends AppCompatActivity {
    private static String TAG = "HMSSettingsActivity";
    String fromScreen = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if(getIntent()!=null) {
            fromScreen = getIntent().getStringExtra("from");
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment(fromScreen))
                .commit();


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        InstabugTrackingDelegate.notifyActivityGotTouchEvent(ev, this);
        return super.dispatchTouchEvent(ev);
    }


    public static List<CameraEnumerationAndroid.CaptureFormat> getDeviceList(Context context) {
        Log.v(TAG, "getDeviceList method");
        List<CameraEnumerationAndroid.CaptureFormat> formats = new ArrayList<CameraEnumerationAndroid.CaptureFormat>();
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED){
            CameraEnumerator enumerator;
            Log.v(TAG, "Have camera permission");
            if (Camera2Enumerator.isSupported(context)) {
                enumerator = new Camera2Enumerator(context);
            } else {
                enumerator = new Camera1Enumerator(false);
            }
            String[] deviceInfos = enumerator.getDeviceNames();
            for (int i = 0; i < deviceInfos.length; i++) {
                if (enumerator.isFrontFacing(deviceInfos[i])) {
                    formats = enumerator.getSupportedFormats(deviceInfos[i]);
                    for(CameraEnumerationAndroid.CaptureFormat myformat : formats)
                    {
                        Log.v(TAG, "Width: "+myformat.width+" Height: "+myformat.height+" FPS: "+myformat.framerate.max/1000);
                    }
                }
            }
            Log.v(TAG, "Device screen formats size: "+formats.get(0).width);
        }

        return formats;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        String TAG = "HMSSettingsActivity";
        SwitchPreferenceCompat publishVideoSwitch;
        SwitchPreferenceCompat publishAudioSwitch;
        ListPreference codecPreference;
        ListPreference resolutionPreference;
        static EditTextPreference frameRatePreference;
        EditTextPreference bitRatePreference;
        List<CameraEnumerationAndroid.CaptureFormat> deviceFormats = new ArrayList<CameraEnumerationAndroid.CaptureFormat>();

        String fromScreen;
        SettingsFragment(String from)
        {
            this.fromScreen = from;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            publishAudioSwitch = (SwitchPreferenceCompat) findPreference("publish_audio");
            publishVideoSwitch = (SwitchPreferenceCompat) findPreference("publish_video");
            codecPreference = (ListPreference) findPreference("codec");
            resolutionPreference = (ListPreference) findPreference("resolution");
            frameRatePreference = (EditTextPreference) findPreference("video_framerate");
            bitRatePreference = (EditTextPreference) findPreference("video_bitrate");

            deviceFormats= getDeviceList(getContext());

            if(deviceFormats!=null && deviceFormats.size() > 0)
                setListPreferenceData(deviceFormats, resolutionPreference);
            else
                setListPreferenceData(null, resolutionPreference);



            if(fromScreen.equals("launchscreen"))
            {
                publishAudioSwitch.setVisible(true);
                publishVideoSwitch.setVisible(true);
                codecPreference.setVisible(false);
                resolutionPreference.setVisible(true);
                frameRatePreference.setVisible(false);
                bitRatePreference.setVisible(true);
            }
            else
            {
                //Toast.makeText(getContext(), "All settings config won't be visible during the call", Toast.LENGTH_LONG).show();
                publishAudioSwitch.setVisible(false);
                publishVideoSwitch.setVisible(false);
                codecPreference.setVisible(false);
                resolutionPreference.setVisible(true);
                frameRatePreference.setVisible(false);
                bitRatePreference.setVisible(true);
            }

        }


        protected static void setListPreferenceData(List<CameraEnumerationAndroid.CaptureFormat> formats, ListPreference lp) {
            if(formats!=null && formats.size()>0  && lp!= null)
            {
                List<String> resEntries = new ArrayList<String>();

                for(int i=0; i<formats.size(); i++)
                {
                    Log.v("Supported formats", "Width: " + formats.get(i).width + "x" + formats.get(i).height + "@" + formats.get(i).framerate.max/1000);
                    int width = formats.get(i).width;
                    int height = formats.get(i).height;
                    int frame = formats.get(i).framerate.max/1000;
                    resEntries.add( width + "x" + height + "@" + frame);
                }
                if(resEntries.size()>0)
                {
                    CharSequence[] entries = resEntries.toArray(new CharSequence[resEntries.size()]);
                    CharSequence[] entryValues = resEntries.toArray(new CharSequence[resEntries.size()]);
                    lp.setEntries(entries);
                    lp.setEntryValues(entryValues);
                }
            }
            if(formats==null)
            {
                CharSequence[] entries = {"3840 x 2160", "1920 x 1080", "1280 x 720", "640 x 480", "320 x 240", "160 x 120"};
                CharSequence[] entryValues = {"3840 x 2160", "1920 x 1080", "1280 x 720", "640 x 480", "320 x 240", "160 x 120"};
                lp.setEntries(entries);
                lp.setDefaultValue("640 x 480");
                lp.setEntryValues(entryValues);
            }
        }



    }




}