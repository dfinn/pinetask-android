package com.pinetask.app.list_items;

import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.pinetask.app.common.PineTaskUtil;
import com.pinetask.app.db.UsesKeyIdentifier;

import java.io.Serializable;
import java.util.Objects;

/** Extension for PineTaskItem to hold additional properties used at runtime but not stored in the database. **/
public class PineTaskItemExt extends PineTaskItem implements Serializable, UsesKeyIdentifier
{
    protected String mKey;
    @Exclude    // No need to include this when writing the object to Firebase.
    @Override
    public String getId() { return mKey; }
    @Override
    public void setId(String key) { mKey = key; }

    /** Indicates if the item is new (ie, has not yet been displayed to the user) **/
    private boolean mIsNewItem;
    @Exclude
    public boolean getIsNewItem() { return mIsNewItem; }
    public PineTaskItemExt setIsNewItem(boolean isNew) { mIsNewItem = isNew; return this; }

    /** ID of the list that this item belongs to **/
    private String mListId;
    @Exclude
    public String getListId() { return mListId; }
    public PineTaskItemExt setListId(String listId) { mListId = listId; return this; }

    /** Cost of the item (used in shopping trip mode) **/
    private Float mCost;
    public Float getCost() { return mCost; }
    public void setCost(Float cost) { mCost = cost; }

    public PineTaskItemExt(String key, String itemDescription, boolean isNew, String listId)
    {
        super(itemDescription);
        mKey = key;
        mIsNewItem = isNew;
        mListId = listId;
    }

    public PineTaskItemExt()
    {
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PineTaskItemExt)
        {
            PineTaskItemExt other = (PineTaskItemExt) obj;
            return other.getId().equals(getId());
        }
        else
        {
            return false;
        }
    }

    public boolean exactlyEqual(PineTaskItemExt other)
    {
        return TextUtils.equals(getId(), other.getId()) && TextUtils.equals(getListId(), other.getListId()) && (getIsNewItem() == other.getIsNewItem()) && super.exactlyEqual(other)
                && PineTaskUtil.equalsOrNull(getCost(), other.getCost());
    }

    public void updateFrom(PineTaskItemExt updatedItem)
    {
        super.updateFrom(updatedItem);
        mKey = updatedItem.getId();
        mIsNewItem = updatedItem.getIsNewItem();
        mListId = updatedItem.getListId();
        mCost = updatedItem.getCost();
    }

    @Exclude
    public String getDebugInfo()
    {
        return String.format("key=%s, cost=%.2f, completed=%b", mKey, mCost, mIsCompleted);
    }
}
