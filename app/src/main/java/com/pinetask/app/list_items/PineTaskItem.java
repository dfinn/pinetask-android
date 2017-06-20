package com.pinetask.app.list_items;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Map;

/** Represents one item in a list. **/
public class PineTaskItem implements Serializable
{
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
    long mCreatedAt;
    public Map<String, String> getCreatedAt() { return ServerValue.TIMESTAMP; }
    @Exclude
    public long getCreatedAtMs() { return mCreatedAt; }
    public void setCreatedAt(long createdAt) { mCreatedAt = createdAt; }

    public PineTaskItem()
    {
    }

    public PineTaskItem(String itemDescription)
    {
        mItemDescription = itemDescription;
        mClaimedBy = null;
        mIsCompleted = false;
    }

    public void updateFrom(PineTaskItem updatedItem)
    {
        mItemDescription = updatedItem.getItemDescription();
        mClaimedBy = updatedItem.getClaimedBy();
        mIsCompleted = updatedItem.getIsCompleted();
        mCreatedAt = updatedItem.getCreatedAtMs();
    }

    @Override
    public String toString()
    {
        return String.format("[%s] %s", mIsCompleted ? "X" : " ", mItemDescription);
    }
}
