package com.spse.gameresolutionchanger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //Options variables
    TextView FPSPercentage;
    TextView nativeResolution;
    TextView tweakedResolution;
    SeekBar resolutionSeekBar;


    ProgressBar testBar;
    int lastProgress = 0; //This is used to set the progress before API 24
    ImageButton addGame;

    SettingsManager settingsManager;
    Boolean settingsShown = false;
    com.google.android.material.switchmaterial.SwitchMaterial[] optionSwitches = new com.google.android.material.switchmaterial.SwitchMaterial[5];

    ConstraintSet layoutSettingsHidden = new ConstraintSet();
    ConstraintSet layoutSettingShown = new ConstraintSet();

    Dialog gameListPopUp;
    List<GameApp> gameList;

    //Recently added games
    GameApp[] recentGameApp = new GameApp[6];
    TextView[] recentGameAppTitle = new TextView[6];
    ImageButton[] recentGameAppLogo = new ImageButton[6];




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingsManager = new SettingsManager(this);

        //Check if we are using native resolution
        if (settingsManager.getOriginalWidth() != settingsManager.getCurrentWidth()){
            settingsManager.setScreenDimension(settingsManager.getOriginalHeight(), settingsManager.getOriginalWidth());
        }

        //Mostly linking stuff to the view
        FPSPercentage = findViewById(R.id.textViewPercentage);
        FPSPercentage.setText("+" + (int)(settingsManager.getLastResolutionScale()*0.8) + "%");

        nativeResolution = findViewById(R.id.textViewNativeResolution);
        nativeResolution.setText(settingsManager.getOriginalHeight()+"x"+settingsManager.getOriginalWidth());
        tweakedResolution = findViewById(R.id.textViewTweakedResolution);
        tweakedResolution.setText((int)((computeCoefficients(false)*settingsManager.getLastResolutionScale()) + settingsManager.getOriginalHeight()) +"x"+ (int)((computeCoefficients(true)*settingsManager.getLastResolutionScale()) + settingsManager.getOriginalWidth()));



        resolutionSeekBar = findViewById(R.id.seekBarRes);
        resolutionSeekBar.setProgress(settingsManager.getLastResolutionScale());

        testBar = findViewById(R.id.progressBar);
        testBar.setProgress(resolutionSeekBar.getProgress());
        lastProgress = resolutionSeekBar.getProgress();

        addGame = findViewById(R.id.addGameButton);


        gameListPopUp = new Dialog(this);



        if (settingsManager.isFirstLaunch()){
            settingsManager.initializeFirstLaunch();
        }

        //Link option switches
        optionSwitches[0] = findViewById(R.id.switchOptionAggressiveLMK);
        optionSwitches[1] = findViewById(R.id.switchOptionKillServices);
        optionSwitches[2] = findViewById(R.id.switchOptionKillApps);
        optionSwitches[3] = findViewById(R.id.switchOptionKeepDPI);
        optionSwitches[4] = findViewById(R.id.switchOptionOnlyAddGames);

        //Load their previous state:
        optionSwitches[0].setChecked(settingsManager.isLMKActivated());
        optionSwitches[1].setChecked(settingsManager.killServices());
        optionSwitches[2].setChecked(settingsManager.isMurderer());
        optionSwitches[3].setChecked(settingsManager.keepStockDPI());
        optionSwitches[4].setChecked(settingsManager.onlyAddGames());

        //Add the click listener for all of them;
        setOptionsOnClickListener();


        //Link recent games stuff
        recentGameAppTitle[0] = findViewById(R.id.textViewRecentGame1);
        recentGameAppTitle[1] = findViewById(R.id.textViewRecentGame2);
        recentGameAppTitle[2] = findViewById(R.id.textViewRecentGame3);
        recentGameAppTitle[3] = findViewById(R.id.textViewRecentGame4);
        recentGameAppTitle[4] = findViewById(R.id.textViewRecentGame5);
        recentGameAppTitle[5] = findViewById(R.id.textViewRecentGame6);

        recentGameAppLogo[0] = findViewById(R.id.imageViewRecentGame1);
        recentGameAppLogo[1] = findViewById(R.id.imageViewRecentGame2);
        recentGameAppLogo[2] = findViewById(R.id.imageViewRecentGame3);
        recentGameAppLogo[3] = findViewById(R.id.imageViewRecentGame4);
        recentGameAppLogo[4] = findViewById(R.id.imageViewRecentGame5);
        recentGameAppLogo[5] = findViewById(R.id.imageViewRecentGame6);

        //Now we need to load recent games
        for(int i=0; i<6; i++){
            recentGameApp[i] = settingsManager.getRecentGameApp(i+1);
            if(recentGameApp[i] != null) {
                recentGameAppTitle[i].setText(recentGameApp[i].getGameName());
                recentGameAppLogo[i].setImageDrawable(recentGameApp[i].getIcon());

                final int finalI = i;
                recentGameAppLogo[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        GameAppManager.launchGameApp(MainActivity.this, recentGameApp[finalI].getPackageName());
                    }
                });
            }
        }



        layoutSettingsHidden.clone((ConstraintLayout) findViewById(R.id.MainActivity));
        layoutSettingShown.clone(this, R.layout.activity_main_options_shown);

        ImageButton testButton = findViewById(R.id.imageButtonSettingSwitch);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransitionManager.beginDelayedTransition((ConstraintLayout) findViewById(R.id.MainActivity));
                ConstraintSet constrain;
                if(settingsShown){
                    constrain = layoutSettingsHidden;
                    setRecentGameAppClickable(true);
                    setOptionsClickable(false);
                }else{
                    constrain = layoutSettingShown;
                    setRecentGameAppClickable(false);
                    setOptionsClickable(true);
                }

                constrain.applyTo((ConstraintLayout) findViewById(R.id.MainActivity));
                settingsShown = !settingsShown;

            }
        });



        init();




        resolutionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                testBar.incrementProgressBy((i- lastProgress));
                lastProgress = i;
                FPSPercentage.setText("+" + (int)(i*0.8) + "%");

                tweakedResolution.setText((int)((computeCoefficients(false)*i) + settingsManager.getOriginalHeight()) +"x"+ (int)((computeCoefficients(true)*i) + settingsManager.getOriginalWidth()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        addGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("ADDGAME BUTTON","BUTTON CLICKED !");
                gameList = GameAppManager.getGameApps(MainActivity.this);
                showGameListPopup(MainActivity.this);
            }
        });


    }

    void init() {
        if (ExecuteAsRootBase.canRunRootCommands()){
            Log.d("ROOT TEST","Nice, we have root access !");
            ArrayList<String> Commands = new ArrayList<>();
            Commands.add("su -c wm size >> " + MainActivity.this.getApplicationInfo().dataDir + "/test.test");

            ExecuteAsRootBase.execute(Commands);


        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.add_game_plus)
                .setContentTitle("Hello I'm a notification !")
                .setContentText("Do you still see me ?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(this).notify(0,builder.build());

        //It is only the software values, furthermore it is unable to make abstraction of the bottom bar
        Log.d("DISPLAY WIDTH: ", String.valueOf(getResources().getDisplayMetrics().widthPixels));
        Log.d("DISPLAY HEIGHT: ", String.valueOf(getResources().getDisplayMetrics().heightPixels));

        Log.d("DISPLAY DENSITY: ", String.valueOf(getResources().getDisplayMetrics().densityDpi));



        return;
    }

    void showGameListPopup(Context context){
        long test = System.currentTimeMillis();

        int xScreen = context.getResources().getDisplayMetrics().widthPixels;
        int yScreen = context.getResources().getDisplayMetrics().heightPixels;

        gameListPopUp.setContentView(R.layout.add_game_layout);
        gameListPopUp.getWindow().setLayout((int) Math.ceil(xScreen*0.90),(int) Math.ceil(yScreen*0.85) );

        LinearLayout layout = (LinearLayout) gameListPopUp.findViewById(R.id.gameListLayout);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);


        for(int i = 0; i < gameList.size(); i++ ){

            View child = inflater.inflate(R.layout.game_app_item, null);
            TextView title = child.findViewById(R.id.textViewGameAppTitle);
            final TextView packageName = child.findViewById(R.id.textViewGameAppPackageName);
            ImageView icon = child.findViewById(R.id.imageViewGameIcon);

            GameApp game = gameList.get(i);

            title.setText(game.getGameName());
            packageName.setText(game.getPackageName());
            icon.setImageDrawable(game.getIcon());


            layout.addView(child);

            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    test(packageName.getText().toString());
                    settingsManager.addGameApp(packageName.getText().toString());

                    gameListPopUp.dismiss();
                    GameAppManager.launchGameApp(MainActivity.this,packageName.getText().toString());
                }
            });

            child = new Space(this);
            child.setMinimumHeight(10);

            layout.addView(child);


        }




        gameListPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        gameListPopUp.show();
        Log.d("EXEC TIME: ", "ms: " + (test + System.currentTimeMillis()));
    }


    public void test(String str){
        Log.d("PackageName: ", str);
    }




    public void setOptionsClickable(boolean state){
        for(int i = 0; i<5; i++){
            optionSwitches[i].setClickable(state);
        }
    }

    public void setOptionsOnClickListener(){
        optionSwitches[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setLMK(optionSwitches[0].isChecked());
            }
        });

        optionSwitches[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setKillServices(optionSwitches[1].isChecked());
            }
        });

        optionSwitches[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setMurderer(optionSwitches[2].isChecked());
            }
        });

        optionSwitches[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setKeepStockDPI(optionSwitches[3].isChecked());
            }
        });

        optionSwitches[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setOnlyAddGames(optionSwitches[4].isChecked());
            }
        });
    }

    public void setRecentGameAppClickable(boolean state){
        for(int i = 0; i<5; i++){
            recentGameAppLogo[i].setClickable(state);
        }
    }

    public float computeCoefficients(boolean computeWidth){
        if(computeWidth){
            //Compute the width coefficient
            return (float) (-settingsManager.getOriginalWidth()*0.005);
        }else{
            //Compute the height coefficient
            return (float) (-settingsManager.getOriginalHeight()*0.005);
        }
    }

}