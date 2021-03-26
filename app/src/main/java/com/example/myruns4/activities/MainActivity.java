package com.example.myruns4.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.myruns4.R;
import com.example.myruns4.adapters.ViewPageAdapter;
import com.example.myruns4.models.ExerciseEntry;
import com.example.myruns4.models.Login;

import java.util.ArrayList;

/**
 * Create main activity page that holds the two fragments for the activity tracking and history
 *
 * Have a bottom bar and a ViewPager2 that allows for navigation between the two fragments, as well
 * as menu options to change profile and access app settings
 */

public class MainActivity extends AppCompatActivity {
    private Login currentLogin;
    private ViewPager2 pager;
    private ViewPageAdapter pagerAdapter;
    private static final int START_FRAG_INDEX = 0;
    private static final int HISTORY_FRAG_INDEX = 1;

    ArrayList<ExerciseEntry> entryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentLogin = new Login(getApplicationContext());
        setTitle("MainActivity");

        currentLogin.setMapRotated(false);

        // Initialize the ViewPager section of MainActivity to contain the two start and history fragments
        pager = findViewById(R.id.view_pager_main);
        pagerAdapter = new ViewPageAdapter(getSupportFragmentManager(), getLifecycle());
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(2);
    }

    /**
     * Allow for access to a menu that contains options to edit profile and change application
     * settings
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Navigate user to proper activity (edit profile or change application settings) depending
     * on option selected
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:
                handleSettings();
                return true;
            case R.id.edit_profile:
                handleProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Navigate to SettingsActivity page
     */
    private void handleSettings(){
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    /**
     * Navigate back to a modified RegisterActivity that contains current profile details
     * and restricts some profile changes
     */
    private void handleProfile(){
        currentLogin.setNeedAutofill(true);
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
    }

    public void setEntryView(ArrayList<ExerciseEntry> entryView){
        this.entryView = entryView;
    }

    /**
     * Create start fragment
     */
    public void onClickStart(MenuItem item) {
        pager.setCurrentItem(START_FRAG_INDEX);
        currentLogin.setShouldStop(false);
        currentLogin.setSavePressed(false);
        currentLogin.setServiceStarted(false);
    }

    /**
     * Create history fragment
     */
    public void onClickHistory(MenuItem item) {
        pager.setCurrentItem(HISTORY_FRAG_INDEX);
    }
}
