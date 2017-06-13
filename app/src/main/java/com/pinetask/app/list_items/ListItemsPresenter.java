package com.pinetask.app.list_items;

import com.pinetask.app.chat.ChatView;

public interface ListItemsPresenter
{
    void attachView(ListItemsView view);
    void detachView();
    void shutdown();
    void setCompletedStatus(PineTaskItem item, boolean isCompleted);
    void deleteItem(final PineTaskItem item);
    void claimItem(PineTaskItem item);
    void unclaimItem(PineTaskItem item);
    void uncompleteItem(PineTaskItem item);
    void addItem(String description);
}

