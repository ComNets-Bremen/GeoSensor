package de.uni_bremen.comnets.geosensor;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;

/**
 * The DataAdapter transfers the data into the main list elements.
 * Per performance reasons, a RecyclerView is used and the ViewHolders are therefore reused.
 */
class DataAdapter extends RecyclerView.Adapter {
    private long[] dataRecordIDs;
    private DataLab dataLab;
    private Context context;

    /**
     * Construct a DataAdapter
     * @param context The activity context of the list this should be used with.
     * @param dataLab A DataLab used to access the data in the database.
     */
    DataAdapter(Context context, DataLab dataLab){
        this.dataLab = dataLab;
        this.context = context;

        // Get the IDs of the elements that are shown in the list
        dataRecordIDs = dataLab.getFilteredIDs();
        this.setHasStableIds(true);
    }

    /**
     * Inflate the XML when the ViewHolder is created.
     * @param parent The ViewGroup this ViewHolder will be placed in
     * @param viewType ignored
     * @return The ViewHolder for a single list element
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View element = inflater.inflate(R.layout.main_list_element, parent, false);

        return new ViewHolder(element, context);
    }

    /**
     * Put the information from a DataRecord into the ViewHolder
     * @param holder The ViewHolder to be filled
     * @param position The position in the list (to match the right DataRecord)
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // Read the appropriate DataRecord from the database
        DataRecord dataRecord = dataLab.readDataRecordFromDatabase(dataRecordIDs[position]);

        // Date and time
        View element = ((ViewHolder) holder).getMainListElement();
        TextView date = (TextView) element.findViewById(R.id.listItemDate);
        TextView time = (TextView) element.findViewById(R.id.listItemTime);
        TextView coordinates = (TextView) element.findViewById(R.id.listItemSecondLine);

        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

        date.setText(dateFormat.format(dataRecord.getDate()));
        time.setText(timeFormat.format(dataRecord.getDate()));

        // Show the location if it exists.
        if(dataRecord.hasLocation()){
            coordinates.setText(context.getString(R.string.location) + ": " + dataRecord.getFormattedLatitude() + " " + dataRecord.getFormattedLongitude());
        } else {
            coordinates.setText(context.getString(R.string.no_location_data));
        }

        // Show all the measured values, adding enough lines for each measured value
        LinearLayout measureDataLayout = (LinearLayout) element.findViewById(R.id.list_item_measure_data);
        measureDataLayout.removeAllViewsInLayout();
        for (MeasureData measureData : dataRecord.getMeasureData()) {
            TextView textView = new TextView(context);
            textView.setText(measureData.toString());
            measureDataLayout.addView(textView);
        }
    }

    /**
     * Get the database ID of the DataRecord shown in position i
     * @param i the position of the item in the list
     * @return the database primary key for the DataRecord shown there
     */
    @Override
    public long getItemId(int i) {
        return dataRecordIDs[i];
    }

    /**
     * Get the number of items shown in the list
     * @return the number of items
     */
    @Override
    public int getItemCount() {
        return dataRecordIDs.length;
    }

    /**
     * Get the new IDs of the dataRecords to be shown when something like a filter has changed
     */
    void refreshList(){dataRecordIDs = dataLab.getFilteredIDs();}

    /**
     * The ViewHolder class used by the elements of the list.
     * It is used to add an onClickListener to the elements.
     */
    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // each data item is just a string in this case
        private View mainListElement;
        private Context context;

        ViewHolder(View mainListElement, Context context) {
            super(mainListElement);
            this.mainListElement = mainListElement;
            this.context = context;
            mainListElement.setOnClickListener(this);
        }

        View getMainListElement(){
            return mainListElement;
        }

        /**
         * When clicked, the details view for the DataRecord is opened.
         * @param v the View that was clicked
         */
        @Override
        public void onClick(View v) {
            Intent detailsIntent = new Intent(context, DetailsActivity.class);
            detailsIntent.putExtra("id",getItemId());
            context.startActivity(detailsIntent);
        }
    }
}