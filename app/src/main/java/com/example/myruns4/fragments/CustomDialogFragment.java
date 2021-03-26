package com.example.myruns4.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.myruns4.activities.ManualEntryActivity;
import com.example.myruns4.R;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Objects;

/**
 * Handle dialog creation for ManualEntryActivity
 *
 * CustomDialogFragments include a DatePickerDialog, TimePickerDialog, and a custom dialog for text input
 */
public class CustomDialogFragment extends DialogFragment {
    private int selectedItemPosition;
    private static final int DATE_DIALOG_POS = 1;
    private static final int TIME_DIALOG_POS = 2;
    private static final int COMMENT_POSITION = 7;

    public CustomDialogFragment(int selectedItemPosition){
        this.selectedItemPosition = selectedItemPosition;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        switch(selectedItemPosition){       // Determine which dialog to create by checking position of ListView click
            case(DATE_DIALOG_POS):
                final Calendar dateCal = Calendar.getInstance();
                setRetainInstance(true);
                return new DatePickerDialog(Objects.requireNonNull(getActivity()), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        ((ManualEntryActivity) Objects.requireNonNull(getActivity())).updateListViewItems(selectedItemPosition, year+"-"+monthOfYear + "-" +dayOfMonth);
                    }
                }, dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH), dateCal.get(Calendar.DAY_OF_MONTH));

            case(TIME_DIALOG_POS):
                final Calendar timeCal = Calendar.getInstance();
                setRetainInstance(true);
                return new TimePickerDialog(getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                DecimalFormat timeFormat = new DecimalFormat("00");
                                ((ManualEntryActivity) Objects.requireNonNull(getActivity())).updateListViewItems(selectedItemPosition, timeFormat.format(hourOfDay) + ":" + timeFormat.format(minute));
                            }
                        }, timeCal.get(Calendar.HOUR), timeCal.get(Calendar.MINUTE), true);

            default:
                return handleCustomTextDialog();
        }
    }

    /**
     * @return custom dialog with TextInput
     */
    private Dialog handleCustomTextDialog(){
        AlertDialog.Builder inputStringDialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        setRetainInstance(true);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View v = inflater.inflate(R.layout.fragment_custom_dialog, null);
        inputStringDialog.setView(v);

        // Set the title of the Dialog by getting the ManualInputAdapter object from ManualEntryActivity's ArrayList
        // and pulling the String value of the activity
        ((TextView)(v.findViewById(R.id.item_name))).setText(((ManualEntryActivity) getActivity()).getListViewItems(selectedItemPosition).getActivity());

        // Allow the Dialog to accept text if the comment ListView item was chosen (all others receive numbers)
        if(selectedItemPosition == COMMENT_POSITION){
            ((TextInputEditText)v.findViewById(R.id.input_value)).setInputType(InputType.TYPE_CLASS_TEXT);
        }

        inputStringDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


                String units = getUnits();
                // Update the ArrayList containing ManualInputAdapters to have an information value
                // that reflects what the user put into the text box
                ((ManualEntryActivity) Objects.requireNonNull(getActivity())).updateListViewItems(selectedItemPosition, (((TextInputEditText) (v.findViewById(R.id.input_value))).getText()).toString() + " " + units);
                dismiss();
            }
        });
        return inputStringDialog.create();
    }

    // Parse the information originally in the ListView item to get the proper units
    private String getUnits(){
        String units;
        if(selectedItemPosition==COMMENT_POSITION){     // Comment ListView doesn't have units, so don't parse looking for them
            units = "";
        }
        else{
            // Formatting of original values is "x UNIT_NAME," get UNIT_NAME to be returned
            String[] selectedItemInfo = ((ManualEntryActivity)(Objects.requireNonNull(getActivity()))).getListViewItems(selectedItemPosition).getInformation().split(" ");
            units = selectedItemInfo[1];
        }
        return units;
    }
}
