package com.mabe.productions.hrv_madison.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;

/**
 * Created by Benas on 9/7/2016.
 */
public class ViewPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {

    public MeasurementFragment measurementFragment;
    public DataTodayFragment dataTodayFragment;
    public HistoryFragment historyFragment;

    public ViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        measurementFragment = new MeasurementFragment();
        dataTodayFragment = new DataTodayFragment();
        historyFragment = new HistoryFragment();
    }

    @Override
    public Fragment getItem(int position) {

        switch (position){
            case 0:
                return measurementFragment;

            case 1:
                return dataTodayFragment;

            case 2:
                return historyFragment;

        }

        return null;
    }

    @Override
    public int getCount() {

        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return null;
    }
}