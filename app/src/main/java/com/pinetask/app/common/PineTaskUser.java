package com.pinetask.app.common;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.pinetask.app.BR;

import java.io.Serializable;

public class PineTaskUser extends BaseObservable implements Serializable
{
    private String mUserId;
    private String mUserName;

    @Bindable
    public String getUserId() { return mUserId; }

    public void setUserId(String userId)
    {
        mUserId = userId;
        notifyPropertyChanged(BR.userId);
    }

    @Bindable
    public String getUserName() { return mUserName; }

    public void setUserName(String userName)
    {
        mUserName = userName;
        notifyPropertyChanged(BR.userName);
    }

    public PineTaskUser(String userId, String userName)
    {
        mUserId = userId;
        mUserName = userName;
    }
}
