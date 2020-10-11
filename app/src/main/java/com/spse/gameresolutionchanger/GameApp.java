package com.spse.gameresolutionchanger;

import android.graphics.drawable.Drawable;

public class GameApp {
    //Just a "dummy" class to store properties
    private String gameName;
    private Drawable icon;
    private String packageName;

    public GameApp(String gameName, Drawable icon, String packageName){
        this.gameName = gameName;
        this.icon = icon;
        this.packageName = packageName;
    }

    public String getGameName(){
        return gameName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getPackageName() {
        return packageName;
    }
}
