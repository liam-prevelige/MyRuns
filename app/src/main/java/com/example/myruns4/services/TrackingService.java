package com.example.myruns4.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myruns4.R;
import com.example.myruns4.activities.ManualEntryActivity;
import com.example.myruns4.activities.MapActivity;
import com.example.myruns4.fragments.StartFragment;
import com.example.myruns4.models.ExerciseEntry;
import com.example.myruns4.models.Login;
import com.example.myruns4.utils.ExerciseEntryDbHelper;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

/**
 * Create two background threads, one to use AndroidRecognition to determine a user's activity based
 * off of their motion, and another thread to get the current location of the user.
 *
 * Send both values to MapActivity to be loaded onto Map, and when service is closed, if user clicks save
 * then create new Exercise Entry in database
 */
public class TrackingService extends IntentService {
    private static final String CHANNEL_ID = "tracking_location";
    private static final String CHANNEL_NAME = "GPS/Automatic Entry";

    // Default values to be used for specific (initial) map conditions
    private static final String DEFAULT_NAME = "no name";
    private static final String DEFAULT_INPUT = "Automatic";
    private static final String DEFAULT_ACTIVITY = "Still";
    public static final String FINISH_KEY = "finish";

    // Multipliers for converting between units
    private static final double CALORIES_PER_METER = 0.06;
    private static final double METERS_TO_KILOMETERS = .001;
    private static final double METERS_TO_MILES = 0.000621371;
    private static final double METERS_TO_FEET = 3.28084;

    private static final int NOTIFICATION_ID = 1;
    private static final int PENDING_INTENT_REQUEST_CODE = 1;
    private static final long FAST_INTERVAL = 1000;

    private final IBinder mBinder = new DownloadBinder();
    private ExerciseEntry currentExerciseEntry;
    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent activityTypePendingIntent;

    // Instance variables to help track values for updating map and creating ExerciseEntry
    private String inputType;
    private String activityName;
    private double totalDistance;
    private double averageSpeed;
    private double calorieCount;
    private Double startAltitude;
    private long totalDurationSeconds;
    private Date beginningOfTravelTime;
    private Location priorLocation;
    private Login login;
    private Location newLocation;
    private Date startTime;

    private HashMap<String,Long> activitiesFrequency;   // Duration for each measured activity
    ArrayList<LatLng> locationsToBeAdded;   // Locations currently waiting to be added by MapActivity

    // Necessary default constructor
    public TrackingService(){
        super(DEFAULT_NAME);
    }

    public TrackingService(String name) {
        super(name);
    }

    public void onCreate(){
        super.onCreate();
    }

    /**
     * Pull information sent to TrackingService and begin chain of major processes/threads
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("TEST", "onStartCommand()");

        login = new Login(getApplicationContext());     // SharedPreference for this user

        activitiesFrequency = new HashMap<String,Long>();   // Will store duration of each measured activity

        if(intent!=null) {
            processIntentContents(intent);
        }

        // Ensure the SharedPreference values for stopping the service & saving the Entry are back to false
        login.setShouldStop(false);
        login.setSavePressed(false);

        // Send notification that user location is being stored, initialize tracking process afterward
        if(!login.getHasNotified()) {
            setupNotification();
            login.setHasNotified(true);
        }

        return START_STICKY;    // Sticky notification
    }

    /**
     * Get values for input type and activity from intent if they exist, otherwise set to default
     * values
     */
    private void processIntentContents(Intent intent){
        // Grab the input type (GPS/Automatic) from intent, if it doesn't exist set to default value
        if ((inputType = intent.getStringExtra(StartFragment.INPUT_TYPE_KEY)) == null) {
            inputType = DEFAULT_INPUT;
        }
        else intent.removeExtra(StartFragment.INPUT_TYPE_KEY);      // No longer necessary - remove from storage

        // Grab the activity type from intent, if it doesn't exist set to default value
        if ((activityName = intent.getStringExtra(StartFragment.ACTIVITY_NAME_KEY)) == null) {
            activityName = DEFAULT_ACTIVITY;
        }
        else intent.removeExtra(StartFragment.ACTIVITY_NAME_KEY);   // No longer necessary - remove from storage

        // Set to defaults if "Automatic" input - no actual activity processing has occurred yet
        if (inputType.equals(DEFAULT_INPUT))    activityName = DEFAULT_ACTIVITY;
    }

