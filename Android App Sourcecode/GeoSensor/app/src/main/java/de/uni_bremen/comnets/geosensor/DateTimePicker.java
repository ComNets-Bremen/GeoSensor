package de.uni_bremen.comnets.geosensor;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This FateTime picker combines a DatePicker and a TimePicker in order to get a minute-precise
 * Date object for any day.
 *
 * Created by Eike on 19.02.2017.
 */
class DateTimePicker implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
    private int lastPickedYear;
    private int lastPickedMonth;
    private int lastPickedDay;
    private int lastPickedHour;
    private int lastPickedMinute;

    private final OnDateTimePickedListener listener;
    private final FragmentActivity activity;

    /**
     * Get a Date and Time picked by the user and the result delivered to the
     * OnDateTimePickedListener.
     * @param activity The activity requesting this DateTimePicker
     * @param listener A listener listening for the result.
     */
    DateTimePicker(FragmentActivity activity, OnDateTimePickedListener listener){
        this.listener = listener;
        this.activity = activity;

        DatePickerFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.setParent(this);
        datePickerFragment.show(activity.getSupportFragmentManager(), "datePicker");
    }

    /**
     * Method called by the DatePicker to give this the Date picked
     * @param view The DataPicker
     * @param year yyyy
     * @param month mm
     * @param day dd
     */
    public void onDateSet(DatePicker view, int year, int month, int day) {
        lastPickedYear = year;
        lastPickedMonth = month;
        lastPickedDay = day;

        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setParent(this);
        timePickerFragment.show(activity.getSupportFragmentManager(), "timePicker");
    }

    /**
     * Method called by the TimePicker to give this the Time picked
     * @param view the TimePicker
     * @param hourOfDay HH
     * @param minute mm
     */
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        lastPickedHour = hourOfDay;
        lastPickedMinute = minute;

        notifyListener();
    }

    /**
     * Notify the listener given by the requesting Activity when the result is ready
     */
    private void notifyListener(){
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(lastPickedYear,lastPickedMonth,lastPickedDay,lastPickedHour,lastPickedMinute);
        listener.onDateTimePicked(cal.getTime());
    }

    /**
     * This listener can be implemented by any part of the app that starts a DateTimePicker
     */
    static abstract class OnDateTimePickedListener implements Parcelable{
        OnDateTimePickedListener(){}
        public abstract void onDateTimePicked(Date date);

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    /**
     * The DatePicker used here.
     * Code taken from Android API example
     * https://developer.android.com/guide/topics/ui/controls/pickers.html
     */
    public static class DatePickerFragment extends DialogFragment {

        private DateTimePicker parent = null;

        public void setParent(DateTimePicker dtp){
            parent = dtp;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), parent, year, month, day);
        }
    }

    /**
     * The TimePicker used here.
     * Code taken from Android API example
     * https://developer.android.com/guide/topics/ui/controls/pickers.html
     */
    public static class TimePickerFragment extends DialogFragment {
        private DateTimePicker parent = null;

        public void setParent(DateTimePicker dtp){
            parent = dtp;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), parent, hour, minute, true);
        }
    }

}
