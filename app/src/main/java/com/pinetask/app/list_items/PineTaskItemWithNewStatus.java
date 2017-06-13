package com.pinetask.app.list_items;

import com.google.firebase.database.Exclude;

public class PineTaskItemWithNewStatus extends PineTaskItem
{
    /** Indicates if the item is new (ie, has not yet been displayed to the user) **/
    private boolean mIsNewItem;
    @Exclude    // No need to include this when writing the object to Firebase.
    public boolean getIsNewItem() { return mIsNewItem; }
    public void setIsNewItem(boolean isNew) { mIsNewItem = isNew; }

    public PineTaskItemWithNewStatus(String key, String itemDescription, boolean isNew)
    {
        super(key, itemDescription);
        mIsNewItem = isNew;
    }

}
