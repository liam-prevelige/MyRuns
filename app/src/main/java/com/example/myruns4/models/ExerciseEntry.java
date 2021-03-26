package com.example.myruns4.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Store information about a user's exercise activity
 */
public class ExerciseEntry implements Parcelable {
    private int mInputType;        // Manual, GPS or automatic
    private String mActivityType;     // Running, cycling etc.
    private String mDateTime;    // When does this entry happen
    private int mDuration;         // Exercise duration in seconds
    private double mDistance;      // Distance traveled. Either in meters or feet.
    private double mAvgPace;       // Average pace
    private double mAvgSpeed;      // Average speed
    private int mCalorie;          // Calories burnt
    private double mClimb;         // Climb. Either in meters or feet.
    private int mHeartRate;        // Heart rate
    private String mComment;       // Comments
    private ArrayList<LatLng> mLocationList; // Location list
    private int mPrivacy;
    private long thisId;

    // Default constructor, can use for the addition of values one by one
    public ExerciseEntry(){
    }

    /**
     * Construct an entry with all of the necessary information to fill in about a user's activity,
     * uses some manipulation of parameters
     */
    public ExerciseEntry(int mInputType, String mActivityType, String mDateTime, int mDuration,
                         double mDistance, double mAvgPace, double mAvgSpeed, int mCalorie, double mClimb,
                         int mHeartRate, String mComment, ArrayList<LatLng> mLocationList, int mPrivacy) {
        this.mInputType = mInputType;
        this.mActivityType = mActivityType;
        this.mDateTime = mDateTime;
        this.mDuration = mDuration;
        this.mDistance = mDistance;
        this.mAvgPace = mAvgPace;
        this.mAvgSpeed = mAvgSpeed;
        this.mCalorie = mCalorie;
        this.mClimb = mClimb;
        this.mHeartRate = mHeartRate;
        this.mComment = mComment;
        this.mLocationList = mLocationList;
        this.mPrivacy = mPrivacy;
    }

    /**
     * Provided to be used with intent
     */
    protected ExerciseEntry(Parcel in) {
        mInputType = in.readInt();
        mActivityType = in.readString();
        mDateTime = in.readString();
        mDuration = in.readInt();
        mDistance = in.readDouble();
        mAvgPace = in.readDouble();
        mAvgSpeed = in.readDouble();
        mCalorie = in.readInt();
        mClimb = in.readDouble();
        mHeartRate = in.readInt();
        mComment = in.readString();
        mPrivacy = in.readInt();
        thisId = in.readLong();
    }

    /**
     * Provided to be used with intent
     */
    public static final Creator<ExerciseEntry> CREATOR = new Creator<ExerciseEntry>() {
        @Override
        public ExerciseEntry createFromParcel(Parcel in) {
            return new ExerciseEntry(in);
        }

        @Override
        public ExerciseEntry[] newArray(int size) {
            return new ExerciseEntry[size];
        }
    };

    /**
     * Convert any exercise entry into a formatting that's to show data in ManualEntryActivity
     *
     * Takes any relevant data for displaying manual input information, and creates an ArrayList
     * containing ActivityEntryModels with this information
     */
    public ArrayList<ActivityEntryModel> getListViewItems(String metricOrImperialUnits) {
        ArrayList<ActivityEntryModel> listViewItems = new ArrayList<ActivityEntryModel>();

        listViewItems.add(new ActivityEntryModel("Activity", mActivityType));

        // Create a Calendar with the string information that holds the entry's year, month, day, hour, and minute
        Calendar calendar = createDateTimeCalendar(mDateTime);
        Date calendarInfo = calendar.getTime();
        SimpleDateFormat ymd = new SimpleDateFormat("YYYY-MM-dd");
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm");

        listViewItems.add(new ActivityEntryModel("Date", ymd.format(calendarInfo)));
        listViewItems.add(new ActivityEntryModel("Time", hm.format(calendarInfo)));

        listViewItems.add(new ActivityEntryModel("Duration", mDuration + " mins"));
        listViewItems.add(new ActivityEntryModel("Distance", mDistance + " " + metricOrImperialUnits));
        listViewItems.add(new ActivityEntryModel("Calorie", mCalorie + " cals"));
        listViewItems.add(new ActivityEntryModel("Heartbeat", mHeartRate + " bpm"));
        listViewItems.add(new ActivityEntryModel("Comment", mComment));

        return listViewItems;
    }

