package com.spse.gameresolutionchanger.failsafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.spse.gameresolutionchanger.ExecuteADBCommands;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class FailsafeBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Reset to the original physical value
        //This will cause an issue if the user always want a different value as the default value.
        ExecuteADBCommands.execute((ArrayList<String>) Arrays.asList("wm density reset", "wm size reset"), ExecuteADBCommands.canRunRootCommands());
    }
}
