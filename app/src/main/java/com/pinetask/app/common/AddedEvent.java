package com.pinetask.app.common;

/** Wrapper class indicating that an item was added. **/
public class AddedEvent<T> extends AddedOrDeletedEvent<T>
{
    /** If true, indicates that the item is "new", that is, it was created after existing items were loaded. **/
    private boolean mIsNew;
    public boolean getIsNew() { return mIsNew; }
    public void setIsNew(boolean isNew) { mIsNew = isNew; }

    public AddedEvent(T item)
    {
        Item = item;
    }
    public AddedEvent(T item, boolean isNew)
    {
        Item = item; mIsNew = isNew;
    }
}
