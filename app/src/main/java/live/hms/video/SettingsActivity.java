package live.hms.video;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;


import com.brytecam.lib.webrtc.HMSStream;

import live.hms.video.R;

public class SettingsActivity extends AppCompatActivity {
    String TAG = "HMSSettingsActivity";
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
        EditTextPreference frameRatePreference;


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

            if(fromScreen.equals("launchscreen"))
            {
                publishAudioSwitch.setVisible(true);
                publishVideoSwitch.setVisible(true);
                codecPreference.setVisible(false);
                resolutionPreference.setVisible(false);
                frameRatePreference.setVisible(true);
            }
            else
            {
                Toast.makeText(getContext(), "All settings config won't be visible during the call", Toast.LENGTH_LONG).show();
                publishAudioSwitch.setVisible(false);
                publishVideoSwitch.setVisible(false);
                codecPreference.setVisible(false);
                resolutionPreference.setVisible(false);
                frameRatePreference.setVisible(true);
            }

        }
    }





}