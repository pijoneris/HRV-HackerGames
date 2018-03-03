package com.mabe.productions.hrv_madison.initialInfo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mabe.productions.hrv_madison.R;
import com.mabe.productions.hrv_madison.Utils;

public class IntroInitialPage extends AppCompatActivity {

    private TextView txt_request_info_explanation;
    private Button btn_start;
    private ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_initial_page_activity);
        Utils.changeNotifBarColor(Color.parseColor("#3e5266"),getWindow());
        initializeViews();
        setFonts();
    }

    private void setFonts(){

        Typeface verdana = Typeface.createFromAsset(getAssets(),
                "fonts/Verdana.ttf");


        txt_request_info_explanation.setTypeface(verdana);
    }//

    private void initializeViews(){


        Animation start_btn = AnimationUtils.loadAnimation(this,R.anim.bottom_to_top_delay);
        Animation img_anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation request_anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_delay);

        txt_request_info_explanation = (TextView) findViewById(R.id.initial_txt_request_info_explanation);
        btn_start = (Button) findViewById(R.id.initial_start_btn);
        img = (ImageView) findViewById(R.id.initial_app_icon);

        txt_request_info_explanation.startAnimation(request_anim);
        img.startAnimation(img_anim);
        btn_start.startAnimation(start_btn);
    }



    public void start(View view) {
        startActivity(new Intent(this, IntroInitialHeight.class));
    }
}
