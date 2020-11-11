package live.hms.video;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.brytecam.lib.HMSClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LaunchActivity extends AppCompatActivity {

    private Button connectButton;
    private EditText roomIdEditText, userIdEditText, serverEditText, bitrateEditText;
    private String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfaWQiOiI1Zjc0NTU4YTdlZjE3MjNkZWQ2MGExMjgiLCJhY2Nlc3Nfa2V5IjoiNWY3NDU1OGE3ZWYxNzIzZGVkNjBhMTI5Iiwicm9vbV9pZCI6ImFuZHJvaWR0ZXN0IiwicGVlcl9pZCI6ImExZGRmNzZiLTMwZWItNDJiYS1hMjM2LTk2N2IyOTQ0ZmM2YyIsImlzcyI6IjVmNzQ1NThhN2VmMTcyM2RlZDYwYTEyNyIsImp0aSI6IjIwN2FkNWZiLTc4ODEtNDQ1YS1hN2E4LThhY2ExNWNmMmRjYyIsImV4cCI6MTYwNDI4MTM5NCwiaWF0IjoxNjA0MTk0OTk0LCJuYmYiOjE2MDQxOTQ5OTR9.L8KI0-1C52Vsyh0AHgxH8-S1Kzb5TxK7KeSlYBApJlE";
    private String endPointURL = "wss://conf.brytecam.com/ws";
    private HMSClient hmsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        roomIdEditText = (EditText) findViewById(R.id.editTextRoom);
        userIdEditText = (EditText) findViewById(R.id.editTextUserName);
        serverEditText = (EditText) findViewById(R.id.editTextServer);
        bitrateEditText = (EditText) findViewById(R.id.editTextBitrate);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWorkingDialog();
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        String newToken = getNewToken();
                        if(serverEditText.getText().toString().contains("conf.brytecam"))
                        {
                            Intent callIntent = new Intent(LaunchActivity.this, VideoActivity.class);
                            callIntent.putExtra("server", serverEditText.getText().toString());
                            callIntent.putExtra("room", roomIdEditText.getText().toString().length()==0?"android":roomIdEditText.getText().toString());
                            callIntent.putExtra("user", userIdEditText.getText().toString().length()==0?"JohnDoe":userIdEditText.getText().toString());
                            callIntent.putExtra("auth_token", token);
                            callIntent.putExtra("bitrate", bitrateEditText.getText().toString());
                            callIntent.putExtra("env", "conf");
                            startActivity(callIntent);
                        }
                        else
                        {
                           // token = newToken;
                            Intent callIntent = new Intent(LaunchActivity.this, VideoActivity.class);
                            callIntent.putExtra("server", serverEditText.getText().toString());
                            callIntent.putExtra("room", roomIdEditText.getText().toString().length()==0?"android":roomIdEditText.getText().toString());
                            callIntent.putExtra("user", userIdEditText.getText().toString().length()==0?"JohnDoe":userIdEditText.getText().toString());
                            callIntent.putExtra("auth_token", newToken);
                            callIntent.putExtra("bitrate", bitrateEditText.getText().toString());
                            callIntent.putExtra("env", "others");
                            startActivity(callIntent);
                        }
                        removeWorkingDialog();
                    }
                }, 6000);
               }
        });
    }

    String getNewToken()
    {
        //Write your own token generation logic here
        return token;
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