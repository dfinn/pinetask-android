package com.pinetask.app.common;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class Logger
{
    public static void setIsDebugBuild(boolean isDebugBuild) { mIsDebugBuild = isDebugBuild; }
    private static boolean mIsDebugBuild;

    public static void logMsg(Class source, String msg, Object...args)
    {
        if (mIsDebugBuild)
        {
            String threadName = Thread.currentThread().getName();
            Log.i("PineTask_" + source.getSimpleName(), "[" + threadName + "] " + String.format(msg, args));
        }
    }

    public static void logError(Class source, String msg, Object...args)
    {
        if (mIsDebugBuild)
        {
            String threadName = Thread.currentThread().getName();
            Log.e("PineTask_" + source.getSimpleName(), "[" + threadName + "] " + String.format(msg, args));
        }
    }

    public static void logException(Class source, Throwable ex)
    {
        if (mIsDebugBuild)
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(bos);
            ex.printStackTrace(pw);
            pw.flush();
            pw.close();
            String stackTraceStr = bos.toString();
            logError(source, stackTraceStr.replace("%", "%%")); // Escape any % symbols so they aren't interpreted as string formatters.
        }
        else
        {
            Crashlytics.logException(ex);
        }
    }

    public static void logErrorAndException(Class source, Throwable ex, String msg, Object... args)
    {
        logError(source, msg, args);
        logException(source, ex);
    }

}
