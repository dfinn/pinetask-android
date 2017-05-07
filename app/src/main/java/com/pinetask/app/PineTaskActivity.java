package com.pinetask.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.pinetask.common.Logger;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/** Activity base class to provide utility methods like logging helpers. **/
public abstract class PineTaskActivity extends AppCompatActivity
{
    protected PrefsManager mPrefsManager;
    protected boolean mActivityActive;
    final List<Disposable> mActiveDisposables = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mPrefsManager = PrefsManager.getInstance(this);
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

    /** Helper method to subscribe to the Observable provided, and invoke the action specified when onNext() is called.
     *  If onError() is called, the exception is logged and the message is displayed to the user.
     *  When subscribed to, store the Disposable in the mActiveDisposables list.  All of these disposables will be disposed when the activity is destroyed. **/
    protected <T> void observe(Observable<T> observable, DbCallback<T> action)
    {
        observable.subscribe(new Observer<T>()
        {
            @Override
            public void onSubscribe(Disposable d)
            {
                synchronized (mActiveDisposables)
                {
                    mActiveDisposables.add(d);
                    logMsg("observe(Observable): Added disposable to list, size now %d", mActiveDisposables.size());
                }
            }

            @Override
            public void onNext(T o)
            {
                action.onResult(o);
            }

            @Override
            public void onError(Throwable ex)
            {
                Logger.logException(getClass(), ex);
                PineTaskApplication.raiseUserMsg(true, ex.getMessage());
            }

            @Override
            public void onComplete()
            {
            }
        });
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
                PineTaskApplication.raiseUserMsg(true, ex.getMessage());
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
                PineTaskApplication.raiseUserMsg(true, ex.getMessage());
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

    /** Shows the message specified to the user.  If isError is true, the text will be bold and red.
     *  When the message is shown, it hides the items list and chat layout.
     *  An "OK" button shows below the text, and when clicked, it hides the error layout and shows the items list and chat layout again.
     **/
    public void showUserMessage(boolean isError, String message, Object... args)
    {
        if (mActivityActive)
        {
            String text = String.format(message, args);
            logMsg("Showing user message: %s", text.replace("%", "%%"));
            ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(text);
            getSupportFragmentManager().beginTransaction().add(dialog, ErrorDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
        }
    }
}
