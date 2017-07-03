package com.pinetask.app.common;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.pinetask.app.db.DbHelper;

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

    public void showSoftKeyboard(EditText editText)
    {
        editText.setOnFocusChangeListener((View v, boolean hasFocus) ->
        {
            if (hasFocus)
            {
                getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                editText.selectAll();
            }
        });
    }

    public void hideSoftKeyboard()
    {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getDialog().getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
