package com.mabe.productions.hrv_madison.fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.github.channguyen.rsv.RangeSliderView;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.mabe.productions.hrv_madison.MainScreenActivity;
import com.mabe.productions.hrv_madison.R;
import com.mabe.productions.hrv_madison.User;
import com.mabe.productions.hrv_madison.Utils;
import com.mabe.productions.hrv_madison.bluetooth.BluetoothGattService;
import com.mabe.productions.hrv_madison.bluetooth.LeDevicesDialog;
import com.mabe.productions.hrv_madison.database.FeedReaderDbHelper;
import com.mabe.productions.hrv_madison.measurements.BPM;
import com.mabe.productions.hrv_madison.measurements.FrequencyMethod;
import com.mabe.productions.hrv_madison.measurements.RMSSD;
import com.shawnlin.numberpicker.NumberPicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static android.R.id.message;
import static android.support.design.R.attr.layout_scrollFlags;
import static android.support.design.R.attr.theme;


public class MeasurementFragment extends Fragment {

    private NumberPicker measurement_duration;
    private TextView txt_hr;
    private TextView txt_hrv;
    private TextView txt_hr_value;
    private TextView txt_hrv_value;
    public TextView txt_connection_status;
    private AppCompatButton btn_start_measuring;
    private ProgressBar progressbar_measurement;

    private LineChart chart_hr;

    private int[] interval_values;

    private User user;

    private int times = 0;
    private int timePassed = 0;
    private CountDownTimer countDownTimer = null;

    public boolean shouldStartMeasurementImmediately = false;

    private int currentMeasurementState = STATE_WAITING_TO_MEASURE;

