package live.hms.video;

import android.app.Application;

import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

public class HMSApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new Instabug.Builder(this, "83d22f7c2d84a1329e3384b53cffab30")
                .setInvocationEvents(
                        InstabugInvocationEvent.SHAKE,
                        InstabugInvocationEvent.FLOATING_BUTTON)
                .build();
    }
}
