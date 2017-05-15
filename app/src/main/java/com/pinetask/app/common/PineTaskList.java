package com.pinetask.app.common;

import android.support.annotation.NonNull;

import com.google.firebase.database.Exclude;
import com.pinetask.app.db.UsesKeyIdentifier;

import java.io.Serializable;
import java.util.Comparator;

/** Represents a list that can contain any number of list items. **/
public class PineTaskList implements Serializable, UsesKeyIdentifier, Comparable<PineTaskList>
{
    /** Comparator for sorting objects by name. **/
    public static Comparator<PineTaskList> NAME_COMPARATOR = (PineTaskList list1, PineTaskList list2) -> list1.getName().toLowerCase().compareTo(list2.getName().toLowerCase());

    /** Key identifying the list **/
    protected String mKey;
    @Exclude    // No need to include this when writing the object to Firebase.
    public String getKey() { return mKey; }
    public void setKey(String key) { mKey = key; }

    /** Name of the list (ex: "Groceries") **/
    protected String mName;
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    /** User ID of the list owner. **/
    protected String mOwnerId;
    public String getOwnerId() { return mOwnerId; }
    public void setOwnerId(String ownerId) { mOwnerId = ownerId; }

    public PineTaskList()
    {
    }

    public PineTaskList(String key, String name, String ownerId)
    {
        mKey = key;
        mName = name;
        mOwnerId = ownerId;
    }

    @Override
    public String toString()
    {
        return mName;
    }

    /** Lists are considered equal if they have the same ID (firebase key) **/
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PineTaskList ==false) return false;
        PineTaskList otherList = (PineTaskList)obj;
        return mKey.equals(otherList.getKey());
    }

    @Override
    public String getId()
    {
        return mKey;
    }

    @Override
    public void setId(String id)
    {
        setKey(id);
    }

    @Override
    public int compareTo(@NonNull PineTaskList pineTaskList)
    {
        String thisName = mName==null ? "" : mName;
        String otherName = pineTaskList.getName()==null ? "" : pineTaskList.getName();
        return thisName.compareToIgnoreCase(otherName);
    }
}
