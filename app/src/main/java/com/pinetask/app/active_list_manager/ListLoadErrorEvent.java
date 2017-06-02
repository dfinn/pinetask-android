package com.pinetask.app.active_list_manager;

public class ListLoadErrorEvent extends ActiveListEvent
{
    public Throwable Error;
    public ListLoadErrorEvent(Throwable error)
    {
        Error = error;
    }
}
