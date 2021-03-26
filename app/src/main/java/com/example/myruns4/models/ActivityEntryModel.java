package com.example.myruns4.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Store the default or user-added information that comes from the ManualEntryActivity
 * Allow for the storage and retrieval of both activity name and corresponding information
 *
 * Implement Parcelable and necessary methods so the user-added information can be stored
 * in onSaveInstanceState
 */
public class ActivityEntryModel implements Parcelable {
    private String activity;
    private String information;

    public ActivityEntryModel(String activity, String information){
        this.activity = activity;
        this.information = information;
    }

    protected ActivityEntryModel(Parcel in) {
        activity = in.readString();
        information = in.readString();
    }

    public static final Creator<ActivityEntryModel> CREATOR = new Creator<ActivityEntryModel>() {
        @Override
        public ActivityEntryModel createFromParcel(Parcel in) {
            return new ActivityEntryModel(in);
        }

        @Override
        public ActivityEntryModel[] newArray(int size) {
            return new ActivityEntryModel[size];
        }
    };

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    // Implement necessary default method for Parcelable implementation
    @Override
    public int describeContents() {
        return 0;
    }

    // Implement necessary default method for Parcelable implementation
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(activity);
        parcel.writeString(information);
    }
}
