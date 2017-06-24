package com.pinetask.app.common;

import com.pinetask.common.Logger;

/** Base class for presenters to provide utility methods. **/
public abstract class BasePresenter
{
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

    protected abstract void showErrorMessage(String message, Object... args);

    protected void logAndShowError(Throwable ex, String message, Object... args)
    {
        logError(message, args);
        logException(ex);
        showErrorMessage(message, args);
    }
}
