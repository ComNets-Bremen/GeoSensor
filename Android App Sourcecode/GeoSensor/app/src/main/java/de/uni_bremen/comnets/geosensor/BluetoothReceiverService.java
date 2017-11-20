package de.uni_bremen.comnets.geosensor;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

/**
 * The BluetoothReceiverClass is a state-based utility for connecting serial bluetooth devices.
 * It was designed and tested to work with the HC-05 bluetooth serial pass-through module.
 * This Service might be started at all times as it has states for when there is no bluetooth or
 * the service itself should be disabled.
 *
 * This service includes notifications when it os searching or connected.
 * The notifications not only inform the user about the current connection state, but also convince
 * the Android system to keep the software alive.
 * @author Eike Trumann
 */
public class BluetoothReceiverService extends Service
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    // set at the beginning, there is normally no way to return to it
    public final static int STATE_INITIALIZING = -2;
    // This state is set if Bluetooth has been disabled system-wide
    // The service will wait for a broadcast informing it that Bluetooth is turned on
    public final static int STATE_BLUETOOTH_OFF = -1;
    // disabled in the application settings
    public final static int STATE_DISABLED = 0;
    // searching for the Bluetooth device selected by the user
    public final static int STATE_SEARCHING = 1;
    // connected to the device selected in the preferences
    public final static int STATE_CONNECTED = 2;

    // This are the intents that can be sent to control the transducer device
    public final static String INTENT_ACQUIRE_MEASURE_DATA = "acquireMeasureData";
    public final static String INTENT_RESEND_MEASURE_DATA = "resendMeasureData";
    public final static String INTENT_DATA_RECORD_RECEIVED = "dataRecordReceived";

    // the id given to the notification manager for the ongoing notifications (arbitrary)
    private final static int ONGOING_NOTIFICATION_ID = 1;

    // this state is set only at the beginning
    private volatile int state = STATE_INITIALIZING;

    // objects used for the bluetooth communication
    // declared volatile as different threads are used for the bluetooth operations
    private volatile DataLab dataLab;
    private final BluetoothAdapter bluetoothAdapter;
    private volatile BluetoothDevice bluetoothDevice;
    private volatile BluetoothSocket bluetoothSocket;
    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;

    // this LocationListener is used if the gps position is logged by the Android device
    private final BluetoothServiceLocationListener locationListener
            = new BluetoothServiceLocationListener();

    // as Bluetooth works based on global broadcasts we need a BroadcastReceiver to get it's state
    private final BroadcastReceiver broadcastReceiver;

    private volatile Thread lastConnectorThread = null;

    /**
     * This constructor will invisibly be called when starting the intent for this service.
     * It only sets the final (in order to ease synchronisation) variables
     * Note that there is no context (this) available in the constructor
     */
    public BluetoothReceiverService() {
        super();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        broadcastReceiver = new BluetoothStatusListener();

        // In this case the app is very much useless
        if (bluetoothAdapter == null){
            Log.wtf(this.getClass().getCanonicalName(),"There is no bluetooth on this device.");
            throw new UnsupportedOperationException("No Bluetooth on this device");
        }
    }

    /**
     * This is the first regular lifecycle method.
     * It sets all the variables read from the preferences
     * and initializes the OnSharedPreferenceChangeListener and the Broadcast Receiver
     */
    @Override
    public synchronized void onCreate(){
        super.onCreate();

        // the DataLab is used to write to the database
        // is needs the context and thus can not be created in the constructor
        dataLab = new DataLab(getApplicationContext());

        // in order to not always ask the shared preferences before something is done,
        // this class holds local variables representing some relevant preferences
        // to update them we use the OnSharedPreferenceChangeListener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (!sharedPreferences.contains(getString(R.string.pref_key_select_bluetooth))){
            //throw new IllegalStateException("The device to connect to must be set before starting the Receiver");
            sharedPreferences.edit().putBoolean(getString(R.string.pref_key_bluetooth_basic),false).apply();
            state = STATE_DISABLED;
            Intent firstStartIntent = new Intent(this, FirstStartActivity.class);
            firstStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(firstStartIntent);
        }

        // the BroadcastReceiver receives all relevant Bluetooth-related broadcasts
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        if (android.os.Build.VERSION.SDK_INT > 11) {
            // This is only available in newer sdk versions, normally the ACL_CONNECTED does the job
            intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        }

        // the BroadcastReceiver has the same lifecycle as this service and is therefore registered
        // programmatically
        registerReceiver(broadcastReceiver, intentFilter);

        // Also include our application specific intent to get a Measurement
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothReceiverService.INTENT_ACQUIRE_MEASURE_DATA);
        intentFilter.addAction(BluetoothReceiverService.INTENT_RESEND_MEASURE_DATA);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

        // if the service is disabled we stop here, thus it is ready for activation when needed
        if (!sharedPreferences.getBoolean(getString(R.string.pref_key_bluetooth_basic),false)){
            changeState(STATE_DISABLED);
            return;
        }

        // this function shows a system dialog to the user if bluetooth is disabled
        // this is the only time we ask the user, if he disables bluetooth afterwards
        // this service will simply pause until it is switched on again
        checkBluetoothEnabled();

        // at this point everything is ready for prime time and we can try to connect
        if(state == STATE_INITIALIZING){
            initialize();
        }
    }

    /**
     * this function is there to always returns START_STICKY as this service should not be destroyed
     * if it is not doing anything as in that case it would be waiting for a broadcast or
     * a shared preference change
     *
     * @param intent see super class
     * @param flags see super class
     * @param startId see super class
     * @return always returns START_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /**
     * this always returns null as you can not bind to this service
     * @param intent ignored
     * @return always returns null
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * if for some reason this service must be destroyed the resources are closed
     */
    @Override
    public void onDestroy(){
        Log.w(this.getClass().getSimpleName(),"BluetoothReceiverService gets destroyed");

        if (state == STATE_CONNECTED){
            disconnect();
        }
        stopForeground(true);
        unregisterReceiver(broadcastReceiver);

        super.onDestroy();
    }

    /**
     * this method is not directly part of the android lifecycle,
     * but it represents a state change from STATE_INITIALIZING, STATE_BLUETOOTH_OF and
     * STATE_DISABLED into STATE_SEARCHING
     *
     * As a first measure it will try to just connect to the device set in the application settings.
     * The deviceFound method will then handle the rest of the state transition.
     *
     * At the same time STATE_SEARCHING is a state showing an ongiong notification to the user in
     * order to inform him about what is done but also in order to prevent Android from destroying
     * this service (the notification gives a higher priority).
     */
    private void initialize(){
        changeState(STATE_SEARCHING);
        // this device address string represents the only bluetooth device we try to connect to
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = sharedPreferences.getString(getString(R.string.pref_key_select_bluetooth), "");
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        deviceFound(bluetoothDevice);
    }

    /**
     * This is called if for some reason the connection was inadvertently lost.
     * It closes the associated resources.
     */
    private synchronized void disconnect(){
        try{
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
        } catch (IOException | NullPointerException ignored) {}

        if (state == STATE_SEARCHING || state == STATE_CONNECTED){
            initialize();
        }
    }

    /**
     * This is called if the user decides to disable the service by either changing the application
     * setting or disabling bluetooth system-wide. The effect is clearly visible to the user as the
     * ongoing notification ceases to exist. The state is transitioned to STATE_DISABLED.
     */
    private void disable(){
        // This must be done first as otherwise the BroadcastReceiver might have a race condition
        changeState(STATE_DISABLED);

        synchronized (this){
            try{
                outputStream.close();
                inputStream.close();
                bluetoothSocket.close();
            } catch (IOException | NullPointerException ignored) {}
        }
    }

    /**
     * This method is used to build the connection to the device selected by the user.
     * If can be called including a Bluetooth device build based on the address string or a device
     * found during bluetooth device discovery. It will only try to connect if the device matches
     * the one selected by the user in the application settings.
     * This method uses a separate thread as some of the bluetooth operations are blocking
     * @param bluetoothDevice the device found
     */
    private void deviceFound(final BluetoothDevice bluetoothDevice){
        // Get device address from settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = sharedPreferences.getString(getString(R.string.pref_key_select_bluetooth), "");

        // Do not connect if it is not the chosen device
        if(!bluetoothDevice.getAddress().equalsIgnoreCase(deviceAddress)) {
            return;
        }
        this.bluetoothDevice = bluetoothDevice;

        // don't block the UI thread to long
        // effectively this code is a thread singleton
        if(lastConnectorThread == null || !lastConnectorThread.isAlive()){
            lastConnectorThread = new Thread(new ConnectorRunnable());
            lastConnectorThread.start();
        }

    }

    /**
     * This Runnable is used to perform the steps necessary to connect to the bluetooth device
     * It synchronizes most of its work on the service, but accordingly does not block the UI thread
     * for other parts of the app. The runnable tries to establish the connection on it's start and
     * retries every five seconds until the connection can be established. As this might be consume
     * significant amounts of energy, this is only done while the user is actively aware of the
     * search process running.
     */
    private class ConnectorRunnable implements Runnable{
        @Override
        public void run() {
            while(true) {
                synchronized (BluetoothReceiverService.this) {
                    // This thread should not be started while we are not searching for a device
                    if(state != STATE_SEARCHING){
                        return;
                    }
                    // if the device can not be connected for some reason, we get an IOException
                    try {
                        // connecting works more reliable if there is no concurrent discovery
                        bluetoothAdapter.cancelDiscovery();
                        SharedPreferences sharedPreferences = PreferenceManager.
                                getDefaultSharedPreferences(BluetoothReceiverService.this);
                        bluetoothDevice = bluetoothAdapter.getRemoteDevice(
                                sharedPreferences.getString(getString(R.string.pref_key_select_bluetooth), ""));
                        // this uuid is a bluetooth standard for the serial communication profile
                        final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                        if (android.os.Build.VERSION.SDK_INT < 11) {
                            // creating a secure socket will normally work also in newer versions
                            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        } else {
                            // however creating an insecure socket is more reliable
                            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                        }

                        // this is blocking and takes some time (also the ACL_CONNECTED broadcast
                        // will be fired)
                        bluetoothSocket.connect();

                        // the streams are the standard way to get data in and out of the socket
                        outputStream = bluetoothSocket.getOutputStream();
                        inputStream = bluetoothSocket.getInputStream();

                        // if we are here without something throwing, the device is connected
                        // the user is informed by an ongoing notification
                        changeState(STATE_CONNECTED);

                        // the incoming data is then handled by a different thread
                        startReceiverThread();

                    } catch (IOException e) {
                        // if they are not initialized, it's no problem if we can't close them
                        try {
                            outputStream.close();
                            inputStream.close();
                        } catch (IOException | NullPointerException ignored) {
                        }
                        // the bluetooth socket is closed separately as it may be initialized while the
                        // others aren't
                        try {
                            bluetoothSocket.close();
                        } catch (IOException | NullPointerException ignored) {
                        }
                    }
                }
                // The sleep must be outside the synchronized part
                try{
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * This method starts a thread that repeatedly polls the data stream to get hold of the data
     * transmitted. The thread does poll every two seconds which is sufficient as the operating
     * system buffers the streams. According to the protocol, when an End of Text character is
     * received, the incoming data is given to the parser.
     */
    private void startReceiverThread(){
        Thread receiverWorkerThread = new Thread(new Runnable(){
            @Override
            public void run() {
                // The size limit is somewhat arbitrary, but should be sufficient for any use
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024*1024*2);
                Log.d(this.getClass().getSimpleName(),"receiverWorkerThread started");

                // The thread can be interrupted if the system decides to kill the service
                while(state == STATE_CONNECTED && !Thread.currentThread().isInterrupted()) {
                    try {
                        boolean byteAvailable = inputStream.available() > 0;
                        if (byteAvailable) {
                            int newByte = inputStream.read();
                            // -1 means there is no data to be read in the stream
                            if (newByte == -1) {
                                try {
                                    // This sleep should not be too long as it directly affects
                                    // the time needed until a message is received correctly
                                    Thread.sleep(2000);
                                } catch (InterruptedException ignored) {
                                }
                                Thread.yield();
                                continue;
                            }
                            // The ASCII End of Text character (0x03) marks the end of a JSON object
                            if (newByte == 0x03) {
                                handleCompleteJSON(byteBuffer.array());
                                // As the old message should be handled now, we need an empty buffer
                                // ByteBuffers can not just be cleared as old data might remain
                                byteBuffer = ByteBuffer.allocate(1024*1024*2);
                                // The loop continues as the End of Text byte does not need to be
                                // put into the buffer
                                continue;
                            }
                            try {
                                byteBuffer.put((byte) newByte);
                            } catch (BufferOverflowException e) {
                                Log.wtf(this.getClass().getCanonicalName(),"The message sent via " +
                                        "bluetooth was insanely large.");
                                byteBuffer = ByteBuffer.allocate(1024*1024*8);
                            }
                        } else {
                            Thread.yield();
                        }
                    } catch (IOException | NullPointerException e) {
                        // If another thread closes the socket we get an IOException, but don't want
                        // to reconnect
                        synchronized (BluetoothReceiverService.this) {
                            if (state == STATE_CONNECTED) {
                                disconnect();
                            }
                            break;
                        }
                    }
                }
            }
        });
        receiverWorkerThread.start();
    }

    /**
     * This method handled the incoming data messages.
     * @param jsonBytes The bytes received via the bluetooth connection
     */
    private synchronized void handleCompleteJSON(final byte[] jsonBytes){
        // The DataRecord that is generated from the JSON input
        DataRecord dataRecord;
        // The bytes can be handled as a string
        String jsonString = "";
        try {
            // This truncates bytes received before the JSON content we want to handle
            jsonString = new String(jsonBytes, "US-ASCII").trim();
            // in this case it is not a valid JSON
            if(!jsonString.contains("{"))
                return;
            // everything before the first { is garbage
            jsonString = jsonString.substring(jsonString.indexOf("{"));
        } catch (UnsupportedEncodingException e) {
            // This app does not work on devices not supporting the ASCII charset
            Log.wtf(this.getClass().getSimpleName(), "THIS SYSTEM DOES NOT SUPPORT ASCII CHARSET");
        }
        try {
            // The data is given to the DataLab to handle the parsing
            // A DataRecord which is not jet stored in the database is returned
            dataRecord = dataLab.parseJSON(jsonString , new Date());
        } catch (ParseException p) {
            Log.w(this.getClass().getSimpleName(),"JSON Parsing from Bluetooth failed. "+ p.toString() +" This is the " +
                    "problematic message: \""+jsonString+"\"");
            // A negative acknowledgement is sent to the Arduino
            requestResend();
            return;
        }

        // The shared preferences contain the
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        /* // This is only useful when the performance of the gps should be compared
        Boolean gpsEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_gps_background), false);
        if (gpsEnabled){
            synchronized (this){
                if(locationListener != null && locationListener.last != null) {
                    dataRecord.addLocation(locationListener.last);
                }
            }
        } */

        // The dataRecord is actually saved to the database
        long id = dataLab.saveDataRecord(dataRecord);

        acknowledgeReceive();

        // Asynchronously adding a location is done by the DataLab as the Location should be saved
        // directly into the database
        if(sharedPreferences.getBoolean(
                getString(R.string.pref_key_location_switch),true)){
            dataLab.requestDeviceLocationAdded(id);
        }

        // This broadcast is used to redraw the
        Intent notifyDataSetChanged = new Intent(INTENT_DATA_RECORD_RECEIVED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(notifyDataSetChanged);

        if(sharedPreferences.getBoolean(
                getString(R.string.pref_key_incoming_data_notification),true)){
            showReceiveNotification(dataRecord);
        }
    }

    /**
     * When a message has been received correctly, an acknowledge is sent back to the Arduino
     */
    private void acknowledgeReceive(){
        try{
            outputStream.write(0x06); // ACK
        } catch (IOException ignored) {
        }
    }

    /**
     * When the parsing fails for some reason, the App requests the data to be resend.
     * The Arduino software may or may not include the means to do so.
     */
    private void requestResend(){
        try{
            outputStream.write(0x15); // NAK
        } catch (IOException ignored) {
        }
    }

    /**
     * This method issues a request to the user to enable bluetooth.
     */
    private void checkBluetoothEnabled(){
        if (!bluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableIntent);
            state = STATE_BLUETOOTH_OFF;
        }
    }

    /**
     * Change the state (among the constant ones specified in this class).
     * @param newState On of the state constants defined for use in this class
     */
    private void changeState(int newState){

        state = newState;
        refreshNotification();

        // The background GPS is active while the bluetooth module is connected
        synchronized (locationListener){
            if (state == STATE_CONNECTED){
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                if(sharedPreferences.getBoolean(getString(R.string.pref_key_gps_background),false)){
                    enableGPS();
                }
            } else {
                disableGPS();
            }
        }
    }

    /**
     * When the state changes, the ongoing notification shown to the user also needs to change
     */
    private void refreshNotification(){
        if (state == STATE_SEARCHING){
            startForeground(ONGOING_NOTIFICATION_ID,buildConnectingNotification());
            return;
        }

        if (state == STATE_CONNECTED){
            startForeground(ONGOING_NOTIFICATION_ID,buildConnectedNotification());
            return;
        }

        stopForeground(true);
    }

    private Notification buildConnectingNotification(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setOngoing(true);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.drawable.ic_bluetooth_searching_black_24dp);
        } else {
            notificationBuilder.setSmallIcon(android.R.drawable.ic_menu_search);
        }
        notificationBuilder.setContentTitle(getString(R.string.bluetooth_searching));

        // From Android developers
        Intent resultIntent = new Intent(this, SettingsActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notificationBuilder.setContentIntent(resultPendingIntent);

        return notificationBuilder.build();
    }

    private Notification buildConnectedNotification(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setOngoing(true);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.drawable.ic_bluetooth_connected_black_24dp);
        } else {
            notificationBuilder.setSmallIcon(android.R.drawable.ic_dialog_info);
        }
        notificationBuilder.setContentTitle(getString(R.string.bluetooth_active));
        notificationBuilder.setContentText(String.format(getString(R.string.bluetooth_active_text),bluetoothDevice.getName()));

        // From Android developers
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notificationBuilder.setContentIntent(resultPendingIntent);

        return notificationBuilder.build();
    }

    /**
     * This method shows a big text notification including
     * @param dataRecord The data record containing the data to be shown in the big text
     */
    private void showReceiveNotification(DataRecord dataRecord) {
        StringBuilder text = new StringBuilder();
        for (MeasureData measureData : dataRecord.getMeasureData()){
            text.append(measureData.toString());
            text.append(System.getProperty("line.separator"));
        }
        // The last one does not need the line.separator
        text.deleteCharAt(text.length()-1);

        Intent resultIntent = new Intent(this, DetailsActivity.class);
        resultIntent.putExtra("id",dataRecord.getDatabaseID());

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = taskStackBuilder.getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT);

        android.support.v4.app.NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.data_received))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                        .setContentIntent(resultPendingIntent);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.drawable.ic_add_location_white_24dp);
        } else {
            notificationBuilder.setSmallIcon(android.R.drawable.ic_menu_save);
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(2, notificationBuilder.build());
    }

    private void enableGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(locationManager == null){
            // The device is not able to provide locations
            return;
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        String bestLocationProvider = locationManager.getBestProvider(criteria, false);

        if(bestLocationProvider == null){
            return;
        }

        if (!locationManager.isProviderEnabled(bestLocationProvider)) {
            Intent locationIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            locationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(locationIntent);
        }

        if (PermissionsManager.isPermissionGranted(this, "ACCESS_FINE_LOCATION")) {
            //noinspection ResourceType
            locationListener.last = locationManager.getLastKnownLocation(bestLocationProvider);
            if(Looper.myLooper() == null){
                Looper.prepare();
            }
            //noinspection ResourceType
            locationManager.requestLocationUpdates(bestLocationProvider, 5000, 1, locationListener);
        }
    }

    private void disableGPS() {
        if(locationListener != null) {
            try {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.i(this.getClass().getCanonicalName(),"Disabling GPS failed",e);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        switch(key){
            case "pref_key_bluetooth_basic":
                Thread changeStateThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!sharedPreferences.getBoolean(getString(R.string.pref_key_bluetooth_basic), false)) {
                            disable();
                        } else {
                            synchronized(BluetoothReceiverService.this) {
                                if (!sharedPreferences.contains(getString(R.string.pref_key_select_bluetooth))){
                                    sharedPreferences.edit().putBoolean(getString(R.string.pref_key_bluetooth_basic),false).apply();
                                    changeState(STATE_DISABLED);
                                    Intent firstStartIntent = new Intent(BluetoothReceiverService.this, FirstStartActivity.class);
                                    firstStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(firstStartIntent);
                                } else {
                                    checkBluetoothEnabled();
                                    if (state != STATE_SEARCHING && state != STATE_CONNECTED) {
                                        initialize();
                                    }
                                }
                            }
                        }
                    }
                });
                changeStateThread.start();
                return;

            case "pref_key_select_bluetooth":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disable();

                        // We yield here in order to let the receiver thread wake up in case it is active
                        Thread.yield();

                        if(sharedPreferences.getBoolean(getString(R.string.pref_key_bluetooth_basic), false)) {
                            if(state != STATE_SEARCHING && state != STATE_CONNECTED){
                                initialize();
                            }
                        }
                    }
                }).start();
                return;

            case "pref_key_gps_background":
                changeState(state);
                return;
        }
    }

    private class BluetoothServiceLocationListener implements LocationListener{
        Location last = null;

        @Override
        public void onLocationChanged(Location location) {
            last = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public class BluetoothStatusListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(BluetoothReceiverService.this.getClass().getCanonicalName(), "Bluetooth: " + action);

            synchronized (BluetoothReceiverService.this) {
                switch (action) {
                    case (BluetoothAdapter.ACTION_STATE_CHANGED):
                        switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                            case (BluetoothAdapter.STATE_TURNING_OFF):
                            case (BluetoothAdapter.STATE_OFF):
                                disable();
                                return;
                            case (BluetoothAdapter.STATE_ON):
                                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                                boolean bluetooth_basic = pref.getBoolean(context.getString(
                                        R.string.pref_key_bluetooth_basic), false);
                                if (bluetooth_basic) {
                                    if(state != STATE_SEARCHING && state != STATE_CONNECTED){
                                        initialize();
                                    }
                                }
                                return;
                        }
                        break;

                    case (BluetoothDevice.ACTION_ACL_DISCONNECTED): {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        SharedPreferences sharedPreferences = PreferenceManager.
                                getDefaultSharedPreferences(BluetoothReceiverService.this);
                        String deviceAddress = sharedPreferences.getString(getString(
                                R.string.pref_key_select_bluetooth), "");
                        if (device.getAddress().equalsIgnoreCase(deviceAddress)) {
                            disconnect();
                        }
                        return;
                    }

                    case (INTENT_ACQUIRE_MEASURE_DATA): {
                        if(state == STATE_CONNECTED) {
                            try{
                                outputStream.write(0x11); // DC1 Device Control 1
                            } catch (IOException ignored) { }
                        } else {
                            Toast.makeText(BluetoothReceiverService.this, getString(
                                    R.string.acquireNotConnected), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    case (INTENT_RESEND_MEASURE_DATA): {
                        if(state == STATE_CONNECTED) {
                            requestResend();
                        } else {
                            Toast.makeText(BluetoothReceiverService.this, getString(
                                    R.string.acquireNotConnected), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                }
            }
        }
    }
}

