package com.spse.gameresolutionchanger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GameAppManager {


    public static List<GameApp> getGameApps(MainActivity context, boolean onlyAddGames){
        PackageManager gamePM = context.getPackageManager();
        List<GameApp> gameAppList = new ArrayList<>();
        List<PackageInfo> gameAppInfo = gamePM.getInstalledPackages(0);

        for(PackageInfo info : gameAppInfo){

            if (!isValidPackage(context, info, onlyAddGames)) {
                continue;
            }

            String appName = info.applicationInfo.loadLabel(gamePM).toString();
            String packages = info.applicationInfo.packageName;

            WrappedDrawable wrappedIcon = new WrappedDrawable(info.applicationInfo.loadIcon(gamePM));
            wrappedIcon.setBounds(0,0,160,160);

            gameAppList.add(new GameApp(appName, wrappedIcon, packages));

        }
        return gameAppList;
    }

    public static GameApp getGameApp(Context context, String packageName){
        if (packageName.equals("")) return null;
        PackageManager PM = context.getPackageManager();
        try {

            ApplicationInfo app = PM.getApplicationInfo(packageName, 0);
            WrappedDrawable wrappedIcon = new WrappedDrawable(PM.getApplicationIcon(app));
            wrappedIcon.setBounds(0,0,180,180);
            String name = PM.getApplicationLabel(app).toString();

            return new GameApp(name,wrappedIcon,packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isSystemPackage( PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    @SuppressLint("NewApi")
    private static boolean isGamePackage(MainActivity context, ApplicationInfo appInfo, boolean onlyAddGames) {
        //Little hack to just consider everything as a game when we want everything
        if(!onlyAddGames){return true;}

        //Check both the new and deprecated flag since some apps only use the deprecated one...
        return ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (appInfo.category == ApplicationInfo.CATEGORY_GAME)) || ((appInfo.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME);
    }

    private static boolean isValidPackage(MainActivity context, PackageInfo pkgInfo, boolean onlyAddGames){

        if((isGamePackage(context, pkgInfo.applicationInfo, onlyAddGames)
                && !isSystemPackage(pkgInfo)
                && !pkgInfo.packageName.equals(context.getApplicationContext().getPackageName())

        )){
            for(int i=1;i<=6;i++){
                if(context.settingsManager.getRecentGameApp(i) != null){
                    if(context.settingsManager.getRecentGameApp(i).getPackageName().equals(pkgInfo.packageName)){
                        return false;
                    }
                }
            }
            return true;
        }
        return false;



    }

    private static void murderApps(MainActivity context){
        ArrayList<String> murdererList = new ArrayList<>();

        PackageManager gamePM = context.getPackageManager();
        List<PackageInfo> gameAppInfo = gamePM.getInstalledPackages(0);


        final String[] unkillableAppList = new String[]{
                context.getApplication().getPackageName(),
                "com.topjohnwu.magisk",
                "eu.chainfire.supersu-1"
        };

        for(int i=0;i < gameAppInfo.size(); i++){
            String packageName = gameAppInfo.get(i).packageName;

            boolean isKillable = true;
            for(String unkillableApp : unkillableAppList){
                if(isSystemPackage(gameAppInfo.get(i)) || packageName.equals(unkillableApp)){
                    isKillable = false;
                    break;
                }
            }
            if(isKillable){
                murdererList.add("am force-stop ".concat(packageName));
            }
        }

        ExecuteADBCommands.execute(murdererList, true);

    }


    private static void activateLMK(MainActivity context){
        ExecuteADBCommands.execute("cat /sys/module/lowmemorykiller/parameters/minfree > " + context.getApplicationInfo().dataDir + "/lastLMKProfile.backup", true);

        ExecuteADBCommands.execute("echo 2560,5120,11520,25600,35840,358400 > /sys/module/lowmemorykiller/parameters/minfree", true);
    }

    public static void restoreOriginalLMK(MainActivity context){
        ExecuteADBCommands.execute("cat " + context.getApplicationInfo().dataDir + "/lastLMKProfile.backup > /sys/module/lowmemorykiller/parameters/minfree", true);
        new File(context.getApplicationInfo().dataDir + "/lastLMKProfile.backup").delete();
    }

    public static void launchGameApp(MainActivity context, String packageName){



        //Applying modifications before launching the app:
        SettingsManager st = context.settingsManager;
        int resolutionScale = context.resolutionSeekBar.getProgress();


        //Save the resolution chosen for the game:
        st.setLastResolutionScale(context.resolutionSeekBar.getProgress());

        //Changing the DPI causes the activity layout to restart from scratch, so we have to let a trace informing that we are just changing stuff, not relaunching the app:

        if(!st.keepStockDPI()) {
            ExecuteADBCommands.execute("echo '' > " + context.getApplicationInfo().dataDir + "/tmp", st.isRoot());
        }

        st.setScreenDimension(
                (int)(Math.ceil(context.coefficients[1]*resolutionScale) + st.getOriginalHeight()),
                (int)(Math.ceil(context.coefficients[0]*resolutionScale) + st.getOriginalWidth())
        );

        if(st.isMurderer()){
            murderApps(context);
        }

        if(st.isLMKActivated()){
            activateLMK(context);
        }


        //Launch the app
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);//null pointer check in case package name was not found

            //Then I kill myself to let all the memory for the game;
            context.finish();

        }
    }


}
