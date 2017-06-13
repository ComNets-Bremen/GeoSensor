package de.eiketrumann.geosensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
/**
 * The main activity is (apart from the first start when this activity opens a FirstStartActivity)
 * the entry point to the application. It contains a scrollable list of the data records saved in
 * the database. For performance reasons the activity uses a RecyclerView that refills old UI
 * elements with new contents (view holder design pattern).
 *
 * @author Eike Trumann
 */
public class MainActivity extends GeoSensorActivity{

    /** The DataAdapter is used to format the data from the database */
    private DataAdapter dataAdapter;

    /** The BroadcastReceiver is used to handle changes in the underlying data*/
    private BroadcastReceiver broadcastReceiver = new MainActivityBroadcastReceiver();

    /**
     * The onCreate lifecycle method is used for a lot of different purposes as it is one of the
     * first methods called by the framework on application startup:
     * - Prepare the toolbar used in this activity
     * - Instantiate the RecyclerView containing the main list
     * - Prepare the adapter that transfers the data into the views
     * - Have the runtime permissions (location and bluetooth) checked
     * - Start the background service used to sustain the bluetooth connection
     *
     * @param savedInstanceState Bundle stored in onDestroy
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Set the Toolbar as action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);

        // Prepare the list itself
        final RecyclerView mainList = (RecyclerView) findViewById(R.id.MainList);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mainList.setLayoutManager(layoutManager);

        // Some design element
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mainList.getContext(),
                DividerItemDecoration.VERTICAL);
        mainList.addItemDecoration(dividerItemDecoration);

        // Prepare the adapter for the data in the main list
        DataLab dataLab = new DataLab(this);
        dataAdapter = new DataAdapter(this, dataLab);
        mainList.setAdapter(dataAdapter);

        // Check for necessary runtime permissions
        PermissionsManager.getLocationPermission(this);

        // Start the bluetooth service
        Intent bluetoothServiceIntent = new Intent(this, BluetoothReceiverService.class);
        startService(bluetoothServiceIntent);
    }

    /**
     * Refresh the data (something might have changes while the activity was in the background)
     * Register the broadcast receiver to be notified when something changes.
     */
    @Override
    public void onResume(){
        super.onResume();
        dataAdapter.refreshList();
        dataAdapter.notifyDataSetChanged();

        IntentFilter intentFilter = new IntentFilter(
                BluetoothReceiverService.INTENT_DATA_RECORD_RECEIVED);
        intentFilter.addAction(FilterState.FILTER_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                intentFilter);
    }

    /**
     * Unregister the BroadcastReceiver as the list does not need to be refreshed while in
     * background.
     */
    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    /**
     * Most of the work is done in super, but this class needs to set the visibility for the icons
     * it needs.
     * @param menu Given by the framework.
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_sort).setVisible(true);
        menu.findItem(R.id.action_filter).setVisible(true);
        menu.findItem(R.id.action_list).setVisible(false);
        return true;
    }


    /**
     * Handles the result of runtime permission requests. The requests are made by the
     * PermissionsManager class, but the results are given to the Activity context and need to be
     * passed through to the PermissionsManager
     * @param requestCode An arbitrary (but unique) request code associated to a specific permission
     * @param permissions String that gives the names of the permissions handled in this request
     * @param grantResults Indicates if the permission has been granted or denied
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        PermissionsManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    /**
     * This BroadcastReceiver handles changes in the data set like new elements, a new filter or a
     * new sort order by asking the adapter to refresh everything.
     */
    private class MainActivityBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            dataAdapter.refreshList();
            dataAdapter.notifyDataSetChanged();
        }
    }

}
