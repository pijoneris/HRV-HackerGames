package com.mabe.productions.hrv_madison.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.mabe.productions.hrv_madison.R;
import com.mabe.productions.hrv_madison.User;
import com.mabe.productions.hrv_madison.measurements.Measurement;

import java.util.ArrayList;

//todo: card view animations and title snaping like 'prisiuk antraste'
public class DataTodayFragment extends Fragment {

    //FrequencyCardView
    private TextView freq_card_txt_freq_band;
    private TextView freq_card_txt_freq_band_date;
    private TextView freq_card_txt_after_this_measure;
    private TextView freq_card_txt_hf_after_measurament;
    private TextView freq_card_txt_lf_after_measurement;
    private TextView freq_card_txt_vlf_after_measurement;
    private TextView freq_card_txt_norm;
    private TextView freq_card_txt_norm_hf;
    private TextView freq_card_txt_norm_vhf;
    private TextView freq_card_txt_norm_lf;


    //HrvCardView
    private TextView hrv_card_txt_hrv;
    private TextView hrv_card_txt_hrv_band_date;
    private TextView hrv_card_txt_bpm;
    private TextView hrv_card_txt_bpm_after_measurament;
    private TextView hrv_card_txt_bpm_norm_after_measurement;
    private TextView hrv_card_txt_hrv_after;
    private TextView hrv_card_txt_average_hrv;
    private TextView hrv_card_txt_average_norm_hrv;



    private PieChart frequency_chart;
    private PieChart health_index_chart;
    private LineChart bpm_line_chart;
    private TextView bpm_card_txt_bpm;
    private TextView bpm_card_txt_date;

    private TextView bpm_card_txt_average;
    private TextView bpm_card_value_average;

    private TextView bpm_card_txt_hrv_average_value;
    private TextView bpm_card_hrv_average_value;




