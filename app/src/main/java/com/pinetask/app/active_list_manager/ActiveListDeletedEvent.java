package com.pinetask.app.active_list_manager;

import com.pinetask.app.common.DeletedEvent;

public class ActiveListDeletedEvent extends ActiveListEvent
{
    public String DeletedListName;
    public ActiveListDeletedEvent(String deletedListName)
    {
        DeletedListName = deletedListName;
    }
}
