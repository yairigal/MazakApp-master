package project.android.com.mazak.Controller;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import project.android.com.mazak.Database.Database;
import project.android.com.mazak.Database.Factory;
import project.android.com.mazak.Model.Entities.Delegate;
import project.android.com.mazak.Model.Entities.Grade;
import project.android.com.mazak.Model.Entities.GradesList;
import project.android.com.mazak.Model.GradesModel;
import project.android.com.mazak.Model.IRefresh;
import project.android.com.mazak.Model.getOptions;
import project.android.com.mazak.R;


public class AverageFragment extends Fragment implements IRefresh {

    View root;
    Database db;
    GradesList grades;
    private LayoutInflater mainInflaytor;
    Spinner year,sem;
    Button calc;


    public AverageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mainInflaytor = inflater;
        root = inflater.inflate(R.layout.fragment_average, container, false);
        year = (Spinner) root.findViewById(R.id.avgSpinner);
        sem = (Spinner) root.findViewById(R.id.semAvgSpinner);
        calc = (Button) root.findViewById(R.id.calcAvgButton);
        if (getDatabaseFactory()) {
            getGradesAsync(new Delegate() {
                @Override
                public void function(Object obj) {
                    initSpinners();
                    onFinisehd();
                }
            });
        }
        return root;
    }

    private void initSpinners() {
        sem.setEnabled(false);
        final HashMap<Integer, HashMap<Integer, GradesList>> map = GradesModel.sortBySemesterAndYear(grades);
        final ArrayList<String> yearArray = new ArrayList<>();
        for (int i=1;i<=map.size();i++)
            yearArray.add(String.valueOf(i));
        year.setAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item,yearArray));
        year.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sem.setEnabled(true);
                ArrayList<String> semArray = new ArrayList<>();
                for (int i=0;i<map.get(position+1).size();i++)
                    semArray.add(String.valueOf(GradesModel.intToSemster(i)));
                sem.setAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item,semArray));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sem.setEnabled(false);
            }
        });
        calc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GradesList grades = map.get(year.getSelectedItemPosition()+1).get(sem.getSelectedItemPosition());
                HashMap<String, Float> avgs = GradesModel.getAvg(grades);
                calc.setText("ממוצע: "+ roundTo2OrLessAfterPoint(avgs.get("avg")));
            }
        });
    }

    private void getGradesAsync(final Delegate delegate) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    grades = db.getGrades(getOptions.fromMemory);
                } catch (Exception e) {
                    try {
                        grades = db.getGrades(getOptions.fromWeb);
                    } catch (Exception e1) {
                        Snackbar.make(root, "Error getting grades", Snackbar.LENGTH_LONG).show();
                        e1.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                delegate.function(null);
            }
        }.execute();
    }

    private void onFinisehd() {
        calculateAverageAndRename();
        addYearsCards();
    }

    private void calculateAverageAndRename() {
        HashMap<String, Float> map = GradesModel.getAvg(grades);
        String avg = roundTo2OrLessAfterPoint(map.get("avg")),
                nza = roundTo2OrLessAfterPoint(map.get("nz")),
                kod = roundTo2OrLessAfterPoint(map.get("kodesh"));
        TextView total = (TextView) root.findViewById(R.id.averageTextview);
        total.setText(avg);
        TextView nz = (TextView) root.findViewById(R.id.NZ_AVGTextview);
        nz.setText(nza);
        TextView kodesh = (TextView) root.findViewById(R.id.averageKodeshTextview);
        kodesh.setText(kod);
    }

    private String roundTo2OrLessAfterPoint(float num) {
        if (num == ((int) num)) // no digits after point
            return String.valueOf((int)num);
        return String.valueOf(((float) Math.round(num * 100) / 100));
    }

    private boolean getDatabaseFactory() {
        try {
            db = Factory.getInstance();
            return true;
        } catch (Exception e) {
            Snackbar.make(root, e.getMessage(), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }

    private void addYearsCards(){
        HashMap<Integer, GradesList> map = GradesModel.sortByYears(grades);
        ((ViewGroup)(root.findViewById(R.id.mainLayoutAverage))).removeAllViews();
        for(int i = 1;i<=map.size();i++){
            View Mainview = mainInflaytor.inflate(R.layout.average_year,null);
            ((TextView)Mainview.findViewById(R.id.CardTitle_YearAverage)).setText("שנה "+ i);
            ((TextView)Mainview.findViewById(R.id.NZInput_averageYear)).setText(roundTo2OrLessAfterPoint(GradesModel.getAvg(map.get(i)).get("nz")));
            ((TextView)Mainview.findViewById(R.id.averageInput_averageYear)).setText(roundTo2OrLessAfterPoint(GradesModel.getAvg(map.get(i)).get("avg")));
            ((LinearLayout)root.findViewById(R.id.mainLayoutAverage)).addView(Mainview);
        }
    }

    @Override
    public void Refresh() {
        onFinisehd();
    }
}
