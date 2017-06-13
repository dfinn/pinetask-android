package com.pinetask.app.common;

/** Wrapper class indicating that an item was added. **/
public class AddedEvent<T> extends ChildEventBase<T>
{
    public AddedEvent(T item)
    {
        Item = item;
    }
}
