package de.uni_bremen.comnets.geosensor;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * The app class has been modified in order to allow arbitrary modules to get the application context.
 * Created by Eike Trumann on 01.03.2017.
 */

public class App extends Application {
    // This solution has been inspired by
    // http://stackoverflow.com/questions/2002288/static-way-to-get-context-on-android

    // This should not be a leak as the application Context has the same lifecycle as the process
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    /**
     * This should be the very first method called when the application ist started
     * It saves the Application context for use by classes that don't have a context in their own
     */
    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();
    }

    /**
     * Get the application context
     * @return The application context
     * @throws IllegalStateException thrown if the application has not yet been created
     *      should not occur according to the Android application lifecycle
     */
    public static Context getContext() throws IllegalStateException{
        if(context == null){
            throw new IllegalStateException("The application context has not been initialized yet");
        }
        return context;
    }
}
