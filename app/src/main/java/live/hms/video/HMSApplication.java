package live.hms.video;

import android.app.Application;

import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

public class HMSApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //Instabug SDK for bug reporting
        new Instabug.Builder(this, getString(R.string.instabug_token))
                .setInvocationEvents(
                        InstabugInvocationEvent.SHAKE,
                        InstabugInvocationEvent.FLOATING_BUTTON)
                .build();
    }
}
