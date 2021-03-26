package com.example.myruns4.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myruns4.R;
import com.example.myruns4.adapters.ManualInputAdapter;
import com.example.myruns4.fragments.CustomDialogFragment;
import com.example.myruns4.fragments.StartFragment;
import com.example.myruns4.models.ActivityEntryModel;
import com.example.myruns4.models.ExerciseEntry;
//import com.example.myruns4.models.LatLng;
import com.google.android.gms.maps.model.LatLng;
import com.example.myruns4.models.Login;
import com.example.myruns4.utils.EntryTask;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Load options for manual user input of a given activity and corresponding details or load the details
 * of a past exercise entry
 */
public class ManualEntryActivity extends AppCompatActivity{
    private static final String ACTIVITY_NAME_KEY = "activity_name";
    private static final String SAVED_LIST_VIEW_ITEMS = "saved_items";
    private static final int MANUAL_ENTRY_VALUE = 0;
    private static final String EXERCISE_ENTRY_KEY = "exercise_entry_key";


    private static final int INSERT_ENTRY_COMMAND = 0;
    public static final int DELETE_ENTRY_COMMAND = 1;

    private static final int ACTIVITY_INDEX = 0;
    private static final int DATE_INDEX = 1;
    private static final int TIME_INDEX = 2;
    private static final int DURATION_INDEX = 3;
    private static final int DISTANCE_INDEX = 4;
    private static final int CALORIE_INDEX = 5;
    private static final int HEARTBEAT_INDEX = 6;
    private static final int COMMENT_INDEX = 7;

    private static final int PRIVACY_DISABLED = 0;
    private static final int PRIVACY_ENABLED = 1;
    private static final int METRIC_UNITS = 0;

    ListView manualEntryList;
    ArrayList<ActivityEntryModel> listViewItems;
    ManualInputAdapter manualInputAdapter;
    String activityName;

    ExerciseEntry entry;

    Login currProfileLogin;

    /**
     * Create the layout and handle two different operations depending on whether new data will be
     * added by user or if an old exercise entry is being requested
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_input);
        setTitle("Manual Entry Activity");


        currProfileLogin = new Login(getApplicationContext());

        manualEntryList = findViewById(R.id.manual_input_list);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if(getIntent().getParcelableExtra(EXERCISE_ENTRY_KEY) != null) {
            Intent intent = getIntent();
            entry = intent.getParcelableExtra(EXERCISE_ENTRY_KEY);
            listViewItems = entry.getListViewItems(getUnitType());
        }
        else{
            activityName = getIntent().getStringExtra(ACTIVITY_NAME_KEY);

            // Create the list of activity information options and content
            // If values have already been written in, update the default list values with what's stored
            listViewItems = new ArrayList<ActivityEntryModel>();
            if(savedInstanceState != null){
                listViewItems = savedInstanceState.getParcelableArrayList(SAVED_LIST_VIEW_ITEMS);
            }
            else{
                defaultActivityEntryList();
            }
        }

        // Adapter to control addition of ListView items
        manualInputAdapter = new ManualInputAdapter(this, listViewItems);
        manualEntryList.setAdapter(manualInputAdapter);

        if(entry==null) createListClickListener();
    }

    /**
     * Store list in case activity information values have been inputted by user
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_LIST_VIEW_ITEMS, listViewItems);
    }

    /**
     * Create options to save/delete depending on current mode and go back to main activity on top
     * activity bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(entry != null) {
            getMenuInflater().inflate(R.menu.historyactivitymenu, menu);
        }
        else{
            getMenuInflater().inflate(R.menu.savemenu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Back button will navigate user back to MainActivity
     *
     * Save button will store the values of the entry when adding new information, delete button
     * will remove the entry when viewing past information
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(entry!=null){
            if(item.getItemId() == R.id.delete){
                deleteItemEntry();
            }
        }
        else {
            if (item.getItemId() == R.id.save) {
                constructExerciseEntry();
            }
            else{
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Use an AsyncTask to request a deletion from the SQL database
     *
     * Method is only used when looking at past data
     */
    private void deleteItemEntry(){
        EntryTask deleteEntry = new EntryTask(this, DELETE_ENTRY_COMMAND, entry.getId(), entry);
        deleteEntry.execute();
    }

