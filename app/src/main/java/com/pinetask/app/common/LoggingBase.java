package com.pinetask.app.common;

/** Helper class to extend from that provides logging convenience helpers. **/
public class LoggingBase
{
    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }

    protected void logError(String msg, Object...args)
    {
        Logger.logError(getClass(), msg, args);
    }

    protected void logException(Throwable ex)
    {
        Logger.logException(getClass(), ex);
    }

    protected void logErrorAndException(Throwable ex, String msg, Object... args)
    {
        logError(msg, args);
        logException(ex);
    }


}
