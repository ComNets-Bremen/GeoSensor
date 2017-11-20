package de.uni_bremen.comnets.geosensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * The map card shows a small map including location markers for a single DataRecord
 *
 * Created by Eike Trumann on 27.02.2017.
 */
@SuppressLint("ViewConstructor")
public class MapCard extends CardView implements OnMapReadyCallback{
    // This is the DataRecord we get the locations from
    private final DataRecord dataRecord;

    /**
     * Construct a MapCard using the locations from a specific DataRecord
     * @param context The activity that shows this card
     * @param dataRecord The DataRecord from which the locations will be shown
     */
    MapCard(Context context, DataRecord dataRecord){
        super(context);
        this.dataRecord = dataRecord;
        init();
    }

    /**
     * Initialize the card and start teh asynchronous drawing process
     */
    private void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View card = inflater.inflate(R.layout.map_card, this);
        MapView mapView = (MapView) card.findViewById(R.id.card_map_view);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    /**
     * This is called when Google is able to provide a map (might not be called when offline)
     * @param googleMap The google map provided by the library
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // A LatLngBounds.Builder can determine the outer borders of a map needed to include the
        // given locations
        LatLngBounds.Builder latLngBoundsBuilder = LatLngBounds.builder();

        // A marker is added for every position and it is included in the bounds calculation
        for(Location l : dataRecord.getLocations()){
            LatLng position = new LatLng(l.getLatitude(), l.getLongitude());
            latLngBoundsBuilder.include(position);
            MarkerOptions marker = new MarkerOptions().position(position).title(l.getProvider());
            googleMap.addMarker(marker);
        }

        // The camera is moved to show the markers and a reasonable padding
        LatLngBounds latLngBounds = latLngBoundsBuilder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds,
                (int) (40 * getResources().getDisplayMetrics().density + 0.5f));
        googleMap.setPadding(0, (int) (60 * getResources().getDisplayMetrics().density + 0.5f),0,0);
        googleMap.setMaxZoomPreference(16);
        googleMap.moveCamera(cameraUpdate);

        // If someone clicks the map a larger map containing the same markers is shown
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Intent detailsMap = new Intent(getContext(), DetailsMapsActivity.class);
                detailsMap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                detailsMap.putExtra("dataRecordID",dataRecord.getDatabaseID());
                getContext().startActivity(detailsMap);
            }
        });
    }
}
