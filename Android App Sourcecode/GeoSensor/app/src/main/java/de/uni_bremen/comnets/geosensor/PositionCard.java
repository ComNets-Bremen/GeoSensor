package de.uni_bremen.comnets.geosensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * This card shows the geographic location data derived from a single sensor
 * in the details view.
 * Created by Eike on 18.02.2017.
 */
@SuppressLint("ViewConstructor")
public class PositionCard extends CardView {
    /** The location visualized by this card */
    private final Location location;
    /** The CardView (Activity) context */
    private final Context context;

    /**
     * Construct a card showing the given Location
     * @param context The CardView context the card is shown in
     * @param loc The Location to be shown
     */
    PositionCard(Context context, Location loc){
        super(context);
        location = loc;
        this.context = context;
        init();
    }

    /**
     * This method puts the actual information into the text fields displayed
     * Additional information that does not exist in every Location is shown in TextViews
     * dynamically added.
     * If an internet connection is available, Googles reverse geocoding service is used to show an
     * address.
     */
    private void init(){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View card = inflater.inflate(R.layout.position_card, this);
        LinearLayout layout = (LinearLayout) card.findViewById(R.id.card_position_layout);
        TextView title = (TextView) card.findViewById(R.id.card_position_title);
        TextView date = (TextView) card.findViewById(R.id.card_position_date);
        TextView time = (TextView) card.findViewById(R.id.card_position_time);
        TextView lat = (TextView) card.findViewById(R.id.card_position_lat);
        TextView lng = (TextView) card.findViewById(R.id.card_position_lng);

        title.setText(String.format(getResources().getString(R.string.position_card_title), location.getProvider()));

        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
        date.setText(String.format(getResources().getString(R.string.date),dateFormat.format(location.getTime())));
        time.setText(String.format(getResources().getString(R.string.time),timeFormat.format(location.getTime())));

        final int preferredFormat = Location.FORMAT_DEGREES;
        lat.setText(String.format(getResources().getString(R.string.lat),Location.convert(location.getLongitude(),preferredFormat)));
        lng.setText(String.format(getResources().getString(R.string.lng),Location.convert(location.getLatitude(),preferredFormat)));

        if(Utils.hasInternet(context)){
            TextView address = new TextView(context);
            address.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimaryDark));
            address.setText(getResources().getString(R.string.loading));
            layout.addView(address);
            Thread reverseGeocodeThread = new Thread(new ReverseGeocodeThread(address));
            reverseGeocodeThread.start();
        }

        if (location.hasAltitude()){
            TextView altitude = new TextView(context);
            altitude.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimaryDark));
            altitude.setText(String.format(getResources().getString(R.string.altitude),location.getAltitude()));
            layout.addView(altitude);
        }

        if (location.hasAccuracy()){
            TextView accuracy = new TextView(context);
            accuracy.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimaryDark));
            accuracy.setText(String.format(getResources().getString(R.string.accuracy),location.getAccuracy()));
            layout.addView(accuracy);
        }
    }

    /**
     * This Runnable asynchronously retrieves a human-readable address and buts it into the
     * specified TextView.
     */
    private class ReverseGeocodeThread implements Runnable{
        private TextView textView;

        ReverseGeocodeThread(TextView textView){
            this.textView = textView;
        }

        @Override
        public void run() {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try{
                List<Address> addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                if (!addressList.isEmpty()){

                    final String addressString = String.format(getResources().getString(R.string.address_format),
                            addressList.get(0).getAddressLine(0),
                            addressList.get(0).getAddressLine(1));

                    PositionCard.this.post(new Runnable() {
                        public void run() {
                            textView.setText(addressString);
                        }
                    });
                }
            } catch (IOException e) {
                Log.d(this.getClass().getSimpleName(),e.toString());
            }
        }
    }
}