    /**
     * If a user is adding new information manually, construct a new exercise entry to be stored
     * in the SQL database. Use an AsyncTask to do so after object creation
     */
    private void constructExerciseEntry(){
        ArrayList<ActivityEntryModel> listActivities = manualInputAdapter.getListActivities();

        Log.d("TEST", "constructExerciseEntry()");
        int mInputType = MANUAL_ENTRY_VALUE;
        String mActivityType = listActivities.get(ACTIVITY_INDEX).getInformation();
        String mDateTime = createDateTimeString(listActivities);
        Log.d("TESTING", mDateTime + "");
        int mDuration = Integer.parseInt(listActivities.get(DURATION_INDEX).getInformation().split(" ")[0]);
        double mDistance = Double.parseDouble(listActivities.get(DISTANCE_INDEX).getInformation().split(" ")[0]);
        double mAvgPace = 0;        // No pace for ManualEntry
        double mAvgSpeed = 0;       // No speed for ManualEntry
        int mCalorie = Integer.parseInt(listActivities.get(CALORIE_INDEX).getInformation().split(" ")[0]);
        double mClimb = 0;      // No climb for ManualEntry
        int mHeartRate = Integer.parseInt(listActivities.get(HEARTBEAT_INDEX).getInformation().split(" ")[0]);
        String mComment = listActivities.get(COMMENT_INDEX).getInformation();
        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();        // No LatLngs for ManualEntry
        int mPrivacy;
        if(currProfileLogin.getPrivacySettingEnabled()) mPrivacy = PRIVACY_ENABLED;
        else mPrivacy = PRIVACY_DISABLED;

        ExerciseEntry entry = new ExerciseEntry(mInputType, mActivityType, mDateTime, mDuration,
                mDistance, mAvgPace, mAvgSpeed, mCalorie, mClimb, mHeartRate, mComment, latLngs, mPrivacy);

        uploadExerciseEntry(this, entry);
    }

    public static void uploadExerciseEntry(Context context, ExerciseEntry entry){
        int mRowIndex = 0;  // No row index necessary for addition
        EntryTask changeEntries = new EntryTask(context, INSERT_ENTRY_COMMAND, mRowIndex, entry);

        changeEntries.execute();
    }

    /**
     * Helper method to get the date and time of the exercise entry (accessing past info) in the proper
     * formatting
     */
    private String createDateTimeString(ArrayList<ActivityEntryModel> listActivities){
        String yearMonthDay = listActivities.get(DATE_INDEX).getInformation();
        String hourMinute = listActivities.get(TIME_INDEX).getInformation();

        return yearMonthDay + " " + hourMinute;
    }

    /**
     * Track which element of ListView has been clicked so proper dialog can be made and modifications
     * stored in list of activities and info
     */
    public void createListClickListener(){
        manualEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if(position!=0) {   // Don't allow modification of the activity name itself
                    CustomDialogFragment customDialog = new CustomDialogFragment(position);     // Handle custom creation of dialog
                    customDialog.show(getSupportFragmentManager(), "default");
                }
            }
        });{
        }
    }

    /**
     * Default list of values for activity inputs and information. List can be modified and information
     * displayed based on user changes
     */
    private void defaultActivityEntryList(){
        listViewItems.add(new ActivityEntryModel("Activity", activityName));
        listViewItems.add(new ActivityEntryModel("Date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())));
        DecimalFormat timeFormat = new DecimalFormat("00");     // ensure hours and minutes are always expressed with two digits
        listViewItems.add(new ActivityEntryModel("Time",  timeFormat.format(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) + ":" + timeFormat.format(Calendar.getInstance().get(Calendar.MINUTE))));
        listViewItems.add(new ActivityEntryModel("Duration", "0 mins"));
        listViewItems.add(new ActivityEntryModel("Distance", "0 " + getUnitType()));
        listViewItems.add(new ActivityEntryModel("Calorie", "0 cals"));
        listViewItems.add(new ActivityEntryModel("Heartbeat", "0 bpm"));
        listViewItems.add(new ActivityEntryModel("Comment", ""));
    }

    /**
     * Check user preferences to determine whether currently using imperial or metric system
     */
    private String getUnitType(){
        if(currProfileLogin.getUnitPreference() == METRIC_UNITS){
            return "kms";
        }
        else{
            return "miles";
        }
    }

    /**
     * Update the elements of the list of activity information and corresponding values, and redisplay List
     */
    public void updateListViewItems(int position, String newInformation){
        listViewItems.get(position).setInformation(newInformation);
        manualInputAdapter.notifyDataSetChanged();
    }

    /**
     * Get the list of activity information and corresponding values
     */
    public ActivityEntryModel getListViewItems(int position){
        return listViewItems.get(position);
    }
}
