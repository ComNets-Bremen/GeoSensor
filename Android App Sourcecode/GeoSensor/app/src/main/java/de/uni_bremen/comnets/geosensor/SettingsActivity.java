package de.uni_bremen.comnets.geosensor;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

/**
 * This Activity basically works as a container for the SettingsFragment.
 * It however contains an onPreferenceChangedListener that handles some
 * preference changes
 *
 * Created by Eike Trumann on 15.02.2017.
 */

public class SettingsActivity extends GeoSensorActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener{

    /**
     * Sets the toolbar and the content view
     * @param savedInstanceState Android lifecycle Bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);
    }

    /**
     * Handles the toolbar creation.
     * Inherits most functionality from super.
     * The filter and settings options are disabled as they are pointless here.
     * @param menu The toolbar
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_filter).setVisible(false);
        return true;
    }

    /**
     * Registers an onPreferenceChanged listener to handle user input.
     */
    @Override
    public void onResume(){
        super.onResume();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);
    }


    /**
     * Unregister the onPreferenceChanged listener as no user input occurs while paused
     */
    @Override
    public void onPause(){
        super.onPause();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.unregisterOnSharedPreferenceChangeListener(this);

    }

    /**
     * Handles some of the preference changes.
     * Starts the BluetoothReceiverService if it hasn't been active before.
     * Warns the user if they want to change both csv separator and decimal mark to comma
     * @param sharedPreferences The applications shared preferences
     * @param key The preference that was changed
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (getString(R.string.pref_key_bluetooth_basic).equals(key)){
            boolean state = sharedPreferences.getBoolean(key,false);
            if(state) {
                Intent bluetoothServiceIntent = new Intent(this, BluetoothReceiverService.class);
                startService(bluetoothServiceIntent);
            }
            return;
        }

        if (getString(R.string.pref_key_csv_separator).equals(key)
                || getString(R.string.pref_key_decimal_mark).equals(key)){
            String csvSeparator = sharedPreferences.getString(getString(R.string.pref_key_csv_separator),",");
            String decimalMark = sharedPreferences.getString(getString(R.string.pref_key_decimal_mark),".");
            if(csvSeparator.equals(decimalMark)) {
                AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
                alertDialog.setTitle(getString(android.R.string.dialog_alert_title));
                alertDialog.setMessage(getString(R.string.csv_separator_decimal_mark_warning_message));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
    }

}
