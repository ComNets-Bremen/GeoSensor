package de.eiketrumann.geosensor;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * The csvWriter has capabilities to export data from the databse to a csv-file and
 * save it to either private storage (to give a file provider uri) or to public storage (to transfer
 * using usb or open from a third-party file manager.
 * Created by Eike Trumann on 02.03.17.
 */
class CSVWriter {
    // This is the list of IDs given by the caller to be included in the export
    private final long[] dataRecordIDs;

    // This list contains all the data records meant to be exported
    private LinkedList<DataRecord> dataRecordList;

    // The context must be provided in order to gain access to the file system and show dialogs
    private final Context context;

    // These are the number of Locations and DataRecords that should be included in the csv export
    private int maxLocationCount;
    private int maxMeasureDataCount;

    // The Separator is chosen by the user
    private final char CSV_SEPARATOR;
    // This is also a user setting limiting the number of exported locations to one
    private final boolean onlyExportBestLocation;
    // The numberFormat is used to allow the user to choose the decimal mark
    private final NumberFormat numberFormat;

    // The DateFormat is fixed and can not be changed by the user to allow consistent
    // machine- unf human-readability
    @SuppressLint("SimpleDateFormat")
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * A CSVWriter object is always bound to a fixed set of DataRecords it writes to a file.
     * @param context the Activity context launching the export
     * @param dataRecordIDs a list of database IDs that should be included in the export
     */
    CSVWriter(Context context, long[] dataRecordIDs){
        this.context = context;
        this.dataRecordIDs = dataRecordIDs;

        // The cell separators and the decimal separator are saved in the shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        CSV_SEPARATOR = sharedPreferences.getString(context.getString(R.string.pref_key_csv_separator),",").charAt(0);
        onlyExportBestLocation = sharedPreferences.getBoolean(context.getString(R.string.pref_key_export_best_position),false);
        char decimalSeparator = sharedPreferences.getString(context.getString(R.string.pref_key_decimal_mark),".").charAt(0);
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator(decimalSeparator);
        numberFormat = new DecimalFormat( "############.############" , decimalFormatSymbols);
    }

    /**
     * This method takes care of the actual export.
     * On devices running version prior to Android 4.4 KitKat, this method saves the data to
     * the external storage and shows the path to the user in an AlertDialog
     * In newer Android version an Intent including a FileProvider link is generated.
     * The user can then select an app to handle the intent.
     * The export format is generated according to the format set by the user in the preferences.
     */
    void export(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataLab dataLab = new DataLab(context);
                dataRecordList = new LinkedList<>();
                for (long dataRecordID : dataRecordIDs) {
                    dataRecordList.add(dataLab.readDataRecordFromDatabase(dataRecordID));
                }

                if (onlyExportBestLocation){
                    maxLocationCount = 1;
                } else {
                    maxLocationCount = maxLocationCount();
                }

                maxMeasureDataCount = maxMeasureDataCount();

                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    Intent exportIntent = new Intent(Intent.ACTION_SEND);
                    exportIntent.putExtra(Intent.EXTRA_STREAM, getPrivateStorageFileURI());
                    exportIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    exportIntent.setType("text/csv");
                    try {
                        context.startActivity(exportIntent);
                    } catch (ActivityNotFoundException e) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                                alertDialog.setTitle(context.getString(R.string.no_export_application));
                                alertDialog.setMessage(context.getString(R.string.no_export_application_message));
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        });

                    }
                } else {
                    final String path = saveFile();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                            alertDialog.setTitle(context.getString(R.string.saved_file));
                            alertDialog.setMessage(String.format(context.getString(R.string.saved_file_message),path));
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Maximum number of Locations included in any of the DataRecords
     * @return The number of Location the DataRecord having the most Locations has
     */
    private int maxLocationCount(){
        int maxResult = 0;
        for (DataRecord dataRecord: dataRecordList) {
            if (dataRecord.getLocations().size() > maxResult){
                maxResult = dataRecord.getLocations().size();
            }
        }
        return maxResult;
    }

    /**
     * Maximum number of MeasureData objects included in any of the DataRecords
     * @return The number of MeasureData objects the DataRecord having the most has
     */
    private int maxMeasureDataCount(){
        int maxResult = 0;
        for (DataRecord dataRecord: dataRecordList) {
            if (dataRecord.getMeasureData().size() > maxResult){
                maxResult = dataRecord.getMeasureData().size();
            }
        }
        return maxResult;
    }

    /**
     * Get an URI linking to the exported data in the private storage of this app
     * @return an URI linking to the exported data in the private storage of this app
     */
    private Uri getPrivateStorageFileURI(){
        File exportPath = new File(context.getFilesDir(), "export");
        //noinspection ResultOfMethodCallIgnored
        exportPath.mkdir();
        String filename = "GeoSensor.csv";
        saveToFile(exportPath, filename);
        File exportFile = new File(exportPath, filename);
        return FileProvider.getUriForFile(context, "de.eiketrumann.geosensor.MainActivity", exportFile);
    }

    /**
     * Get an URI linking to the exported data in the public storage of this app
     * @return an URI linking to the exported data in the public storage of this app
     */
    private Uri getExternalStorageFileURI(){
        File exportPath = new File(context.getExternalFilesDir(null), "GeoSensor");
        //noinspection ResultOfMethodCallIgnored
        exportPath.mkdir();
        @SuppressLint("SimpleDateFormat")
        final DateFormat exportDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String filename = "GeoSensor Export "+exportDateFormat.format(new Date())+".csv";
        saveToFile(exportPath, filename);
        File exportFile = new File(exportPath, filename);
        return Uri.fromFile(exportFile);
    }

    /**
     * Save the complete csv table into a file on the external storage
     * @return The absolute path of the exported file
     */
    private String saveFile(){
        File exportPath = new File(Environment.getExternalStorageDirectory(), "GeoSensor");
        //noinspection ResultOfMethodCallIgnored
        exportPath.mkdir();
        @SuppressLint("SimpleDateFormat")
        final DateFormat exportDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String filename = "GeoSensor Export "+exportDateFormat.format(new Date())+".csv";
        saveToFile(exportPath, filename);
        File exportFile = new File(exportPath, filename);
        return exportFile.getAbsolutePath();
    }

    /**
     * Save the table to the filename specified
     * This method might fail on versions after Android 4.4
     * and requires a runtime permission in versions after Android 6.0
     * @param exportPath The path to be used
     * @param filename The filename.
     */
    private void saveToFile(File exportPath, String filename){
        File exportFile = new File(exportPath, filename);
        try {
            //noinspection ResultOfMethodCallIgnored
            exportFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(exportFile);
            outputStream.write(getTable().getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Log.w(this.getClass().getSimpleName(),e.toString());
        }
    }

    /**
     * Get a string containing the complete csv data to be exported
     * @return
     */
    private String getTable(){
        StringBuilder result = new StringBuilder();
        result.append(getTitleLine());
        result.append("\r\n");

        for(DataRecord dataRecord: dataRecordList){
            result.append(getOneLine(dataRecord));
            result.append("\r\n");
        }

        return result.toString();
    }

    /**
     * Generate the column titles for the csv file
     * @return The title line (no trailing newline)
     */
    private String getTitleLine(){
        StringBuilder result = new StringBuilder();

        // General Info
        result.append(DataLab.DataRecordEntry.COLUMN_RECEIVE_TIME);
        result.append(CSV_SEPARATOR);
        result.append(DataLab.DataRecordEntry.COLUMN_COMMENT);
        result.append(CSV_SEPARATOR);
        result.append(DataLab.DataRecordEntry.COLUMN_ARDUINO_SOFTWARE);
        result.append(CSV_SEPARATOR);
        result.append(DataLab.DataRecordEntry.COLUMN_ARDUINO_TIME);
        result.append(CSV_SEPARATOR);

        // Locations
        for (int i = 0; i < maxLocationCount; i++){
            result.append(DataLab.LocationEntry.COLUMN_LATITUDE);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.LocationEntry.COLUMN_LONGITUDE);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.LocationEntry.COLUMN_TIME);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.LocationEntry.COLUMN_PROVIDER);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.LocationEntry.COLUMN_ALTITUDE);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.LocationEntry.COLUMN_ACCURACY);
            result.append("_").append(i).append(CSV_SEPARATOR);
        }

        // MeasureData
        for (int i = 0; i < maxMeasureDataCount; i++){
            result.append(DataLab.MeasureDataEntry.COLUMN_TYPE);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.MeasureDataEntry.COLUMN_NAME);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.MeasureDataEntry.COLUMN_SENSOR);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.MeasureDataEntry.COLUMN_VALUE);
            result.append("_").append(i).append(CSV_SEPARATOR);
            result.append(DataLab.MeasureDataEntry.COLUMN_UNIT);
            result.append("_").append(i).append(CSV_SEPARATOR);
        }

        return result.toString();
    }

    /**
     * Get a line describing the given DataRecord
     * @param dataRecord The DataRecord object containing the data to be exported
     * @return A single line containing the data formatted as csv
     */
    private String getOneLine(DataRecord dataRecord){
        StringBuilder result = new StringBuilder();

        // General Info
        result.append(dateFormat.format(dataRecord.getReceiveTime()));
        result.append(CSV_SEPARATOR);
        result.append(dataRecord.getComment());
        result.append(CSV_SEPARATOR);
        result.append(dataRecord.getArduinoSoftware());
        result.append(CSV_SEPARATOR);
        result.append(dataRecord.getArduinoTime());
        result.append(CSV_SEPARATOR);

        // Locations
        for (int i = 0; i < maxLocationCount; i++){
            if(i < dataRecord.getLocations().size()) {
                Location loc;
                if(onlyExportBestLocation){
                    loc = dataRecord.getLocation();
                } else {
                    loc = dataRecord.getLocations().get(i);
                }
                result.append(numberFormat.format(loc.getLatitude()));
                result.append(CSV_SEPARATOR);
                result.append(numberFormat.format(loc.getLongitude()));
                result.append(CSV_SEPARATOR);
                result.append(dateFormat.format(loc.getTime()));
                result.append(CSV_SEPARATOR);
                result.append(loc.getProvider());
                result.append(CSV_SEPARATOR);
                result.append(numberFormat.format(loc.getAltitude()));
                result.append(CSV_SEPARATOR);
                result.append(numberFormat.format(loc.getAccuracy()));
                result.append(CSV_SEPARATOR);
            } else {
                result.append(CSV_SEPARATOR).append(CSV_SEPARATOR).append(CSV_SEPARATOR)
                        .append(CSV_SEPARATOR).append(CSV_SEPARATOR).append(CSV_SEPARATOR);
            }

        }

        // MeasureData
        for (int i = 0; i < maxMeasureDataCount; i++){
            if(i < dataRecord.getMeasureData().size()) {
                MeasureData measureData = dataRecord.getMeasureData().get(i);
                result.append(measureData.getType());
                result.append(CSV_SEPARATOR);
                result.append(measureData.getName());
                result.append(CSV_SEPARATOR);
                result.append(measureData.getSensor());
                result.append(CSV_SEPARATOR);
                result.append(numberFormat.format(measureData.getValue()));
                result.append(CSV_SEPARATOR);
                result.append(measureData.getUnit());
                result.append(CSV_SEPARATOR);
                } else {
                result.append(CSV_SEPARATOR).append(CSV_SEPARATOR).append(CSV_SEPARATOR)
                        .append(CSV_SEPARATOR).append(CSV_SEPARATOR);
            }
        }
        return result.toString();
    }
}
