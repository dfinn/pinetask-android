package com.pinetask.app.active_list_manager;

import com.pinetask.app.common.PineTaskList;

public class ListLoadedEvent extends ActiveListEvent
{
    public PineTaskList ActiveList;
    public ListLoadedEvent(PineTaskList activeList)
    {
        ActiveList = activeList;
    }
}
