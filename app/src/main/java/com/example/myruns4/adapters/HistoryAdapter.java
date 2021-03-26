package com.example.myruns4.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myruns4.R;
import com.example.myruns4.fragments.StartFragment;
import com.example.myruns4.models.ExerciseEntry;
import com.example.myruns4.models.Login;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Display all of the entries in a provided list (from the SQL database) on the HistoryFragment
 */
public class HistoryAdapter extends ArrayAdapter<ExerciseEntry> {
    private static final String ERROR_MESSAGE = "ERROR";
    private static final int METRIC_UNITS = 0;

    private List<ExerciseEntry> entryList;

    /**
     * Create the adapter with the list of exercise entries provided by the HistoryFragment
     */
    public HistoryAdapter(Context context, List<ExerciseEntry> entryList){
        super(context, 0, entryList);
        this.entryList = entryList;
    }

    /**
     * Display text of general information about each entry in the ListView
     * Information includes the input type, activity, distance traveled, duration, the date, and the
     * time in which it occurred
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.history_list_entry, parent, false);
        if(entryList.size() > 0) {      // Make sure entry list is populated
            // Format the summarized text using helper methods
            String textInputActivity = getTextForInputAndActivity(position);
            String textDistanceDuration = getTextForDistanceAndDuration(position);
            String textDateTime = getTextForDateAndTime(position);

            // Set each ListView element with its corresponding summarized details
            ((TextView) v.findViewById(R.id.input_and_activity)).setText(textInputActivity);
            ((TextView) v.findViewById(R.id.distance_and_duration)).setText(textDistanceDuration);
            ((TextView) v.findViewById(R.id.date_and_time)).setText(textDateTime);
        }
        return v;
    }

    /**
     * Display the Input and Activity type in the proper format
     *
     * 'INPUT TYPE': 'ACTIVITY'
     */
    private String getTextForInputAndActivity(int position) {
        Log.d("TESTING", entryList.size() + " ENTRY LIST SIZE");
        if (entryList.size() > 0) {
            String inputType = StartFragment.getInputTypeAtIndex(entryList.get(position).getmInputType());
            String activityType = entryList.get(position).getmActivityType();

            return inputType + ": " + activityType;
        }
        return ERROR_MESSAGE;   // If the entry list is empty, produce an error message as the text
    }

    /**
     * Display the distance and duration of the exercise entry type in the proper format and units
     *
     * 'DISTANCE W/ UNITS', 'DURATION W/ UNITS'
     */
    private String getTextForDistanceAndDuration(int position){
        if (entryList.size() > 0) {
            double distance = entryList.get(position).getmDistance();
            int time = entryList.get(position).getmDuration();
            DecimalFormat numberFormat = new DecimalFormat("#0.00");

            return numberFormat.format(distance) + " " + getDurationUnitType() + ", " + time + " mins";
        }
        return ERROR_MESSAGE;   // If the entry list is empty, produce an error message as the text
    }

    /**
     * Helper method to check user unit preferences and return a string of proper unit type
     */
    private String getDurationUnitType(){
        Login currProfileLogin = new Login(getContext().getApplicationContext());
        if(currProfileLogin.getUnitPreference() == METRIC_UNITS){
            return "kms";
        }
        else{
            return "miles";
        }
    }

    /**
     * Get the pre-formatted string that represents date & time the exercise entry was completed
     *
     * 'YEAR'-'MONTH'-'DAY' 'HOUR':'MINUTE'
     */
    private String getTextForDateAndTime(int position){
        if (entryList.size() > 0) {
            return entryList.get(position).getmDateTime();
        }
        return ERROR_MESSAGE;
    }
}
