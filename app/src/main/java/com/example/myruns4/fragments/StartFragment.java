package com.example.myruns4.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.myruns4.activities.ManualEntryActivity;
import com.example.myruns4.activities.MapActivity;
import com.example.myruns4.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;


/**
 * Contain options to enter to track a new activity. Provide options for Manual, GPS, and Automatic
 * activity entry
 *
 * One of the fragments on MainActivity, currently displays UI, doesn't track running or manual values
 */
public class StartFragment extends Fragment {
    private static final String PAGE_TYPE = "page_type";
    public static final String ACTIVITY_NAME_KEY = "activity_name";
    public static final String INPUT_TYPE_KEY = "input_type_key";
    private static final String ACTIVITY_TYPE_KEY = "activity_type_key";
    private static final int AUTOMATIC_ENTRY_POSITION = 2;


    private static final String[] ACTIVITY_SPINNER_OPTIONS  = {"Running", "Walking", "Standing", "Cycling", "Hiking",
            "Downhill Skiing", "Cross-Country Skiing", "Snowboarding", "Skating", "Swimming",
            "Mountain Biking", "Wheelchair"};

    private static final String[] INPUT_SPINNER_OPTIONS = {"Manual", "GPS", "Automatic"};


    private View view;

    public StartFragment() {
        // Required empty public constructor
    }

    /**
     * Initialize StartFragment as one of several fragments
     */
    public static StartFragment newInstance(String position) {
        StartFragment fragment = new StartFragment();
        Bundle args = new Bundle();
        args.putString(PAGE_TYPE, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Once view has been created, build out fragment ViewPage with options for method of activity
     * input (Manual, GPS, Automatic) and options for types of activities
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_start, container, false);

        createInputSpinner(v);

        // Only build out spinner for choosing activity if the options should be enabled (input type is Manual)
        if((v.findViewById(R.id.input_type)).isEnabled()) createActivitySpinner(v);

        configureNextButton(v);
        view = v;

        // Reload the already selected input and activity, if they were chosen before fragment was recreated
        if(savedInstanceState!=null){
            ((Spinner) Objects.requireNonNull(v).findViewById(R.id.input_type)).setSelection((int)savedInstanceState.get(INPUT_TYPE_KEY));
            ((Spinner) Objects.requireNonNull(v).findViewById(R.id.activity_type)).setSelection((int)savedInstanceState.get(ACTIVITY_TYPE_KEY));
        }
        return v;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Store the spinner options for input and activity chosen by user to be reloaded upon fragment recreation
        if(view !=null) {
            outState.putInt(INPUT_TYPE_KEY, ((Spinner) Objects.requireNonNull(view).findViewById(R.id.input_type)).getSelectedItemPosition());
            outState.putInt(ACTIVITY_TYPE_KEY, ((Spinner) Objects.requireNonNull(view).findViewById(R.id.activity_type)).getSelectedItemPosition());
        }
    }

    /**
     * Create the spinner that will display options for adding a new activity
     */
    private void createInputSpinner(final View v){
        Spinner inputSpinner = v.findViewById(R.id.input_type);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(Objects.requireNonNull(this.getActivity()), android.R.layout.simple_spinner_item, INPUT_SPINNER_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputSpinner.setAdapter(adapter);

        inputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // The second spinner for activity type should only be enabled if the data input
                // mode is not on automatic
                if(i == AUTOMATIC_ENTRY_POSITION){
                    Spinner activitySpinner = v.findViewById(R.id.activity_type);
                    activitySpinner.setEnabled(false);
                }
                else{
                    Spinner activitySpinner = v.findViewById(R.id.activity_type);
                    activitySpinner.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Necessary empty method
            }
        });
    }

    /**
     * Create the spinner that will hold the different activities you can log with Manual or GPS mode
     */
    private void createActivitySpinner(View v){
        Spinner activitySpinner = v.findViewById(R.id.activity_type);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(Objects.requireNonNull(this.getActivity()), android.R.layout.simple_spinner_item, ACTIVITY_SPINNER_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);
    }

    /**
     * Handle the different pages the next button can switch to depending on the selected method
     * of measuring activity
     */
    private void configureNextButton(final View view){
        FloatingActionButton next = view.findViewById(R.id.next_button);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // If the user is inputting activity data manually, navigate to ManualInputActivity and
                // pass along the name of the activity being logged with an intent
                if(((Spinner)view.findViewById(R.id.input_type)).getSelectedItem().toString().equals("Manual")){
                    Intent intent = new Intent(getActivity(), ManualEntryActivity.class);
                    intent.putExtra(ACTIVITY_NAME_KEY, ((Spinner)view.findViewById(R.id.activity_type)).getSelectedItem().toString());
                    startActivity(intent);
                }
                // If not in manual entry mode, go to the MapActivity to have movements tracked
                else{
                    Intent intent = new Intent(getActivity(), MapActivity.class);
                    String inputType = ((Spinner)view.findViewById(R.id.input_type)).getSelectedItem().toString();
                    String activityType = ((Spinner)view.findViewById(R.id.activity_type)).getSelectedItem().toString();
                    Log.d("TEST", "Start input " + inputType + " " + activityType);
                    intent.putExtra(INPUT_TYPE_KEY, inputType);
                    intent.putExtra(ACTIVITY_NAME_KEY, activityType);
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Returns the name of the input type (Manual, GPS, Automatic) in the spinner given an integer
     * value of the index of the spinner item
     */
    public static String getInputTypeAtIndex(int index){
        return INPUT_SPINNER_OPTIONS[index];
    }

    /**
     * Searches the spinner of input types to determine the corresponding index
     */
    public static int getIndexofInputType(String inputType){
        Log.d("TEST", inputType);
        for(int i = 0; i < INPUT_SPINNER_OPTIONS.length; i++){
            if(INPUT_SPINNER_OPTIONS[i].toUpperCase().equals(inputType.toUpperCase())){
                return i;
            }
        }
        return -1;
    }
}
