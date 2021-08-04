package com.spse.gameresolutionchanger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spse.gameresolutionchanger.recyclerview.ViewAdapter;

import java.io.File;
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_DENIED){
                showNoRootPopup();
            }
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
        settingsSwitch.setOnClickListener(view -> {
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

        });

        resolutionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                circularProgressBar.incrementProgressBy((i- lastProgress));
                lastProgress = i;
                FPSPercentage.setText(String.format("+%d%%", (int) (i * 0.8)));

                tweakedResolution.setText(String.format("%s\n%dx%d%s",
                        getString(R.string.resolution_tweaked),
                        (int) (Math.ceil(coefficients[1] * i) + settingsManager.getOriginalHeight()),
                        (int) (Math.ceil(coefficients[0] * i) + settingsManager.getOriginalWidth()),
                        getString(R.string.progressive)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        addGame.setOnClickListener(view -> showAddGame(true, settingsManager.findFirstEmptyRecentGameApp()));
    }


    void init() {
        int canWriteSecureSettings = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS);
        Log.d("CAN WRITE SECURE SET: ", String.valueOf(canWriteSecureSettings));

        Log.d("DEVICE RAM","IS LOW ?");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ) {
            int canQueryPackages = ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES);
            Log.d("QUERY PACKAGES", String.valueOf(canQueryPackages));
            if(canQueryPackages == PackageManager.PERMISSION_DENIED ){
                Toast.makeText(this,"No permission",Toast.LENGTH_LONG).show();
            }
        }
    }


    public void showGameList(boolean onlyAddGames, int position){
        final Dialog gameListPopUp = new Dialog(this);
        gameListPopUp.setContentView(R.layout.add_game_layout);

        RecyclerView recyclerView = gameListPopUp.findViewById(R.id.recyclerViewList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(new ViewAdapter(GameAppManager.getPackages(this, onlyAddGames), this, gameListPopUp, position));

        gameListPopUp.findViewById(R.id.textViewDontFindGame).setOnClickListener(v -> {
            gameListPopUp.dismiss();
            showAddGame(false, position);
        });


        gameListPopUp.show();
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
        for (CheckBox optionCheckbox : optionCheckboxes) {
            optionCheckbox.setClickable(state);
        }
    }

    private void setOptionsOnClickListener(){
        //If you have a non-rooted device, use the non rooted behavior instead
        if(settingsManager.isRoot()) {
            optionCheckboxes[0].setOnCheckedChangeListener((buttonView, isChecked) -> settingsManager.setLMK(isChecked));
            optionCheckboxes[1].setOnCheckedChangeListener((buttonView, isChecked) -> settingsManager.setMurderer(isChecked));
            optionCheckboxes[2].setOnCheckedChangeListener((buttonView, isChecked) -> settingsManager.setKeepStockDPI(isChecked));

        }else{
            View.OnClickListener clickListener = v -> Toast.makeText(MainActivity.this, getString(R.string.no_permission_toast), Toast.LENGTH_SHORT).show();
            for (CheckBox optionCheckbox : optionCheckboxes) {
                optionCheckbox.setOnClickListener(clickListener);
            }
        }


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
            int finalI = i;
            recentGameApp[i] = settingsManager.getRecentGameApp(i+1);
            if(recentGameApp[i] != null) {
                recentGameAppTitle[i].setText(recentGameApp[i].getGameName());
                recentGameAppLogo[i].setImageDrawable(recentGameApp[i].getIcon());

                recentGameAppLogo[i].setOnClickListener(view -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.choice_title);
                    builder.setMessage(R.string.choice_text);
                    builder.setPositiveButton(R.string.choice_launch_app, (dialog, which) -> GameAppManager.launchGameApp(MainActivity.this, recentGameApp[finalI].getPackageName()));
                    builder.setNeutralButton(R.string.choice_remove_app, (dialog, which) -> removeGameUI(finalI));
                    AlertDialog dialog = builder.show();

                    Button positiveButton = dialog.findViewById(android.R.id.button1);
                    Button neutralButton = dialog.findViewById(android.R.id.button3);
                    positiveButton.setScaleX(1.3f);
                    positiveButton.setScaleY(1.3f);
                    neutralButton.setScaleX(1.3f);
                    neutralButton.setScaleY(1.3f);

                });
                continue;
            }
            //No game yet, let's add a game
            recentGameAppTitle[i].setText(R.string.empty_recent_game);
            recentGameAppLogo[i].setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.empty_recent_game));

            recentGameAppLogo[i].setOnClickListener(v -> showAddGame(true, finalI));

        }
    }

    private void setRecentGameAppClickable(boolean state){
        for(ImageButton button : recentGameAppLogo){
            button.setClickable(state);
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
                .setPositiveButton(R.string.reset_popup_positive_choice, (dialog, which) -> {
                    dialog.dismiss();
                    GameAppManager.restoreOriginalLMK(MainActivity.this);
                    settingsManager.setScreenDimension(settingsManager.getOriginalHeight(), settingsManager.getOriginalWidth());
                })
                .setNegativeButton(R.string.reset_popup_negative_choice, (dialog, which) -> {
                    Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showNoRootPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);


        builder.setTitle(R.string.no_root_popup_title)
                .setMessage(R.string.no_root_popup_text)
                .setPositiveButton(R.string.no_root_popup_positive_choice, (dialog, which) -> {
                    dialog.dismiss();
                    int canWriteSecureSettings = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_SECURE_SETTINGS);
                    if(canWriteSecureSettings == PackageManager.PERMISSION_DENIED){
                        showNoPermissionsPopup();
                    }else{
                        //The no-root method is ready, so we register it in the settings;
                        settingsManager.setRootState(false);
                    }


                })
                .setNegativeButton(R.string.no_root_popup_negative_choice, (dialog, which) -> {
                    Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                    dialog.dismiss();
                    finish();
                }).setCancelable(false);


        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showNoPermissionsPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.no_permission_popup_title)
                .setMessage(R.string.no_permission_popup_text)
                .setPositiveButton(R.string.no_permission_popup_positive_choice, (dialog, which) -> {
                    dialog.dismiss();

                    Uri url = Uri.parse(getString(R.string.link_to_setup));
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                    finish();

                })
                .setNegativeButton(R.string.no_permission_popup_negative_choice, (dialog, which) -> {
                    Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                    dialog.dismiss();
                    finish();
                });


        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showDisclaimerPopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.disclaimer_popup_title)
                .setMessage(R.string.disclaimer_popup_text)
                .setPositiveButton(R.string.disclaimer_popup_positive_choice, (dialog, which) -> {
                    dialog.dismiss();
                    //The user accepted, meaning he entered the app for the first time
                    settingsManager.initializeFirstLaunch();

                })
                .setNegativeButton(R.string.disclaimer_popup_negative_choice, (dialog, which) -> {
                    Log.d("NEGATIVE CHOICE BUTTON", "PRESSED");
                    dialog.dismiss();
                    finish();
                }).setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.show();
    }


    private void showAddGame(boolean onlyAddGames, int gameAppIndex){
        showGameList(onlyAddGames, gameAppIndex);
    }

    public void addGameUI(String packageName, int index){
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