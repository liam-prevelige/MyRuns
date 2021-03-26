package com.example.myruns4.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myruns4.R;
import com.example.myruns4.models.Login;

import java.util.Objects;

/**
 * Create activity to allow user to edit application settings, including privacy, units, sign out,
 * and website access
 *
 * Activity is accessed after logging in and navigating via MainActivity
 */
public class SettingsActivity extends AppCompatActivity {
    private int checkedItem;
    Switch privacySwitch;
    Login currentProfileInfo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");

        currentProfileInfo = new Login(getApplicationContext());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // If the profile has any associated unit preferences, get the stored value (default returns -1)
        checkedItem = currentProfileInfo.getUnitPreference();

        // If the profile has any associated privacy preference, get the stored preference
        privacySwitch = findViewById(R.id.privacy_switch);
        privacySwitch.setChecked(currentProfileInfo.getPrivacySettingEnabled());
    }

    /**
     * Allow for navigation with back arrow back to MainActivity
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(this, MainActivity.class));
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method called when the privacy settings text element is clicked
     * Toggle the switch and record the change in privacy settings preference
     */
    public void onClickPrivacy(View view){
       privacySwitch.toggle();
       currentProfileInfo.setPrivacySettingEnabled(privacySwitch.isChecked());
    }

    /**
     * Method called when the unit preferences text element is clicked
     * Open a dialog with radio boxes that records and stores the unit preferences
     */
    public void onClickUnitPref(View view) {
        final String[] unitOptions = {"Metric (Kilometers)", "Imperial (Miles)"};   // The two options to be displayed with radio buttons

        final AlertDialog.Builder unitPrefDialog = new AlertDialog.Builder(this);
        unitPrefDialog.setTitle("Unit Preference");
        unitPrefDialog.setSingleChoiceItems(unitOptions, checkedItem, new DialogInterface
                .OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
                checkedItem = item;
                currentProfileInfo.setUnitPreference(item);
                currentProfileInfo.setUnitPreferenceChanged(true);
                dialogInterface.dismiss();
            }
        });
        // Allow a user to cancel request or maintain current preference without selected a radio box option
        unitPrefDialog.setPositiveButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertUnitPref = unitPrefDialog.create();
        alertUnitPref.show();
    }

    /**
     * Method called when the webpage text element is clicked
     * Open the tab on the internet with the URL posted in the text element
     */
    public void onClickWebpage(View view) {
        Intent openWebsite = new Intent(Intent.ACTION_VIEW);
        openWebsite.setData(Uri.parse(((TextView)findViewById(R.id.webpage_url)).getText().toString()));
        startActivity(openWebsite);
    }

    /**
     * Method called when the sign out text element is clicked
     * Bring a user back to the LoginActivity page to sign in again
     */
    public void onClickSignOut(View view) {
        finishAffinity();   // Clear activity stack before changing to LoginActivity
        startActivity(new Intent(this, LoginActivity.class));
    }
}