    private static final int STATE_MEASURING = 0;
    private static final int STATE_WAITING_TO_MEASURE = 1;
    private static final int STATE_REVIEW_DATA = 2;
    private int hearRate=0;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.measurement_fragment, container, false);
        initializeViews(view);
        setFonts();
        return view;
    }



    private void initializeViews(View view) {


        txt_connection_status = view.findViewById(R.id.txt_connection_status);
        txt_hr = view.findViewById(R.id.txt_hr);
        txt_hr_value = view.findViewById(R.id.txt_hr_value);
        txt_hrv = view.findViewById(R.id.txt_hrv);
        txt_hrv_value = view.findViewById(R.id.txt_hrv_value);
        chart_hr = view.findViewById(R.id.hr_chart);
        //Customizing HR chart
        chart_hr.setData(new LineData());
        chart_hr.getLineData().setDrawValues(false);
        chart_hr.getLegend().setEnabled(false);
        chart_hr.getXAxis().setDrawAxisLine(false);
        chart_hr.getAxisRight().setDrawAxisLine(false);
        chart_hr.getAxisLeft().setDrawAxisLine(false);
        chart_hr.getAxisLeft().setDrawGridLines(false);
        chart_hr.getXAxis().setDrawGridLines(false);
        chart_hr.getAxisRight().setDrawGridLines(false);
        chart_hr.setDescription(null);
        chart_hr.getAxisLeft().setDrawLabels(false);
        chart_hr.getAxisRight().setDrawLabels(false);
        chart_hr.getXAxis().setDrawLabels(false);
        chart_hr.setTouchEnabled(false);
        chart_hr.setViewPortOffsets(0f, 0f, 0f, 0f);
        //chart_hr.setAutoScaleMinMaxEnabled(true);

        btn_start_measuring = view.findViewById(R.id.button_start_measuring);

        btn_start_measuring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Cia tiesiog tuos state patikrinti ir paziureti, ka tas mygtukas turi daryti dabar is vienos is triju galimybiu
                switch(currentMeasurementState){

                    case STATE_MEASURING:
                        cancelMeasurement();
                        break;

                    case STATE_WAITING_TO_MEASURE:

                        Date lastMeasurementDate = Utils.getDateFromString(Utils.readFromSharedPrefs_string(getContext(), FeedReaderDbHelper.FIELD_LAST_MEASUREMENT_DATE, FeedReaderDbHelper.SHARED_PREFS_USER_DATA));

                        boolean hasMeasuredToday = false;

                        if(lastMeasurementDate != null){
                            Calendar calendar = Calendar.getInstance();
                            int today = calendar.get(Calendar.DAY_OF_YEAR);
                            calendar.setTime(lastMeasurementDate);
                            int measurementDay = calendar.get(Calendar.DAY_OF_YEAR);
                            hasMeasuredToday = today == measurementDay ? true : false;

                        }else{
                            //TODO: user is about to measure for the first time. We may want to add a tutorial or something
                        }


                        if(hasMeasuredToday){

                            Utils.buildAlertDialogPrompt(
                                    getContext(),
                                    getString(R.string.please_wait),
                                    getString(R.string.already_measured_today_message),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            //if device is connected
                                            if(BluetoothGattService.isGattDeviceConnected){
                                                start_calculation();
                                            }else{
                                                autoConnectDevice();
                                            }
                                        }
                                    },
                                    null
                            );


                        }else{
                            //if device is connected
                            if(BluetoothGattService.isGattDeviceConnected){
                                start_calculation();
                            }else{
                                autoConnectDevice();
                            }
                        }


                        break;

                    case STATE_REVIEW_DATA:
                        currentMeasurementState = STATE_WAITING_TO_MEASURE;
                        cancelMeasurement();

                        //todo: use static viewpager variable
                        //Switching to today's tab and updating data
                        ViewPager parentViewPager = getActivity().findViewById(R.id.viewpager);
                        ViewPagerAdapter adapter = (ViewPagerAdapter) parentViewPager.getAdapter();
                        adapter.dataTodayFragment.updateData();
                        parentViewPager.setCurrentItem(1);
                        break;
                }

            }
        });

        measurement_duration = view.findViewById(R.id.number_picker);
        progressbar_measurement = view.findViewById(R.id.measurement_progress_bar);
        progressbar_measurement.setMax(100);

    }

    private void addEntry(int hr) {

        //TODO: bubbles
        LineData data = chart_hr.getData();
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        if (set == null) {
            //Creating a line with single hr value
            ArrayList<Entry> singleValueList = new ArrayList<>();
            singleValueList.add(new Entry(0, hr));
            set = new LineDataSet(singleValueList, "HR");
            set.setLineWidth(getContext().getResources().getDimension(R.dimen.line_width));
            set.setDrawValues(false);
            set.setDrawCircleHole(false);
            set.setCircleRadius(getContext().getResources().getDimension(R.dimen.circle_radius));
            set.setCircleColor(Color.parseColor("#F62459"));
            set.setColor(Color.parseColor("#F62459"));
            set.setDrawFilled(true);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColors(new int[]{
                    Color.parseColor("#a6f62459"),
                    Color.TRANSPARENT
            });
            drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setSize(240, 160);
            set.setFillDrawable(drawable);
            data.addDataSet(set);
        } else {
            set.addEntry(new Entry(set.getEntryCount(), hr));
        }

        data.notifyDataChanged();
        chart_hr.notifyDataSetChanged();

        //chart_hr.setData(data);
        //chart_hr.animate();
        //chart_hr.moveViewToX(set.getEntryCount());
        chart_hr.setVisibleXRangeMaximum(6);
        chart_hr.setVisibleXRangeMinimum(6);
        //chart_hr.setAutoScaleMinMaxEnabled(true);

        chart_hr.moveViewToAnimated(set.getEntryCount(), chart_hr.getY(), YAxis.AxisDependency.RIGHT, 800l);

    }

    private void setFonts() {
        Typeface face_slogan = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/CORBEL.TTF");
        Typeface futura = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/futura_light.ttf");

        Typeface verdana = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/Verdana.ttf");


        txt_hr.setTypeface(futura);
        txt_hr_value.setTypeface(futura);
        txt_hrv.setTypeface(futura);
        txt_connection_status.setTypeface(futura);
        txt_hrv_value.setTypeface(futura);
    }

    //Method to receive data from GAT SERVER if connected.
    public void onMeasurement(int heartRate, int[] intervals) {
        this.hearRate = heartRate;
        interval_values = intervals;


        txt_hr_value.setText("" + heartRate);
        addEntry(heartRate);

    }

    public void start_calculation(){
        currentMeasurementState = STATE_MEASURING;
        txt_connection_status.setText(R.string.measuring);
        btn_start_measuring.setText(R.string.cancel);
        measurement_duration.setEnabled(false);




                //Calculation objects
                final RMSSD hrv = new RMSSD();
                final FrequencyMethod fft = new FrequencyMethod();
                final BPM bpm = new BPM();


                //Starting measuring
                countDownTimer = new CountDownTimer(measurement_duration.getValue()*60000,1000l){
                    @Override
                    public void onTick(long l) {
                        hrv.calculateRMSSD();
                        timePassed++;

                        if(interval_values == null){
                            return;
                        }

                        //Calculating FrequencyDomainMethod: 'times' has to be power of 2
                        for(int i = 0; i<interval_values.length; i++){
                            times++;
                            fft.add_to_freq_array(interval_values[i]);
                            if(times==16 || times == 64 || times==256){
                                Log.i("DATA",""+times);
                                fft.calculate_frequencies(times);

                            }


                        }
                        long duration = measurement_duration.getValue()*60000;

                        progressbar_measurement.setProgress((int) ( ( timePassed*1000d / (double) duration) * 100d));

                        //Calculating HRV
                        hrv.addIntervals(interval_values);
                        bpm.addBPM(hearRate);
                        //Seting values
                        txt_hrv_value.setText(String.valueOf(hrv.getRmssd()));
                    }

                    @Override
                    public void onFinish() {
/*
                        rmssd_value = hrv.calculateRMSSD();
                        LF = fft.getLF_value();
                        HF = fft.getHF_value();
                        VLF = fft.getVLF_value();
                        VHF = fft.getVHF_value();

                        Log.i("DATA", "Highest BPM: " + highest_bpm + "|" +
                                "Lowest BPM: " + lowest_bpm + "|"+
                                "Highest HRV: " + highest_rmssd + "|"+
                                "Lowest HRV: " + lowest_rmssd + "|"+
                                "LF: " + LF + "|"+
                                "HF: " + HF + "|"+
                                "VLF : " + VLF + "|"+
                                "VHF : " + VHF);
*/


                        User.addMeasurementData(getContext(), hrv, bpm, fft, true);
                        User user = User.getUser(getContext());
                        user.generateDailyReccomendation(getContext());


                        //Getting last measurement date
                        Date lastMeasurementDate = Utils.getDateFromString(Utils.readFromSharedPrefs_string(getContext(), FeedReaderDbHelper.FIELD_LAST_MEASUREMENT_DATE, FeedReaderDbHelper.SHARED_PREFS_USER_DATA));

                        Calendar calendar = Calendar.getInstance();
                        int thisWeek = calendar.get(Calendar.WEEK_OF_YEAR);
                        String todayInString = Utils.getStringFromDate(calendar.getTime());

                        if(lastMeasurementDate != null){

                            calendar.setTime(lastMeasurementDate);
                            int lastMeasurementWeek = calendar.get(Calendar.WEEK_OF_YEAR);

                            //Checking if user has measured this week. If not, updating user's weekly program
                            if(thisWeek != lastMeasurementWeek){
                                user.generateWeeklyProgram(getContext());
                            }

                        }else{
                            //User has measured for the first time ever
                            user.generateWeeklyProgram(getContext());
                        }




                        Utils.Vibrate(getContext(),1000);
                        Utils.saveToSharedPrefs(getContext(), FeedReaderDbHelper.FIELD_LAST_MEASUREMENT_DATE, todayInString, FeedReaderDbHelper.SHARED_PREFS_USER_DATA);

                        txt_connection_status.setText(R.string.measurement_is_over);
                        currentMeasurementState = STATE_REVIEW_DATA;
                        btn_start_measuring.setText(R.string.review_btn);

                    }
                }.start();
    }


