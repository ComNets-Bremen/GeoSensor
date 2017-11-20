package de.uni_bremen.comnets.geosensor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * The SettingsFragment inflates and initialises the application settings
 * Created by Eike Trumann on 15.02.2017.
 */
public class SettingsFragment extends PreferenceFragmentCompat  {
    // The request codes are arbitrary, but they must be distinct
    private final int REQUEST_ENABLE_BLUETOOTH = 1337;
    private final int REQUEST_CONNECT_DEVICE = 4711;

    /**
     * The onCreatePreferences method initialises the preferences
     * @param b unused
     * @param s unused
     */
    @Override
    public void onCreatePreferences(Bundle b,String s){
        addPreferencesFromResource(R.xml.fragment_preferences);
        Preference bluetoothSelector = findPreference(getString(R.string.pref_key_select_bluetooth));
        bluetoothSelector.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if(!bluetoothAdapter.isEnabled()){
                    // This code shows an alert asking the user to enable Bluetooth if disabled
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
                    return true;
                } else {
                    // This shows the DeviceListActivity which is used to select a device
                    Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    return true;
                }
            }
        });

        // This part handles deleting old messages
        // The user is asked to input a threshold date
        // DataRecords older than the threshold are deleted from the database
        Preference deleteOld = findPreference(getString(R.string.pref_key_delete_old));
        deleteOld.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DateTimePicker(getActivity(), new DateTimePicker.OnDateTimePickedListener() {
                    @Override
                    public void onDateTimePicked(final Date date) {
                        AlertDialog.Builder deleteMessageBuilder = new AlertDialog.Builder(getContext(),R.style.DialogTheme);
                        deleteMessageBuilder.setTitle(getString(R.string.delete_old));
                        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
                        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
                        String message = String.format(getString(R.string.delete_old_message),dateFormat.format(date)+" "+timeFormat.format(date));
                        deleteMessageBuilder.setMessage(message);

                        deleteMessageBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DataLab dl = new DataLab(getContext());
                                dl.deleteOldDataRecords(date);
                            }
                        });

                        deleteMessageBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){}
                        });

                        AlertDialog deleteMessage = deleteMessageBuilder.create();
                        deleteMessage.setCanceledOnTouchOutside(true);
                        deleteMessage.show();
                    }
                });
                return true;
            }
        });

        // This preference forces a database rebuild dropping all contents
        Preference dropDatabase = findPreference(getString(R.string.pref_key_drop_database));
        dropDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder deleteMessageBuilder = new AlertDialog.Builder(getContext(),R.style.DialogTheme);
                deleteMessageBuilder.setTitle(getString(R.string.drop_database));
                deleteMessageBuilder.setMessage(getString(R.string.drop_database_message));

                deleteMessageBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        DataLab dl = new DataLab(getContext());
                        dl.rebuildDatabase();
                    }
                });

                deleteMessageBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which){}
                });

                AlertDialog deleteMessage = deleteMessageBuilder.create();
                deleteMessage.setCanceledOnTouchOutside(true);
                deleteMessage.show();

                return true;
            }
        });

        // This preference sends an unsolicited NAK to the transducer
        Preference requestResend = findPreference(getString(R.string.pref_key_request_resend));
        requestResend.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getActivity(), getString(R.string.resend_requested),
                        Toast.LENGTH_SHORT).show();
                Intent acquireData = new Intent(BluetoothReceiverService.INTENT_RESEND_MEASURE_DATA);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(acquireData);
                return true;
            }
        });
    }

    /**
     * Apart from the super method, this method associated some preferences to the permissions
     * necessary to use them. We do that in onResume to handle revoked preferences as late as
     * possible. It is however not guaranteed the app will word smoothly if the user revokes
     * runtime permissions.
     */
    @Override
    public void onResume(){
        super.onResume();

        preparePermissions(R.string.pref_key_location_switch,"ACCESS_FINE_LOCATION");
        preparePermissions(R.string.pref_key_bluetooth_basic,"BLUETOOTH");
        preparePermissions(R.string.pref_key_bluetooth_basic,"BLUETOOTH_ADMIN");
    }

    /**
     * Some preferences do have android runtime permissions associated to them.
     * If a permission is revoked for some reason, the associated setting is disabled,
     * if the setting is enabled again the user is asked to re-grant the permission.
     *
     * This method should be called for each setting requiring a permission in the onResume method.
     *
     * @param switchResID The ID of the switch a permission should be associated to
     * @param permission The (string) name of the permission to associate.
     */
    public void preparePermissions(int switchResID, final String permission){
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference)
                getPreferenceManager().findPreference(getString(switchResID));
        if (!PermissionsManager.isPermissionGranted(getContext(),permission)){
            checkBoxPreference.setChecked(false);
        }

        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue){
                    PermissionsManager.getPermission(getActivity(),permission);
                }
                return true;
            }
        });
    }

    /**
     * The onActivityResult method handles the results of Intents that are started for results
     *
     * This class handles two different activity results:
     * - The activation of bluetooth has been requested
     * - The Bluetooth device to connect to has been selected
     * @param requestCode one of the Codes for the requests this class makes
     * @param resultCode The result of the request (e.g. OK or cancelled)
     * @param data unused
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    pref.edit().putString(getString(R.string.pref_key_select_bluetooth),address).apply();

                    // Test if the device is already bonded
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice device = adapter.getRemoteDevice(address);
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        try {
                            // Using reflection seems ugly, but seems to be the usual way to do this
                            Method method = device.getClass().getMethod("createBond", (Class[]) null);
                            method.invoke(device, (Object[]) null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

            case REQUEST_ENABLE_BLUETOOTH:
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
        }
    }
}
