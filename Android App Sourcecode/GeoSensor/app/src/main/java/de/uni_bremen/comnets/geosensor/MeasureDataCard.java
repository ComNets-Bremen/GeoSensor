package de.uni_bremen.comnets.geosensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * The MeasureDataCard shows a single MeasureData, i.e. a single measured value, in the details view.
 *
 * Created by Eike Trumann on 18.02.2017.
 */

@SuppressLint("ViewConstructor")
class MeasureDataCard extends CardView {
    private final MeasureData measureData;

    /**
     * Builds the card using the information frpm the MeasureData object given
     * @param context The Activity context containing the Card View
     * @param measureData The MeasureData object to be visualised
     */
    MeasureDataCard(Context context, MeasureData measureData){
        super(context);
        this.measureData = measureData;
        init();
    }

    /**
     * This puts the actual information into the inflated XML
     */
    private void init(){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View card = inflater.inflate(R.layout.measure_data_card, this);
        TextView title = (TextView) card.findViewById(R.id.card_measure_data_title);
        TextView name = (TextView) card.findViewById(R.id.card_measure_data_name);
        TextView type = (TextView) card.findViewById(R.id.card_measure_data_type);
        TextView value = (TextView) card.findViewById(R.id.card_measure_data_value);

        title.setText(MeasureData.getLocalType(measureData.getType()));
        name.setText(String.format(getResources().getString(R.string.measure_data_name), measureData.getName()));
        type.setText(String.format(getResources().getString(R.string.measure_data_type), measureData.getSensor()));
        value.setText(String.format(getResources().getString(R.string.measure_data_value), measureData.getValue(), measureData.getUnit()));
    }
}

