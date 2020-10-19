package com.spse.gameresolutionchanger;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //Options variables
    TextView FPSPercentage;
    TextView nativeResolution;
    TextView tweakedResolution;
    SeekBar resolutionSeekBar;
    float[] coefficients = new float[2]; //Width, Height


    ProgressBar testBar;
    int lastProgress = 0; //This is used to set the progress before API 24
    ImageButton addGame;
    ImageButton settingsSwitch;

    SettingsManager settingsManager;
    Boolean settingsShown = false;
    com.google.android.material.switchmaterial.SwitchMaterial[] optionSwitches = new com.google.android.material.switchmaterial.SwitchMaterial[4];
    TextView[] optionsTexts = new TextView[4];

    ConstraintSet layoutSettingsHidden = new ConstraintSet();
    ConstraintSet layoutSettingShown = new ConstraintSet();

    Dialog gameListPopUp;
    List<GameApp> gameList;

    //Recently added games
    GameApp[] recentGameApp = new GameApp[6];
    TextView[] recentGameAppTitle = new TextView[6];
    ImageButton[] recentGameAppLogo = new ImageButton[6];


    AlertDialog dialog;




    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingsManager = new SettingsManager(this);

        //Compute coefficient
        coefficients[0] = computeCoefficients(true);
        coefficients[1] = computeCoefficients(false);

        //Check if we are using native resolution
        if (settingsManager.getOriginalWidth() != settingsManager.getCurrentWidth()) {
                showResetPopup();
            }

        //Mostly linking stuff to the view
        FPSPercentage = findViewById(R.id.textViewPercentage);
        FPSPercentage.setText(String.format("+%d%%", (int) (settingsManager.getLastResolutionScale() * 0.8)));

        nativeResolution = findViewById(R.id.textViewNativeResolution);
        nativeResolution.setText(String.format("%dx%d", settingsManager.getOriginalHeight(), settingsManager.getOriginalWidth()));
        tweakedResolution = findViewById(R.id.textViewTweakedResolution);
        tweakedResolution.setText(String.format("%dx%d", (int) ((coefficients[1] * settingsManager.getLastResolutionScale()) + settingsManager.getOriginalHeight()), (int) ((coefficients[0] * settingsManager.getLastResolutionScale()) + settingsManager.getOriginalWidth())));



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

        //Options
        initializeOptions();

        //Add the click listener for all of them;
        setOptionsOnClickListener();

        //Make them uncheckable since there are hidden
        setOptionsClickable(false);


        //Recent GameApps
        initializeRecentGames();



        layoutSettingsHidden.clone((ConstraintLayout) findViewById(R.id.MainActivity));
        layoutSettingShown.clone(this, R.layout.activity_main_options_shown);

        settingsSwitch = findViewById(R.id.imageButtonSettingSwitch);
        settingsSwitch.setOnClickListener(new View.OnClickListener() {
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
                FPSPercentage.setText(String.format("+%d%%", (int) (i * 0.8)));

                tweakedResolution.setText(String.format("%dx%d", (int) (Math.ceil(coefficients[1] * i) + settingsManager.getOriginalHeight()), (int) (Math.ceil(coefficients[0] * i) + settingsManager.getOriginalWidth())));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        addGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddGame();
            }
        });


    }

    void init() {
        if (ExecuteADBCommands.canRunRootCommands()){
            Log.d("ROOT TEST","Nice, we have root access !");
        }
        return;
    }

    void showGameListPopup(Context context){
        long test = System.currentTimeMillis();

        int xScreen = context.getResources().getDisplayMetrics().widthPixels;
        int yScreen = context.getResources().getDisplayMetrics().heightPixels;

        gameListPopUp.setContentView(R.layout.add_game_layout);
        gameListPopUp.getWindow().setLayout((int) Math.ceil(xScreen*0.90),(int) Math.min(Math.ceil(yScreen*0.85),gameList.size()*200 ) + (settingsManager.onlyAddGames() ? 200 : 0) );//The magic number 200 correspond to one GameApp item + one space

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
        if(settingsManager.onlyAddGames()){
            View lastChild = inflater.inflate(R.layout.no_game_app_item, null);
            layout.addView(lastChild);
            lastChild.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Change the option to only add every app
                    settingsManager.setOnlyAddGames(false);
                    optionSwitches[3].setChecked(false);

                    gameListPopUp.dismiss();
                    gameList = GameAppManager.getGameApps(MainActivity.this);
                    showGameListPopup(MainActivity.this);

                }
            });
        }




        gameListPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        gameListPopUp.show();
        Log.d("EXEC TIME: ", "ms: " + (test + System.currentTimeMillis()));
    }


    public void test(String str){
        Log.d("PackageName: ", str);
    }


    private void initializeOptions(){
        //Link option switches
        optionSwitches[0] = findViewById(R.id.switchOptionAggressiveLMK);
        optionSwitches[1] = findViewById(R.id.switchOptionKillApps);
        optionSwitches[2] = findViewById(R.id.switchOptionKeepDPI);
        optionSwitches[3] = findViewById(R.id.switchOptionOnlyAddGames);

        //Load their previous state:
        optionSwitches[0].setChecked(settingsManager.isLMKActivated());
        optionSwitches[1].setChecked(settingsManager.isMurderer());
        optionSwitches[2].setChecked(settingsManager.keepStockDPI());
        optionSwitches[3].setChecked(settingsManager.onlyAddGames());

        //Link options text
        optionsTexts[0] = findViewById(R.id.textViewOptionAggressiveLMK);
        optionsTexts[1] = findViewById(R.id.textViewOptionKillApps);
        optionsTexts[2] = findViewById(R.id.textViewOptionKeepDPI);
        optionsTexts[3] = findViewById(R.id.textViewOptionOnlyAddGames);


    }

    public void setOptionsClickable(boolean state){
        for(int i = 0; i<optionSwitches.length; i++){
            optionSwitches[i].setClickable(state);
            optionsTexts[i].setClickable(state);
        }
    }

    public void setOptionsOnClickListener(){
        //First the switches themselves:
        optionSwitches[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setLMK(optionSwitches[0].isChecked());
            }
        });

        optionSwitches[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setMurderer(optionSwitches[1].isChecked());
            }
        });

        optionSwitches[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setKeepStockDPI(optionSwitches[2].isChecked());
            }
        });

        optionSwitches[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setOnlyAddGames(optionSwitches[3].isChecked());
            }
        });

        //Then the associated text:

        optionsTexts[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionSwitches[0].setChecked(!optionSwitches[0].isChecked());
                settingsManager.setLMK(optionSwitches[0].isChecked());
            }
        });

        optionsTexts[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionSwitches[1].setChecked(!optionSwitches[1].isChecked());
                settingsManager.setMurderer(optionSwitches[1].isChecked());
            }
        });

        optionsTexts[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionSwitches[2].setChecked(!optionSwitches[2].isChecked());
                settingsManager.setKeepStockDPI(optionSwitches[2].isChecked());
            }
        });

        optionsTexts[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionSwitches[3].setChecked(!optionSwitches[3].isChecked());
                settingsManager.setOnlyAddGames(optionSwitches[3].isChecked());
            }
        });
    }

    private void initializeRecentGames(){
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
            }else{
                //No game yet, let's add a game
                recentGameAppLogo[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddGame();
                    }
                });
            }
        }
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

    private void showResetPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.reset_popup_title)
                .setMessage(R.string.reset_popup_text)
                .setPositiveButton(R.string.reset_popup_positive_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        GameAppManager.restoreOriginalLMK(MainActivity.this);
                        settingsManager.setScreenDimension(settingsManager.getOriginalHeight(), settingsManager.getOriginalWidth());

                    }
                })
                .setNegativeButton(R.string.reset_popup_negative_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                        dialog.dismiss();
                    }
                });


        dialog = builder.create();

        dialog.show();
    }

    private void showAddGame(){
        gameList = GameAppManager.getGameApps(MainActivity.this);
        showGameListPopup(MainActivity.this);
    }

    @Override
    public void onBackPressed() {
        if (settingsShown){
            //Simulate a button click on the setting switch button
            settingsSwitch.performClick();
            return;
        }
        super.onBackPressed();
    }
}