package live.hms.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;


public class NetworkReceiver extends BroadcastReceiver {

    String status = "";
    static NetworkInterface networkInterface;

    public NetworkReceiver()
    {

    }

    public NetworkReceiver(NetworkInterface networkInterface)
    {
        this.networkInterface = networkInterface;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        status = NetworkUtil.getConnectivityStatusString(context);
        Log.d("network",status);
        if(status.isEmpty()||status.equals("No internet is available")||status.equals("No Internet Connection")) {
            status="No Internet Connection";
        }
        if(this.networkInterface!=null)
            this.networkInterface.onNetworkStatusChange(status);
        //Toast.makeText(context, status, Toast.LENGTH_LONG).show();
    }

}