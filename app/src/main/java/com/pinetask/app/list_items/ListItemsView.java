package com.pinetask.app.list_items;

import java.util.List;

public interface ListItemsView
{
    /** Shows all items in the list, replacing any current contents. **/
    void showListItems(List<PineTaskItemExt> items);

    /** Adds the specified item to the bottom of the list. **/
    void addItem(PineTaskItemExt item);

    /** Update the item in the list. **/
    void updateItem(PineTaskItemExt item);

    /** Removes the item with the specified ID from the list. **/
    void removeItem(String itemId);

    /** Show the items list and "add" button. **/
    void showListItemsLayouts();

    /** Hide items list and "add" button. **/
    void hideListItemsLayouts();

    /** Show error message to the user **/
    void showError(String message, Object... args);
}
