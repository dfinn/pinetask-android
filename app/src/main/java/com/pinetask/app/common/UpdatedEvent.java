package com.pinetask.app.common;

/** Wrapper class indicating that an item was updated. **/
public class UpdatedEvent<T> extends ChildEventBase<T>
{
    public UpdatedEvent(T item)
    {
        Item = item;
    }
}
