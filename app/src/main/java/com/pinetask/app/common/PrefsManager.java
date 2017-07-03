package com.pinetask.app.common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.content.Context.MODE_PRIVATE;

/** Helper class for items stored in shared preferences. **/
@Singleton
public class PrefsManager
{
    private final String PREFS_NAME = "PineTaskPrefs";
    private SharedPreferences mSharedPreferences;

    @Inject
    public PrefsManager(PineTaskApplication context)
    {
        mSharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    /** ID of the currently displayed list (string). **/
    private final String CURRENT_LIST_ID_KEY = "CurrentListId";
    public String getCurrentListId() { return mSharedPreferences.getString(CURRENT_LIST_ID_KEY, null); }
    public void setCurrentListId(String listId) { mSharedPreferences.edit().putString(CURRENT_LIST_ID_KEY, listId).apply(); }

    /** Boolean value indicating if this is the first app launch. **/
    private final String IS_FIRST_LAUNCH_KEY = "IsFirstLaunch";
    public boolean getIsFirstLaunch() { return mSharedPreferences.getBoolean(IS_FIRST_LAUNCH_KEY, true); }
    public void setIsFirstLaunch(boolean isFirstLaunch) { mSharedPreferences.edit().putBoolean(IS_FIRST_LAUNCH_KEY, isFirstLaunch).apply(); }

    /** Boolean values indicating whether the first launch tooltips have been shown for various app features. **/
    public boolean isTipShown(String key) { return mSharedPreferences.getBoolean(key, false); }
    public void setTipShown(String key) { mSharedPreferences.edit().putBoolean(key, true).apply(); }
}
