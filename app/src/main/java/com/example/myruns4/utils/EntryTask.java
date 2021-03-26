package com.example.myruns4.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myruns4.activities.MapActivity;
import com.example.myruns4.models.ExerciseEntry;
import com.example.myruns4.models.Login;

/**
 * Manipulate exercise entries in the SQL database using background threads
 */
public class EntryTask extends AsyncTask<Void, Void, Void> {
    private static final int INSERT_ENTRY_COMMAND = 0;
    private static final int DELETE_ENTRY_COMMAND = 1;
    private static final int DELETE_ALL_ENTRIES_COMMAND = 2;

    private ExerciseEntryDbHelper mEntriesDb;
    private ExerciseEntry operateThisEntry;
    private Context context;
    private int entryCommand;
    private long rowIndex;

    /**
     * Pull data necessarily for all commands at once
     */
    public EntryTask(@NonNull Context context, int entryCommand, long rowIndex, ExerciseEntry entryOperate){
        mEntriesDb = new ExerciseEntryDbHelper(context);
        operateThisEntry = entryOperate;
        this.context = context;
        this.entryCommand = entryCommand;
        this.rowIndex = rowIndex;
    }

    /**
     * Complete command for manipulating SQL database in the background
     */
    @Override
    protected Void doInBackground(Void... voids) {
        if(entryCommand == INSERT_ENTRY_COMMAND){
            rowIndex = mEntriesDb.insertEntry(operateThisEntry);
        }
        else if(entryCommand == DELETE_ENTRY_COMMAND){
            mEntriesDb.removeEntry(rowIndex);
        }
        else if(entryCommand == DELETE_ALL_ENTRIES_COMMAND){
            mEntriesDb.clearAllEntries();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d("TEST", "onPostExecute");
        if(context!=context.getApplicationContext()) {
            ((Activity) context).finish();      // End activity that calls task after completion
        }
    }
}
