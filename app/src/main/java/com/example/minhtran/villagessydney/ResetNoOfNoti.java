package com.example.minhtran.villagessydney;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


public class ResetNoOfNoti extends BroadcastReceiver  {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.NotificationSetting), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(context.getString(R.string.noOfNoti), 0);
        editor.commit();
       //PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        Log.i("RESET", "ALL RESETED");
    }
}