//TODO: change text to measuring.... while measuring
    public void disconnected(){
        currentMeasurementState = STATE_WAITING_TO_MEASURE;
        cancelMeasurement();
        txt_connection_status.setText(R.string.failed_connection_status);
        txt_hr_value.setText("-");
        btn_start_measuring.setText(R.string.measure_btn);

    }


    //Connection to bt device and gatt server
    private void autoConnectDevice(){

        String MAC_adress = Utils.readFromSharedPrefs_string(getContext(), FeedReaderDbHelper.BT_FIELD_MAC_ADRESS,FeedReaderDbHelper.SHARED_PREFS_DEVICES);
        String device_name = Utils.readFromSharedPrefs_string(getContext(),FeedReaderDbHelper.BT_FIELD_DEVICE_NAME,FeedReaderDbHelper.SHARED_PREFS_DEVICES);


        //If there is saved device --> connect
        if(!MAC_adress.equals("") && Utils.isBluetoothEnabled()){
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_adress);
            shouldStartMeasurementImmediately = true;
            getContext().startService(new Intent(getContext(), BluetoothGattService.class).putExtra("device",device));
            txt_connection_status.setText(getString(R.string.connecting_to) + " " + device_name);
        }else{

            //If there is no saved device --> add one
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=  PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MainScreenActivity.PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }else{


                if(Utils.isBluetoothEnabled()){
                    shouldStartMeasurementImmediately = true;
                    LeDevicesDialog dialog = new LeDevicesDialog(getContext());
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if(!BluetoothGattService.isGattDeviceConnected){
                                shouldStartMeasurementImmediately = false;
                            }
                        }
                    });

                }else{
                    Toast.makeText(getContext(), "Please enable bluetooth!", Toast.LENGTH_LONG).show(); //TODO: add a nice dialog or something
                }
            }
        }

    }

    //TODO: check if timer gets cancelled (it most likely does)
    private void cancelMeasurement(){
        currentMeasurementState = STATE_WAITING_TO_MEASURE;
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        measurement_duration.setEnabled(true);
        btn_start_measuring.setText(R.string.measure_btn);
        txt_connection_status.setText("");
        progressbar_measurement.setProgress(0);
        txt_hrv_value.setText("-");
        timePassed = 0;
        times=0;
    }



}