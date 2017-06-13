package de.eiketrumann.geosensor;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/**
 * The filter utils handle the filter and sorting state in the app.
 *
 * Created by Eike Trumann on 27.02.2017.
 */
class FilterUtils {

    /**
     * Show a filter chooser allowing the user to set different filters to the data set.
     * The filters are then saved to the global FilterState of the app
     * @param activity The activity
     */
    static void filterChooser(final AppCompatActivity activity) {
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
        CharSequence filters[] = new CharSequence[]{
                activity.getString(R.string.filter_start_time),
                activity.getString(R.string.filter_end_time),
                activity.getString(R.string.filter_remove)};
        if(FilterState.getStartTime().getTime() != 0) {
            filters[0] = String.format(activity.getString(R.string.filter_start_time_set),
                    dateTimeFormat.format(FilterState.getStartTime()));
        }
        if(FilterState.getEndTime().getTime() != Long.MAX_VALUE) {
            filters[1] = String.format(activity.getString(R.string.filter_end_time_set),
                    dateTimeFormat.format(FilterState.getEndTime()));
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle(R.string.filter_dialog_title);
        dialog.setItems(filters, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        new DateTimePicker(activity, new DateTimePicker.OnDateTimePickedListener() {
                            @Override
                            public void onDateTimePicked(Date date) {
                                FilterState.setStartTime(date);
                            }
                        });
                        break;
                    }

                    case 1: {
                        new DateTimePicker(activity, new DateTimePicker.OnDateTimePickedListener() {
                            @Override
                            public void onDateTimePicked(Date date) {
                                FilterState.setEndTime(date);
                            }
                        });
                        break;
                    }

                    case 2:
                        FilterState.resetFilter();
                        Toast.makeText(activity, activity.getString(R.string.filter_removed), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        dialog.show();
    }

    static void sortChooser(final AppCompatActivity activity) {
        CharSequence filters[] = new CharSequence[]{
                activity.getString(R.string.sort_oldest_first),
                activity.getString(R.string.sort_newest_first)
        };

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle(R.string.sort_dialog_title);
        dialog.setItems(filters, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        FilterState.orderOldestFirst();
                        break;
                    }

                    case 1: {
                        FilterState.orderNewestFirst();
                        break;
                    }
                }
            }
        });
        dialog.show();
    }
}
