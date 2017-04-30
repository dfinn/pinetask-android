package com.pinetask.common;

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

    protected void logException(Class source, Throwable ex)
    {
        Logger.logException(source, ex);
    }


}
