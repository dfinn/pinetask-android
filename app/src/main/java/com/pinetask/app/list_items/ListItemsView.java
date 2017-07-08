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

    /** Show dialog prompting the user to enter cost of the item. **/
    void showCostInputDialog(PineTaskItemExt item);

    /** Show cost fields on each list item, and the total cost field at the bottom of the view. **/
    void showCostFields();

    /** Hide per-item cost fields and total cost field **/
    void hideCostFields();

    /** Show total cost of all displayed items **/
    void showTotalCost(float total);
}
