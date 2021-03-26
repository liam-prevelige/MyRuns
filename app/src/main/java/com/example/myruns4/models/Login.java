package com.example.myruns4.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Store information related to one individual's Login/Profile
 *
 * Use SharedPreference so information can be accessed across activities and app sessions
 */
public class Login {
    private SharedPreferences profile;

    public void clearProfile(){
        profile.edit().clear().apply();
    }

    public Login(Context activityWithInfo){
        profile = PreferenceManager.getDefaultSharedPreferences(activityWithInfo);
    }

    public void commit(){
        profile.edit().commit();
    }

    public SharedPreferences getProfile() {
        return profile;
    }

    public void setImageURI(String imageURI){
        profile.edit().putString("profileURI", imageURI).apply();
    }

    public String getImageURI(){
        return profile.getString("profileURI", "empty");
    }

    public void setImagePath(String imagePath){
        profile.edit().putString("profilePath", imagePath).apply();
    }

    public String getImagePath(){
        return profile.getString("profilePath", "empty");
    }

    public String getImage(){
        return profile.getString("profilePic", "empty");
    }

    public void setName(String name){
        profile.edit().putString("name", name).apply();
    }

    public String getName(){
        return profile.getString("name", "empty");
    }

    public void setGender(String gender){
        profile.edit().putString("gender", gender).apply();
    }

    public String getGender(){
        return profile.getString("gender", "empty");
    }

    public void setEmail(String email){
        profile.edit().putString("email", email).apply();
    }

    public String getEmail(){
        return profile.getString("email", "empty");
    }

    public void setPassword(String password){
        profile.edit().putString("password", password).apply();
    }

    public String getPassword(){
        return profile.getString("password", "empty");
    }

    public void setPhone(String phone){
        profile.edit().putString("phone", phone).apply();
    }

    public String getPhone(){
        return profile.getString("phone", "empty");
    }

    public void setMajor(String major){
        profile.edit().putString("major", major).apply();
    }

    public String getMajor(){
        return profile.getString("major", "empty");
    }

    public void setDartClass(String dartClass){
        profile.edit().putString("dartClass", dartClass).apply();
    }

    public String getDartClass() {
        return profile.getString("dartClass", "empty");
    }

    /**
     * Set whether the profile information should be filled in at activity start
     */
    public void setNeedAutofill(boolean isFromLoginActivity){
        profile.edit().putBoolean("fromLoginActivity", isFromLoginActivity).apply();
    }

    /**
     * Get whether the profile information should be filled in at activity start
     */
    public boolean getNeedAutofill(){
        return profile.getBoolean("fromLoginActivity", false);
    }

    /**
     * Set whether the user has opted to post anonymous records
     */
    public void setPrivacySettingEnabled(boolean privacySetting){
        profile.edit().putBoolean("privacySettingEnabled", privacySetting).apply();
    }

    /**
     * Get whether the user has opted to post anonymous records
     */
    public boolean getPrivacySettingEnabled(){
        return profile.getBoolean("privacySettingEnabled", false);
    }

    /**
     * Set what units the user wants to use for their activity information
     */
    public void setUnitPreference(int unitPreference){
        profile.edit().putInt("unit preference", unitPreference).apply();
    }

    /**
     * Get what units the user wants to use for their activity information
     */
    public int getUnitPreference(){
        return profile.getInt("unit preference", -1);
    }

    public boolean getUnitPreferenceChanged(){
        return profile.getBoolean("unit preference changed", false);
    }

    public void setUnitPreferenceChanged(boolean changed){
        profile.edit().putBoolean("unit preference changed", changed).apply();
    }


    //Is the TrackingService currently running?
    public boolean getServiceStarted(){
        return profile.getBoolean("service started", false);
    }

    public void setServiceStarted(Boolean serviceStarted){
        profile.edit().putBoolean("service started", serviceStarted).apply();
    }


    // Is the MapActivity currently running?
    public boolean getMapActivityStarted(){
        return profile.getBoolean("map activity started", false);
    }

    public void setMapActivityStarted(Boolean serviceStarted){
        profile.edit().putBoolean("map activity started", serviceStarted).apply();
    }


    // Should the TrackingService and respective threads stop completely?
    public boolean getShouldStop(){
        return profile.getBoolean("should service stop", false);
    }

    public void setShouldStop(Boolean shouldStop){
        profile.edit().putBoolean("should service stop", shouldStop).apply();
    }


    // Was the save button pressed in the MapActivity?
    public boolean getSavePressed(){
        return profile.getBoolean("save pressed", false);
    }

    public void setSavePressed(Boolean savePressed){
        profile.edit().putBoolean("save pressed", savePressed).apply();
    }

    // Is the map being requested one that's loaded from history?
    public boolean getLoadingMapFromHistory(){
        return profile.getBoolean("map loaded from history", false);
    }

    public void setLoadingMapFromHistory(Boolean historyMap){
        profile.edit().putBoolean("map loaded from history", historyMap).apply();
    }

    //Was the map just rotated?
    public boolean getMapRotated(){
        return profile.getBoolean("map rotated", false);
    }

    public void setMapRotated(Boolean mapRotated){
        profile.edit().putBoolean("map rotated", mapRotated).apply();
    }

    public boolean getHasNotified(){
        return profile.getBoolean("notified", false);
    }

    public void setHasNotified(Boolean hasNotified){
        profile.edit().putBoolean("notified", hasNotified).apply();
    }
}
