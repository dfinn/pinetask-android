package com.pinetask.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

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

    Bus mEventBus;
    protected PrefsManager mPrefsManager;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logMsg("onCreate");

        PineTaskApplication application = (PineTaskApplication) getActivity().getApplication();
        mEventBus = application.getEventBus();
        logMsg("Registering event bus");
        mEventBus.register(this);
        mPrefsManager = PrefsManager.getInstance(application);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        logMsg("onDestroy");

        // Unregister event bus
        PineTaskApplication application = (PineTaskApplication) getActivity().getApplication();
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
