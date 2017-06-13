package com.pinetask.app.list_items;

import com.google.firebase.database.Exclude;

import org.joda.time.DateTime;

import java.io.Serializable;

/** Represents one item in a list. **/
public class PineTaskItem implements Serializable
{
    /** Key identifying the item **/
    protected String mKey;
    @Exclude    // No need to include this when writing the object to Firebase.
    public String getKey() { return mKey; }
    public void setKey(String key) { mKey = key; }

    /** OperationDescription of the item (ex: "bananas") **/
    protected String mItemDescription;
    public String getItemDescription() { return mItemDescription; }
    public void setItemDescription(String description) { mItemDescription = description; }

    /** ID of the person who has claimed the item, or null if unclaimed. **/
    protected String mClaimedBy;
    public String getClaimedBy() { return mClaimedBy; }
    public void setClaimedBy(String claimedBy) { mClaimedBy = claimedBy; }

    /** Set to true after the item has been completed (ie, purchased) **/
    protected boolean mIsCompleted;
    public boolean getIsCompleted() { return mIsCompleted; }
    public void setIsCompleted(boolean isCompleted) { mIsCompleted = isCompleted; }

    /** Timestamp when the item was created. **/
    protected long mCreatedAt;
    public long getCreatedAt() { return mCreatedAt; }
    public void setCreatedAt(long createdAt) { mCreatedAt = createdAt; }

    public PineTaskItem()
    {
    }

    public PineTaskItem(String key, String itemDescription)
    {
        mKey = key;
        mItemDescription = itemDescription;
        mClaimedBy = null;
        mIsCompleted = false;
        mCreatedAt = DateTime.now().getMillis();
    }

    public void updateFrom(PineTaskItem updatedItem)
    {
        mItemDescription = updatedItem.getItemDescription();
        mClaimedBy = updatedItem.getClaimedBy();
        mIsCompleted = updatedItem.getIsCompleted();
        mCreatedAt = updatedItem.getCreatedAt();
    }

    @Override
    public String toString()
    {
        return String.format("[%s] %s", mIsCompleted ? "X" : " ", mItemDescription);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PineTaskItem)
        {
           PineTaskItem other = (PineTaskItem) obj;
            return other.getKey().equals(getKey());
        }
        else
            {
            return false;
        }
    }
}
