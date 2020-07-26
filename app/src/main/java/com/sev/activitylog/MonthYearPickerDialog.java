package com.sev.activitylog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class MonthYearPickerDialog extends DialogFragment {
    private Spinner year, month;
    private DatePickerDialog.OnDateSetListener listener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialog = LayoutInflater.from(getContext()).inflate(R.layout.month_year_dialog_layout, null);
        year = (Spinner)dialog.findViewById(R.id.yearSpinner);
        final Calendar cal = Calendar.getInstance();
        final int maxYear = cal.get(Calendar.YEAR);
        final int minYear = 2000;
        String[] vals = new String[maxYear - minYear + 1];
        for(int i = 0; i < vals.length; ++i){
            vals[i] = String.valueOf(minYear + i);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, vals);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        year.setAdapter(adapter);
        year.setSelection(vals.length - 1);
        month = (Spinner)dialog.findViewById(R.id.monthSpinner);
        builder.setView(dialog);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(listener != null){
                    listener.onDateSet(null, year.getSelectedItemPosition() + minYear, month.getSelectedItemPosition(), 0);
                }
            }
        }).setNegativeButton(R.string.cancel, null);
        return builder.create();
    }
    public void setListener(DatePickerDialog.OnDateSetListener l) {
        listener = l;
    }
}