    /**
     * Notify user that app is storing location and measuring activity in background. Allow user to
     * navigate back to MapActivity if app is left and notification is pressed
     */
    public void setupNotification() {
        // Bring user back to MapActivity if/when notification pressed
        Intent notificationIntent = new Intent(this.getApplicationContext(), MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        // Build a notifier that informs user of processes and app
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(CHANNEL_NAME)
                .setContentText("Currently tracking your location")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);     // Show on screen now

        initExerciseEntry();    // Now that user has been notified of intentions, begin tracking process
    }

    /**
     * Create the shell of an ExerciseEntry to be saved if the user wants to store the current GPS/
     * Automatic activity session
     */
    private void initExerciseEntry(){
        currentExerciseEntry = new ExerciseEntry();

        // Initialize the input type which won't change over course of service
        currentExerciseEntry.setmInputType(StartFragment.getIndexofInputType(inputType));
        if(inputType.equals("GPS")){        // If it's GPS then user selected their own activity, save in entry as well
            currentExerciseEntry.setmActivityType(activityName);
        }
        else if (inputType.equals("Automatic")){    // No activity recognition processing has occurred, store default activity ("Still")
            currentExerciseEntry.setmActivityType(DEFAULT_ACTIVITY);
        }

        // Store default values, will be updated later
        currentExerciseEntry.setmAvgSpeed(0);
        currentExerciseEntry.setmClimb(0);
        currentExerciseEntry.setmDistance(0);

        // Begin getting location information from user
        startLocationUpdate();
    }

    /**
     * Set up a LocationRequest background thread to get user's location information via callback on
     * a set time interval
     */
    private void startLocationUpdate(){
        locationsToBeAdded = new ArrayList<LatLng>();   // Store values waiting to be added by MapActivity

        // Now that location processing will begin, record the starting time to help get Entry info later
        if(beginningOfTravelTime == null)   beginningOfTravelTime = Calendar.getInstance().getTime();

        // Request location updates with high accuracy and frequency (at expense of battery life)
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(MapActivity.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FAST_INTERVAL);

        // Assuming user hasn't ended the service using backbutton/save, begin requesting location updates
        // and send results to callback
        if(!login.getShouldStop()) {
            getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }

        // Start activity recognition updates
        startActivityUpdate();
    }

    /**
     * Use Google's ActivityRecognitionClient to determine what activity a user is engaged in. Request
     * updates on semi-consistent time intervals.
     */
    private void startActivityUpdate(){
        activityRecognitionClient = new ActivityRecognitionClient(this);

        // Only use activityrecognition if the user hasn't selected GPS version, where they would
        // have already specified their activity
        if(inputType.equals("Automatic")) {
            Intent mIntentService = new Intent(this, TrackingService.class);

            activityTypePendingIntent = PendingIntent.getService(this, PENDING_INTENT_REQUEST_CODE,
                    mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);

            activityRecognitionClient.requestActivityUpdates(MapActivity.UPDATE_INTERVAL, activityTypePendingIntent);
        }
    }

    /**
     * Callback to get and process location updates, sending information to MapActivity
     */
    private LocationCallback mLocationCallback = new LocationCallback(){

        /**
         * Once a location has been found, process and send to MapActivity
         */
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            double currentSpeed = updateLocationInformation(locationResult);

            if(login.getMapActivityStarted()) {
                sendInfoToMapActivity(currentSpeed);
            }
            priorLocation = newLocation;

            if(login.getShouldStop()){
                if(login.getSavePressed()){
                    login.setMapRotated(false);
                    handleServiceSave();
                }
                getFusedLocationProviderClient(getApplicationContext()).removeLocationUpdates(mLocationCallback);
            }
        }

        /**
         * Helper method to update variables given new location information
         */
        private double updateLocationInformation(LocationResult locationResult){
            newLocation = locationResult.getLastLocation();
            if(startAltitude==null) startAltitude = newLocation.getAltitude();
            if(priorLocation!=null) totalDistance += newLocation.distanceTo(priorLocation);

            LatLng currLoc = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());

            locationsToBeAdded.add(currLoc);
            currentExerciseEntry.addToLocationList(currLoc);

