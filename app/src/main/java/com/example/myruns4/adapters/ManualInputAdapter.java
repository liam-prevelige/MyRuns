package com.example.myruns4.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myruns4.R;
import com.example.myruns4.models.ActivityEntryModel;

import java.util.ArrayList;

/**
 * Create manual input options visual for the ManualEntryActivity ListView
 */
public class ManualInputAdapter extends ArrayAdapter<ActivityEntryModel> {
    private ArrayList<ActivityEntryModel> listActivities;   // contains data storing options for a given activity/sport

    public ManualInputAdapter(@NonNull Context context, ArrayList<ActivityEntryModel> listActivities) {
        super(context, 0, listActivities);
        this.listActivities = listActivities;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.event_information_list_entry, parent, false);
        ((TextView) v.findViewById(R.id.event_title)).setText(listActivities.get(position).getActivity());
        ((TextView) v.findViewById(R.id.event_info)).setText(listActivities.get(position).getInformation());

        return v;
    }

    public ArrayList<ActivityEntryModel> getListActivities(){
        return listActivities;
    }
}
