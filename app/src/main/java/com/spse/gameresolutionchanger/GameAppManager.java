package com.spse.gameresolutionchanger;

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
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class GameAppManager {

    public static List<GameApp> getGameApps(Context context){
        PackageManager gamePM = context.getPackageManager();
        List<GameApp> gameAppList = new ArrayList<>();
        List<PackageInfo> gameAppInfo = gamePM.getInstalledPackages(0);

        for(int i=0; i < gameAppInfo.size(); i++){
            PackageInfo info = gameAppInfo.get(i);

            if ((!isSystemPackage(info))) {
                String appName = info.applicationInfo.loadLabel(gamePM).toString();
                String packages = info.applicationInfo.packageName;

                WrappedDrawable wrappedIcon = new WrappedDrawable(info.applicationInfo.loadIcon(gamePM));
                wrappedIcon.setBounds(0,0,160,160);

                gameAppList.add(new GameApp(appName, wrappedIcon, packages));

            }

        }
        return gameAppList;
    }

    public static GameApp getGameApp(Context context, String packageName){
        PackageManager PM = context.getPackageManager();
        try {

            ApplicationInfo app = PM.getApplicationInfo(packageName, 0);
            WrappedDrawable wrappedIcon = new WrappedDrawable(PM.getApplicationIcon(app));
            wrappedIcon.setBounds(0,0,65,65);
            String name = PM.getApplicationLabel(app).toString();

            return new GameApp(name,wrappedIcon,packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isSystemPackage(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public static void launchGameApp(Activity context, String packageName){
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);//null pointer check in case package name was not found

            //Then I kill myself to let all the memory for the game;
            context.finishAndRemoveTask();

        }
    }


}
