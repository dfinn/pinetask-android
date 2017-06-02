package com.pinetask.app.common;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.pinetask.app.db.DbHelper;
import com.pinetask.common.Logger;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;

/** Base class for dialogs, providing utility helper methods. **/
public class PineTaskDialogFragment extends DialogFragment
{
    @Inject protected DbHelper mDbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        PineTaskApplication.getInstance().getAppComponent().inject(this);
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }

    protected void logException(Throwable ex)
    {
        Logger.logException(getClass(), ex);
    }

    protected CompletableObserver activityObserver(String operationDescription, Runnable completeAction)
    {
        PineTaskActivity activity = (PineTaskActivity) getActivity();
        return activity.activityObserver(operationDescription, completeAction);
    }

    protected CompletableObserver activityObserver(String operationDescription)
    {
        PineTaskActivity activity = (PineTaskActivity) getActivity();
        return activity.activityObserver(operationDescription, null);
    }

}
