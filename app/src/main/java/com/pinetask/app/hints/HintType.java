package com.pinetask.app.hints;

/** Enumeration for different types of startup tips that will be shown to users the first time the app is run. **/
public enum HintType
{
    FIRST_LIST_ADDED("first_list_added"),
    FIRST_LIST_ITEM_ADDED("first_list_item_added");

    private String mKey;
    public String getKey() { return mKey; }

    HintType(String key)
    {
        mKey = key;
    }
}
