package com.pinetask.app.main;

public class InviteInfo
{
    public String InviteId;
    public String ListId;
    public InviteInfo(String inviteId, String listId)
    {
        InviteId = inviteId;
        ListId = listId;
    }

    @Override
    public String toString()
    {
        return String.format("InviteId=%s, ListId=%s", InviteId, ListId);
    }
}
