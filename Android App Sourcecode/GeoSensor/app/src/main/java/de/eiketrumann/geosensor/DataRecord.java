package de.eiketrumann.geosensor;

import android.content.Context;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The DataRecord is the main data object used in this app.
 * It represents all data acquired together after one trigger button press.
 * Therefore it includes the receive time, the general information included in the data,
 * the locations given by the arduino and the fused location provider and all measured values.
 *
 * Created by Eike on 15.02.2017.
 */

class DataRecord {
    /** List of the geographic locations associated with this object */
    private final List<Location> locations;
    /** List of the measured values associated with this object */
    private final List<MeasureData> measureData;
    /** Data transmitted by the Arduino */
    @SuppressWarnings("SpellCheckingInspection")
    private final String arduinoSoftware;
    @SuppressWarnings("SpellCheckingInspection")
    private long arduinoTime;
    private String comment;

    private final Date receiveTime;

    /** The database primary key used to identify this DataRecord */
    private long databaseID = -1;

    /**
     * Build a DataRecord containing the data given.
     * @param locations A list of the associated Locations
     * @param measureData A list of the associated measured values
     * @param arduinoSoftware The software version run on the Arduino transducer device
     * @param arduinoTime The time since the Arduino was started
     * @param comment The modifiable comment string (originally given by the Arduino)
     * @param receiveTime The time this DataRecord was received
     * @param databaseID The database primary key associated qith this DataRecord. -1 if not yet
     *                   saved into the database.
     */
    @SuppressWarnings("SpellCheckingInspection")
    DataRecord(List<Location> locations, List<MeasureData> measureData,
                      String arduinoSoftware, long arduinoTime, String comment, Date receiveTime, long databaseID){
        this.locations = locations;
        this.measureData = measureData;
        this.arduinoSoftware = arduinoSoftware;
        this.arduinoTime = arduinoTime;
        this.comment = comment;
        this.receiveTime = receiveTime;
        this.databaseID = databaseID;
    }

    /**
     * Get a list of the Locations associated with this DataRecord
     * @return A lIst of Locations
     */
    List<Location> getLocations() {
        return locations;
    }


    /**
     * Get a list of the measured values associated with this DataRecord
     * @return A lIst of MeasureData objects containing the measured values
     */
    List<MeasureData> getMeasureData() {
        return measureData;
    }

    /**
     * Get the latitude of the best location this data record has
     * @return a double containing the latitude in degrees (south is negative)
     */
    double getLatitude() {
        if (!locations.isEmpty())
            return getLocation().getLatitude();
        return 0;
    }

    /**
     * Get the longitude of the best location this data record has
     * @return a double containing the longitude in degrees (east is negative)
     */
    double getLongitude() {
        if (!locations.isEmpty())
            return getLocation().getLongitude();
        return 0;
    }

    /** This is the location format used throughout the app */
    private final int preferredFormat = Location.FORMAT_DEGREES;

    /**
     * Get the latitude of the best location this data record has as a formatted string
     * @return A string formatted in degrees
     */
    String getFormattedLatitude() {
        return Location.convert(getLatitude(),preferredFormat)+"°";
    }

    /**
     * Get the longitude of the best location this data record has as a formatted string
     * @return A string formatted in degrees
     */
    String getFormattedLongitude() {
        return Location.convert(getLongitude(),preferredFormat)+"°";
    }

    /**
     * Get the suppostly most precise location available for this data record.
     * Locations having a little time difference to the receive time and a higher accuracy are
     * preferred.
     * @return A location associated with this DataRecord.
     */
    public Location getLocation() {
        // Lowest score is best
        int[] score = new int [locations.size()];
        for (int i = 0; i < locations.size(); i++){
            score[i] += (int) Math.abs(receiveTime.getTime() - locations.get(i).getTime());
            if(locations.get(i).hasAccuracy()){
                score[i] += 1000 * locations.get(i).getAccuracy();
            } else {
                score[i] += 10000;
            }
        }

        // Get the index of the smallest number in the array
        int lowest = Integer.MAX_VALUE;
        int best = 0;
        for (int i = 0; i < locations.size(); i++){
            if (score[i] < lowest){
                lowest = score[i];
                best = i;
            }
        }

        return locations.get(best);
    }

    /**
     * Get the receive time
     * @return a Date object containing the receive time
     */
    public Date getDate() {
        return receiveTime;
    }

    /**
     * Get the software version string transmitted by the Arduino
     * @return a version string
     */
    @SuppressWarnings("SpellCheckingInspection")
    String getArduinoSoftware() {
        return arduinoSoftware;
    }

    /**
     * Get the uptime transmitted by the Arduino
     * @return a time in milliseconds since startup
     */
    @SuppressWarnings("SpellCheckingInspection")
    long getArduinoTime() {
        return arduinoTime;
    }

    /**
     * Get the comment string set by the user or transmitted by the Arduino software
     * @return the comment sting
     */
    String getComment() {
        return comment;
    }

    /**
     * Get te snippet string to be shown in the map view
     * @return A formatted string containing the comment and the measured values
     */
    String getSnippet() {
        String ret = getComment();
        for(MeasureData md : measureData){
            ret += "\n "+md.toString();
        }
        return ret;
    }

    /**
     * Get the id of this DataRecord in the database.
     * @return -1 if not saved to database, database primary key otherwise
     */
    long getDatabaseID() { return databaseID; }

    /**
     * @return The time this data record was received by the Android app
     */
    Date getReceiveTime(){
        return receiveTime;
    }

    /**
     * @return true if there is a at least one location associated to this data record
     */
    boolean hasLocation() {
        return locations.size() != 0;
    }

    /**
     * Set the comment associated to this data record.
     * The ole comment will be replaced and the new comment will be saved to the database.
     * @param context A context (used to access the database)
     * @param comment The new comment string.
     */
    void setComment(Context context, String comment){
        this.comment = comment;
        DataLab dl = new DataLab(context);
        dl.editComment(databaseID, comment);
    }

    /**
     * Set the database id when this DataRecord has been saved to the database.
     * @param databaseID The database primary key.
     */
    void setDatabaseID(long databaseID){
        this.databaseID = databaseID;
    }

}
