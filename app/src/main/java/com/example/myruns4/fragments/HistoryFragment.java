package com.example.myruns4.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.myruns4.activities.ManualEntryActivity;
import com.example.myruns4.activities.MapActivity;
import com.example.myruns4.adapters.HistoryAdapter;
import com.example.myruns4.models.Login;
import com.example.myruns4.utils.EntryListLoader;

import com.example.myruns4.R;
import com.example.myruns4.models.ExerciseEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Contain past activities that were inputted or measured, displayed as fragment on MainActivity
 * For MyRuns3 this is ManualInputs only
 *
 * Access information from SQL Database
 */
public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ExerciseEntry>>  {
    private static final String PAGE_TYPE = "page_type";
    public static final String EXERCISE_ENTRY_KEY = "exercise_entry_key";
    private static final int GET_FROM_INDEX_COMMAND = 2;
    private static final int GET_ALL_COMMAND = 3;
    private static final int MANUAL_ENTRY_INPUT_VALUE = 0;
    public static final String LOCATION_LIST_KEY = "location list key";

    private ListView historyEntryList;
    private FragmentActivity mActivity;

    private HistoryAdapter historyAdapter;

    // Loader that will be an EntryListLoader, gets the database items asynchronously
    private LoaderManager mLoader;

    // Store all ExerciseEntries in the database
    private List<ExerciseEntry> entriesList;

    public HistoryFragment() {
        // Required empty public constructor
    }

    /**
     * Initialize HistoryFragment as one of several fragments
     */
    public static HistoryFragment newInstance(String position) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle values = new Bundle();
        values.putString(PAGE_TYPE, position);
        fragment.setArguments(values);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
    }

    /**
     * Format the page and ask the loader to provide all inputs in the SQL database
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View listView = inflater.inflate(R.layout.fragment_history, container, false);

        Log.d("TEST", "onCreateView()");

        entriesList = new ArrayList<ExerciseEntry>();
        historyEntryList = listView.findViewById(R.id.history_input_list);

        // Use an adapter to display the ListView of entries
        historyAdapter = new HistoryAdapter(getActivity(), entriesList);
        historyEntryList.setAdapter(historyAdapter);

        createListClickListener();  // Track which entry is clicked

        return listView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start the loader and request all entries in the SQL database
        mLoader = LoaderManager.getInstance(this);
        mLoader.initLoader(GET_ALL_COMMAND, null, this).forceLoad();
    }

    /**
     * Listen to track which entry is clicked to display detailed information via ManualEntryActivity
     * and the provided ExerciseEntry
     */
    private void createListClickListener(){
        historyEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (entriesList.get(position).getmInputType() == MANUAL_ENTRY_INPUT_VALUE) {
                    Intent intent = new Intent(getActivity(), ManualEntryActivity.class);
                    intent.putExtra(EXERCISE_ENTRY_KEY, entriesList.get(position));
                    startActivity(intent);
                }
                else{
                    Intent intent = new Intent(getActivity(), MapActivity.class);
                    Login login = new Login(Objects.requireNonNull(getContext()).getApplicationContext());
                    login.setLoadingMapFromHistory(true);
                    intent.putExtra(EXERCISE_ENTRY_KEY, entriesList.get(position));
                    intent.putParcelableArrayListExtra(LOCATION_LIST_KEY, entriesList.get(position).getmLocationList());
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Ensure the HistoryFragment is updated with the most recent set of entries whenever user has
     * this fragment open
     */
    @Override
    public void onResume() {
        Objects.requireNonNull(mLoader.getLoader(GET_ALL_COMMAND)).onContentChanged();
        super.onResume();
    }


    /**
     * Specify that a specialized AsyncLoader will be used, and provide the corresponding command
     * and information
     */
    @NonNull
    @Override
    public Loader<List<ExerciseEntry>> onCreateLoader(int id, @Nullable Bundle args) {
        if(id == GET_ALL_COMMAND || id == GET_FROM_INDEX_COMMAND){
            if(GET_ALL_COMMAND == id) {
                long rowIndex = 0;
                return new EntryListLoader(mActivity, rowIndex, id);
            }
        }
        return null;
    }

    /**
     * Once the loader has finished pulling information, update the UI by changing the adapter's information
     * and notifying it of this changed list
     */
    @Override
    public void onLoadFinished(@NonNull Loader<List<ExerciseEntry>> loader, List<ExerciseEntry> data) {
        entriesList = data;
        historyAdapter.clear();
        historyAdapter.addAll(entriesList);
        historyAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<ExerciseEntry>> loader) {
    }
}
