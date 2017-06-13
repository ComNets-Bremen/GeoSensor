package de.eiketrumann.geosensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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
 * The GeneralInfo card shows the general information for a DataRecord in the details view
 * Created by Eike on 18.02.2017.
 */

@SuppressLint("ViewConstructor")
public class GeneralInfoCard extends CardView {
    private final DataRecord dataRecord;
    private final Context context;

    /**
     * Prepares a the card using information from the DataRecord given
     * @param context The activity context of the activity showing the card
     * @param dataRecord The DataRecord the information is taken from
     */
    GeneralInfoCard(Context context, DataRecord dataRecord){
        super(context);
        this.dataRecord = dataRecord;
        this.context = context;
        init();
    }

    /**
     * This method actually puts the information into the TextViews
     */
    private void init(){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View card = inflater.inflate(R.layout.general_info_card, this);
        LinearLayout layout = (LinearLayout) card.findViewById(R.id.card_general_info_layout);

        TextView title = (TextView) card.findViewById(R.id.card_general_info_title);
        title.setText(getResources().getString(R.string.card_general_info_title));

        TextView date = (TextView) card.findViewById(R.id.card_general_info_date);
        TextView time = (TextView) card.findViewById(R.id.card_general_info_time);
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
        date.setText(String.format(getResources().getString(R.string.receive_date),dateFormat.format(dataRecord.getReceiveTime())));
        time.setText(String.format(getResources().getString(R.string.receive_time),timeFormat.format(dataRecord.getReceiveTime())));

        if (dataRecord.getArduinoSoftware() != null & !dataRecord.getArduinoSoftware().equals("")){
            TextView arduinoSoftware = new TextView(context);
            arduinoSoftware.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimaryDark));
            arduinoSoftware.setText(String.format(getResources().getString(R.string.arduino_software),dataRecord.getArduinoSoftware()));
            layout.addView(arduinoSoftware);
        }

        if (!(dataRecord.getArduinoTime() == 0)){
            TextView arduinoTime = new TextView(context);
            arduinoTime.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimaryDark));
            arduinoTime.setText(String.format(getResources().getString(R.string.arduino_time),dataRecord.getArduinoTime()));
            layout.addView(arduinoTime);
        }
    }
}

