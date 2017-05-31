package com.pinetask.app.common;

import com.pinetask.app.db.DbCallback;
import com.pinetask.common.Logger;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/** Base class for presenters to provide utility methods. **/
public abstract class BasePresenter
{
    final List<Disposable> mActiveDisposables = new ArrayList<>();

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
                showErrorMessage(ex.getMessage());
            }

            @Override
            public void onComplete()
            {
            }
        });
    }

    /** To be called when the view is being destroyed. **/
    protected void shutdown()
    {
        synchronized (mActiveDisposables)
        {
            logMsg("shutdown: disposing of %d active disposables", mActiveDisposables.size());
            for (Disposable d : mActiveDisposables)
            {
                d.dispose();
            }
        }
    }

    protected abstract void showErrorMessage(String message, Object... args);
}
