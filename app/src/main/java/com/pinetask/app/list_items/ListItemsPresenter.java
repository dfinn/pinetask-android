package com.pinetask.app.list_items;

public interface ListItemsPresenter
{
    void attachView(ListItemsView view);
    void detachView();
    void shutdown();
    void addItem(String description);
    void updateItem(PineTaskItemExt item);
    void deleteItem(PineTaskItemExt item);
    void claimItem(PineTaskItemExt item);
    void unclaimItem(PineTaskItemExt item);
    void setCompletedStatus(PineTaskItemExt item, boolean isCompleted);
}

