package com.mabe.productions.hrv_madison.fragments;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.circularprogressbar.CircularProgressBar;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.mabe.productions.hrv_madison.MainScreenActivity;
import com.mabe.productions.hrv_madison.PulseZoneView;
import com.mabe.productions.hrv_madison.R;
import com.mabe.productions.hrv_madison.User;
import com.mabe.productions.hrv_madison.Utils;
import com.mabe.productions.hrv_madison.bluetooth.BluetoothGattService;
import com.mabe.productions.hrv_madison.bluetooth.LeDevicesDialog;
import com.mabe.productions.hrv_madison.database.FeedReaderDbHelper;
import com.mabe.productions.hrv_madison.measurements.WorkoutMeasurements;
import com.tooltip.Tooltip;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


import pl.droidsonroids.gif.GifImageView;

//todo: kol workoutina, reikia patikrinti ar pasibaige/nepasibaige nustatytas laikas
public class WorkoutFragment extends Fragment {

    private static final long VIBRATE_DURATION_TIME_ENDED = 1000l;
    private static final long VIBRATE_DURATION_CONNECTION_LOST = 5000l;

    private CircularProgressBar progressbar_duration;
    private TextView txt_calories_burned;
    private TextView txt_current_pace;
    private TextView txt_distance;
    private TextView txt_bpm;
    private EditText editText_seconds;
    private EditText editText_minutes;
    public TextView txt_connection_status;
    private AppCompatButton btn_toggle;
    private AppCompatButton button_personalised_workout;
    private ImageView img_pause;
    private ImageView img_stop;
    private LinearLayout layout_workout_progress;
    private LinearLayout layout_time;
    private LinearLayout layout_reccomended_workout;
    private LinearLayout layout_bpm;
    private LinearLayout layout_pulse_zone;

    private PulseZoneView pulseZoneView;

    private TextView txt_reccomended_duration;
    private TextView txt_reccomended_pulse;
    private TextView reccomended_pulse;
    private TextView reccomended_duration;
    private TextView txt_minutes;
    private TextView txt_pulse_zone;
    private TextView txt_personolized_workout;
    private AppCompatImageButton imgButton_view_duration_info;
    private AppCompatImageButton imgButton_view_pulse_info;

    private TextView txt_intensity;
    private TextView txt_intensity_status;
    public Tooltip infoDuration =null;
    public Tooltip infoPulse =null;
    private GifImageView workout_tab_running_gif;

    private Thread pauseThread;
    private Timer timer = null;
    public boolean shouldStartWorkoutImmediately = false;
    private static final long TIMER_STEP = 1000;


    //todo: we calculate pulse zone, need to display it;

    public static final int STATE_BEFORE_WORKOUT = 0;
    public static final int STATE_WORKING_OUT = 1;
    public static final int STATE_TIME_ENDED = 2;
    public static final int STATE_PAUSED = 3;

    private long timePassed = 0;
    private double calories_burned = 0;
    private int pulse_zone = 0;
    private long userSpecifiedWorkoutDuration = 0;
    private boolean isTimerRunning = false;
    private boolean runThread;
    int workout_state = STATE_BEFORE_WORKOUT;
    private boolean isLocationListeningEnabled= false;

    private float totalDistance = 0;


    private ArrayList<LatLng> route = new ArrayList<>();
    private ArrayList<Integer> bpmArrayList = new ArrayList<Integer>();
    private ArrayList<Float> paceData = new ArrayList<Float>();

    private FusedLocationProviderClient mFusedLocationClient;

    private Animation anim_left_to_right;
    private Animation anim_right_to_left;
    private Animation anim_bottom_top_delay;
    private Animation anim_top_to_bottom_delay;
    private Animation anim_running_man_left_to_right;
    private Animation anim_top_to_bottom;
    private Animation anim_fade_out;
    private int required_pulse_zone;
    private float HRMax;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {



        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.workout_fragment, container, false);

        User userInfo = User.getUser(getContext());

