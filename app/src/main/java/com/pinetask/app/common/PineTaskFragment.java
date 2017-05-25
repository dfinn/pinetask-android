package com.pinetask.app.common;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/** Base class for fragments, providing utility helper methods. **/
public class PineTaskFragment extends DialogFragment
{
    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }
    protected void logError(String msg, Object...args)
    {
        Logger.logError(getClass(), msg, args);
    }

    @Inject protected Bus mEventBus;
    @Inject protected FirebaseDatabase mDatabase;
    @Inject protected PrefsManager mPrefsManager;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logMsg("onCreate");
        PineTaskApplication.getInstance().getAppComponent().inject(this);
        mEventBus.register(this);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        logMsg("onDestroy");

        // Unregister event bus
        logMsg("UnRegistering event bus");
        mEventBus.unregister(this);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        logMsg("onViewStateRestored");
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        logMsg("onSaveInstanceState");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        logMsg("onPause");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        logMsg("onStop");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        logMsg("onResume");
    }

    @Override
    public void onStart()
    {
        super.onStart();
        logMsg("onStart");
    }
}