    public DataTodayFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.data_today_fragment, container, false);

        initializeViews(view);
        setFonts();
        frequency_pieChart();
        health_index_pieChart();
        bpm_lineChart();
        updateData();
        return view;

    }

    public void updateData(){
        //TODO: populate cardviews with measurement data
        User user = User.getUser(getContext());

        Measurement measurement = user.getLastMeasurement(getContext());
        if(measurement!=null){
            int bpmValues[] = measurement.getBpm_data();
            int rmssdValues[] = measurement.getRmssd_data();

            int maxGraphValue = bpmValues.length > rmssdValues.length ? bpmValues.length : rmssdValues.length;

            for(int i = 0; i<bpmValues.length; i++){
                if(i<=maxGraphValue){
                    addEntryBpm(bpmValues[i],maxGraphValue);

                }

            }

            for(int i = 0; i<rmssdValues.length; i++){
                if(i<=maxGraphValue){
                    addEntryRmssd(rmssdValues[i], maxGraphValue);

                }
            }

            freq_card_txt_norm_hf.setText(String.valueOf(measurement.getHF_band()));
            freq_card_txt_norm_lf.setText(String.valueOf(measurement.getHF_band()));
            freq_card_txt_norm_vhf.setText(String.valueOf(measurement.getVHF_band()));
            bpm_card_hrv_average_value.setText(String.valueOf(measurement.getRmssd()));
            bpm_card_value_average.setText(String.valueOf((int)measurement.getAverage_bpm()));

        }

        bpm_line_chart.animateY(2000, Easing.EasingOption.EaseInOutSine);

    }

    private void initializeViews(View view){

        //Frequency PieChart
        freq_card_txt_freq_band = (TextView) view.findViewById(R.id.frequency_bands_text_view);
        freq_card_txt_freq_band_date  = (TextView) view.findViewById(R.id.frequency_bands_measurement_date);
        freq_card_txt_after_this_measure = (TextView) view.findViewById(R.id.freq_card_txt_after_this_measure);
        freq_card_txt_hf_after_measurament = (TextView) view.findViewById(R.id.freq_card_txt_hf_after_measurement);
        freq_card_txt_lf_after_measurement = (TextView) view.findViewById(R.id.freq_card_txt_lf_after_measurement);
        freq_card_txt_vlf_after_measurement = (TextView) view.findViewById(R.id.freq_card_txt_vlf_after_measurement);
        freq_card_txt_norm = (TextView) view.findViewById(R.id.freq_card_txt_norm);
        freq_card_txt_norm_hf = (TextView) view.findViewById(R.id.freq_card_norm_hf);
        freq_card_txt_norm_vhf = (TextView) view.findViewById(R.id.freq_card_norm_lf);
        freq_card_txt_norm_lf = (TextView) view.findViewById(R.id.freq_card_norm_vlf);
        frequency_chart = (PieChart) view.findViewById(R.id.chart_frequencies);
        health_index_chart = (PieChart) view.findViewById(R.id.chart_health_index);

        //HRV PieChart
        hrv_card_txt_hrv = (TextView) view.findViewById(R.id.health_index_text_view);
        hrv_card_txt_hrv_band_date = (TextView) view.findViewById(R.id.health_index_measurement_date);
        hrv_card_txt_bpm= (TextView)view. findViewById(R.id.hrc_card_txt_bpm);
        hrv_card_txt_bpm_after_measurament= (TextView) view.findViewById(R.id.hrc_card_txt_average_bpm);
        hrv_card_txt_bpm_norm_after_measurement= (TextView) view.findViewById(R.id.hrc_card_txt_norm_bpm);
        hrv_card_txt_hrv_after= (TextView) view.findViewById(R.id.hrc_card_txt_hrv);
        hrv_card_txt_average_hrv= (TextView)view.findViewById(R.id.hrc_card_txt_average_hrv);
        hrv_card_txt_average_norm_hrv= (TextView) view.findViewById(R.id.hrc_card_txt_norm_hrv);

        //BPM PieChart
        bpm_line_chart = (LineChart) view.findViewById(R.id.chart_bpm);
        bpm_card_txt_bpm = (TextView) view.findViewById(R.id.bpm_index_text_view);
        bpm_card_txt_date = (TextView) view.findViewById(R.id.bpm_index_measurement_date);
        bpm_card_txt_average = (TextView) view.findViewById(R.id.bpm_txt_average);
        bpm_card_value_average = (TextView) view.findViewById(R.id.bpm_value);

        bpm_card_txt_hrv_average_value = (TextView) view.findViewById(R.id.bpm_card_txt_hrv_average_value);
        bpm_card_hrv_average_value = (TextView) view.findViewById(R.id.bpm_card_hrv_average_value);



    }



    private void setFonts(){
        Typeface futura = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/futura_light.ttf");
        Typeface corbel = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/Corbel Bold.ttf");

        frequency_chart.setCenterTextTypeface(futura);
        health_index_chart.setCenterTextTypeface(futura);


        //FrequencyCardView
        freq_card_txt_freq_band.setTypeface(futura);
        freq_card_txt_freq_band_date.setTypeface(futura);
        freq_card_txt_after_this_measure.setTypeface(futura);
        freq_card_txt_hf_after_measurament.setTypeface(futura);
        freq_card_txt_lf_after_measurement.setTypeface(futura);
        freq_card_txt_vlf_after_measurement.setTypeface(futura);
        freq_card_txt_norm.setTypeface(futura);
        freq_card_txt_norm_hf.setTypeface(futura);
        freq_card_txt_norm_vhf.setTypeface(futura);
        freq_card_txt_norm_lf.setTypeface(futura);


        //HrvCardView
        hrv_card_txt_hrv.setTypeface(futura);
        hrv_card_txt_hrv_band_date.setTypeface(futura);
        hrv_card_txt_bpm.setTypeface(futura);
        hrv_card_txt_bpm_after_measurament.setTypeface(futura);
        hrv_card_txt_bpm_norm_after_measurement.setTypeface(futura);
        hrv_card_txt_hrv_after.setTypeface(futura);
        hrv_card_txt_average_hrv.setTypeface(futura);
        hrv_card_txt_average_norm_hrv.setTypeface(futura);

        bpm_card_txt_bpm.setTypeface(futura);
        bpm_card_txt_date.setTypeface(futura);
        bpm_card_txt_average.setTypeface(futura);
        bpm_card_value_average.setTypeface(futura);
        bpm_card_txt_hrv_average_value.setTypeface(futura);
        bpm_card_hrv_average_value.setTypeface(futura);



        bpm_line_chart.getLegend().setEnabled(false);
        bpm_line_chart.getXAxis().setDrawAxisLine(false);
        bpm_line_chart.getAxisRight().setDrawAxisLine(false);
        bpm_line_chart.getAxisLeft().setDrawAxisLine(true);
        bpm_line_chart.getAxisLeft().setDrawGridLines(true);
        bpm_line_chart.getXAxis().setDrawGridLines(false);
        bpm_line_chart.getAxisRight().setDrawGridLines(false);
        bpm_line_chart.setDescription(null);
        bpm_line_chart.getAxisLeft().setDrawLabels(true);
        bpm_line_chart.getAxisRight().setDrawLabels(false);
        bpm_line_chart.getXAxis().setDrawLabels(false);
        bpm_line_chart.setTouchEnabled(false);
        bpm_line_chart.setViewPortOffsets(0f, 0f, 0f, 0f);
    }

    private void frequency_pieChart(){

        //Casual modifications
        frequency_chart.setUsePercentValues(true);
        frequency_chart.setDrawSliceText(false);
        frequency_chart.getDescription().setEnabled(false);

        //Space inside chart and color
        frequency_chart.setTransparentCircleRadius(50f);
        frequency_chart.setHoleColor(Color.TRANSPARENT);
        frequency_chart.setHoleRadius(80f);
        frequency_chart.setCenterText("VLF/LF/HF");
        frequency_chart.setCenterTextSize(20f);
        frequency_chart.setCenterTextColor(Color.WHITE);


        //Remove X-axis values
        frequency_chart.setDrawEntryLabels(false);

        //Animate pieChart
        frequency_chart.animateY(1000, Easing.EasingOption.EaseInOutCubic);

        ArrayList<PieEntry> values = new ArrayList<>();
        values.add(new PieEntry(65f,"BPM"));
        values.add(new PieEntry(50f,"BPM"));
        values.add(new PieEntry(20f,"BPM"));

        //Modify Y-axis value
        final PieDataSet dataSet = new PieDataSet(values,"Frequencies");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(3f);
        dataSet.setColors(new int[]{Color.parseColor("#e74c3c"), Color.parseColor("#2980b9"), Color.parseColor("#8e44ad")});
        dataSet.setDrawValues(false);

        //Modify Data looks
        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.YELLOW);

        //Setting data
        frequency_chart.setData(data);

        //Modify data
        Legend legend = frequency_chart.getLegend();
        legend.setEnabled(false);
    }

    private void addEntryBpm(int hr, int max_points) {

        LineData data = bpm_line_chart.getData();
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        if (set == null) {
            //Creating a line with single hr value
            ArrayList<Entry> singleValueList = new ArrayList<>();
            singleValueList.add(new Entry(0, hr));
            set = new LineDataSet(singleValueList, "HR");
            set.setLineWidth(getContext().getResources().getDimension(R.dimen.line_width));
            set.setDrawValues(false);
            set.setDrawCircleHole(false);
            set.setDrawCircles(false);
            set.setCircleRadius(getContext().getResources().getDimension(R.dimen.circle_radius));
            set.setCircleColor(Color.parseColor("#F62459"));
            set.setColor(Color.parseColor("#F62459"));
            set.setDrawFilled(false);

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
        bpm_line_chart.notifyDataSetChanged();

        //chart_hr.setData(data);
        //chart_hr.animate();
        //chart_hr.moveViewToX(set.getEntryCount());
        bpm_line_chart.setVisibleXRangeMaximum(max_points);
        bpm_line_chart.setVisibleXRangeMinimum(0);
        //chart_hr.setAutoScaleMinMaxEnabled(true);


    }

    private void addEntryRmssd(int rmssd, int max_points) {

        LineData data = bpm_line_chart.getData();
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(1);
        if (set == null) {
            //Creating a line with single hr value
            ArrayList<Entry> singleValueList = new ArrayList<>();
            singleValueList.add(new Entry(0, rmssd));
            set = new LineDataSet(singleValueList, "HR");
            set.setLineWidth(getContext().getResources().getDimension(R.dimen.line_width));
            set.setDrawValues(false);
            set.setDrawCircleHole(false);
            set.setDrawCircles(false);
            set.setCircleRadius(getContext().getResources().getDimension(R.dimen.circle_radius));
            set.setCircleColor(Color.parseColor("#F62459"));
            set.setColor(Color.parseColor("#2ecc71"));
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
            set.addEntry(new Entry(set.getEntryCount(), rmssd));
        }

        data.notifyDataChanged();
        bpm_line_chart.notifyDataSetChanged();

        //chart_hr.setData(data);
        //chart_hr.animate();
        //chart_hr.moveViewToX(set.getEntryCount());
        bpm_line_chart.setVisibleXRangeMaximum(max_points);
        bpm_line_chart.setVisibleXRangeMinimum(0);
        //chart_hr.setAutoScaleMinMaxEnabled(true);


    }

    private void bpm_lineChart(){
            LineData data = new LineData();
        //Creating a line with single hr value

            //BPM DATA SET
            ArrayList<Entry> singleValueList = new ArrayList<>();
            singleValueList.add(new Entry(0, 16));
            singleValueList.add(new Entry(1, 24));
            singleValueList.add(new Entry(2, 64));
            singleValueList.add(new Entry(3, 35));
            LineDataSet set = new LineDataSet(singleValueList, "HR");
            set.setLineWidth(1);
            set.setDrawValues(false);
            set.setDrawCircleHole(false);
            set.setDrawCircles(false);
            set.setCircleRadius(getContext().getResources().getDimension(R.dimen.circle_radius));
            set.setCircleColor(Color.parseColor("#FFFFFF"));
            set.setColor(Color.parseColor("#F62459"));
            set.setDrawFilled(false);

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

            //RMSSD DATA SET
        ArrayList<Entry> secondValueList = new ArrayList<>();
        secondValueList.add(new Entry(0, 35));
        secondValueList.add(new Entry(1, 65));
        secondValueList.add(new Entry(2, 14));
        secondValueList.add(new Entry(3, 34));
        LineDataSet rmssdSet = new LineDataSet(secondValueList, "HR");
        rmssdSet.setLineWidth(1);
        rmssdSet.setDrawValues(false);
        rmssdSet.setDrawCircleHole(false);
        rmssdSet.setDrawCircles(false);
        rmssdSet.setCircleRadius(getContext().getResources().getDimension(R.dimen.circle_radius));
        rmssdSet.setCircleColor(Color.parseColor("#FFFFFF"));
        rmssdSet.setColor(Color.parseColor("#2ecc71"));
        rmssdSet.setDrawFilled(false);


        data.addDataSet(rmssdSet);

        bpm_line_chart.setData(data);



    }

    private void health_index_pieChart(){

        //Casual modifications
        health_index_chart.setUsePercentValues(true);
        health_index_chart.setDrawSliceText(false);
        health_index_chart.getDescription().setEnabled(false);

        //Space inside chart and color
        health_index_chart.setTransparentCircleRadius(50f);
        health_index_chart.setHoleColor(Color.TRANSPARENT);
        health_index_chart.setHoleRadius(80f);
        health_index_chart.setCenterText("Index\n20%");
        health_index_chart.setCenterTextSize(20f);
        health_index_chart.setCenterTextColor(Color.WHITE);


        //Remove X-axis values
        health_index_chart.setDrawEntryLabels(false);

        //Animate pieChart
        health_index_chart.animateY(1000, Easing.EasingOption.EaseInOutCubic);

        ArrayList<PieEntry> values = new ArrayList<>();
        values.add(new PieEntry(30f,"BPM"));
        values.add(new PieEntry(70f,"asfBPM"));

        //Modify Y-axis value
        final PieDataSet dataSet = new PieDataSet(values,"Frequencies");
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(3f);
        dataSet.setColors(new int[]{Color.parseColor("#20bf6b"), Color.parseColor("#26de81")});
        dataSet.setDrawValues(false);

        //Modify Data looks
        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.YELLOW);

        //Setting data
        health_index_chart.setData(data);

        //Modify data
        Legend legend = health_index_chart.getLegend();
        legend.setEnabled(false);

    }



}
