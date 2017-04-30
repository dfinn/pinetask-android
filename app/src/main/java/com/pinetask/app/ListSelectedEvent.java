package com.pinetask.app;

/** Posted to EventBus when the user has selected a list to switch to. **/
public class ListSelectedEvent
{
    String ListId;
    public ListSelectedEvent(String listId)
    {
        ListId = listId;
    }
}
