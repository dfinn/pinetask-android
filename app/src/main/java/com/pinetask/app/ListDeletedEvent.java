package com.pinetask.app;

/** Posted to the EventBus when a list has been deleted. **/
public class ListDeletedEvent
{
    String ListId;
    public ListDeletedEvent(String listId)
    {
        ListId = listId;
    }
}
