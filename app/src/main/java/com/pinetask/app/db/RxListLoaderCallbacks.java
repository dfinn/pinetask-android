package com.pinetask.app.db;

import com.pinetask.app.common.PineTaskListWithCollaborators;

import java.util.List;

public interface RxListLoaderCallbacks
{
    void onListsLoaded(List<PineTaskListWithCollaborators> lists);
    void onListAdded(PineTaskListWithCollaborators list);
    void onListDeleted(String listId);
    void onListUpdated(PineTaskListWithCollaborators list);
    void onError(Throwable error);
}
