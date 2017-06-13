package de.eiketrumann.geosensor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Map;

/**
 * This class is a static utility class used to handle runtime permissions in Android Versions from
 * 6.0 Marshmallow.
 *
 * Most notably, this class is important for the location permission as this needs to be actively
 * accepted by the user. The other permissions are granted automatically in mainstream versions of
 * Android.
 *
 * Created by Eike Trumann on 21.02.17.
 */

class PermissionsManager{

    private final static int ACCESS_FINE_LOCATION = 1;

    private final static Map<String,Integer> myPermissionNumbers = new ArrayMap<>();
    static{
        myPermissionNumbers.put("ACCESS_FINE_LOCATION",1);
        myPermissionNumbers.put("BLUETOOTH",2);
        myPermissionNumbers.put("BLUETOOTH_ADMIN",3);
        myPermissionNumbers.put("ACCESS_COARSE_LOCATION",4);
        myPermissionNumbers.put("WRITE_EXTERNAL_STORAGE",5);
    }

    private final static Map<String,String> myPermissionNames = new ArrayMap<>();
    static{
        myPermissionNames.put("ACCESS_FINE_LOCATION", Manifest.permission.ACCESS_FINE_LOCATION);
        myPermissionNames.put("BLUETOOTH", Manifest.permission.BLUETOOTH);
        myPermissionNames.put("BLUETOOTH_ADMIN", Manifest.permission.BLUETOOTH_ADMIN);
        myPermissionNames.put("ACCESS_COARSE_LOCATION",Manifest.permission.ACCESS_COARSE_LOCATION);
        myPermissionNames.put("WRITE_EXTERNAL_STORAGE",Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * This method tries to get the fine location permission was granted
     * @param activity The activity triggering this check.
     */
    static void getLocationPermission(Activity activity){
        Context context = activity.getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        if(pref.getBoolean(context.getString(R.string.pref_key_location_switch),false)) {
            getPermission(activity,"ACCESS_FINE_LOCATION");
            getPermission(activity,"ACCESS_COARSE_LOCATION");
        }
    }

    /**
     * This method tries to get the permission given in the String.
     * As this might show a dialog to the user we can not block to wait for the result.
     * @param activity The requesting Activity
     * @param permission The name of the permission (only among those specified in this class)
     */
    static void getPermission(Activity activity, String permission){
        Context context = activity.getApplicationContext();
        if (!isPermissionGranted(context,permission)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{myPermissionNames.get(permission)},
                    myPermissionNumbers.get(permission));
        }
    }

    /**
     * @param context an arbitrary context
     * @param name The name of the permission as a string (only among those specified in this class)
     * @return True if the permission is granted
     */
    static boolean isPermissionGranted(Context context, String name){
        int permission = ContextCompat.checkSelfPermission(context, myPermissionNames.get(name));
        return (permission == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * This method handles the results if a permission is actively granted or denied by the user.
     * If the location permission is denied, the location recording setting will be disabled.
     * @param activity The requesting Activity
     * @param requestCode The request code as specified by this class.
     * @param permissions The permissions granted or denied in this request
     * @param grantResults The result: granted or denied.
     */
    static void onRequestPermissionsResult(Activity activity, int requestCode,
                                                  @NonNull String permissions[], @NonNull int[] grantResults) {
        // we switch by the request code as we know what we requested.
        switch (requestCode) {
            case ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, activity.getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, activity.getString(R.string.location_disabled), Toast.LENGTH_LONG).show();
                    Context context = activity.getApplicationContext();
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                    pref.edit().putBoolean(context.getString(R.string.pref_key_location_switch),false).apply();
                }
                break;
            }
        }
    }
}
