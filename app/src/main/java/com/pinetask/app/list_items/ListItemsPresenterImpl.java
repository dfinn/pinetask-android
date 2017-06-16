package com.pinetask.app.list_items;

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
    private DbHelper mDbHelper;
    private String mUserId;
    private ListItemsView mView;
    private Disposable mActiveListSubscription;
    private ListItemsRepository mListItemsRepository;
    private PineTaskApplication mApplication;
    private ActiveListManager mActiveListManager;

    public ListItemsPresenterImpl(PineTaskApplication application, DbHelper dbHelper, ActiveListManager activeListManager, String userId)
    {
        mApplication = application;
        mDbHelper = dbHelper;
        mUserId = userId;
        mActiveListManager = activeListManager;
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

    private void onListItemsLoaded(List<PineTaskItemExt> items)
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
                AddedEvent<PineTaskItemExt> addedEvent = (AddedEvent<PineTaskItemExt>) childEvent;
                mView.addItem(addedEvent.Item);
            }
            else if (childEvent instanceof DeletedEvent)
            {
                DeletedEvent<PineTaskItemExt> deletedEvent = (DeletedEvent<PineTaskItemExt>) childEvent;
                mView.removeItem(deletedEvent.Item.getId());
            }
            else if (childEvent instanceof UpdatedEvent)
            {
                UpdatedEvent<PineTaskItemExt> updatedEvent = (UpdatedEvent<PineTaskItemExt>) childEvent;
                mView.updateItem(updatedEvent.Item);
            }
        }
    }

    private void onListItemsLoadError(Throwable ex)
    {
        logError("Error loading items for list");
        logException(ex);
    }

    /** Start async request to add a new item to the database.  If error occurs, it will be logged and shown to the user. **/
    @Override
    public void addItem(String description)
    {
        PineTaskList activeList = mActiveListManager.getActiveList();
        if (activeList != null)
        {
            PineTaskItemExt item = mDbHelper.addPineTaskItem(activeList.getId(), description, this::showErrorMessage);
            if (mView != null) mView.addItem(item);
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }
    }

    /** Make async request to perform database update, and then update the view immediately. **/
    @Override
    public void updateItem(PineTaskItemExt item)
    {
        mDbHelper.updateItem(item, this::showErrorMessage);
        if (mView != null) mView.updateItem(item);
    }

    /** Sets the "is completed" status for the item to the state specified, and then updates the item in the database. **/
    @Override
    public void setCompletedStatus(PineTaskItemExt item, boolean isCompleted)
    {
        logMsg("Setting completion status for '%s' to %b", item.getItemDescription(), isCompleted);
        if (isCompleted) item.setClaimedBy(mUserId);
        item.setIsCompleted(isCompleted);
        updateItem(item);
    }

    /** Make async request to delete item from database, and immediately remove item from the view. **/
    @Override
    public void deleteItem(PineTaskItemExt item)
    {
        logMsg("Deleting item: %s", item.getItemDescription());
        mDbHelper.deleteItem(item, this::showErrorMessage);
        if (mView != null) mView.removeItem(item.getId());
    }

    /** Sets the item to be claimed by the current user, and then updates the item in the database. **/
    @Override
    public void claimItem(PineTaskItemExt item)
    {
        logMsg("Claiming item '%s'", item.getItemDescription());
        item.setClaimedBy(mUserId);
        updateItem(item);
    }

    /** Sets the item to be unclaimed (set claimed_by to null), and then updates the item in the database. **/
    @Override
    public void unclaimItem(PineTaskItemExt item)
    {
        logMsg("Unclaiming item '%s'", item.getItemDescription());
        item.setClaimedBy(null);
        updateItem(item);
    }

    @Override
    protected void showErrorMessage(String message, Object... args)
    {
        if (mView != null) mView.showError(message, args);
    }
}
