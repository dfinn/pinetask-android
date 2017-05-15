package com.pinetask.app.db;

public interface DbCallback<T>
{
    void onResult(T data);
}
