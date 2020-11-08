package com.spse.gameresolutionchanger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //Options variables
    TextView FPSPercentage;
    TextView tweakedResolution;
    SeekBar resolutionSeekBar;
    float[] coefficients = new float[2]; //Width, Height


    ProgressBar circularProgressBar;
    int lastProgress = 0; //This is used to set the progress before API 24
    ImageButton settingsSwitch;

    SettingsManager settingsManager;
    Boolean settingsShown = false;
    CheckBox[] optionCheckboxes = new CheckBox[3];

    ConstraintSet layoutSettingsHidden = new ConstraintSet();
    ConstraintSet layoutSettingShown = new ConstraintSet();

    //Dialog gameListPopUp;
    List<GameApp> gameList;

    //Recently added games
    GameApp[] recentGameApp = new GameApp[6];
    TextView[] recentGameAppTitle = new TextView[6];
    ImageButton[] recentGameAppLogo = new ImageButton[6];








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

        //The file created when DPI is changed
        File tmpFile = new File(this.getApplicationInfo().dataDir + "/tmp");
        if (tmpFile.exists()){
            tmpFile.delete();
        }else {
            if (settingsManager.getOriginalWidth() != settingsManager.getCurrentWidth()) {
                showResetPopup();
            }
        }

        //Mostly linking stuff to the view
        FPSPercentage = findViewById(R.id.textViewPercentage);
        FPSPercentage.setText(String.format("+%d%%", (int) (settingsManager.getLastResolutionScale() * 0.8)));

        TextView nativeResolution = findViewById(R.id.textViewNativeResolution);
        nativeResolution.setText(String.format("%s\n%dx%d%s", getString(R.string.resolution), settingsManager.getOriginalHeight(), settingsManager.getOriginalWidth(), getString(R.string.progressive)));
        tweakedResolution = findViewById(R.id.textViewTweakedResolution);
        tweakedResolution.setText(String.format("%s\n%dx%d%s", getString(R.string.resolution_tweaked),(int) ((coefficients[1] * settingsManager.getLastResolutionScale()) + settingsManager.getOriginalHeight()), (int) ((coefficients[0] * settingsManager.getLastResolutionScale()) + settingsManager.getOriginalWidth()) , getString(R.string.progressive)));



        resolutionSeekBar = findViewById(R.id.seekBarRes);
        resolutionSeekBar.setProgress(settingsManager.getLastResolutionScale());

        circularProgressBar = findViewById(R.id.progressBar);
        circularProgressBar.setProgress(resolutionSeekBar.getProgress());
        lastProgress = resolutionSeekBar.getProgress();

        ImageButton addGame = findViewById(R.id.addGameButton);

        init();

        if(!settingsManager.isRoot()){
            showNoRootPopup();
        }else{
            settingsManager.setRootState(true);
        }

        if (settingsManager.isFirstLaunch()){
            showDisclaimerPopup();
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

        resolutionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                circularProgressBar.incrementProgressBy((i- lastProgress));
                lastProgress = i;
                FPSPercentage.setText(String.format("+%d%%", (int) (i * 0.8)));

                tweakedResolution.setText(String.format("%s\n%dx%d%s", getString(R.string.resolution_tweaked),(int) (Math.ceil(coefficients[1] * i) + settingsManager.getOriginalHeight()), (int) (Math.ceil(coefficients[0] * i) + settingsManager.getOriginalWidth()), getString(R.string.progressive)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        addGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddGame(true, settingsManager.findFirstEmptyRecentGameApp());
            }
        });


    }


    void init() {
        int canWriteSecureSettings = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS);
        Log.d("CAN WRITE SECURE SET: ", String.valueOf(canWriteSecureSettings));

        Log.d("DEVICE RAM","IS LOW ?");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ) {
            int canQueryPackages = ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES);
            Log.d("QUERY PACKAGES", String.valueOf(canQueryPackages));
            if(canQueryPackages == -1 /*Permission denied*/ ){
                Toast.makeText(this,"No permission",Toast.LENGTH_LONG).show();
            }
        }

        return;
    }

    void showGameListPopup(Context context, boolean onlyAddGames, final int gameAppIndex){
        final Dialog gameListPopUp = new Dialog(this);

        int xScreen = context.getResources().getDisplayMetrics().widthPixels;
        int yScreen = context.getResources().getDisplayMetrics().heightPixels;

        gameListPopUp.setContentView(R.layout.add_game_layout);
        gameListPopUp.getWindow().setLayout((int) Math.ceil(xScreen*0.90),(int) Math.min(Math.ceil(yScreen*0.85),gameList.size()*200 + (onlyAddGames ? 200 : 0)) );//The magic number 200 correspond to one GameApp item + one space

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
                    //settingsManager.addGameApp(packageName.getText().toString());

                    gameListPopUp.dismiss();
                    addGameUI(packageName.getText().toString(), gameAppIndex);
                    //GameAppManager.launchGameApp(MainActivity.this,packageName.getText().toString());
                }
            });

            child = new Space(this);
            child.setMinimumHeight(10);

            layout.addView(child);


        }
        if(onlyAddGames){
            View lastChild = inflater.inflate(R.layout.no_game_app_item, null);
            layout.addView(lastChild);
            lastChild.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    gameListPopUp.dismiss();
                    gameList = GameAppManager.getGameApps(MainActivity.this, false);
                    showGameListPopup(MainActivity.this, false, gameAppIndex);

                }
            });
        }

        gameListPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        gameListPopUp.show();
    }


    public void test(String str){
        Log.d("PackageName: ", str);
    }


    private void initializeOptions(){
        //Link option switches
        optionCheckboxes[0] = findViewById(R.id.checkBoxAggressive);
        optionCheckboxes[1] = findViewById(R.id.checkBoxMurderer);
        optionCheckboxes[2] = findViewById(R.id.checkBoxStockDPI);

        //Load their previous state:
        optionCheckboxes[0].setChecked(settingsManager.isLMKActivated());
        optionCheckboxes[1].setChecked(settingsManager.isMurderer());
        optionCheckboxes[2].setChecked(settingsManager.keepStockDPI());


    }

    public void setOptionsClickable(boolean state) {
        for (int i = 0; i < optionCheckboxes.length; i++) {
            optionCheckboxes[i].setClickable(state);
        }
        return;
    }

    private void setOptionsOnClickListener(){
        //If you have a non-rooted device, use the non rooted behavior instead
        if(settingsManager.isRoot()) {
            optionCheckboxes[0].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settingsManager.setLMK(optionCheckboxes[0].isChecked());
                }
            });

            optionCheckboxes[1].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settingsManager.setMurderer(optionCheckboxes[1].isChecked());
                }
            });
        }else{
            optionCheckboxes[0].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    optionCheckboxes[0].setChecked(false);
                    Toast.makeText(MainActivity.this, getString(R.string.no_permission_toast), Toast.LENGTH_SHORT).show();
                }
            });

            optionCheckboxes[1].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    optionCheckboxes[1].setChecked(false);
                    Toast.makeText(MainActivity.this, getString(R.string.no_permission_toast), Toast.LENGTH_SHORT).show();
                }
            });
        }

        optionCheckboxes[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.setKeepStockDPI(optionCheckboxes[2].isChecked());
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

        loadRecentGamesUI();
    }

    private void loadRecentGamesUI(){
        //Now we need to load recent games
        for(int i=0; i<6; i++){
            recentGameApp[i] = settingsManager.getRecentGameApp(i+1);
            final int finalI = i;
            if(recentGameApp[i] != null) {
                recentGameAppTitle[i].setText(recentGameApp[i].getGameName());
                recentGameAppLogo[i].setImageDrawable(recentGameApp[i].getIcon());


                recentGameAppLogo[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        GameAppManager.launchGameApp(MainActivity.this, recentGameApp[finalI].getPackageName());
                    }
                });
                recentGameAppLogo[i].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        //The long click is used to delete an app
                        //Todo create a complete sub menu
                        showRemoveGamePopup(MainActivity.this, finalI);
                        return false;
                    }
                });
            }else{
                //No game yet, let's add a game
                recentGameAppTitle[i].setText(R.string.empty_recent_game);
                recentGameAppLogo[i].setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.empty_recent_game));

                recentGameAppLogo[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddGame(true, finalI);
                    }
                });
            }
        }
        return;
    }

    private void setRecentGameAppClickable(boolean state){
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


        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showNoRootPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.no_root_popup_title)
                .setMessage(R.string.no_root_popup_text)
                .setPositiveButton(R.string.no_root_popup_positive_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int canWriteSecureSettings = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_SECURE_SETTINGS);
                        if(canWriteSecureSettings == PackageManager.PERMISSION_DENIED){
                            showNoPermissionsPopup();
                        }else{
                            //The no-root method is ready, so we register it in the settings;
                            settingsManager.setRootState(false);
                        }


                    }
                })
                .setNegativeButton(R.string.no_root_popup_negative_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                        dialog.dismiss();
                        finish();
                    }
                }).setCancelable(false);


        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showNoPermissionsPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.no_permission_popup_title)
                .setMessage(R.string.no_permission_popup_text)
                .setPositiveButton(R.string.no_permission_popup_positive_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        Uri url = Uri.parse(getString(R.string.link_to_setup));
                        Intent intent = new Intent(Intent.ACTION_VIEW, url);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        }
                        finish();

                    }
                })
                .setNegativeButton(R.string.no_permission_popup_negative_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                        dialog.dismiss();
                        finish();
                    }
                });


        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showDisclaimerPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.disclaimer_popup_title)
                .setMessage(R.string.disclaimer_popup_text)
                .setPositiveButton(R.string.disclaimer_popup_positive_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //The user accepted, meaning he entered the app for the first time
                        settingsManager.initializeFirstLaunch();

                    }
                })
                .setNegativeButton(R.string.disclaimer_popup_negative_choice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                        dialog.dismiss();
                        finish();
                    }
                }).setCancelable(false);


        AlertDialog dialog = builder.create();

        dialog.show();
    }

    void showRemoveGamePopup(Context context, final int gameAppIndex){
        final Dialog gameListPopUp = new Dialog(this);

        final int xScreen = context.getResources().getDisplayMetrics().widthPixels;
        final int yScreen = context.getResources().getDisplayMetrics().heightPixels;

        gameListPopUp.setContentView(R.layout.add_game_layout);
        gameListPopUp.getWindow().setLayout((int) Math.ceil(xScreen*0.90),(int) Math.min(Math.ceil(yScreen*0.85), 204*2 /*204 x number of items*/) );//The magic number 200 correspond to one GameApp item + one space

        LinearLayout layout = (LinearLayout) gameListPopUp.findViewById(R.id.gameListLayout);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);



        //First the GameApp to show details
        View child = inflater.inflate(R.layout.game_app_item, null);
        TextView title = child.findViewById(R.id.textViewGameAppTitle);
        TextView packageName = child.findViewById(R.id.textViewGameAppPackageName);
        ImageView icon = child.findViewById(R.id.imageViewGameIcon);

        GameApp game = settingsManager.getRecentGameApp(gameAppIndex+1);

        title.setText(game.getGameName());
        packageName.setText(game.getPackageName());
        icon.setImageDrawable(game.getIcon());

        layout.addView(child);


        //Then a space
        child = new Space(this);
        child.setMinimumHeight(10);

        layout.addView(child);

        //Then the remove from list button
        child = inflater.inflate(R.layout.detail_item, null);
        title = child.findViewById(R.id.textViewGameAppTitle);
        packageName = child.findViewById(R.id.textViewGameAppPackageName);

        title.setText(getString(R.string.remove_popup_title));
        packageName.setText(getString(R.string.remove_popup_text));

        layout.addView(child);

        child.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeGameUI(gameAppIndex);
                gameListPopUp.dismiss();
            }
        });





        gameListPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        gameListPopUp.show();
    }

    private void showAddGame(boolean onlyAddGames, int gameAppIndex){
        gameList = GameAppManager.getGameApps(MainActivity.this, onlyAddGames);
        showGameListPopup(MainActivity.this, onlyAddGames, gameAppIndex);
    }

    private void addGameUI(String packageName, int index){
        settingsManager.addGameApp(packageName, index);
        loadRecentGamesUI();
    }

    private void removeGameUI(int index){
        settingsManager.removeGameApp(index);
        loadRecentGamesUI();
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