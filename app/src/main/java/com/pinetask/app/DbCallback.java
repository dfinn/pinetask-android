package com.pinetask.app;

public interface DbCallback<T>
{
    void onResult(T data);
}
