package com.pinetask.common;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class Logger
{
    public static void logMsg(Class source, String msg, Object...args)
    {
        if (BuildConfig.DEBUG)
        {
            Log.i("PineTask_" + source.getSimpleName(), String.format(msg, args));
        }
    }

    public static void logError(Class source, String msg, Object...args)
    {
        if (BuildConfig.DEBUG)
        {
            Log.e("PineTask_" + source.getSimpleName(), String.format(msg, args));
        }
    }

    public static void logException(Class source, Throwable ex)
    {
        if (BuildConfig.DEBUG)
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(bos);
            ex.printStackTrace(pw);
            pw.flush();
            pw.close();
            String stackTraceStr = bos.toString();
            logError(source, stackTraceStr.replace("%", "%%")); // Escape any % symbols so they aren't interpreted as string formatters.
        }
    }

}
