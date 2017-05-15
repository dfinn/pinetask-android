package com.pinetask.app.db;

public interface CompleteOrErrorCallback
{
    void onComplete();
    void onError(Throwable ex);
}
