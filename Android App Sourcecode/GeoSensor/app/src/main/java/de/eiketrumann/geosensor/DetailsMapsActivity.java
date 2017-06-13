package de.eiketrumann.geosensor;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * The DetailsMapActivity is an activity that shows only a map containing the different location
 * markers of only one data record. It is not a full activity, but more a modal dialog as it does
 * not provide the full action bar, but only a back arrow.
 */
public class DetailsMapsActivity extends GeoSensorActivity implements OnMapReadyCallback {

    /** */
    private DataRecord dataRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_maps);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);

        DataLab dataLab = new DataLab(this);
        dataRecord = dataLab.readDataRecordFromDatabase(getIntent().getLongExtra("dataRecordID",-1));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Disabled all symbols in the ActionBar and adds a back button
     * @param menu the Menu that needs to be inflated
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // A back arrow is added to the ActionBar
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        return true;
    }

    /**
     * This handles input in the menu / Action bar
     * As we disabled all symbols but the back arrow, it is the only one we need to handle
     * @param item hte selected item
     * @return true, if the click has been handled, false if the item in unknown
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * In this specific case this method takes all the locations from the dataRecord and puts
     * the respective markers on the map. The map is then zoomed to contain all the markers
     */
    @SuppressLint("SimpleDateFormat")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            locale = getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            locale = getResources().getConfiguration().locale;
        }
        DateFormat df = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM, locale);

        // The LatLngBounds is used to choose the right map part
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (Location l : dataRecord.getLocations()) {
            LatLng position = new LatLng(l.getLatitude(), l.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(position).title(l.getProvider())
                    .snippet(df.format(l.getTime())));
            boundsBuilder.include(position);
        }

        if(dataRecord.getLocations().size() > 1) {
            LatLngBounds latLngBounds = boundsBuilder.build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds,
                    getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels,
                    (int) (40 * getResources().getDisplayMetrics().density + 0.5f));
            int padding = (int) (60 * getResources().getDisplayMetrics().density + 0.5f);
            googleMap.setPadding(padding/2,padding*2,padding/2,padding/2);
            googleMap.setMaxZoomPreference(17);
            googleMap.moveCamera(cameraUpdate);
        // if we have only one location, the map is centered and set to a reasonable zoom
        } else if(dataRecord.getLocations().size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(dataRecord.getLocation().getLatitude(),
                            dataRecord.getLocation().getLongitude()), 15));
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Alert");
            alertDialog.setMessage(getString(R.string.no_location_message));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        googleMap.resetMinMaxZoomPreference();
    }
}
