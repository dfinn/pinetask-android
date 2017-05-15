package com.pinetask.app.common;

/** Posted to the EventBus when a list has been deleted. **/
public class ListDeletedEvent
{
    public String ListId;
    public ListDeletedEvent(String listId)
    {
        ListId = listId;
    }
}
