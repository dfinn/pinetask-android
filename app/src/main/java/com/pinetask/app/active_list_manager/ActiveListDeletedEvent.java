package com.pinetask.app.active_list_manager;

public class ActiveListDeletedEvent extends ActiveListEvent
{
    public String DeletedListName;
    public ActiveListDeletedEvent(String deletedListName)
    {
        DeletedListName = deletedListName;
    }
}
