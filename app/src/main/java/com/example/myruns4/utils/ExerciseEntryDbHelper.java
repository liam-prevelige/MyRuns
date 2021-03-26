package com.example.myruns4.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.myruns4.models.ExerciseEntry;
import com.google.android.gms.maps.model.LatLng;
import com.example.myruns4.models.Login;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Create & process a SQL Database containing ExerciseEntrys
 *
 * Used in conjunction with AsyncTask & AsyncTaskLoader
 */
public class ExerciseEntryDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "entries.db";
    private static final int DATABASE_VERSION = 1;

    public static final String COLUMN_ENTRY = "entries";

    private static final String ID = "_id";
    private static final String INPUT_TYPE = "input_type";
    private static final String ACTIVITY_TYPE = "activity_type";
    private static final String DATE_TIME = "date_time";
    private static final String DURATION = "duration";
    private static final String DISTANCE = "distance";
    private static final String AVG_PACE = "avg_pace";
    private static final String AVG_SPEED = "avg_speed";
    private static final String CALORIES = "calories";
    private static final String CLIMB = "climb";
    private static final String HEARTRATE = "heartrate";
    private static final String COMMENT = "comment";
    private static final String PRIVACY = "privacy";
    private static final String GPS_DATA = "gps_data";
    public static final int METRIC_UNITS = 0;
    private static final double IMPERIAL_TO_METRIC = 1.609344;      // Conversion multiplier for miles to km
    private static final double METRIC_TO_IMPERIAL = 0.621371192;   // Conversion multiplier for km to miles
    private static final double FEET_TO_METERS = 0.3048;
    private static final double METERS_TO_FEET = 3.28084;

    private Login loginInfo;

    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + COLUMN_ENTRY + "(" + ID + " INTEGER " +
            "PRIMARY KEY AUTOINCREMENT, input_type INTEGER NOT NULL, activity_type INTEGER NOT NULL, " +
            "date_time DATETIME NOT NULL, duration INTEGER NOT NULL, distance FLOAT, avg_pace FLOAT, " +
            "avg_speed FLOAT, calories INTEGER, climb FLOAT, heartrate INTEGER, comment TEXT, " +
            "privacy INTEGER, gps_data TEXT )";

    public ExerciseEntryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        loginInfo = new Login(context.getApplicationContext());
    }

    /**
     * Create table schema if not exists
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    /**
     *  Insert a item given each column value
     */
    public long insertEntry(ExerciseEntry entry) {
        SQLiteDatabase entriesDb = getWritableDatabase();

        // Create Content Values to insert into entries list for each column
        ContentValues entryVals = new ContentValues();
        entryVals.put(INPUT_TYPE, entry.getmInputType());
        entryVals.put(ACTIVITY_TYPE, entry.getmActivityType());
        entryVals.put(DATE_TIME, entry.getmDateTime());
        entryVals.put(DURATION, entry.getmDuration());
        entryVals.put(DISTANCE, entry.getmDistance());
        entryVals.put(AVG_PACE, entry.getmAvgPace());
        entryVals.put(AVG_SPEED, entry.getmAvgSpeed());
        entryVals.put(CALORIES, entry.getmCalorie());
        entryVals.put(CLIMB, entry.getmClimb());
        entryVals.put(HEARTRATE, entry.getmHeartRate());
        entryVals.put(COMMENT, entry.getmComment());
        entryVals.put(PRIVACY, entry.getmPrivacy());
        entryVals.put(GPS_DATA, entry.getmGPSData());

        // Add entry to database, and store index of added entry
        long index = entriesDb.insert(COLUMN_ENTRY, null, entryVals);

        entriesDb.close();

        Log.d("TEST", "Index value: " + index);

        return index;
    }

    /**
     * Remove an entry by giving its index
     */
    public void removeEntry(long rowIndex) {
        SQLiteDatabase entriesDb = getWritableDatabase();
        entriesDb.delete(COLUMN_ENTRY, ID + " = " + rowIndex, null);
        entriesDb.close();
    }

    /**
     *  Query a specific entry by its index.
     */
    public ExerciseEntry fetchEntryByIndex(long rowId) {
        SQLiteDatabase entriesDb = getReadableDatabase();
        Cursor entryAtIndex = entriesDb.query(COLUMN_ENTRY, null, ID + " = " + rowId,
                null, null, null, null);

        ExerciseEntry entry = helperFetchEntryByIndex(rowId, entryAtIndex);
        entriesDb.close();
        return entry;
    }

    /**
     * Helper method for fetchEntryByIndex, create a ExerciseEntry given a Cursor that is a row
     * from the database
     */
    private ExerciseEntry helperFetchEntryByIndex(Long rowID, Cursor rowCursor){
        rowCursor.moveToFirst();

        // Pull all of the values from the column and store the values to be used in an ExerciseEntry
        // constructor
        int mInputType = rowCursor.getInt(rowCursor.getColumnIndex(INPUT_TYPE));
        String mActivityType = rowCursor.getString(rowCursor.getColumnIndex(ACTIVITY_TYPE));
        int mDuration = rowCursor.getInt(rowCursor.getColumnIndex(DURATION));
        double mDistance = rowCursor.getDouble(rowCursor.getColumnIndex(DISTANCE));
        double mAvgPace = rowCursor.getDouble(rowCursor.getColumnIndex(AVG_PACE));
        double mAvgSpeed = rowCursor.getDouble(rowCursor.getColumnIndex(AVG_SPEED));
        int mCalorie = rowCursor.getInt(rowCursor.getColumnIndex(CALORIES));
        double mClimb = rowCursor.getDouble(rowCursor.getColumnIndex(CLIMB));
        int mHeartRate = rowCursor.getInt(rowCursor.getColumnIndex(HEARTRATE));
        String mComment = rowCursor.getString(rowCursor.getColumnIndex(COMMENT));
        int mPrivacy = rowCursor.getInt(rowCursor.getColumnIndex(PRIVACY));
        String mGPSData = rowCursor.getString(rowCursor.getColumnIndex(GPS_DATA));

        double[] newVals = handleCheckUnitsChanged(rowID, mDistance, mAvgSpeed, mClimb);
        mDistance = newVals[0];
        mAvgSpeed = newVals[1];
        mClimb = newVals[2];

        String mDateTime = rowCursor.getString(rowCursor.getColumnIndex(DATE_TIME));

        ArrayList<LatLng> mLocationList = getLatLongFromJSON(mGPSData);
        Log.d("TESTINGLOAD", mLocationList.toString());

        rowCursor.close();

        // Construct and return ExerciseEntry using modified constructor
        ExerciseEntry currEntry =  new ExerciseEntry(mInputType, mActivityType, mDateTime,
                mDuration, mDistance, mAvgPace, mAvgSpeed, mCalorie, mClimb, mHeartRate, mComment,
                mLocationList, mPrivacy);

        currEntry.setId(rowID);     // Update ID with SQL row index so data can be called later

        return currEntry;
    }

    /**
     * Check if user has requested to convert units, change entry values and update
     * database if so, otherwise return double with two-digit decimal
     */
    private double[] handleCheckUnitsChanged(long rowId, double mDistance, double mAverageSpeed, double mClimb){
        if(loginInfo.getUnitPreferenceChanged()){
            SQLiteDatabase entriesDb = getWritableDatabase();

            // Convert distance between km and miles depending on changed unit
            mDistance = convertToProperLargeUnits(mDistance);
            String sqlChangeDistance = "UPDATE entries SET distance = " + mDistance + " WHERE _id = " + rowId;
            entriesDb.execSQL(sqlChangeDistance);

            // Convert avg speed and climb between meters and feet depending on changed unit
            mAverageSpeed = convertToProperSmallUnits(mAverageSpeed);
            String sqlChangeAvgSpeed = "UPDATE entries SET avg_speed = " + mAverageSpeed + " WHERE _id = " + rowId;
            entriesDb.execSQL(sqlChangeAvgSpeed);

            mClimb = convertToProperSmallUnits(mClimb);
            String sqlChangeClimb = "UPDATE entries SET climb = " + mClimb + " WHERE _id = " + rowId;
            entriesDb.execSQL(sqlChangeClimb);

            entriesDb.close();
        }

        return new double[]{mDistance, mAverageSpeed, mClimb};  // Return all values that need to be updated
    }

    /**
     * Helper method for converting between feet and meters
     */
    private double convertToProperSmallUnits(double originalValue){
        if(loginInfo.getUnitPreference() == METRIC_UNITS){
            originalValue = originalValue * FEET_TO_METERS;
        }
        else{
            originalValue = originalValue * METERS_TO_FEET;
        }
        return originalValue;
    }

    /**
     * Helper method for converting between km and miles
     */
    private double convertToProperLargeUnits(double originalValue){
        if(loginInfo.getUnitPreference() == METRIC_UNITS){
            originalValue = originalValue * IMPERIAL_TO_METRIC;
        }
        else{
            originalValue = originalValue * METRIC_TO_IMPERIAL;
        }
        return originalValue;
    }

    /**
     * Functionality to be used in MyRuns4
     *
     * Given a String that is a JSON representation of the coordinates for entry, convert back into
     * a list of LatLng objects
     */
    private ArrayList<LatLng> getLatLongFromJSON(String jsonGPSData){
        ArrayList<LatLng> mLocationList = new ArrayList<LatLng>();
        try {
            JSONArray jsonLocationList = new JSONArray(jsonGPSData);
            for(int i = 0; i < jsonLocationList.length(); i++){
                JSONObject currLocation = jsonLocationList.getJSONObject(i);
                double latitude = Double.parseDouble(currLocation.getString("latitude"));
                double longitude = Double.parseDouble(currLocation.getString("longitude"));

                mLocationList.add(new LatLng(latitude, longitude));
            }
        }
        catch(Exception e){
            System.err.println("Invalid JSON String");
            e.printStackTrace();
        }
        return mLocationList;
    }

    /**
     * Query the entire table, return all rows
     */
    public ArrayList<ExerciseEntry> fetchEntries() {
        SQLiteDatabase entriesDb = getReadableDatabase();

        // Use cursor to pull and store rows to be returned
        Cursor allEntriesData = entriesDb.query(COLUMN_ENTRY, null, null,
                null, null, null, null);

        // List to be returned containing all data
        ArrayList<ExerciseEntry> entries = new ArrayList<ExerciseEntry>();

        if((allEntriesData != null) && (allEntriesData.getCount() > 0)) {   // Ensure data is present
            allEntriesData.moveToFirst();
            while (!allEntriesData.isAfterLast()) {     // Loop through every row and convert row to a new ExerciseEntry object
                ExerciseEntry currEntry = fetchEntryByIndex(allEntriesData.getLong(0));
                if(currEntry!=null) {
                    entries.add(currEntry);
                }
                allEntriesData.moveToNext();
            }
            entriesDb.close();
        }
        if(allEntriesData!=null) {
            allEntriesData.close();
        }

        // Units are handled in fetchEntryByIndex, so set the tracker of changed units to false
        if(loginInfo.getUnitPreferenceChanged()) {
            loginInfo.setUnitPreferenceChanged(false);
        }
        return entries;
    }

    /**
     * Remove all entries from the database - used when new user is registered
     */
    public void clearAllEntries(){
        SQLiteDatabase entriesDb2 = getWritableDatabase();
        entriesDb2.delete(COLUMN_ENTRY, null, null);
        entriesDb2.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + COLUMN_ENTRY);
        onCreate(db);
    }
}
