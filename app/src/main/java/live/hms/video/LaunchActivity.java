package live.hms.video;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.brytecam.lib.webrtc.HMSStream;

import org.json.JSONException;
import org.json.JSONObject;

import live.hms.video.R;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by Karthikeyan NG
 * Copyright © 100ms.live. All rights reserved.
 * Initial login screen with user details
 */

public class LaunchActivity extends AppCompatActivity {
    private String TAG = "HMSMainActivity";
    private Button connectButton;
    private EditText roomIdEditText, userIdEditText, serverEditText;
    private String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2Nlc3Nfa2V5IjoiNWY5ZWRjNmJkMjM4MjE1YWVjNzcwMGUyIiwiYXBwX2lkIjoiNWY5ZWRjNmJkMjM4MjE1YWVjNzcwMGUxIiwicm9vbV9pZCI6ImFuZHJvaWQiLCJwZWVyX2lkIjoiSm9obkRvZSIsImlhdCI6MTYwNDYzOTY2OSwiZXhwIjoxNjA0NzI2MDY5LCJpc3MiOiI1ZjllZGM2YmQyMzgyMTVhZWM3NzAwZGYiLCJqdGkiOiIyZDQyODgzYi05NjM0LTRjYzEtOTc5ZC04Zjc4MGVjMGZlMmEifQ.DG-aSav45Kt4DONn6617qPuPx9TMwsyvGjx_QPbwS04";
    private String newToken ="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        roomIdEditText = (EditText) findViewById(R.id.editTextRoom);
        userIdEditText = (EditText) findViewById(R.id.editTextUserName);
        serverEditText = (EditText) findViewById(R.id.editTextServer);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(serverEditText.getText().toString().contains("conf.brytecam"))
                {
                    Intent callIntent = new Intent(LaunchActivity.this, VideoActivity.class);
                    callIntent.putExtra("server", serverEditText.getText().toString());
                    callIntent.putExtra("room", roomIdEditText.getText().toString());
                    callIntent.putExtra("user", userIdEditText.getText().toString().length()==0?"JohnDoe":userIdEditText.getText().toString());
                    callIntent.putExtra("auth_token", token);
                    callIntent.putExtra("env", "conf");
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(callIntent);

                }
                else {
                    //showWorkingDialog();
                    getNewToken();
                }
            }
        });
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
            Intent callIntent = new Intent(LaunchActivity.this, SettingsActivity.class);
            callIntent.putExtra("from", "launchscreen");
            startActivity(callIntent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    void getNewToken()
    {
        String resStr = "";
        // create your json here
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("room_id", roomIdEditText.getText().toString());
            jsonObject.put("peer_id", userIdEditText.getText().toString());
            jsonObject.put("env", serverEditText.getText().toString().split("\\.")[0].replace("wss://", "") );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v("HMSClient", jsonObject.toString());

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        // put your json here
        RequestBody body = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url("https://ms-internal-apps-token-service-klly5pwlrz1c.runkit.sh/")
                .post(body)
                .build();

        Response response = null;
        JSONObject jsonObj = null;
        String  val = "";
        try {
            response = client.newCall(request).execute();
            resStr = response.body().string();
            Log.v("HMSClient", "token: "+resStr);

            jsonObj = new JSONObject(resStr);
            //Log.v("token", jsonObj.getString("token"));
            val= jsonObj.getString("token");

            // removeWorkingDialog();

            Intent callIntent = new Intent(LaunchActivity.this, VideoActivity.class);
            callIntent.putExtra("server", serverEditText.getText().toString());
            callIntent.putExtra("room", roomIdEditText.getText().toString());
            callIntent.putExtra("user", userIdEditText.getText().toString().length() == 0 ? "JohnDoe" : userIdEditText.getText().toString());
            callIntent.putExtra("auth_token", val);
            callIntent.putExtra("env", "others");
            callIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(callIntent);



        } catch (Exception e) {
            // removeWorkingDialog();
            Toast.makeText(getApplicationContext(), "Error in receiving token", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        // return val;
    }


    private ProgressDialog working_dialog;

    private void showWorkingDialog() {
        working_dialog = ProgressDialog.show(LaunchActivity.this, "","Working please wait...", true);
    }

    private void removeWorkingDialog() {
        if (working_dialog != null) {
            working_dialog.dismiss();
            working_dialog = null;
        }
    }


}