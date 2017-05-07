package com.pinetask.app;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/** Helper class for items stored in shared preferences. **/
public class PrefsManager
{
    private static String PREFS_NAME = "PineTaskPrefs";
    private static PrefsManager mInstance;
    private SharedPreferences mSharedPreferences;

    /** Return the singleton instance of PrefsManager for getting/setting application settings. **/
    public static synchronized PrefsManager getInstance(Context context)
    {
        if (mInstance == null)
        {
            mInstance = new PrefsManager(context);
        }
        return mInstance;
    }

    public PrefsManager(Context context)
    {
        mSharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    /** Indicates if startup tutorial has been completed. **/
    private static String TUTORIAL_COMPLETED_KEY = "TutorialCompleted";
    public boolean getTutorialCompleted() { return mSharedPreferences.getBoolean(TUTORIAL_COMPLETED_KEY, false); }
    public void setTutorialCompleted(boolean completed) { mSharedPreferences.edit().putBoolean(TUTORIAL_COMPLETED_KEY, completed).commit(); }

    /** ID of the currently displayed list (string). **/
    private static String CURRENT_LIST_ID_KEY = "CurrentListId";
    public String getCurrentListId() { return mSharedPreferences.getString(CURRENT_LIST_ID_KEY, null); }
    public void setCurrentListId(String listId) { mSharedPreferences.edit().putString(CURRENT_LIST_ID_KEY, listId).commit(); }

    /** Boolean value indicating if this is the first app launch. **/
    private static String IS_FIRST_LAUNCH_KEY = "IsFirstLaunch";
    public boolean getIsFirstLaunch() { return mSharedPreferences.getBoolean(IS_FIRST_LAUNCH_KEY, true); }
    public void setIsFirstLaunch(boolean isFirstLaunch) { mSharedPreferences.edit().putBoolean(IS_FIRST_LAUNCH_KEY, isFirstLaunch).commit(); }
}
