package com.pinetask.app.common;

/** Wrapper class indicating that the item was deleted. **/
public class DeletedEvent<T> extends AddedOrDeletedEvent<T>
{
    public DeletedEvent(T item)
    {
        Item = item;
    }
}
