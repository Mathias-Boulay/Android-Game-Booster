package com.spse.gameresolutionchanger;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GameAppManager {

    public static GameApp getGameApp(Context context, String packageName){
        if (packageName.equals("")) return null;
        PackageManager PM = context.getPackageManager();
        try {
            ApplicationInfo app = PM.getApplicationInfo(packageName, 0);
            WrappedDrawable wrappedIcon = new WrappedDrawable(PM.getApplicationIcon(app), 0,0,180,180);
            String name = PM.getApplicationLabel(app).toString();

            return new GameApp(name,wrappedIcon,packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<String> getPackages(Context ctx, boolean onlyAddGames){
        PackageManager gamePM = ctx.getPackageManager();
        ArrayList<String> packageList = new ArrayList<>();
        List<PackageInfo> gameAppInfo = gamePM.getInstalledPackages(0);
        for(PackageInfo info : gameAppInfo){
            if(isSystemPackage(info)) continue;
            if(onlyAddGames && !isGamePackage(info.applicationInfo, onlyAddGames)) continue;

            packageList.add(info.packageName);
        }
        return packageList;
    }

    private static boolean isSystemPackage( PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }


    private static boolean isGamePackage(ApplicationInfo appInfo, boolean onlyAddGames) {
        //Little hack to just consider everything as a game when we want everything
        if(!onlyAddGames){return true;}

        //Check both the new and deprecated flag since some apps only use the deprecated one...
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return (appInfo.category == ApplicationInfo.CATEGORY_GAME)
                    || (appInfo.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
        }
        return (appInfo.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
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

        ADB.execute(murdererList, true);

    }


    private static void activateLMK(Context context){
        ADB.execute("cat /sys/module/lowmemorykiller/parameters/minfree > " + context.getApplicationInfo().dataDir + "/lastLMKProfile.backup", true);

        ADB.execute("echo 2560,5120,11520,25600,35840,358400 > /sys/module/lowmemorykiller/parameters/minfree", true);
    }

    public static void restoreOriginalLMK(Context context){
        ADB.execute("cat " + context.getApplicationInfo().dataDir + "/lastLMKProfile.backup > /sys/module/lowmemorykiller/parameters/minfree", true);
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
            ADB.execute("echo '' > " + context.getApplicationInfo().dataDir + "/tmp", st.isRoot());
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
