package com.example.myruns4.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.myruns4.models.ExerciseEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Load exercise entries from the SQL database using a background thread to be displayed in
 * history fragment
 */
public class EntryListLoader extends AsyncTaskLoader<List<ExerciseEntry>> {
    private static final int GET_FROM_INDEX_COMMAND = 2;
    private static final int GET_ALL_COMMAND = 3;

    private ArrayList<ExerciseEntry> entries;
    private ExerciseEntryDbHelper entriesDbHelper;
    private long rowIndex;
    private int entryCommand;

    /**
     * Pull values necessary for all load commands at once
     */
    public EntryListLoader(@NonNull Context context, long rowIndex, int entryCommand) {
        super(context);
        entries = new ArrayList<ExerciseEntry>();
        entriesDbHelper = new ExerciseEntryDbHelper(context);
        this.rowIndex = rowIndex;
        this.entryCommand = entryCommand;
    }

    /**
     * Get data using a background thread, returning a completed list of exercise entries (whose
     * content depends on command) upon completion of queue()
     */
    @Nullable
    @Override
    public List<ExerciseEntry> loadInBackground() {
        if(entryCommand == GET_FROM_INDEX_COMMAND){
            entries.add(entriesDbHelper.fetchEntryByIndex(rowIndex));
        }
        else if(entryCommand == GET_ALL_COMMAND){
            entries = entriesDbHelper.fetchEntries();
        }
        return entries;
    }

    @Override
    protected void onForceLoad() {
        super.onForceLoad();
    }
}
