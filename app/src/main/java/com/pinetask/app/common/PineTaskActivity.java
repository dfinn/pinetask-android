package com.pinetask.app.common;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.pinetask.app.db.DbCallback;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/** Activity base class to provide utility methods like logging helpers. **/
public abstract class PineTaskActivity extends AppCompatActivity
{
    protected boolean mActivityActive;
    final List<Disposable> mActiveDisposables = new ArrayList<>();

    @Inject protected PrefsManager mPrefsManager;
    @Inject protected PineTaskApplication mPineTaskApplication;
    @Inject protected DbHelper mDbHelper;
    @Inject protected Bus mBus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        PineTaskApplication.getInstance().getAppComponent().inject(this);
    }

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        logMsg("onPostResume");
        mActivityActive = true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        logMsg("onPause");
        mActivityActive = false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        synchronized (mActiveDisposables)
        {
            logMsg("onDestroy: disposing of %d active disposables", mActiveDisposables.size());
            for (Disposable d : mActiveDisposables)
            {
                d.dispose();
            }
        }
        logMsg("onDestroy returning");
    }


    protected CompletableObserver activityObserver(String operationDescription)
    {
        return activityObserver(operationDescription, null);
    }

    /** Helper method to return an observer that can be used to subscribe to a Completable and log messages indicating subscribe/complete/error.
     *  If completeAction is non-null, invoke the callback specified when onComplete() is called.
     *  If onError() is called, the exception is logged and the message is displayed to the user.
     *  Sets the activity to non-idle state after subscribe is called until either onComplete() or onError().
     *  When subscribed to, store the Disposable in the mActiveDisposables list.  All of these disposables will be disposed when the activity is destroyed. **/
    protected CompletableObserver activityObserver(String operationDescription, Runnable completeAction)
    {
        return new CompletableObserver()
        {
            @Override
            public void onSubscribe(Disposable d)
            {
                PineTaskApplication.getInstance().addActiveTask();
                mActiveDisposables.add(d);
                logMsg("Begin operation: %s (mActiveDisposables size=%d)", operationDescription, mActiveDisposables.size());
            }

            @Override
            public void onComplete()
            {
                logMsg("Completed: %s", operationDescription);
                PineTaskApplication.getInstance().endActiveTask();
                if (completeAction!=null) completeAction.run();
            }

            @Override
            public void onError(Throwable ex)
            {
                PineTaskApplication.getInstance().endActiveTask();
                logError("Error in operation '%s'", operationDescription);
                logException(ex);
                showUserMessage(false, ex.getMessage());
            }
        };
    }

    /** Observer for a single result that will just pass the result to a callback when available.
     *  Increments the activity's active task count while the query is in progress.
     *  If an error occurs, logs the exception and raises a user message. **/
    public <T> SingleObserver<T> singleObserver(final DbCallback<T> callback)
    {
        return new SingleObserver<T>()
        {
            @Override
            public void onSubscribe(Disposable d)
            {
                PineTaskApplication.getInstance().addActiveTask();
            }

            @Override
            public void onSuccess(T t)
            {
                PineTaskApplication.getInstance().endActiveTask();
                callback.onResult(t);
            }

            @Override
            public void onError(Throwable ex)
            {
                PineTaskApplication.getInstance().endActiveTask();
                logException(ex);
                showUserMessage(false, ex.getMessage());
            }
        };
    }

    protected void setResultAndFinish(int resultCode)
    {
        setResult(resultCode);
        finish();
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }

    protected void logException(Throwable ex)
    {
        Logger.logException(getClass(), ex);
    }

    protected void logError(String msg, Object...args)
    {
        Logger.logError(getClass(), msg, args);
    }

    public void showUserMessage(boolean finishActivity, String message, Object... args)
    {
        if (mActivityActive)
        {
            String text = String.format(message, args);
            logMsg("Showing user message: %s", text.replace("%", "%%"));
            ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(text, finishActivity);
            getSupportFragmentManager().beginTransaction().add(dialog, ErrorDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
        }
    }

    public void hideSoftKeyboard()
    {
        View focusedView = getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

}
