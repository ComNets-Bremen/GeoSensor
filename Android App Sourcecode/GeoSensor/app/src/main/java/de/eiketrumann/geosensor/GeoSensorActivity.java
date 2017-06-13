package de.eiketrumann.geosensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This Activity is the parent class of the Activities used in this app.
 * It is based on AppCompatActivity in order to support material design on older API levels.
 */
public abstract class GeoSensorActivity extends AppCompatActivity {
    // The toolbar uses as action bar
    protected Menu menu;
    // A broadcast receiver used to follow the filter state in order to color the symbol
    private BroadcastReceiver broadcastReceiver = new GeoSensorActivityBroadcastReceiver();

    /**
     * Register the BroadcastReceiver for the filter state.
     */
    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(FilterState.FILTER_CHANGED));
    }

    /**
     * Unregister the Broadcast receiver
     */
    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }


    /**
     * Create the toolbar.
     * Sets the acquireData Button visible or invisible according to the setting
     * @param menu The toolbar
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        menu.findItem(R.id.action_acquire_data).setVisible(sharedPreferences.getBoolean(
                getString(R.string.pref_key_show_acquire_data),true));

        this.menu = menu;
        setFilterColor();
        return true;
    }

    /**
     * Handles the generic behaviour when an options item has been selected
     * Opens the Activity for the list view, map view and settings.
     * Starts the export and data acquisition process.
     * Starts the filter and sorting chooser.
     * @param item The item selected
     * @return true if the item has been handled
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            case R.id.action_list:
                Intent listIntent = new Intent(this, MainActivity.class);
                startActivity(listIntent);
                return true;

            case R.id.action_maps:
                Intent intent = new Intent(this, MainMapsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_filter:
                FilterUtils.filterChooser(this);
                return true;

            case R.id.action_sort:
                FilterUtils.sortChooser(this);
                return true;

            case R.id.action_export: {
                DataLab dataLab = new DataLab(this);
                CSVWriter csvWriter = new CSVWriter(this, dataLab.getFilteredIDs());
                csvWriter.export();
                return true;
            }

            case android.R.id.home:
                finish();
                return true;

            case R.id.action_acquire_data: {
                Intent acquireData = new Intent(BluetoothReceiverService.INTENT_ACQUIRE_MEASURE_DATA);
                LocalBroadcastManager.getInstance(this).sendBroadcast(acquireData);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set the color of the filter icon: white if there is no filter, accent color if there is one
     */
    private void setFilterColor(){
        MenuItem filterSymbol = menu.findItem(R.id.action_filter);
        if (FilterState.isFilterSet()){
            filterSymbol.setIcon(R.drawable.ic_filter_list_pink_24dp);
        } else {
            filterSymbol.setIcon(R.drawable.ic_filter_list_white_24dp);
        }
    }

    /**
     * This BroadCast receiver listens for changes in the filter to recolor the filter icon
     */
    private class GeoSensorActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setFilterColor();
        }
    }
}
