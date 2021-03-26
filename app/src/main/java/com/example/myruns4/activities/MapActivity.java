package com.example.myruns4.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myruns4.R;
import com.example.myruns4.fragments.HistoryFragment;
import com.example.myruns4.fragments.StartFragment;
import com.example.myruns4.models.ExerciseEntry;
import com.example.myruns4.models.Login;
import com.example.myruns4.services.TrackingService;
import com.example.myruns4.utils.EntryTask;
import com.example.myruns4.utils.ExerciseEntryDbHelper;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Two functions:
 * 1. Use the information provided by threads from TrackingService to display information about an activity
 * on a map, while providing functionality to store the entry.
 *
 * 2. Given a past GPS/Automatic entry, display corresponding information
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Keys for receiving/sending information
    public static final String DISTANCE_KEY = "total_distance";
    public static final String ALTITUDE_KEY = "altitude_key";
    public static final String CURR_SPEED_KEY = "curr_speed_key";
    public static final String AVG_SPEED_KEY = "avg_speed_key";
    public static final String CALORIE_KEY = "calorie_key";
    public static final String BROADCAST_DETECTED_ACTIVITY = "activity_name_intent";
    public static final String BROADCAST_DETECTED_LOCATION = "activity_entry_info_intent";
    public static final String LOCATION_KEY = "current_location";

    private static final int LOCATION_REQUEST_CODE = 2;
    private static final int MAP_PERM_INDEX = 0;
    private static final String TRACKING_SERVICE_NAME = "tracking service";

    public static final int UPDATE_INTERVAL = 2000; // every N milliseconds, update __
    private static final int MIN_CONFIDENCE_VALUE = 70;
    private static final float ZOOM_DISTANCE = 18;  // How far to zoom in on point in map
    private static final float POLYLINE_WIDTH = 10;     // Width of line connecting measured locations

    // Unit conversion multipliers
    private static final double KILOMETERS_TO_METERS = 1000;
    private static final double MILES_TO_FEET = 5280;

    public static final String ACTIVITY_TYPE = "activity_type";
    private static final String DEFAULT_INPUT_TYPE = "Automatic";
    private static final String DEFAULT_ACTIVITY_NAME = "Still";
    private static final String ACTIVITY_TITLE = "Map";

    // Variables for displaying information to map
    private GoogleMap mMap;
    private PolylineOptions polyLineOptions;
    private TrackingService mTrackingService;
    private Marker firstMarker;
    private Marker lastMarker;
    private ArrayList<LatLng> storeAllCoordinates;
    private ArrayList<LatLng> historyAllCoordinates;

    private Login profile;
    private ExerciseEntry exerciseEntry;
    private boolean loadingFromHistory;
    boolean serviceBound;
    private String inputType;
    private String activityName;

    /**
     * Handle creation of MapActivity and the processes that will be undergone
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(ACTIVITY_TITLE);
        setContentView(R.layout.activity_map);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        profile = new Login(getApplicationContext());
        storeAllCoordinates = new ArrayList<LatLng>();      // Master list of coordinates measured

        // Handle operations differently based on whether creating new entry or loading old one
        if(loadingFromHistory = profile.getLoadingMapFromHistory()){
            if((exerciseEntry = getIntent().getParcelableExtra(HistoryFragment.EXERCISE_ENTRY_KEY)) == null){
                finish();   // close activity if no info can be found despite request for displaying history entry
            }
            historyAllCoordinates = getIntent().getParcelableArrayListExtra(HistoryFragment.LOCATION_LIST_KEY);
            loadMap();
        }
        else {
            handleServiceOnCreateStartup();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        profile.setMapRotated(true);
    }

    /**
     * Start operations necessary for creating a TrackingService if new entry requested by user
     */
    private void handleServiceOnCreateStartup(){
        // Check if values were passed for an input or activity, otherwise set to default values
        if ((inputType = getIntent().getStringExtra(StartFragment.INPUT_TYPE_KEY)) == null) inputType = DEFAULT_INPUT_TYPE;
        if ((activityName = getIntent().getStringExtra(StartFragment.ACTIVITY_NAME_KEY)) == null) activityName = DEFAULT_ACTIVITY_NAME;
        if(inputType.equals(DEFAULT_INPUT_TYPE)) activityName = DEFAULT_ACTIVITY_NAME;


        // Start a tracking service with a callback to MapActivity once map is ready to be displayed
        mTrackingService = new TrackingService(TRACKING_SERVICE_NAME);
        ((SupportMapFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.map))).getMapAsync(this);
    }

    /**
     * Load map without looking for values from intent, all that's necessary when reloading history
     */
    private void loadMap(){
        checkAndRequestPermissions();       // Permissions are necessary for location services


        mTrackingService = new TrackingService(TRACKING_SERVICE_NAME);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    /**
     * Create an activity bar, have a delete option if loading old entry, otherwise have a save option
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(profile.getLoadingMapFromHistory()){
            getMenuInflater().inflate(R.menu.historyactivitymenu, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.savemenu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Determine whether the delete, save, or back button was processed and handle as necessary
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(profile.getLoadingMapFromHistory()){
            if(item.getItemId() == R.id.delete){
                deleteEntry();  // Remove entry from SQL database upon request
            }
            // Reset variables of SharedPreference for next time in MapActivity
            profile.setLoadingMapFromHistory(false);
            loadingFromHistory = false;
        }
        else {
            if (item.getItemId() == R.id.save) {
                profile.setSavePressed(true);   // Call helper method in Service by changing a tracked boolean
            }
            profile.setShouldStop(true);    // Stop the service once saved
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Callback indicating map is loaded, so call method to specialize in setup of map
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            setupMap(googleMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to delete entry from SQL database when viewing saved entry
     */
    private void deleteEntry(){
        EntryTask deleteEntry = new EntryTask(this, ManualEntryActivity.DELETE_ENTRY_COMMAND, exerciseEntry.getId(), exerciseEntry);
        deleteEntry.execute();
    }

    /**
     * Once map is loaded, set the map up differently based on whether or not
     * an older entry is currently being loaded
     */
    private void setupMap(GoogleMap googleMap) {
        // Similar ways of setting up map regardless of loading or creating entry
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if(loadingFromHistory){
            buildAndUploadMapInfo();    // Helper method to pull necessary variable info and send to map
        }
        else {
            createAndBindTrackingService();     // Helper method to bind tracking service for new entry creation
        }
    }

    /**
     * Call information from exercise entry, originally stored in intent, to be loaded into map
     */
    private void buildAndUploadMapInfo(){
        activityName = exerciseEntry.getmActivityType();
        double speed = exerciseEntry.getmAvgSpeed();
        double avgSpeed = exerciseEntry.getmAvgSpeed();
        double calories = exerciseEntry.getmCalorie();
        double climb = exerciseEntry.getmClimb();
        double distance = getCorrectDistance(exerciseEntry.getmDistance());

        // Call method that directly updates MapFragment
        updateMap(historyAllCoordinates, speed, avgSpeed, calories, climb, distance);
    }

    /**
     * Start a TrackingService that is bound to this activity, get Location updates and send the
     * current values for input type and activity to be processed
     */
    private void createAndBindTrackingService(){
        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.putExtra(StartFragment.INPUT_TYPE_KEY, inputType);
        serviceIntent.putExtra(StartFragment.ACTIVITY_NAME_KEY, activityName);
        startService(serviceIntent);
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        serviceBound = true;
    }

    /**
     * Change value for distance based on current units and what's stored in an entry
     */
    private double getCorrectDistance(double mDistance){
        if(profile.getUnitPreference() == ExerciseEntryDbHelper.METRIC_UNITS){
            return mDistance * KILOMETERS_TO_METERS;
        }
        else{
            return mDistance * MILES_TO_FEET;
        }
    }

    /**
     * Make sure location permissions have been granted so MapActivity can be received
     */
    private void checkAndRequestPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    /**
     * Check whether permissions have been granted, and respond to user's action if they didn't
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        if (!(grantResults.length > 0 && grantResults[MAP_PERM_INDEX] == PackageManager.PERMISSION_GRANTED)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder explainPermissionUse = new AlertDialog.Builder(this);
                explainPermissionUse.setTitle("Change Permissions in Device Settings:");
                explainPermissionUse.setMessage("Location & Internet is necessary for Maps functionality");
                explainPermissionUse.show();
            }
        }
    }

    /**
     * Process the connection between TrackingService and MapActivity as this activity's lifecycle
     * changes
     */
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TrackingService.DownloadBinder mBinder = (TrackingService.DownloadBinder) iBinder;
            mTrackingService = mBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            unbindService(mServiceConnection);
        }
    };

    /**
     * Receive information from the tracking service that should be added to the map fragment
     */
    BroadcastReceiver mMapActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If activity is being sent, update with new activity name
            if (intent.getAction().equals(BROADCAST_DETECTED_ACTIVITY)) {

                activityName = intent.getStringExtra(ACTIVITY_TYPE);
                Log.d("TEST", "onReceive()" + activityName);

            }
            // If location information is being sent, update the map with the new location and
            // corresponding measurements
            else if (intent.getAction().equals(BROADCAST_DETECTED_LOCATION)) {
                List<LatLng> newCoordinates = intent.getParcelableArrayListExtra(LOCATION_KEY);
                double currentClimb = intent.getDoubleExtra(ALTITUDE_KEY, 0);
                double currentSpeed = intent.getDoubleExtra(CURR_SPEED_KEY, 0);
                double avgSpeed = intent.getDoubleExtra(AVG_SPEED_KEY, 0);
                double calorieCount = intent.getDoubleExtra(CALORIE_KEY, 0);
                double totalDistance = intent.getDoubleExtra(DISTANCE_KEY, 0);

                updateMap(newCoordinates, currentSpeed, avgSpeed, calorieCount, currentClimb, totalDistance);
            }
        }
    };

    /**
     * Given parameters that are displayed visibly on map, make the changes to the UI
     */
    private void updateMap(List<LatLng> newCoordinates, double currentSpeed, double averageSpeed,
                           double calorieCount, double currentClimb, double totalDistance) {
        if(newCoordinates!=null) updateCoordinates(newCoordinates);

        DecimalFormat decimalFormat = new DecimalFormat("0.00");    // Make sure two decimals are used

        // String values that will be put on the map
        String activityHeadline = "Activity: " + activityName;
        String currentSpeedHeadline = "Speed: " + decimalFormat.format(currentSpeed) + " " + getUnits() + "/s";
        String climbedHeadline = "Climbed: " + decimalFormat.format(currentClimb) + " " + getUnits();
        String calorieHeadline = "Calories: " + decimalFormat.format(calorieCount) + " cal";
        String distanceHeadline = "Distance: " + decimalFormat.format(totalDistance) + " " + getUnits();
        String avgSpeedHeadline = "Avg Speed: " + decimalFormat.format(averageSpeed) + " " + getUnits() + "/s";

        // Update the TextViews with the new string values
        ((TextView)findViewById(R.id.text_activity_name)).setText(activityHeadline);
        ((TextView)findViewById(R.id.text_cur_speed)).setText(currentSpeedHeadline);
        ((TextView)findViewById(R.id.text_avg_speed)).setText(avgSpeedHeadline);
        ((TextView)findViewById(R.id.text_climbed)).setText(climbedHeadline);
        ((TextView)findViewById(R.id.text_calorie)).setText(calorieHeadline);
        ((TextView)findViewById(R.id.text_distance)).setText(distanceHeadline);
    }

    /**
     * Get the string value for units depending on user's unit preferences
     */
    private String getUnits(){
        if(profile.getUnitPreference()== ExerciseEntryDbHelper.METRIC_UNITS){
            return "m";
        }
        else{
            return "ft";
        }
    }

    /**
     * Given a list of coordinates, update the map's line showing the movement path and the start
     * and end markers. Save all new coordinates to master list
     */
    private void updateCoordinates(List<LatLng> newCoordinates){
        storeAllCoordinates.addAll(newCoordinates);     // Store new coordinates

        // Zoom in on user's current location
        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(storeAllCoordinates.get(storeAllCoordinates.size()-1), ZOOM_DISTANCE);
        mMap.animateCamera(location);

        // Format a polyline - the line showing the user's path - to be visually appealing
        if (polyLineOptions == null) {
            polyLineOptions = new PolylineOptions().color(Color.RED).width(POLYLINE_WIDTH);
        }

        // Create the marker showing starting position if it doesn't already exist
        if(firstMarker==null){
            firstMarker = mMap.addMarker(new MarkerOptions()
                    .position(storeAllCoordinates.get(0))
                    .title("Your starting point!")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        // Remove and update the last marker once new points are registered and added to the map
        if(lastMarker!=null){
            lastMarker.remove();
        }
        lastMarker = mMap.addMarker(new MarkerOptions()
                .position(storeAllCoordinates.get(storeAllCoordinates.size()-1))
                .title("Your current ending point!")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Update the user path with the new points
        mMap.addPolyline(polyLineOptions.addAll(newCoordinates));
    }

    /**
     * Given output from the activity recognition algorithm, return a String representation of the
     * activity if the confidence is greater than a constant - if less, return original activity value
     */
    public static String handleUserActivity(int type, int confidence, String priorActivity) {
        String label = "Unknown";
        if(confidence <= MIN_CONFIDENCE_VALUE) return priorActivity;     // Confidence level must meet threshold for potential change in activity
        switch (type) {
            case DetectedActivity.IN_VEHICLE: {
                label = "Driving";
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                label = "Cycling";
                break;
            }
            case DetectedActivity.ON_FOOT: {
                label = "Walking/Running";
                break;
            }
            case DetectedActivity.RUNNING: {
                label = "Running";
                break;
            }
            case DetectedActivity.STILL: {
                label = "Still";
                break;
            }
            case DetectedActivity.TILTING: {
                label = "Tilting";
                break;
            }
            case DetectedActivity.WALKING: {
                label = "Walking";
                break;
            }
            case DetectedActivity.UNKNOWN: {
                break;
            }
        }
        return label;
    }

    /**
     * If getting a new entry, reconnect the broadcast/receiver relationship with the TrackerService
     * once Activity is started again
     */
    @Override
    protected void onStart() {
        super.onStart();
        if(!profile.getLoadingMapFromHistory()){
            profile.setMapActivityStarted(true);
            LocalBroadcastManager.getInstance(this).registerReceiver(mMapActivityReceiver,
                    new IntentFilter(BROADCAST_DETECTED_ACTIVITY));
            LocalBroadcastManager.getInstance(this).registerReceiver(mMapActivityReceiver,
                    new IntentFilter(BROADCAST_DETECTED_LOCATION));
        }
    }

    /**
     * If getting a new entry, disconnect the broadcast/receiver relationship so wasted intents aren't
     * sent to MapActivity
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(!loadingFromHistory) {
            profile.setMapActivityStarted(false);   // Allow Service to see activity no longer running
            if (mMapActivityReceiver != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mMapActivityReceiver);
            }
        }
    }

    /**
     * If getting a new entry, disconnect the broadcast/receiver relationship so wasted intents aren't
     * sent to MapActivity
     */
    @Override
    protected void onDestroy() {
        if(!loadingFromHistory) {
            profile.setMapActivityStarted(false);   // Allow Service to see activity no longer running
            if (serviceBound && mServiceConnection != null) unbindService(mServiceConnection);
            mTrackingService.stopSelf();
        }
        super.onDestroy();
    }
}
