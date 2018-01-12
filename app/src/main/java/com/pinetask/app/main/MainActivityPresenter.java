package com.pinetask.app.main;

import com.pinetask.app.common.PineTaskList;

public interface MainActivityPresenter
{
    void attach(MainActivityView view);
    void detach();
    void shutdown();
    void onListSelectorClicked();
    void onListSelected(PineTaskList list);
    void purgeCompletedItems(String listId);
    void uncompleteAllItems(String listId);
    void onPurgeCompletedItemsSelected();
    void onUncompleteAllSelected();
    void startShoppingTrip();
    void stopShoppingTrip();
    boolean isShoppingTripActive();
}