    /**
     * Helper method to construct a calendar using a string in a standard format
     *
     * String contains year, month, day, hour, and minute of exercise entry
     */
    private Calendar createDateTimeCalendar(String dateTime){
        String[] separateDateTime = dateTime.split(" ");

        String[] yearMonthDay = separateDateTime[0].split("-");
        String[] hourMinute = separateDateTime[1].split(":");

        Calendar.Builder calendar = new Calendar.Builder();
        calendar.setDate(Integer.parseInt(yearMonthDay[0]), Integer.parseInt(yearMonthDay[1]), Integer.parseInt(yearMonthDay[2]));
        calendar.setTimeOfDay(Integer.parseInt(hourMinute[0]), Integer.parseInt(hourMinute[1]), 0);

        return calendar.build();
    }

    // Get the database index of the ExerciseEntry
    public long getId(){
        return thisId;
    }

    // Store the database index of the ExerciseEntry
    public void setId(long newId){
        thisId = newId;
    }

    public int getmInputType() {
        return mInputType;
    }

    public void setmInputType(int mInputType) {
        this.mInputType = mInputType;
    }

    public String getmActivityType() {
        return mActivityType;
    }

    public void setmActivityType(String mActivityType) {
        this.mActivityType = mActivityType;
    }

    public String getmDateTime() {
        return mDateTime;
    }

    public void setmDateTime(String mDateTime) {
        this.mDateTime = mDateTime;
    }

    public int getmDuration() {
        return mDuration;
    }

    public void setmDuration(int mDuration) {
        this.mDuration = mDuration;
    }

    public double getmDistance() {
        return mDistance;
    }

    public void setmDistance(double mDistance) {
        this.mDistance = mDistance;
    }

    public double getmAvgPace() {
        return mAvgPace;
    }

    public void setmAvgPace(double mAvgPace) {
        this.mAvgPace = mAvgPace;
    }

    public double getmAvgSpeed() {
        return mAvgSpeed;
    }

    public void setmAvgSpeed(double mAvgSpeed) {
        this.mAvgSpeed = mAvgSpeed;
    }

    public int getmCalorie() {
        return mCalorie;
    }

    public void setmCalorie(int mCalorie) {
        this.mCalorie = mCalorie;
    }

    public double getmClimb() {
        return mClimb;
    }

    public void setmClimb(double mClimb) {
        this.mClimb = mClimb;
    }

    public int getmHeartRate() {
        return mHeartRate;
    }

    public void setmHeartRate(int mHeartRate) {
        this.mHeartRate = mHeartRate;
    }

    public String getmComment() {
        return mComment;
    }

    public void setmComment(String mComment) {
        this.mComment = mComment;
    }

    public ArrayList<LatLng> getmLocationList() {
        return this.mLocationList;
    }

    private ArrayList<LatLng> createLocationList(String GPSData){
        return new ArrayList<LatLng>();
    }

    public void setmLocationList(ArrayList<LatLng> mLocationList) {
        this.mLocationList = mLocationList;
    }

    /**
     * Add points one-by-one to object's location list
     */
    public void addToLocationList(LatLng newLocation){
        if(mLocationList != null) {
            mLocationList.add(newLocation);
        }
        else{
            mLocationList = new ArrayList<LatLng>();
            mLocationList.add(newLocation);
        }
    }

    // Not used in MyRuns4
    public int getmPrivacy() {
        return mPrivacy;
    }

    // Not used in MyRuns4
    public void setmPrivacy(int mPrivacy) {
        this.mPrivacy = mPrivacy;
    }

    // Convert the location list to a String
    public String getmGPSData(){
        return getJSONFromLatLong(mLocationList);
    }

    /**
     * Given an ArrayList of coordinates representing locations of entry, convert to a String
     * to allow storage in SQL Database
     */
    private String getJSONFromLatLong(ArrayList<LatLng> mLocationList){
        JSONArray jsonGPSData = new JSONArray();
        try{
            for(LatLng currentCoordinate : mLocationList){
                JSONObject jsonCoordinate = new JSONObject();
                jsonCoordinate.put("latitude", currentCoordinate.latitude);
                jsonCoordinate.put("longitude", currentCoordinate.longitude);
                jsonGPSData.put(jsonCoordinate);
            }
        }
        catch(Exception e){
            System.err.println("Failed construction of jsonGPSData");
            e.printStackTrace();
        }
        Log.d("TESTINGLOAD", jsonGPSData.toString());
        return jsonGPSData.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mInputType);
        parcel.writeString(mActivityType);
        parcel.writeString(mDateTime);
        parcel.writeInt(mDuration);
        parcel.writeDouble(mDistance);
        parcel.writeDouble(mAvgPace);
        parcel.writeDouble(mAvgSpeed);
        parcel.writeInt(mCalorie);
        parcel.writeDouble(mClimb);
        parcel.writeInt(mHeartRate);
        parcel.writeString(mComment);
        parcel.writeInt(mPrivacy);
        parcel.writeLong(thisId);
    }
}
