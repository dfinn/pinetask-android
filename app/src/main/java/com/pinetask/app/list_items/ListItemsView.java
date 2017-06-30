package com.pinetask.app.list_items;

import java.util.List;

public interface ListItemsView
{
    /** Adds the specified item to the bottom of the list. **/
    void addItem(PineTaskItemExt item);

    /** Show pop-up message to notify a new item was added. **/
    void notifyNewItemAdded(PineTaskItemExt item);

    /** Update the item in the list. **/
    void updateItem(PineTaskItemExt item);

    /** Removes the item with the specified ID from the list. **/
    void removeItem(String itemId);

    /** Show the items list and "add" button. **/
    void showListItemsLayouts();

    void clearListItems();

    /** Hide items list and "add" button. **/
    void hideListItemsLayouts();

    /** Show error message to the user **/
    void showError(String message, Object... args);
}
