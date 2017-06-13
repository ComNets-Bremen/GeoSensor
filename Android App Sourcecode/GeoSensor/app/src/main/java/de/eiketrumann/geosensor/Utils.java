package de.eiketrumann.geosensor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Class for static utility functions that don't belong anywhere else
 * Created by Eike on 28.02.2017.
 */

class Utils {

    // Method taken from Android Developer Examples
    // https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
    static boolean hasInternet(Context context){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }
}
