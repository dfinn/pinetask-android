package com.pinetask.app.list_items;

import java.util.List;

public interface ListItemsView
{
    /** Shows all items in the list, replacing any current contents. **/
    void showListItems(List<PineTaskItem> items);

    /** Adds the specified item to the bottom of the list. **/
    void addItem(PineTaskItem item);

    /** Removes the item with the specified ID from the list. **/
    void removeItem(String itemId);

    /** Updates the specified item **/
    void updateItem(PineTaskItem item);

    /** Show the items list and "add" button. **/
    void showListItemsLayouts();

    /** Hide items list and "add" button. **/
    void hideListItemsLayouts();

    /** Show error message to the user **/
    void showError(String message, Object... args);
}
