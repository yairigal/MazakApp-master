package project.android.com.mazak.Controller;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;

import project.android.com.mazak.Database.Database;
import project.android.com.mazak.Database.Factory;
import project.android.com.mazak.Database.LoginDatabase;
import project.android.com.mazak.Model.Entities.GradesList;
import project.android.com.mazak.Model.Entities.IrurList;
import project.android.com.mazak.Model.GradesModel;
import project.android.com.mazak.Model.ISearch;
import project.android.com.mazak.Model.getOptions;
import project.android.com.mazak.R;

public class FatherTab extends Fragment implements ISearch {

    private ISearch currentFragmet;
    Database db;
    private String username;
    private String password;
    GradesList grades;
    IrurList irurs;
    HashMap<Integer, GradesList> gradesSorted;
    int numOfYears;
    ProgressBar spinner;
    LinearLayout mainLayout;

    public FatherTab() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_father_tab, container, false);

        spinner = (ProgressBar) view.findViewById(R.id.FatherSpinner);
        mainLayout = (LinearLayout) view.findViewById(R.id.TabsFatherLayout);

        username = getArguments().getString("username");
        password = getArguments().getString("password");
        try {
            db = Factory.getInstance(username, password, getActivity());
            getGradesAsync(view,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    /**
     * Getting grades from memory, if failed getting grades from web.
     *
     * @param view
     */
    private void getGradesAsync(final View view, final getOptions options) {
        new AsyncTask<Void, Void, Void>() {
            public String errorMsg;
            public boolean error;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                toggleSpinner(true, mainLayout, spinner);
            }

            @Override
            protected Void doInBackground(Void... params) {
                if(options == getOptions.fromWeb){
                    getGradesFromWebOnly();
                } else {
                    getGradesFromAnywhere();
                }

                if(error)
                    return null;

                gradesSorted = GradesModel.sortByYears(grades);
                numOfYears = gradesSorted.size();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (error) {
                    showSnackbar(errorMsg);
                    toggleSpinner(false, mainLayout, spinner);
                } else {
                    toggleSpinner(false, mainLayout, spinner);
                    setupTabs(view);
                }
            }

            private void getGradesFromAnywhere() {
                try {
                    grades = db.getGrades(getOptions.fromMemory);
                } catch (Exception e) {
                    try {
                        if (isNetworkAvailable(getContext()))
                            grades = db.getGrades(getOptions.fromWeb);
                        else
                            throw new NetworkErrorException();
                    } catch (Exception e1) {
                        errorMsg = checkErrorTypeAndMessage(e1);
                        error = true;
                    }
                }
            }

            private void getGradesFromWebOnly(){
                try{
                    grades = db.getGrades(getOptions.fromWeb);
                } catch (Exception e) {
                    errorMsg = checkErrorTypeAndMessage(e);
                    error = true;
                }
            }

            private void showSnackbar(String errorMsg) {
                if (errorMsg.contains("password"))
                    Snackbar.make(getView(), errorMsg, Snackbar.LENGTH_LONG).setAction("CHANGE", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                LoginDatabase.getInstance(getContext()).clearLoginInformation();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Intent Login = new Intent(getContext(), LoginActivity.class);
                            startActivity(Login);
                        }
                    }).show();
                else
                    Snackbar.make(getView(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        }.execute();
    }

    private String checkErrorTypeAndMessage(Exception e1) {
        String errorMsg;
        if (e1 instanceof UnknownHostException)
            errorMsg = "'mazak.jct.ac.il' might be down";
        else if (e1 instanceof NullPointerException)
            errorMsg = "Check your username or password";
        else if (e1 instanceof NetworkErrorException)
            errorMsg = "Check your internet connection";
        else
            errorMsg = "Error acquiring grades";
        return errorMsg;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setupTabs(View v) {
        TabLayout tabLayout = (TabLayout) v.findViewById(R.id.tabLayout);
        if (numOfYears > 3)
            tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        for (int i = 0; i < numOfYears; i++)
            tabLayout.addTab(tabLayout.newTab());

        final ViewPager viewPager = (ViewPager) v.findViewById(R.id.viewPager);
        tabLayout.setupWithViewPager(viewPager, true);
        viewPager.setAdapter(new SamplePageAdapter(getFragmentManager()));
        //if(checkHebrew())
        tabLayout.getTabAt(numOfYears - 1).select();
    }

    @Override
    public void Filter(String query) {
        currentFragmet.Filter(query);
    }

    @Override
    public void clearFilter() {
        currentFragmet.clearFilter();
    }

    @Override
    public void Refresh() {
        getGradesAsync(getView(),getOptions.fromWeb);
    }

    private class SamplePageAdapter extends FragmentStatePagerAdapter {
        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        public SamplePageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle b = new Bundle();
            b.putInt("year", position + 1);
            GradesList currentYearList = gradesSorted.get(position + 1);
            b.putSerializable("list", currentYearList);
            Fragment fragment = new gradesViewFragment();
            fragment.setArguments(b);
            currentFragmet = (ISearch) fragment;
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (checkHebrew())
                return getYearTitle(position + 1);
            return getYearTitle(position + 1);
        }

        @Override
        public int getCount() {
            return numOfYears;
        }
    }

    private GradesList getGradesByYear(HashMap<Integer, HashMap<Integer, GradesList>> gradesSorted, int i) {
        GradesList lst = new GradesList();
        for (int j = 0; j < gradesSorted.get(i).size(); j++)
            lst.addAll(gradesSorted.get(i).get(j));
        return lst;
    }


    boolean checkHebrew() {
        return Locale.getDefault().toString().equals("iw_IL") || Locale.getDefault().toString().equals("he_IL");
    }

    String getYearTitle(int year) {
        String title = "";
        switch (year) {
            case 1:
                title = "First Year";
                break;
            case 2:
                title = "Second Year";
                break;
            case 3:
                title = "Third Year";
                break;
            case 4:
                title = "Fourth Year";
                break;
            case 5:
                title = "Fifth Year";
                break;
            default:
                title = "Other";
                break;
        }
        return title;
    }

    public static void toggleSpinner(boolean toggle, View toDismiss, ProgressBar toShow) {
        if (toggle) {
            toDismiss.setVisibility(View.GONE);
            toShow.setVisibility(View.VISIBLE);
            //swipeRefreshLayout.setRefreshing(true);
        } else {
            toDismiss.setVisibility(View.VISIBLE);
            toShow.setVisibility(View.GONE);
            //swipeRefreshLayout.setRefreshing(false);
        }
    }

    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}