            totalDurationSeconds = TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeInMillis() - beginningOfTravelTime.getTime());

            double displayDistance = changeToDisplayDistanceUnits(totalDistance);

            averageSpeed = displayDistance / totalDurationSeconds;
            double currentSpeed = changeToDisplayDistanceUnits(newLocation.getSpeed());

            calorieCount = totalDistance * CALORIES_PER_METER;

            // Compensate for bug where time elapsed, used as denominator in avg speed calculation, equals 0
            if(Double.isInfinite(averageSpeed) || Double.isNaN(averageSpeed)) averageSpeed = 0;

            return currentSpeed;
        }

        /**
         * Helper method to transfer new findings from TrackingService to receiver of MapActivity
         */
        private void sendInfoToMapActivity(double currentSpeed){
            // Build intent with all necessary info to display on map
            Intent intent = new Intent(MapActivity.BROADCAST_DETECTED_LOCATION);
            intent.putExtra(MapActivity.LOCATION_KEY, locationsToBeAdded);
            intent.putExtra(MapActivity.CURR_SPEED_KEY, currentSpeed);
            intent.putExtra(MapActivity.AVG_SPEED_KEY, averageSpeed);
            intent.putExtra(MapActivity.ALTITUDE_KEY, changeToDisplayDistanceUnits(newLocation.getAltitude() - startAltitude));
            intent.putExtra(MapActivity.CALORIE_KEY, calorieCount);
            intent.putExtra(MapActivity.DISTANCE_KEY, changeToDisplayDistanceUnits(totalDistance));

            // Send newly built intent to MapActivity
            if(!login.getShouldStop()) {
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }

            // Reset locations list now that the info has been stored and sent to MapActivity
            locationsToBeAdded = new ArrayList<LatLng>();
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }
    };

    /**
     * ActivityRecognition uses this method as a callback; pull the result from the intent when it
     * exists, and update the activity information accordingly
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("TEST", "onHandleIntent()");

        try {
            if (ActivityRecognitionResult.hasResult(intent)) {   // Check to see if intent contains new activity output
                ActivityRecognitionResult mActivityResult = ActivityRecognitionResult.extractResult(intent);    // grab activity value
                if (mActivityResult != null && !mActivityResult.toString().equals("")) {
                    Log.d("TEST", "onHandleIntentInside()");

                    updateActivityDuration();   // now that activity might be changing, log the previous duration
                    DetectedActivity mDetectedActivity = mActivityResult.getMostProbableActivity();     // activity with highest confidence value

                    // Get string value of activity given the integer value provided by recognition
                    activityName = MapActivity.handleUserActivity(mDetectedActivity.getType(), mDetectedActivity.getConfidence(), activityName);

                    // Send new activity update to MapActivity
                    Intent sendActivityIntent = new Intent(MapActivity.BROADCAST_DETECTED_ACTIVITY);
                    sendActivityIntent.putExtra(MapActivity.ACTIVITY_TYPE, activityName);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(sendActivityIntent);
                }
            }
        }
        catch(Exception e){
            System.err.println("Error when getting activity information");
            e.printStackTrace();
        }
    }

    /**
     * Log the amount of time a given activity has gone on for - used to get longest activity when entry is saved
     */
    private void updateActivityDuration(){
        Date endTime = Calendar.getInstance().getTime();    // current time
        long priorDuration = 0;

        // If this activity already had a time stored, get the old value before updating
        if(startTime != null && activitiesFrequency != null && activityName != null) {
            if (activitiesFrequency.containsKey(activityName)) {
                priorDuration = activitiesFrequency.get(activityName);
            }

            // Time in seconds that this activity has occurred since start of map
            long updatedTime = priorDuration + TimeUnit.MILLISECONDS.toSeconds(endTime.getTime() - startTime.getTime());

            activitiesFrequency.put(activityName, updatedTime);     // Put in hashmap so activity with largest duration can be taken later
        }
        startTime = Calendar.getInstance().getTime();       // Start a new timer now that activity has begun
    }

    /**
     * Automatic ActivityRecognition may find that multiple activities occurred, but log the entry by
     * whichever activity was most common during this period
     */
    private String getMajorityActivity(){
        updateActivityDuration();   // Make sure all activity durations are up to date

        String longestActivity = activityName;      // Current activity must be in the hashmap, make default value

        // Check the duration of every activity that has occurred, return the one with the largest duration
        for(String activity : activitiesFrequency.keySet()){
            if(longestActivity.equals("") || activitiesFrequency.get(activity) > activitiesFrequency.get(longestActivity)){
                longestActivity = activity;
            }
        }
        return longestActivity;
    }

    /**
     * If the thread is being destroyed, disconnect MapActivity and check whether or not to stop
     * all threads
     */
    @Override
    public void onDestroy() {
        Log.d("TEST", "onDestroy()");
        if(activityRecognitionClient != null){
            activityRecognitionClient.removeActivityUpdates(activityTypePendingIntent);
        }
        if(login.getShouldStop()){
            if(login.getMapActivityStarted()){
                Intent sendActivityIntent = new Intent(FINISH_KEY);
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendActivityIntent);
            }
            login.setMapRotated(false);
            login.setServiceStarted(false);
            login.setHasNotified(false);
            stopSelf();
        }
        super.onDestroy();
    }

    /**
     * If the save button has been pressed, update the exercise entry and send result to the database
     * for accessing later in the HistoryFragment
     */
    private void handleServiceSave(){
        updateEntryBeforeShutdown();
        if(currentExerciseEntry.getmActivityType().equals("")) currentExerciseEntry.setmActivityType(DEFAULT_ACTIVITY);
        ManualEntryActivity.uploadExerciseEntry(getApplicationContext(), currentExerciseEntry);
        login.setSavePressed(false);    // Make sure boolean is false for next time service is called
    }

    /**
     * Grab all parameters for ExerciseEntry from what has been gathered in the TrackingService recording
     * so far - update entry with all of this information
     */
    private void updateEntryBeforeShutdown(){
        // Name of non-GPS recording should be the most frequent activity that occurred over the lifetime
        // of the service
        if(!inputType.equals("GPS")) {
            currentExerciseEntry.setmActivityType(getMajorityActivity());
        }

        currentExerciseEntry.setmDateTime(getDateTime());   // Call helper method to get properly formatted String
        currentExerciseEntry.setmDuration((int) TimeUnit.SECONDS.toMinutes(totalDurationSeconds));
        currentExerciseEntry.setmDistance(changeToEntryDistanceUnits(totalDistance));
        currentExerciseEntry.setmAvgPace(0);    // Not necessary to implement average pace
        currentExerciseEntry.setmAvgSpeed(averageSpeed);
        currentExerciseEntry.setmCalorie((int) calorieCount);
        currentExerciseEntry.setmClimb(changeToDisplayDistanceUnits(newLocation.getAltitude() - startAltitude));
        currentExerciseEntry.setmHeartRate(0);  // No heart rate for GPS/Automatic activity
        currentExerciseEntry.setmComment("");   // No comment for GPS/Automatic activity;
        currentExerciseEntry.setmPrivacy(login.getPrivacySettingEnabled() ? 1 : 0); // Privacy setting is 1 for enabled
    }

    /**
     * Helper method to get the current year, month, day, hour, and minute in properly formatted String
     */
    private String getDateTime(){
        Calendar cal = Calendar.getInstance();  // Current time

        // Create a string in proper formatting of current 'year-month-day hour:minute'
        DecimalFormat timeFormat = new DecimalFormat("00");
        return cal.get(Calendar.YEAR) + "-" +
                timeFormat.format((cal.get(Calendar.MONTH) + 1)) + "-" +
                timeFormat.format(cal.get(Calendar.DAY_OF_MONTH)) + " " +
                timeFormat.format(cal.get(Calendar.HOUR)) + ":" +
                timeFormat.format(cal.get(Calendar.MINUTE));
    }

    /**
     * Helper method to convert a distance in meters to whatever must be stored in the entry
     *
     * Entry requires larger units like kilometers and miles
     */
    private double changeToEntryDistanceUnits(double distanceMeters){
        if(login.getUnitPreference() == ExerciseEntryDbHelper.METRIC_UNITS){
            return distanceMeters * METERS_TO_KILOMETERS;
        }
        else{
            return distanceMeters * METERS_TO_MILES;
        }
    }

    /**
     * Helper method to convert a distance in meters to whatever must be displayed
     *
     * Entry requires smaller units like feet and meters
     */
    private double changeToDisplayDistanceUnits(double distanceMeters){
        if(login.getUnitPreference() == ExerciseEntryDbHelper.METRIC_UNITS){
            return distanceMeters;
        }
        else{
            return distanceMeters * METERS_TO_FEET;
        }
    }

    /**
     * Allow for binding functionality to MapActivity
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return (mBinder);
    }

    public class DownloadBinder extends Binder {
        public TrackingService getService(){
            return TrackingService.this;
        }
    }
}
