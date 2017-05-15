package com.pinetask.app.common;

/** Posted to EventBus when the user has selected a list to switch to. **/
public class ListSelectedEvent
{
    public String ListId;
    public ListSelectedEvent(String listId)
    {
        ListId = listId;
    }
}