        initializeViews(view);
        initializeAnimations();
        setState(STATE_BEFORE_WORKOUT);
        setFonts();
        updateData();
        return view;
    }

    public void disconnected(){
        txt_connection_status.setText(R.string.failed_connection_status);
        //If user is measuring, pausing the measurement
        if(workout_state == STATE_WORKING_OUT || workout_state == STATE_TIME_ENDED){
            txt_connection_status.setText(R.string.lost_connection);
            Utils.vibrate(getContext(), VIBRATE_DURATION_CONNECTION_LOST);
            setState(STATE_PAUSED);
        }
    }

    private void initializeViews(View rootView){
        txt_intensity = rootView.findViewById(R.id.txt_intensity);
        txt_intensity_status = rootView.findViewById(R.id.txt_intensity_status);
        pulseZoneView = rootView.findViewById(R.id.pulse_zone_progress);
        layout_pulse_zone = rootView.findViewById(R.id.layout_pulse_zone);
        imgButton_view_pulse_info = rootView.findViewById(R.id.imgButton_view_pulse_info);
        imgButton_view_duration_info = rootView.findViewById(R.id.imgButton_view_duration_info);
        layout_bpm = rootView.findViewById(R.id.layout_bpm);
        txt_personolized_workout = rootView.findViewById(R.id.txt_personolized_workout);
        button_personalised_workout = rootView.findViewById(R.id.button_personalised_workout);
        layout_reccomended_workout = rootView.findViewById(R.id.layout_reccomended_workout);
        txt_reccomended_duration = rootView.findViewById(R.id.txt_reccomended_duration);
        txt_reccomended_pulse = rootView.findViewById(R.id.txt_reccomended_pulse);
        reccomended_pulse = rootView.findViewById(R.id.reccomended_pulse_zone);
        reccomended_duration = rootView.findViewById(R.id.reccomended_duration);
        txt_minutes = rootView.findViewById(R.id.txt_minutes);
        txt_pulse_zone = rootView.findViewById(R.id.txt_pulse_zone);
        progressbar_duration = rootView.findViewById(R.id.progress_bar_duration);
        txt_calories_burned = rootView.findViewById(R.id.calories_burned);
        txt_current_pace = rootView.findViewById(R.id.running_pace);
        txt_distance = rootView.findViewById(R.id.distance_run);
        txt_bpm = rootView.findViewById(R.id.workout_bpm);
        txt_connection_status = rootView.findViewById(R.id.txt_connection_status_workout);
        img_pause = rootView.findViewById(R.id.img_pause_workout);
        img_stop = rootView.findViewById(R.id.img_stop_workout);
        layout_workout_progress = rootView.findViewById(R.id.workout_progress_layout);
        editText_minutes = rootView.findViewById(R.id.edittext_minutes);
        editText_seconds = rootView.findViewById(R.id.edittext_seconds);
        layout_time = rootView.findViewById(R.id.time_layout);
        img_stop.setOnClickListener(stopButtonListener);
        workout_tab_running_gif = rootView.findViewById(R.id.workout_tab_running_gif);
        setupEditTextBehavior();


        imgButton_view_duration_info.setOnClickListener(durationInfoListener);
        imgButton_view_pulse_info.setOnClickListener(durationPulseListener);
        btn_toggle = rootView.findViewById(R.id.button_start_workout);
    }



    public void updateData(){
        User user = User.getUser(getContext());
        required_pulse_zone = user.getPulseZone();
        pulseZoneView.setRequiredPulseZone(required_pulse_zone);
        editText_minutes.setText("" + (int) user.getWorkoutDuration());
        reccomended_duration.setText("" + (int) user.getWorkoutDuration());
        reccomended_pulse.setText( user.getPulseZone()+""+Utils.getNumberSuffix(user.getPulseZone()));
    }

    private void initializeAnimations(){
        anim_left_to_right = AnimationUtils.loadAnimation(getContext(), R.anim.left_to_right);
        anim_right_to_left = AnimationUtils.loadAnimation(getContext(), R.anim.right_to_left);
        anim_running_man_left_to_right = AnimationUtils.loadAnimation(getContext(), R.anim.running_man_left_to_right);
        anim_bottom_top_delay = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_to_top_delay);
        anim_top_to_bottom_delay = AnimationUtils.loadAnimation(getContext(), R.anim.top_to_bottom_delay);
        anim_top_to_bottom = AnimationUtils.loadAnimation(getContext(), R.anim.top_to_bottom);
        anim_fade_out = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);

    }

    private void startedWorkoutAnimations(){

        workout_tab_running_gif.startAnimation(anim_running_man_left_to_right);
        img_stop.startAnimation(anim_left_to_right);
        img_pause.startAnimation(anim_right_to_left);
        layout_workout_progress.startAnimation(anim_top_to_bottom_delay);
    }

    private void timeEndedAnimations(){
        btn_toggle.startAnimation(anim_top_to_bottom_delay);
    }

    private boolean checkForGPS(){

        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=  PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MainScreenActivity.PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            return false;
        }

        if(!Utils.isGPSEnabled(getContext())){
            Toast.makeText(getContext(), "Please enable GPS!", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    //todo: clean up
    private boolean autoConnectDevice(){
        String MAC_adress = Utils.readFromSharedPrefs_string(getContext(), FeedReaderDbHelper.BT_FIELD_MAC_ADRESS, FeedReaderDbHelper.SHARED_PREFS_DEVICES);
        String device_name = Utils.readFromSharedPrefs_string(getContext(),FeedReaderDbHelper.BT_FIELD_DEVICE_NAME,FeedReaderDbHelper.SHARED_PREFS_DEVICES);

        if(BluetoothGattService.isGattDeviceConnected){
            return true;
        }

        //If there is saved device --> connect
        if(!MAC_adress.equals("") && Utils.isBluetoothEnabled()){
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC_adress);
            shouldStartWorkoutImmediately = true;
            getContext().startService(new Intent(getContext(), BluetoothGattService.class).putExtra("device", device));
            txt_connection_status.setText(getString(R.string.connecting_to) + " " + device_name);
            return false;
        }else{

            //If there is no saved device --> add one


                if(Utils.isBluetoothEnabled()){
                    shouldStartWorkoutImmediately = true;
                    LeDevicesDialog dialog = new LeDevicesDialog(getContext());
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if(!BluetoothGattService.isGattDeviceConnected){
                                shouldStartWorkoutImmediately = false;
                            }
                        }
                    });

                }else{
                    //todo: fix to open a dialog
                    Toast.makeText(getContext(), "Please enable bluetooth!", Toast.LENGTH_LONG).show(); //TODO: add a nice dialog or something


                }

            return false;

        }
    }


    private View.OnClickListener durationInfoListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            if(infoPulse!=null){
                infoPulse.dismiss();
            }

            if(infoDuration ==null){
                infoDuration = new Tooltip.Builder(view)
                        .setText("This is reccomended duration of your workout")
                        .setDismissOnClick(true)
                        .setBackgroundColor(getActivity().getResources().getColor(R.color.colorAccent))
                        .setTextColor(getActivity().getResources().getColor(R.color.white))
                        .setCornerRadius(7f)
                        .setGravity(Gravity.TOP)
                        .show();

            }

            if(!infoDuration.isShowing()){
                infoDuration.show();
            }

        }
    };

    private View.OnClickListener durationPulseListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            if(infoDuration!=null){
                infoDuration.dismiss();
            }

            if(infoPulse ==null){
                infoPulse = new Tooltip.Builder(view)
                        .setText("This is recommended pulse zone of your workout")
                        .setDismissOnClick(true)
                        .setBackgroundColor(getActivity().getResources().getColor(R.color.colorAccent))
                        .setTextColor(getActivity().getResources().getColor(R.color.white))
                        .setCornerRadius(7f)
                        .setGravity(Gravity.TOP)
                        .show();

            }

            if(!infoPulse.isShowing()){
                infoPulse.show();
            }

        }
    };

    private View.OnClickListener resumeButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            if(BluetoothGattService.isGattDeviceConnected){
                runThread = false;
                setState(STATE_WORKING_OUT);
            }else{
                Toast.makeText(getContext(), "Please connect heart rate monitor!", Toast.LENGTH_LONG).show();
            }
        }
    };

    private View.OnClickListener pauseButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            timePauseFlashing();
            setState(STATE_PAUSED);
        }
    };

    private View.OnClickListener stopButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Utils.buildAlertDialogPrompt(getContext(),
                                         getString(R.string.please_wait),
                                         getString(R.string.do_you_really_want_to_stop_training_prompt),
                                         getString(R.string.end),
                                         getString(R.string.cancel),
                                         new DialogInterface.OnClickListener() {
                                             @Override
                                             public void onClick(DialogInterface dialog, int which) {

                                                 runThread = false;

                                                 setState(STATE_BEFORE_WORKOUT);
                                             }
                                         },
                                         null
            );
        }
    };

    private View.OnClickListener reviewProgressButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            calories_burned = Math.round(calories_burned * 100.0) / 100.0;
            totalDistance = (float) (Math.round(totalDistance * 100.0) / 100.0);

            WorkoutMeasurements workout = new WorkoutMeasurements(
                    Calendar.getInstance().getTime(),
                    userSpecifiedWorkoutDuration,
                    0,
                    Utils.convertIntArrayListToArray(bpmArrayList),
                    Utils.convertFloatArrayListToArray(paceData),
                    Utils.convertLatLngArrayListToArray(route),
                    (float)calories_burned,
                    MainScreenActivity.user.getPulseZone(),
                    totalDistance
            );

            User.addWorkoutData(getContext(), workout, true);



            setState(STATE_BEFORE_WORKOUT);
            ViewPager parentViewPager = getActivity().findViewById(R.id.viewpager);
            ViewPagerAdapter adapter = (ViewPagerAdapter) parentViewPager.getAdapter();
            adapter.dataTodayFragment.updateData();
            parentViewPager.setCurrentItem(1);



        }
    };

    private View.OnClickListener startTrainingButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //todo: check if permission is granted and gps is on
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());



            if(!checkForGPS()){
                return;
            }

            if(BluetoothGattService.isGattDeviceConnected){
                 setState(STATE_WORKING_OUT);

            }else{
                Utils.buildAlertDialogPrompt(
                        getContext(),
                        "Please wait!",
                        "Do you want to proceed to workout without HR monitor?",
                        "Yes",
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setState(STATE_WORKING_OUT);
                            }
                        },
                        null
                );
            }
        }
    };


    //Transitions the fragment from one state to another
    public void setState(int state){
        int previous_state = workout_state;
        workout_state = state;

        switch(workout_state){

            case STATE_BEFORE_WORKOUT:
                workout_tab_running_gif.setVisibility(View.INVISIBLE);
                layout_pulse_zone.setVisibility(View.GONE);
                bpmArrayList.clear();
                paceData.clear();
                route.clear();
                btn_toggle.setVisibility(View.VISIBLE);
                btn_toggle.setText(R.string.start_training);
                btn_toggle.setOnClickListener(startTrainingButtonListener);
                stopLocationListener();
                //todo: set texts based on reccomended workout duration
                editText_seconds.setText("00");
                editText_minutes.setText("00");
                layout_bpm.setVisibility(View.GONE);
                layout_reccomended_workout.setVisibility(View.VISIBLE);
                img_pause.setVisibility(View.GONE);
                img_stop.setVisibility(View.GONE);
                layout_workout_progress.setVisibility(View.GONE);
                setProgressBarDuration(1,1);
                editText_minutes.setEnabled(true);
                editText_seconds.setEnabled(true);

                //Todo: fix that, it's ugly now :(
                User user = User.getUser(getContext());
                if(user.getTodaysMeasurement()==null){
                    button_personalised_workout.setVisibility(View.VISIBLE);
                    txt_personolized_workout.setVisibility(View.GONE);
                }else{
                    button_personalised_workout.setVisibility(View.GONE);
                    txt_personolized_workout.setVisibility(View.VISIBLE);
                }
                //I suspect that disabling editTexts removes their listeners
                setupEditTextBehavior();
                cancelTimer();
                break;

            case STATE_WORKING_OUT:
                if(infoDuration!=null){
                    infoDuration.dismiss();
                }
                if(infoPulse!=null){
                    infoPulse.dismiss();
                }
                workout_tab_running_gif.setVisibility(View.VISIBLE);
                if(previous_state == STATE_BEFORE_WORKOUT){
                    startedWorkoutAnimations();
                }

                startLocationListener();
                layout_bpm.setVisibility(View.VISIBLE);
                layout_reccomended_workout.setVisibility(View.GONE);
                txt_personolized_workout.setVisibility(View.GONE);
                button_personalised_workout.setVisibility(View.GONE);
                btn_toggle.setVisibility(View.GONE);
                img_pause.setVisibility(View.VISIBLE);
                img_pause.setImageResource(R.drawable.ic_pause_button);
                img_stop.setVisibility(View.VISIBLE);
                img_pause.setOnClickListener(pauseButtonListener);
                layout_workout_progress.setVisibility(View.VISIBLE);
                editText_minutes.setEnabled(false);
                editText_seconds.setEnabled(false);
                layout_pulse_zone.setVisibility(View.VISIBLE);
                int minutes = Integer.valueOf(editText_minutes.getText().toString());
                int seconds = Integer.valueOf(editText_seconds.getText().toString());

                if(previous_state == STATE_BEFORE_WORKOUT){
                    userSpecifiedWorkoutDuration = minutes * 60000 + seconds * 1000;

                }

                startTimer();


                break;

            case STATE_TIME_ENDED:
                if(previous_state == STATE_WORKING_OUT){
                    timeEndedAnimations();
                }
                startLocationListener();

                progressbar_duration.setProgress(100f);

                btn_toggle.setText(R.string.end_training);
                btn_toggle.setVisibility(View.VISIBLE);
                btn_toggle.setOnClickListener(reviewProgressButtonListener);
                Utils.vibrate(getContext(), VIBRATE_DURATION_TIME_ENDED);
                //todo: decide how to show extra time user has been working out
                break;

            case STATE_PAUSED:
                pauseTimer();
                pauseLocationListener();
                workout_tab_running_gif.setVisibility(View.INVISIBLE);
                img_pause.setImageResource(R.drawable.ic_resume);
                img_pause.setOnClickListener(resumeButtonListener);

                break;

        }
    }


    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {



            if(route.size() > 0){
                if(workout_state == STATE_WORKING_OUT || workout_state == STATE_TIME_ENDED){
                    totalDistance+=distance(route.get(route.size()-1),
                                            new LatLng(locationResult.getLastLocation().getLatitude(),
                                                       locationResult.getLastLocation().getLongitude()));
                    txt_distance.setText(String.valueOf((Math.round(totalDistance * 100.0) / 100.0)));
                }
            }

            Location location = locationResult.getLastLocation();
            route.add(new LatLng(location.getLatitude(), location.getLongitude()));
            paceData.add(location.getSpeed()*0.06f); //converting to km/min
            txt_current_pace.setText(String.valueOf(Math.round(paceData.get(paceData.size()-1) * 100.0) / 100.0));
            Log.i("TEST", "latitude: " + location.getLatitude() + " longtitude: " + location.getLongitude() + " speed: " + location.getSpeed());
        };
    };


    private void startLocationListener() {
        if(isLocationListeningEnabled){
            return;
        }

        isLocationListeningEnabled = true;

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        //noinspection MissingPermission
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                                    mLocationCallback,
                                                    null /* Looper */);

    }

    private void pauseLocationListener(){
        if(!isLocationListeningEnabled){
            return;
        }
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        isLocationListeningEnabled = false;
    }

    private void stopLocationListener(){
        pauseLocationListener();
        route.clear();
    }

    public void onMeasurement(int bpm){
        txt_bpm.setText(String.valueOf(bpm));
        if(workout_state == STATE_WORKING_OUT || workout_state == STATE_TIME_ENDED){
            bpmArrayList.add(bpm);
            int gender = MainScreenActivity.user.getGender();
            int age = Utils.getAgeFromDate(MainScreenActivity.user.getBirthday());
            //todo: ,
            // weight to int
            int weight = (int) MainScreenActivity.user.getWeight();

            calories_burned = calories_burned + calculateCalories(gender,age,weight, bpm, 1f/60f);
            pulse_zone = pulseZone(gender,age,bpm);
            float realPercentage = calculateUIPercentage(HRMax*0.5f,HRMax,bpm);
            pulseZoneView.setProgressPercentage(realPercentage);
            setIntensityStatus(txt_intensity_status,required_pulse_zone,pulse_zone);
            Log.i("TEST", "REALPRCTG: " + realPercentage);
            txt_calories_burned.setText(String.valueOf(Math.round(calories_burned * 100.0) / 100.0)); //Rounding and displaying calories

        }


    }


    private void setProgressBarDuration(int duration, int timePassed){
        float percentage = ( (float) timePassed) / ((float) duration);
        progressbar_duration.setProgress(100-percentage*100);

    }

    //Timer gets started. It does so based on timePassed value.
    private void startTimer(){
        if(timer == null){
            timer = new Timer();
        }else{
            timer.cancel();
            timer = new Timer();
        }

        isTimerRunning = true;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {




                        if(timePassed == userSpecifiedWorkoutDuration){
                            setState(STATE_TIME_ENDED);
                        }

                        //User is working out longer, than specified duration
                        if(timePassed > userSpecifiedWorkoutDuration){

                            int minutes = (int) (timePassed - userSpecifiedWorkoutDuration)/60000;
                            int seconds = Math.round((timePassed - userSpecifiedWorkoutDuration)/1000 - (minutes*60));

                            if(seconds < 10){
                                editText_minutes.setText("+"+minutes + "");
                                editText_seconds.setText("0" + seconds);
                            }else{
                                editText_seconds.setText(seconds + "");
                                editText_minutes.setText("+"+minutes + "");
                            }
                        }else{ //User is within his specified time limits
                            setProgressBarDuration((int) userSpecifiedWorkoutDuration, (int) (userSpecifiedWorkoutDuration -timePassed));

                            int minutes = (int) (userSpecifiedWorkoutDuration -timePassed)/60000;
                            int seconds = Math.round((userSpecifiedWorkoutDuration -timePassed)/1000 - (minutes*60));
                            if(seconds < 10){
                                editText_minutes.setText(minutes + "");
                                editText_seconds.setText("0" + seconds);
                            }else{
                                editText_seconds.setText(seconds + "");
                                editText_minutes.setText(minutes + "");
                            }
                        }

                        timePassed+=TIMER_STEP;

                    }
                });
            }
        }, 0, TIMER_STEP);
    }

    //Cancels the timer, but doesn't reset timepassed, so the timer can be resumed using startTimer()
    private void pauseTimer(){
        if(timer == null || !isTimerRunning){
            return;
        }
        timer.cancel();
        isTimerRunning = false;

    }

    //Cancels the timer, and sets timepassed to 0
    private void cancelTimer(){
        timePassed = 0;
        if(timer == null || !isTimerRunning){
            return;
        }

        timer.cancel();
    isTimerRunning = false;

}


    private void setupEditTextBehavior(){
        View.OnClickListener editTextClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText textView = ((EditText) view);
                textView.setSelection(0, textView.getText().length());
            }
        };

        KeyListener keyListener = new KeyListener(){

            @Override
            public int getInputType() {
                return 0;
            }

            @Override
            public boolean onKeyDown(View view, Editable editable, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_DEL){
                    return true;
                }

                return false;
            }

            @Override
            public boolean onKeyUp(View view, Editable editable, int i, KeyEvent keyEvent) {
                return false;
            }

            @Override
            public boolean onKeyOther(View view, Editable editable, KeyEvent keyEvent) {
                return false;
            }

            @Override
            public void clearMetaKeyState(View view, Editable editable, int i) {

            }
        };

        editText_minutes.setOnClickListener(editTextClickListener);
        editText_seconds.setOnClickListener(editTextClickListener);
        //editText_minutes.setKeyListener(keyListener);
        //editText_seconds.setKeyListener(keyListener);

        editText_minutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(editText_minutes.getSelectionStart() == 2){
                    editText_seconds.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editText_seconds.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(s.length() == 0){
                    return;
                }

                if(Integer.parseInt(s.subSequence(0, 1).toString()) >= 6){
                    editText_seconds.setText("00");
                }

                if(editText_seconds.getSelectionStart() == 2){
                    editText_seconds.clearFocus();
                    editText_minutes.clearFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }



    private double calculateCalories(int gender, int age, int weight, int heartRate, double timePassed){

        double calories = 0;

        switch(gender){
            case User.GENDER_FEMALE:
                calories = ((((double)age*0.074d)-(((double)weight)*0.05741d))+(((double) heartRate)*0.4472d) - 20.4022d)*timePassed/4.184d;
                break;

            case User.GENDER_MALE:
                calories = ((((double)age*0.2017d)-(((double)weight)*0.09036d))+(((double) heartRate)*0.6309d) - 20.4022d)*timePassed/4.184d;
                break;
        }

        //Log.i("TEST", "gender: " + gender + "\nage: " + age + "\nweight: " + weight + "\nheartRate " + heartRate + "\ntimePassed " + timePassed + "\nCalories burned: " + calories + "\n\n");


        return calories/1000; //Conmverting to KCal
    }


    private int pulseZone(int gender, int age, int bpm){

        HRMax =  ((gender==0 ? 202 : 216) - (gender==0 ? 0.55f : 1.09f) * age);
        int pulseZone = 0;

        float hrPercentage = ((float) bpm)/HRMax*100f;


        if(hrPercentage>=50 && hrPercentage<=60){
            pulseZone = 1;
        }else if(hrPercentage>60 && hrPercentage<=70){
            pulseZone = 2;
        }else if(hrPercentage>70 && hrPercentage<=80){
            pulseZone = 3;
        }else if(hrPercentage>80 && hrPercentage<=90){
            pulseZone = 4;
        }else if(hrPercentage>90 && hrPercentage<=100){
            pulseZone = 5;
        }
        Log.i("TEST", "HRMAX: " + HRMax + " | " + "BPM: " + bpm + " | " + "hrPercentage: " + hrPercentage + " | " + "pulseZone: " + pulseZone);
        return pulseZone;
    }


    private float calculateUIPercentage(float minimumHR, float maximumHR, int currentHR){
        if(currentHR<minimumHR){
            return 0.05f;
        }
        return ((currentHR-minimumHR)/(maximumHR-minimumHR));
    }


    //In kilometers
    private float distance (LatLng pointA, LatLng pointB) {

        float lat_a = (float) pointA.latitude;
        float lng_a = (float) pointA.longitude;

        float lat_b = (float) pointB.latitude;
        float lng_b = (float) pointB.longitude;

        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue()/1000f;
    }



    private void timePauseFlashing(){
        final boolean[] visibility = {true};
        runThread = true;
        pauseThread = new Thread() {
            @Override
            public void run() {
                while (runThread) {
                Log.i("TEST", String.valueOf(interrupted()));
                    try {
                        Thread.sleep(400);  //1000ms = 1 sec
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if(runThread){
                                    if(visibility[0]){
                                        layout_time.setVisibility(View.INVISIBLE);
                                    }else{
                                        layout_time.setVisibility(View.VISIBLE);
                                    }
                                    visibility[0] = !visibility[0];
                                }else{
                                    layout_time.setVisibility(View.VISIBLE);
                                }


                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        pauseThread.start();

    }

    private void setFonts(){

        Typeface futura = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/futura_light.ttf");

        txt_reccomended_duration.setTypeface(futura);
        txt_reccomended_pulse.setTypeface(futura);
        reccomended_pulse.setTypeface(futura);
        reccomended_duration.setTypeface(futura);
        txt_minutes.setTypeface(futura);
        txt_pulse_zone.setTypeface(futura);
        txt_intensity_status.setTypeface(futura);
        txt_intensity.setTypeface(futura);

    }

    private void setIntensityStatus(TextView intensityStatusView, int requiredPulseZone, int currentPulseZone){
        if(currentPulseZone == requiredPulseZone){
            intensityStatusView.setText("Optimal intensity!");
        }else if(currentPulseZone<requiredPulseZone){
            intensityStatusView.setText("Intensity too low!");
        }else if(currentPulseZone>requiredPulseZone){
            intensityStatusView.setText("Intensity too high!");
        }

    }





}
