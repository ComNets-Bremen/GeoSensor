package de.uni_bremen.comnets.geosensor;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Date;

/**
 * The filter state is a global state representing which data records should be visualised.
 * It also contains information regarding the sort order.
 * Created by Eike on 05.03.2017.
 */

class FilterState {
    /** Message used in Broadcasts regarding the filter */
    final static String FILTER_CHANGED = "filterChanged";
    private static boolean isFilterSet = false;
    private static Date startTime = new Date(0);
    private static Date endTime = new Date (Long.MAX_VALUE);
    /** this is the sort order: oldest or newest first */
    private static boolean oldestFirst = false;

    /**
     * Resets the filter to show all DataRecords
     */
    static void resetFilter(){
        startTime = new Date(0);
        endTime = new Date (Long.MAX_VALUE);
        isFilterSet = false;
        datasetChangedIntent();
    }

    /**
     * Set the sort order to newest first
     */
    static void orderNewestFirst(){
        oldestFirst = false;
        datasetChangedIntent();
    }

    /** Set the sort order to oldest first */
    static void orderOldestFirst(){
        oldestFirst = true;
        datasetChangedIntent();
    }

    /**
     * @return true if the sortOrder is set to oldest first
     */
    static boolean isOldestFirst(){
        return oldestFirst;
    }

    /**
     * @return true if a filter is set
     */
    static boolean isFilterSet() {
        return isFilterSet;
    }

    /**
     * @return the start time (older elements will not be shown). Zero if not set.
     */
    static Date getStartTime() {
        return startTime;
    }

    /**
     * Set the start time of the filter (older Items will not be shown)
     * @param startTime A Date set to the time to be set as the start time
     */
    static void setStartTime(Date startTime) {
        isFilterSet = true;
        datasetChangedIntent();
        FilterState.startTime = startTime;
    }

    /**
     * @return the end time (newer items will not be shown). Zero if not set.
     */
    static Date getEndTime() {
        return endTime;
    }

    /**
     * Set the end time of the filter (newer Items will not be shown)
     * @param endTime A Date set to the time to be set as the end time
     */
    static void setEndTime(Date endTime) {
        isFilterSet = true;
        datasetChangedIntent();
        FilterState.endTime = endTime;
    }

    /**
     * Send a broadcast notifying receivers when the filter was changed
     */
    private static void datasetChangedIntent(){
        Intent notifyDataSetChanged = new Intent(FILTER_CHANGED);
        LocalBroadcastManager.getInstance(App.getContext()).sendBroadcast(notifyDataSetChanged);
    }




}
