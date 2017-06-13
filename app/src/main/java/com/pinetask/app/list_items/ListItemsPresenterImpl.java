package com.pinetask.app.list_items;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.active_list_manager.ListLoadedEvent;
import com.pinetask.app.active_list_manager.NoListsAvailableEvent;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.UpdatedEvent;
import com.pinetask.app.db.DbHelper;

import java.util.List;

import io.reactivex.disposables.Disposable;

public class ListItemsPresenterImpl extends BasePresenter implements ListItemsPresenter
{
    DbHelper mDbHelper;
    ActiveListManager mActiveListManager;
    String mUserId;
    ListItemsView mView;
    Disposable mActiveListSubscription;
    ListItemsRepository mListItemsRepository;
    PineTaskApplication mApplication;

    public ListItemsPresenterImpl(PineTaskApplication application, DbHelper dbHelper, ActiveListManager activeListManager, String userId)
    {
        mApplication = application;
        mDbHelper = dbHelper;
        mActiveListManager = activeListManager;
        mUserId = userId;
        mActiveListSubscription = activeListManager.subscribe(this::onActiveListEvent, ex -> logError("Error getting event from ActiveListManager"));
    }

    private void onActiveListEvent(ActiveListEvent event)
    {
        if (event instanceof ListLoadedEvent)
        {
            ListLoadedEvent listLoadedEvent = (ListLoadedEvent) event;
            loadItemsForList(listLoadedEvent.ActiveList);
        }
        else if (event instanceof NoListsAvailableEvent)
        {
            if (mView != null)
            {
                mView.hideListItemsLayouts();
            }
        }
    }

    @Override
    public void attachView(ListItemsView view)
    {
        mView = view;
        if (mListItemsRepository != null) mView.showListItems(mListItemsRepository.getItems());
    }

    @Override
    public void detachView()
    {
        mView = null;
    }

    @Override
    public void shutdown()
    {
        if (mActiveListSubscription != null) mActiveListSubscription.dispose();
        if (mListItemsRepository != null) mListItemsRepository.shutdown();
    }

    private void loadItemsForList(PineTaskList list)
    {
        logMsg("Loading items for list %s", list.getId());
        if (mListItemsRepository != null) mListItemsRepository.shutdown();
        if (mView != null) mView.hideListItemsLayouts();
        mListItemsRepository = new ListItemsRepository(mDbHelper, list, this::onListItemsLoaded, this::processChildEvent, this::onListItemsLoadError);
    }

    private void onListItemsLoaded(List<PineTaskItem> items)
    {
        if (mView != null)
        {
            mView.showListItemsLayouts();
            mView.showListItems(items);
        }
    }

    private void processChildEvent(ChildEventBase childEvent)
    {
        if (mView != null)
        {
            if (childEvent instanceof AddedEvent)
            {
                AddedEvent<PineTaskItem> addedEvent = (AddedEvent<PineTaskItem>) childEvent;
                mView.addItem(addedEvent.Item);
            }
            else if (childEvent instanceof DeletedEvent)
            {
                DeletedEvent<PineTaskItem> deletedEvent = (DeletedEvent<PineTaskItem>) childEvent;
                mView.removeItem(deletedEvent.Item.getKey());
            }
            else if (childEvent instanceof UpdatedEvent)
            {
                UpdatedEvent<PineTaskItem> updatedEvent = (UpdatedEvent<PineTaskItem>) childEvent;
                mView.updateItem(updatedEvent.Item);
            }
        }
    }

    private void onListItemsLoadError(Throwable ex)
    {
        logError("Error loading items for list");
        logException(ex);
    }

    /** Adds a new item to the database. **/
    public void addItem(String description)
    {
        PineTaskItem item = new PineTaskItem(null, description);
        mListItemsRef.push().setValue(item);
    }

    /** Make async request to perform database update, and then update the view immediately. **/
    public void updateItem(String listId, PineTaskItem item)
    {
        mDbHelper.updateItem(listId, item);
        if (mView != null) mView.updateItem(item);
    }

    /** Sets the "is completed" status for the item to the state specified, and then updates the item in the database. **/
    public void setCompletedStatus(PineTaskItem item, boolean isCompleted)
    {
        logMsg("Setting completion status for '%s' to %b", item.getItemDescription(), isCompleted);
        item.setClaimedBy(mUserId);
        item.setIsCompleted(isCompleted);
        updateItem(item);
    }

    /** Make async request to delete item from database, and immediately remove item from the view. **/
    public void deleteItem(String listId, PineTaskItem item)
    {
        logMsg("Deleting item: %s", item.getItemDescription());
        mDbHelper.deleteItem(listId, item).subscribe(() ->
        {
            logMsg("Item %s deleted", item.getKey());
        }, ex ->
        {
            logError("Error deleting item %s", item.getKey());
            logException(ex);
            showErrorMessage(mApplication.getString(R.string.error_deleting_item));
        });
        if (mView != null) mView.removeItem(item.getKey());
    }

    /** Sets the item to be claimed by the current user, and then updates the item in the database. **/
    public void claimItem(PineTaskItem item)
    {
        logMsg("Claiming item '%s'", item.getItemDescription());
        item.setClaimedBy(mUserId);
        updateItem(item);
    }

    /** Sets the item to be unclaimed (set claimed_by to null), and then updates the item in the database. **/
    public void unclaimItem(PineTaskItem item)
    {
        logMsg("Unclaiming item '%s'", item.getItemDescription());
        item.setClaimedBy(null);
        updateItem(item);
    }

    /** Sets the item to be uncompleted, and then updates the item in the database. **/
    public void uncompleteItem(PineTaskItem item)
    {
        logMsg("Uncompleting item '%s'", item.getItemDescription());
        item.setIsCompleted(false);
        updateItem(item);
    }

    @Override
    protected void showErrorMessage(String message, Object... args)
    {
        if (mView != null) mView.showError(message, args);
    }
}
