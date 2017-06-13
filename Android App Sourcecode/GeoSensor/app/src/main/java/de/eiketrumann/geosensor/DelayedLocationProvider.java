package de.eiketrumann.geosensor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;

/**
 * Copyright Eike Trumann 12.05.15
 * Modified for GeoSensor 14.02.17
 * All rights reserved
 *
 * This code should not be considered a work for the bachelor's thesis as it was submitted as part
 * of a university project before.
 *
 * The Delayed Location Provider asynchronuosly requests a location from Googles FusedLocationProvider.
 * This implementation is only used
 */
class DelayedLocationProvider implements LocationListener {
    private final long dataRecordID;
    private final DataLab dataLab;
    private final GoogleApiClient mGoogleApiClient;

    /**
     * Starts a DelayedLocationProvider
     * @param dataRecordID the location will be added to this dataRecord entry in the database when
     *                     it is ready
     * @param dataLab the dataLab that should be used to access the database
     * @param apiClient a Google API client as necessary to get access to the fused location provider
     * @param appContext the application context (which is used to control permissions)
     */
    DelayedLocationProvider(long dataRecordID, DataLab dataLab, GoogleApiClient apiClient,
                                   Context appContext) {
        this.dataRecordID = dataRecordID;
        this.dataLab = dataLab;
        mGoogleApiClient = apiClient;

        /* On newer versions we must check for permissions, the user is asked to grant permissions
         * when he activates any location features, but he may disable location access in the system
         * settings*/
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(appContext,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appContext,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
        {
                Log.i(this.getClass().getCanonicalName(), "The DelayedLocationProvider has been " +
                        "called without having the location permissions granted");
            return;
        }

        /* If the last known location is rather new it is used instead of waiting for a new one
        * this is useful if someone has location updated enabled */
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (lastKnownLocation != null && new Date().getTime() - lastKnownLocation.getTime() < 15000
                && lastKnownLocation.getAccuracy() < 10){
            dataLab.addLocation(lastKnownLocation, dataRecordID);
        } else {
            /* This configuration means if we can not get high accuracy, we get the best the API can
            do after 40 seconds*/
            LocationRequest mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setMaxWaitTime(40000)
                    .setNumUpdates(1).setExpirationDuration(60000);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        }
    }

    /**
     * This method is called by the location provider when the location is fixed
     * @param location the current position of the device
     */
    @Override
    public void onLocationChanged(Location location) {
        dataLab.addLocation(location,dataRecordID);
        unregisterListener();
    }

    /**
     * Unregister the location updates for this listener
     */
    private void unregisterListener() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
}
