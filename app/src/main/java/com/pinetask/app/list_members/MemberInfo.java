package com.pinetask.app.list_members;

/** Container for information displayed in the members recyclerview. **/
public class MemberInfo
{
    public String Name;
    public String UserId;
    public boolean IsOwner;
    public boolean CanBeDeleted;

    public MemberInfo(String name, String userId, boolean isOwner, boolean canBeDeleted)
    {
        Name = name;
        UserId = userId;
        IsOwner = isOwner;
        CanBeDeleted = canBeDeleted;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof MemberInfo)) return false;
        MemberInfo memberInfo = (MemberInfo) obj;
        return UserId.equals(memberInfo.UserId);
    }
}
