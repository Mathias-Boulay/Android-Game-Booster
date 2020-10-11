package com.spse.gameresolutionchanger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
    SeekBar resolutionSeekBar;
    ProgressBar testBar;
    int lastProgress = 0; //This is used to set the progress before API 24
    ImageButton addGame;

    SettingsManager settingsManager;

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

        //Mostly linking stuff to the view
        FPSPercentage = findViewById(R.id.textViewPercentage);
        resolutionSeekBar = findViewById(R.id.seekBarRes);
        testBar = findViewById(R.id.progressBar);
        addGame = findViewById(R.id.addGameButton);


        gameListPopUp = new Dialog(this);

        settingsManager = new SettingsManager(this);

        if (settingsManager.isFirstLaunch()){
            settingsManager.initializeFirstLaunch();
        }

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





        init();




        resolutionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                testBar.incrementProgressBy((i- lastProgress));
                lastProgress = i;
                FPSPercentage.setText("+" + (int)(i*0.9) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
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

        //It is only the software values, furthermore it is unable to make abstraction of the bottom bar
        Log.d("DISPLAY WIDTH: ", String.valueOf(getResources().getDisplayMetrics().widthPixels));
        Log.d("DISPLAY HEIGHT: ", String.valueOf(getResources().getDisplayMetrics().heightPixels));

        Log.d("DISPLAY DENSITY: ", String.valueOf(getResources().getDisplayMetrics().densityDpi));



        return;
    }

    void showGameListPopup(Context context){
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
    }


    public void test(String str){
        Log.d("PackageName: ", str);
    }





}