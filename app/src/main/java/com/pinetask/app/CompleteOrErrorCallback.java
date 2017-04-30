package com.pinetask.app;

public interface CompleteOrErrorCallback
{
    void onComplete();
    void onError(Throwable ex);
}
