package de.eiketrumann.geosensor;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Database access helper for GeoSensor App.
 * This class provides the methods to access the database and handle the data objects.
 * This includes parsing the JSON data received from the bluetooth connection.
 *
 * Created by Eike on 18.02.2017.
 * Inspired bz Vogella
 */
class DataLab extends SQLiteOpenHelper implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    /** The database name is the filename on the filesystem. */
    private static final String DATABASE_NAME = "MeasureData.db";
    /** The version indicates a data format and must be adjusted if the database structure is changed*/
    private static final int DATABASE_VERSION = 19;
    /** If localisation is switched on, an API client is needed to access Googles FusedLocationProvider*/
    private GoogleApiClient googleApiClient;
    /** A DataLab needs a context to exist. It seems the Application context works well.*/
    private Context context;

    /** The columns to represent a DataRecord object in the database*/
    static final class DataRecordEntry implements BaseColumns {
        static final String TABLE_DATA_RECORD = "data_record";
        static final String COLUMN_ID = "_id";
        @SuppressWarnings("SpellCheckingInspection")
        static final String COLUMN_ARDUINO_SOFTWARE = "arduino_software";
        @SuppressWarnings("SpellCheckingInspection")
        static final String COLUMN_ARDUINO_TIME = "arduino_time";
        static final String COLUMN_COMMENT = "comment";
        static final String COLUMN_RECEIVE_TIME = "receive_time";
        static final String[] COLUMNS = {COLUMN_ID,COLUMN_ARDUINO_SOFTWARE,COLUMN_ARDUINO_TIME, COLUMN_RECEIVE_TIME, COLUMN_COMMENT};
    }

    /** The columns to represent a Location object in the database*/
    static final class LocationEntry implements BaseColumns {
        static final String TABLE_LOCATION = "location";
        static final String COLUMN_ID = "_id";
        static final String COLUMN_LATITUDE = "lat";
        static final String COLUMN_LONGITUDE = "lng";
        static final String COLUMN_HAS_ALTITUDE = "has_altitude";
        static final String COLUMN_ALTITUDE = "altitude";
        static final String COLUMN_TIME = "time";
        static final String COLUMN_HAS_ACCURACY = "has_accuracy";
        static final String COLUMN_ACCURACY = "accuracy";
        static final String COLUMN_PROVIDER = "provider";
        static final String[] COLUMNS = {COLUMN_ID,COLUMN_LATITUDE,COLUMN_LONGITUDE,
                COLUMN_HAS_ALTITUDE,COLUMN_ALTITUDE,COLUMN_TIME,COLUMN_HAS_ACCURACY,COLUMN_ACCURACY,
                COLUMN_PROVIDER};
    }

    /** The columns to represent a MeasureData object in the database*/
    static final class MeasureDataEntry implements BaseColumns {
        static final String TABLE_MEASURE_DATA = "measure_data";
        static final String COLUMN_ID = "_id";
        static final String COLUMN_TYPE = "type";
        static final String COLUMN_SENSOR = "sensor";
        static final String COLUMN_NAME = "name";
        static final String COLUMN_VALUE = "value";
        static final String COLUMN_UNIT = "unit";
        static final String[] COLUMNS = {COLUMN_ID,COLUMN_TYPE,COLUMN_SENSOR,COLUMN_NAME,COLUMN_VALUE,COLUMN_UNIT};
    }

    /** The table mapping Locations to DataRecords*/
    private static class DataRecordLocationMap implements BaseColumns {
        private static final String TABLE_DATA_RECORD_LOCATION_MAP = "data_record_location_map";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_LOCATION_ID = "location_id";
        private static final String COLUMN_DATA_RECORD_ID = "data_record_id";
        private static final String[] COLUMNS = {COLUMN_ID,COLUMN_LOCATION_ID,COLUMN_DATA_RECORD_ID};
    }

    /** The table mapping MeasureData objects to DataRecord objects*/
    private static class DataRecordMeasureDataMap implements BaseColumns {
        private static final String TABLE_DATA_RECORD_MEASURE_DATA_MAP = "data_record_measure_data_map";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_MEASURE_DATA_ID = "measure_data_id";
        private static final String COLUMN_DATA_RECORD_ID = "data_record_id";
        private static final String[] COLUMNS = {COLUMN_ID,COLUMN_MEASURE_DATA_ID,COLUMN_MEASURE_DATA_ID};
    }


    /**
     * A separate DataLab might be instantiated for any activity needing it.
     *
     * @param context Application or arbitrary Activity context. Activity contexts might leak memory.
     */
    DataLab(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);

        this.context = context;

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
    }

    /**
     * This method is called if no database exists (first installation or app reset).
     * It prepares all tables necessary.
     *
     * It might be also called after a database reset.
     * @param database The database to prepare
     */
    @Override
    public void onCreate(SQLiteDatabase database){
        String createDataRecordTable = "CREATE TABLE " +
                DataRecordEntry.TABLE_DATA_RECORD + "("
                + DataRecordEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
                + DataRecordEntry.COLUMN_ARDUINO_SOFTWARE + " TEXT,"
                + DataRecordEntry.COLUMN_ARDUINO_TIME + " INTEGER,"
                + DataRecordEntry.COLUMN_RECEIVE_TIME + " INTEGER,"
                + DataRecordEntry.COLUMN_COMMENT + " TEXT"
                + ")";
        database.execSQL(createDataRecordTable);

        String createLocationTable = "CREATE TABLE " +
                LocationEntry.TABLE_LOCATION + "("
                + LocationEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
                + LocationEntry.COLUMN_LATITUDE + " REAL,"
                + LocationEntry.COLUMN_LONGITUDE + " REAL,"
                + LocationEntry.COLUMN_HAS_ALTITUDE + " INTEGER,"
                + LocationEntry.COLUMN_ALTITUDE + " REAL,"
                + LocationEntry.COLUMN_TIME + " INTEGER,"
                + LocationEntry.COLUMN_HAS_ACCURACY + " INTEGER,"
                + LocationEntry.COLUMN_ACCURACY + " REAL,"
                + LocationEntry.COLUMN_PROVIDER + " TEXT"
                + ")";
        database.execSQL(createLocationTable);

        String createMeasureDataTable = "CREATE TABLE " +
                MeasureDataEntry.TABLE_MEASURE_DATA + "("
                + MeasureDataEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
                + MeasureDataEntry.COLUMN_TYPE + " TEXT,"
                + MeasureDataEntry.COLUMN_SENSOR + " TEXT,"
                + MeasureDataEntry.COLUMN_NAME + " TEXT,"
                + MeasureDataEntry.COLUMN_VALUE + " REAL,"
                + MeasureDataEntry.COLUMN_UNIT + " TEXT"
                + ")";
        database.execSQL(createMeasureDataTable);

        String createDataRecordLocationMap = "CREATE TABLE " +
                DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP + "("
                + DataRecordLocationMap.COLUMN_ID + " INTEGER PRIMARY KEY,"
                + DataRecordLocationMap.COLUMN_LOCATION_ID + " INTEGER,"
                + DataRecordLocationMap.COLUMN_DATA_RECORD_ID + " INTEGER"
                + ")";
        database.execSQL(createDataRecordLocationMap);

        String createDataRecordMeasureDataMap = "CREATE TABLE " +
                DataRecordMeasureDataMap.TABLE_DATA_RECORD_MEASURE_DATA_MAP + "("
                + DataRecordMeasureDataMap.COLUMN_ID + " INTEGER PRIMARY KEY,"
                + DataRecordMeasureDataMap.COLUMN_MEASURE_DATA_ID  + " INTEGER,"
                + DataRecordMeasureDataMap.COLUMN_DATA_RECORD_ID + " INTEGER"
                + ")";
        database.execSQL(createDataRecordMeasureDataMap);
    }

    /**
     * Upgrades the database to a new version.
     * Warning: this implementation deletes all database contents.
     * @param database The database to upgrade
     * @param oldVersion old version number
     * @param newVersion new version number
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        // rebuildDatabase();
    }

    /**
     * This method clears all data from the database and puts the basic database layout back in.
     */
    void rebuildDatabase() {
        SQLiteDatabase database = getWritableDatabase();
        database.execSQL("DROP TABLE IF EXISTS " + DataRecordEntry.TABLE_DATA_RECORD);
        database.execSQL("DROP TABLE IF EXISTS " + LocationEntry.TABLE_LOCATION);
        database.execSQL("DROP TABLE IF EXISTS " + MeasureDataEntry.TABLE_MEASURE_DATA);
        database.execSQL("DROP TABLE IF EXISTS " + DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP);
        database.execSQL("DROP TABLE IF EXISTS " + DataRecordMeasureDataMap.TABLE_DATA_RECORD_MEASURE_DATA_MAP);

        onCreate(database);
    }

    /**
     * Saves a single DataRecord objects and all associated Locations and MeasureData objects into
     * the Database
     * @param dr The DataRecord to save
     * @return The unique database id of the DataRecord just saved (might be reused if the
     *      DataRecord is deleted
     */
    long saveDataRecord(DataRecord dr){
        ContentValues data = new ContentValues();
        data.put(DataRecordEntry.COLUMN_ARDUINO_SOFTWARE,dr.getArduinoSoftware());
        data.put(DataRecordEntry.COLUMN_ARDUINO_TIME,dr.getArduinoTime());
        data.put(DataRecordEntry.COLUMN_RECEIVE_TIME,dr.getReceiveTime().getTime());
        data.put(DataRecordEntry.COLUMN_COMMENT,dr.getComment());

        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(DataRecordEntry.TABLE_DATA_RECORD, null, data);

        for(Location l:dr.getLocations()){
            long locationID = saveLocationToDatabase(l);
            ContentValues map = new ContentValues();
            map.put(DataRecordLocationMap.COLUMN_LOCATION_ID, locationID);
            map.put(DataRecordLocationMap.COLUMN_DATA_RECORD_ID, id);
            db.insert(DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP,null,map);
        }

        for(MeasureData m:dr.getMeasureData()){
            long measureDataID = saveMeasureDataToDatabase(m);
            ContentValues map = new ContentValues();
            map.put(DataRecordMeasureDataMap.COLUMN_MEASURE_DATA_ID, measureDataID);
            map.put(DataRecordMeasureDataMap.COLUMN_DATA_RECORD_ID, id);
            db.insert(DataRecordMeasureDataMap.TABLE_DATA_RECORD_MEASURE_DATA_MAP,null,map);
        }

        dr.setDatabaseID(id);

        return id;
    }

    /**
     * Reads one DataRecord from the database
     * @param id The primary key ID for the DataRecord
     * @return The DataRecord read from the database
     */
    DataRecord readDataRecordFromDatabase(long id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DataRecordEntry.TABLE_DATA_RECORD, DataRecordEntry.COLUMNS," "
                +DataRecordEntry.COLUMN_ID+" = ?",new String[] {Long.toString(id)},null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            List<Location> locations = new ArrayList<>();
            Cursor locationMapCursor = db.query(DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP,
                    DataRecordLocationMap.COLUMNS, DataRecordLocationMap.COLUMN_DATA_RECORD_ID+"=?",
                    new String[]{Long.toString(id)},
                    null, null, null);

            if (locationMapCursor != null && locationMapCursor.moveToFirst()) {
                do {
                    int locID = locationMapCursor.getInt(locationMapCursor.getColumnIndex(DataRecordLocationMap.COLUMN_LOCATION_ID));
                    locations.add(readLocationFromDatabase(locID));
                } while (locationMapCursor.moveToNext());

                locationMapCursor.close();
            }

            List<MeasureData> measureDataList = new ArrayList<>();
            Cursor measureDataMapCursor = db.query(DataRecordMeasureDataMap.TABLE_DATA_RECORD_MEASURE_DATA_MAP,
                    DataRecordMeasureDataMap.COLUMNS, DataRecordMeasureDataMap.COLUMN_DATA_RECORD_ID+"=?",
                    new String[]{Long.toString(id)}, null, null, null);
            if (measureDataMapCursor != null && measureDataMapCursor.moveToFirst()) {
                do {
                    int mdID = measureDataMapCursor.getInt(measureDataMapCursor.getColumnIndex(DataRecordMeasureDataMap.COLUMN_MEASURE_DATA_ID));
                    measureDataList.add(readMeasureDataFromDatabase(mdID));
                } while (measureDataMapCursor.moveToNext());

                measureDataMapCursor.close();
            }

            @SuppressWarnings("SpellCheckingInspection")
            String arduinoSoftware = cursor.getString(cursor.getColumnIndex(DataRecordEntry.COLUMN_ARDUINO_SOFTWARE));
            @SuppressWarnings("SpellCheckingInspection")
            long arduinoTime = cursor.getLong(cursor.getColumnIndex(DataRecordEntry.COLUMN_ARDUINO_TIME));
            Date receiveTime = new Date(cursor.getLong(cursor.getColumnIndex(DataRecordEntry.COLUMN_RECEIVE_TIME)));
            String comment = cursor.getString(cursor.getColumnIndex(DataRecordEntry.COLUMN_COMMENT));

            cursor.close();

            return new DataRecord(locations, measureDataList, arduinoSoftware, arduinoTime, comment, receiveTime, id);
        }
        return null;
    }

    /**
     * Get an array of database IDs for the DataRecords that correspond to the currently set filters
     * @return An array of database IDs
     */
    long[] getFilteredIDs(){
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {DataRecordEntry._ID};
        String selection = DataRecordEntry.COLUMN_RECEIVE_TIME+" >= ? AND "+DataRecordEntry.COLUMN_RECEIVE_TIME+" <= ?";
        String[] selectionArgs = { Long.toString(FilterState.getStartTime().getTime()), Long.toString(FilterState.getEndTime().getTime()) };
        String order = DataRecordEntry.COLUMN_RECEIVE_TIME + (FilterState.isOldestFirst() ? " ASC" : " DESC");
        Cursor cursor = db.query(DataRecordEntry.TABLE_DATA_RECORD, columns, selection,
                selectionArgs, null, null, order);

        long[] result = new long[cursor.getCount()];

        if (cursor.moveToFirst()) {
            do {
                result[cursor.getPosition()] = cursor.getLong(cursor.getColumnIndexOrThrow(DataRecordEntry.COLUMN_ID));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return result;
    }

    /**
     * Add a location object to a DataRecord (given by it's database ID)
     * The location will be saved to the database and linked with the DataRecord database entry
     * @param l The Location object
     * @param dataRecordID The ID of the DataRecord the Location should be linked to
     */
    void addLocation(Location l, long dataRecordID){
        long locationID = saveLocationToDatabase(l);
        ContentValues map = new ContentValues();
        map.put(DataRecordLocationMap.COLUMN_LOCATION_ID, locationID);
        map.put(DataRecordLocationMap.COLUMN_DATA_RECORD_ID, dataRecordID);

        SQLiteDatabase db = getReadableDatabase();
        db.insert(DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP,null,map);


    }

    /**
     * Posts a DelayedLocationProvider runnable to the main looper.
     * This means a location will later be added to the given DataRecord in the database
     * @param dataRecordID The ID of the DataRecord that should have the Location added.
     */
    void requestDeviceLocationAdded(final long dataRecordID){
        class DelayedLocationRunnable extends Thread{
            @Override
            public void run(){
                new DelayedLocationProvider(dataRecordID, DataLab.this, googleApiClient, context);
            }
        }
        new Handler(Looper.getMainLooper()).post(new DelayedLocationRunnable());

    }

    /**
     * Saves the given location to the database
     * @param location A Location object
     * @return The primary key given to the Location object
     */
    private long saveLocationToDatabase(Location location){
        ContentValues loc = new ContentValues();
        loc.put(LocationEntry.COLUMN_LATITUDE,location.getLatitude());
        loc.put(LocationEntry.COLUMN_LONGITUDE,location.getLongitude());
        loc.put(LocationEntry.COLUMN_HAS_ALTITUDE,location.hasAltitude());
        loc.put(LocationEntry.COLUMN_ALTITUDE,location.getAltitude());
        loc.put(LocationEntry.COLUMN_TIME,location.getTime());
        loc.put(LocationEntry.COLUMN_HAS_ACCURACY,location.hasAccuracy());
        loc.put(LocationEntry.COLUMN_ACCURACY,location.getAccuracy());
        loc.put(LocationEntry.COLUMN_PROVIDER,location.getProvider());

        SQLiteDatabase db = getWritableDatabase();
        return db.insert(LocationEntry.TABLE_LOCATION, null, loc);
    }

    /**
     * Reads a Location from the database
     * Does not link to a DataRecord and therefore should not be used without linking
     * @param id The primary key for the Location
     * @return A Location object
     */
    private Location readLocationFromDatabase(long id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(LocationEntry.TABLE_LOCATION, LocationEntry.COLUMNS," "+LocationEntry.COLUMN_ID+" = ?",new String[] {Long.valueOf(id).toString()},null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        } else {
            return null;
        }

        Location location = new Location("Database");

        try {
            location.setProvider(cursor.getString(cursor.getColumnIndex(LocationEntry.COLUMN_PROVIDER)));
            location.setLatitude(cursor.getDouble(cursor.getColumnIndex(LocationEntry.COLUMN_LATITUDE)));
            location.setLongitude(cursor.getDouble(cursor.getColumnIndex(LocationEntry.COLUMN_LONGITUDE)));
            // The database represents false as 0 and true as 1
            if (cursor.getInt(cursor.getColumnIndex(LocationEntry.COLUMN_HAS_ALTITUDE)) != 0)
                location.setAltitude(cursor.getDouble(cursor.getColumnIndex(LocationEntry.COLUMN_ALTITUDE)));
            if(cursor.getInt(cursor.getColumnIndex(LocationEntry.COLUMN_HAS_ACCURACY)) != 0)
                location.setAccuracy(cursor.getInt(cursor.getColumnIndex(LocationEntry.COLUMN_ACCURACY)));
            location.setTime(cursor.getLong(cursor.getColumnIndex(LocationEntry.COLUMN_TIME)));
        } catch (NullPointerException e) {
            Log.i(this.getClass().getSimpleName(), e.toString());
            return null;
        } finally {
        cursor.close();
        }

        return location;
    }

    /**
     * Saves the given MeasureData object to the database
     * Does not link to a DataRecord and therefore should not be used without linking
     * @param md A MeasureData object
     * @return The primary key given to the MeasureData object
     */
    private long saveMeasureDataToDatabase(MeasureData md){
        ContentValues data = new ContentValues();
        data.put(MeasureDataEntry.COLUMN_TYPE,md.getType());
        data.put(MeasureDataEntry.COLUMN_SENSOR,md.getSensor());
        data.put(MeasureDataEntry.COLUMN_NAME,md.getName());
        data.put(MeasureDataEntry.COLUMN_VALUE,md.getValue());
        data.put(MeasureDataEntry.COLUMN_UNIT,md.getUnit());

        SQLiteDatabase db = getWritableDatabase();
        return db.insert(MeasureDataEntry.TABLE_MEASURE_DATA, null, data);
    }

    /**
     * Reads a MeasureData object from the database
     * @param id Primary Key ID of the MeasureData object
     * @return A MeasureData object
     */
    private MeasureData readMeasureDataFromDatabase(long id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(MeasureDataEntry.TABLE_MEASURE_DATA, MeasureDataEntry.COLUMNS," "+MeasureDataEntry.COLUMN_ID+" = ?",new String[] {Long.toString(id)},null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        } else {
            return null;
        }

        try {
            String type = cursor.getString(cursor.getColumnIndex(MeasureDataEntry.COLUMN_TYPE));
            String sensor = cursor.getString(cursor.getColumnIndex(MeasureDataEntry.COLUMN_SENSOR));
            String name = cursor.getString(cursor.getColumnIndex(MeasureDataEntry.COLUMN_NAME));
            Double value = cursor.getDouble(cursor.getColumnIndex(MeasureDataEntry.COLUMN_VALUE));
            String unit = cursor.getString(cursor.getColumnIndex(MeasureDataEntry.COLUMN_UNIT));
            return new MeasureData(type, sensor, name, value, unit);
        } catch (NullPointerException e) {
            Log.d(this.getClass().getSimpleName(), e.toString());
            return null;
        } finally {
            cursor.close();
        }
    }

    /**
     * This function parses a JSON string into a
     * @param jsonString the JSON string according to the protocol specification
     * @param receiveTime the time the JSON string was received
     * @return The DataRecord parsed
     * @throws ParseException if the JSON is not formatted right
     */
    DataRecord parseJSON(String jsonString, Date receiveTime) throws ParseException{
        JSONObject json;
        List<Location> locationList = new ArrayList<>(1);
        List<MeasureData> measureDataList;
        try {
            json = new JSONObject(jsonString);

            if(json.has("position")){
                JSONObject locationJsonObject = json.getJSONObject("position");
                Location location = parseLocation(locationJsonObject);
                if (location != null){
                    locationList.add(location);
                }
            }

            measureDataList = parseMeasureData(json.getJSONObject("sensors"));

            String arduinoSoftware = json.optString("arduino_software", "");
            Long arduinoTime = json.optLong("arduino_time", 0);
            String comment = json.optString("comment", "");

            return new DataRecord(locationList,measureDataList,arduinoSoftware,arduinoTime,comment,receiveTime,-1);

        } catch (JSONException e) {
            Log.w(this.getClass().getSimpleName(),e.toString());
            throw new ParseException("Invalid JSON string",0);
        }
    }

    /**
     * This function parses the Location sub-object included in the JSON message received.
      * @param json A JSON string of the Location JSON object.
     * @return The Location ocject parsed from the JSON data. Null id the location is invalid.
     * @throws ParseException Thrown if the message does not conform to the protocol specification
     */
    @SuppressWarnings("SpellCheckingInspection")
    private Location parseLocation(JSONObject json) throws ParseException  {
        try {
            if (json.has("valid") && !json.getBoolean("valid")){
                return null;
            }

            Location loc = new Location("ArduinoGPS");
            loc.setProvider(json.optString("provider", "ArduinoGPS"));
            loc.setLatitude(json.optDouble("latitude", 0));
            loc.setLongitude(json.optDouble("longitude", 0));
            if (json.has("altitude")) {
                loc.setAltitude(json.getDouble("altitude"));
            }
            if (json.has("accuracy")) {
                loc.setAccuracy((float) json.getDouble("accuracy"));
            }

            long date = json.optLong("date", 0);
            long time = json.optLong("time", 0);
            // This is the time format delivered by the TinyGPS++ Library
            DateFormat format = new SimpleDateFormat("ddMMyyHHmmss", Locale.US);
            // The time given by the GPS receiver is always the GPS time.
            // The TinyGPS++ library converts it to the Universal Time Coordinated (UTC)
            format.setTimeZone(TimeZone.getTimeZone("UTC"));

            try {
                // The Locale is not really relevant
                // Formats the time to the fpr,at given
                loc.setTime(format.parse(String.format(Locale.GERMANY, "%06d", date) +
                        String.format(Locale.GERMANY,"%06d", time/100)).getTime());
            } catch (ParseException e) {
                // The date format provided by in the Message must be formatted exactly according
                // to the specificaiton.
                throw new ParseException("Wrong date format", 0);
            }

            return loc;

        } catch (JSONException e) {
            Log.i(this.getClass().getSimpleName(),e.toString());
            throw new ParseException("Invalid JSON string", 0);
        }
    }

    /**
     * This is a list of all the MeasureData objects that can be read from the sensors object
     * included in the JSON message
     * @param json The sensor object included in the JSON message string.
     * @return A list of MeasureData objects parsed from the sensors JSON sub-object
     */
    private List<MeasureData> parseMeasureData(JSONObject json){
        List<MeasureData> list = new LinkedList<>();

        Iterator<String> iterator = json.keys();
        while(iterator.hasNext()){
            String key = iterator.next();
            try{
                JSONObject obj = json.getJSONObject(key);
                String type = obj.optString("type",key);
                String sensor = obj.optString("sensor",key);
                String name = obj.optString("name",key);
                double value;
                try {
                    value = obj.getDouble("value");
                } catch (JSONException e) {
                    value = Double.NaN;
                }
                String unit = obj.optString("unit",key);
                list.add(new MeasureData(type,sensor,name,value,unit));

            } catch (JSONException e) {
                Log.i(this.getClass().getSimpleName(),e.toString());
            }
        }

        return list;
    }

    /**
     * Delete all DataRecords and the associated Locations and MeasureData objects having a receive
     * date before the specified date.
     * THIS CAN NOT BE UNDONE.
     * @param until Threshold data
     */
    void deleteOldDataRecords(Date until){
        SQLiteDatabase db = getWritableDatabase();
        String[] columns = {DataRecordEntry._ID};
        String selection = DataRecordEntry.COLUMN_RECEIVE_TIME+" <= ?";
        String[] selectionArgs = {Long.toString(until.getTime())};
        Cursor cursor = db.query(DataRecordEntry.TABLE_DATA_RECORD, columns, selection,
                selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                deleteDataRecord(cursor.getLong(cursor.getColumnIndexOrThrow(DataRecordEntry.COLUMN_ID)));
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    /**
     * Delete a single DataRecord and the associated MeasureData and Location objects
     * THIS CAN NOT BE UNDONE.
     * @param databaseID The database ID of the DataRecord to be removed
     */
    void deleteDataRecord(long databaseID) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DataRecordEntry.TABLE_DATA_RECORD,"_id=?",new String[]{Long.toString(databaseID)});

        // Delete all associated locations
        Cursor locationMapCursor = db.query(DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP,
                DataRecordLocationMap.COLUMNS, DataRecordLocationMap.COLUMN_DATA_RECORD_ID+"=?",
                new String[]{Long.toString(databaseID)}, null, null, null);
        if (locationMapCursor != null && locationMapCursor.moveToFirst()) {
            do {
                int locID = locationMapCursor.getInt(locationMapCursor.getColumnIndex(DataRecordLocationMap.COLUMN_LOCATION_ID));
                db.delete(DataRecordLocationMap.TABLE_DATA_RECORD_LOCATION_MAP,"_id=?",new String[]
                        {Long.toString(locationMapCursor.getInt(locationMapCursor.
                                getColumnIndex(DataRecordLocationMap.COLUMN_ID)))});
                db.delete(LocationEntry.TABLE_LOCATION,"_id=?",new String[]{Long.toString(locID)});
            } while (locationMapCursor.moveToNext());
            locationMapCursor.close();
        }

        // Delete all associated MeasureData objects
        Cursor measureDataMapCursor = db.query(DataRecordMeasureDataMap.TABLE_DATA_RECORD_MEASURE_DATA_MAP,
                DataRecordMeasureDataMap.COLUMNS, DataRecordMeasureDataMap.COLUMN_DATA_RECORD_ID+"=?",
                new String[]{Long.toString(databaseID)}, null, null, null);
        if (measureDataMapCursor != null && measureDataMapCursor.moveToFirst()) {
            do {
                int mdID = measureDataMapCursor.getInt(measureDataMapCursor.getColumnIndex(DataRecordMeasureDataMap.COLUMN_MEASURE_DATA_ID));
                db.delete(DataRecordMeasureDataMap.TABLE_DATA_RECORD_MEASURE_DATA_MAP,"_id=?",new String[]
                        {Long.toString(measureDataMapCursor.getInt(measureDataMapCursor.
                                getColumnIndex(DataRecordMeasureDataMap.COLUMN_ID)))});
                db.delete(MeasureDataEntry.TABLE_MEASURE_DATA,"_id=?",new String[]{Long.toString(mdID)});
            } while (measureDataMapCursor.moveToNext());
            measureDataMapCursor.close();
        }
    }

    /**
     * Replace the comment of a DataRecord
     * @param dataBaseID The database ID of the DataRecord that should be modified
     * @param comment The new comment
     */
    void editComment(long dataBaseID, String comment){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DataRecordEntry.COLUMN_COMMENT,comment);
        db.update(DataRecordEntry.TABLE_DATA_RECORD, cv, "_id="+dataBaseID, null);
    }

    /**
     * Finalize this DataLab. This disconnects the ApiClient used to access the Fused Location Prov.
     * @throws Throwable From super.
     */
    @Override
    protected void finalize() throws Throwable{
        if(googleApiClient != null) {
            googleApiClient.disconnect();
        }
        SQLiteDatabase db = getWritableDatabase();
        db.close();
        super.finalize();
    }

    /**
     * Empty method necessary to implement Google API callbacks
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    /**
     * Empty method necessary to implement Google API callbacks
     */
    @Override
    public void onConnectionSuspended(int i) {}

    /**
     * Method necessary to implement Google API callbacks
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(this.getClass().getSimpleName(),"Google API connection failed");
    }
}
