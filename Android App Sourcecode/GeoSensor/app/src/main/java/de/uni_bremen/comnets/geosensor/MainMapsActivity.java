package de.uni_bremen.comnets.geosensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Locale;

/**
 * This activity shows markers for the measurements made on a google map.
 *
 * @author Eike Trumann
 */
public class MainMapsActivity extends GeoSensorActivity implements OnMapReadyCallback {

    /** The GoogleMap used to show the data*/
    private GoogleMap mMap;
    /** DataLab used to fetch the data from the database */
    DataLab dataLab;
    /** This map maps the markers on the map to the database IDS of the DataRecords */
    HashMap<Marker,Long> markerIDs = new HashMap<>();
    /** The BroadcastReceiver is used to get information about changes in the data set */
    private BroadcastReceiver broadcastReceiver = new MainMapsActivityBroadcastReceiver();

    /**
     * In onCreate, the toolbar is set and the map is requested asynchronously
     * @param savedInstanceState Android lifecycle Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_maps);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);

        dataLab = new DataLab(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * In onResume, the BroadcastReceiver is registered and the map is refreshed in case it has
     * been drawn before.
     */
    @Override
    public void onResume(){
        super.onResume();
        if(mMap != null){
            mMap.clear();
            onMapReady(mMap);
        }

        IntentFilter intentFilter = new IntentFilter(("dataRecordReceived"));
        intentFilter.addAction(FilterState.FILTER_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                intentFilter);
    }

    /**
     * In onPause, the BroadcastReceiver is unregistered
     */
    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    /**
     * This sets up the toolbar. We don't need a self-reference, but we need the filter here
     * @param menu the Toolbar
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_maps).setVisible(false);
        menu.findItem(R.id.action_filter).setVisible(true);
        return true;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLngBounds latLngBounds = putMarkers();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds,
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels,
                (int) (40 * getResources().getDisplayMetrics().density + 0.5f));
        googleMap.moveCamera(cameraUpdate);

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(MainMapsActivity.this,DetailsActivity.class);
                intent.putExtra("id",markerIDs.get(marker).longValue());
                startActivity(intent);
            }
        });
    }

    /**
     * This method reads the DataRecords from the database, adds a marker to the map for each
     * DataRecord containing location information. DataRecords without location information are not
     * shown on this map. A BoundsBuilder is used to determine the map zoom level and outer bounds.
     * @return A LatLngBounds object containing the bounds the map needs to be zoomed to in order to
     * show all markers put into it. If there are no markers, the University of Bremen will be shown.
     */
    private LatLngBounds putMarkers(){
        Locale locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                locale = getResources().getConfiguration().getLocales().get(0);
            } else{
                //noinspection deprecation
                locale = getResources().getConfiguration().locale;
            }
        DateFormat df = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM, locale);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasMarkers = false;

        long [] data = dataLab.getFilteredIDs();
        for (long id : data) {
            DataRecord dataRecord = dataLab.readDataRecordFromDatabase(id);
            if(dataRecord.hasLocation()){
                LatLng position = new LatLng(dataRecord.getLatitude(), dataRecord.getLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(df.format(dataRecord.getDate()))
                        .snippet(dataRecord.getSnippet()));
                boundsBuilder.include(position);
                hasMarkers = true;
                markerIDs.put(marker,dataRecord.getDatabaseID());
            }
        }

        // https://stackoverflow.com/questions/13904651/android-google-maps-v2-how-to-add-marker-with-multiline-snippet
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(MainMapsActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(MainMapsActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(MainMapsActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

        // If there are no markers, the University of Bremen will be shown.
        if(!hasMarkers){
            LatLng nw1 = new LatLng(53.103682, 8.850324);
            boundsBuilder.include(nw1);
            LatLng mzh = new LatLng(53.106711, 8.852230);
            boundsBuilder.include(mzh);
        }

        return boundsBuilder.build();
    }

    /**
     * If any change in the underlying Data is received, the markers are redrawn.
     * The map is not rezoomed as the user might not expect to have the map moving itself.
     */
    private class MainMapsActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mMap.clear();
            putMarkers();
        }
    }
}
