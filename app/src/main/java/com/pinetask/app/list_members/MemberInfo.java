package com.pinetask.app.list_members;

/** Container for information displayed in the members recyclerview. **/
public class MemberInfo
{
    public String Name;
    public String UserId;
    public boolean IsOwner;
    public boolean IsCurrentUser;

    public MemberInfo(String name, String userId, boolean isOwner, boolean isCurrentUser)
    {
        Name = name;
        UserId = userId;
        IsOwner = isOwner;
        IsCurrentUser = isCurrentUser;
    }
}
