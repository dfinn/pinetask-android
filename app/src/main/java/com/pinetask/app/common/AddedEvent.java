package com.pinetask.app.common;

/** Wrapper class indicating that an item was added. **/
public class AddedEvent<T> extends AddedOrDeletedEvent<T>
{
    public AddedEvent(T item)
    {
        Item = item;
    }
}
