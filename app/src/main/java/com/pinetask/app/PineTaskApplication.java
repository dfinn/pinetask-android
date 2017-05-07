package com.pinetask.app;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

import io.fabric.sdk.android.Fabric;

public class PineTaskApplication extends MultiDexApplication
{
    static Bus mEventBus;
    public static Bus getEventBus() { return mEventBus; }

    static PineTaskApplication mApplicationInstance;
    public static PineTaskApplication getInstance() { return mApplicationInstance; }

    /** For help with Espresso testing: keep track of the number of active tasks, which will be queried by the Espresso idling resource. **/
    private Integer mActiveTasks=0;
    public int getActiveTasks() { return mActiveTasks; }

    /** For help with Espresso testing: runnable to be invoked if the active task count goes to zero. **/
    private Runnable mOnIdleRunnable;
    public void setOnIdleRunnable(Runnable onIdleRunnable) { mOnIdleRunnable = onIdleRunnable; }

    /** Increment the active task count **/
    public void addActiveTask()
    {
        synchronized (mActiveTasks)
        {
            mActiveTasks++;
            logMsg("addActiveTask: %d tasks now active", mActiveTasks);
        }
    }

    /** Decrement the active task count. If it goes to zero and a onIdleRunnable was provided, call it. **/
    public void endActiveTask()
    {
        synchronized (mActiveTasks)
        {
            mActiveTasks--;
            if (mActiveTasks<0)
            {
                logError("Warning: mActiveTasks is negative, resetting to zero");
                mActiveTasks=0;
            }
            if (mActiveTasks==0 && mOnIdleRunnable != null) mOnIdleRunnable.run();
            logMsg("endActiveTask: %d tasks now active", mActiveTasks);
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Logger.setLoggingEnabled(BuildConfig.DEBUG);
        mApplicationInstance = this;
        mEventBus = new Bus();
        Fabric.with(this, new Crashlytics());
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    /** Helper method to raise a info/error message that will be displayed to the user. **/
    public static void raiseUserMsg(boolean isError, String msg, Object... args)
    {
        mEventBus.post(new UserMessage(isError, msg, args));
    }

    /** Helper method to raise a info/error message that will be displayed to the user. **/
    public static void raiseUserMsg(boolean isError, int stringResId, Object... args)
    {
        String str = mApplicationInstance.getString(stringResId);
        String formatted = String.format(str, args);
        mEventBus.post(new UserMessage(isError, formatted));
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }

    protected void logError(String msg, Object...args)
    {
        Logger.logError(getClass(), msg, args);
    }
}
