package com.pinetask.app.common;

import android.support.v4.app.DialogFragment;

import com.pinetask.common.Logger;

import io.reactivex.CompletableObserver;

/** Base class for dialogs, providing utility helper methods. **/
public class PineTaskDialogFragment extends DialogFragment
{
    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
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
