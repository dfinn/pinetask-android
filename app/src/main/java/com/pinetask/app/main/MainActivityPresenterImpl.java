package com.pinetask.app.main;

import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.active_list_manager.ListLoadErrorEvent;
import com.pinetask.app.active_list_manager.ListLoadedEvent;
import com.pinetask.app.active_list_manager.NoListsAvailableEvent;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.launch.StartupMessageDialogFragment;
import com.pinetask.app.manage_lists.StartupMessage;

import io.reactivex.disposables.Disposable;

public class MainActivityPresenterImpl extends BasePresenter implements MainActivityPresenter
{
    MainActivityView mView;
    DbHelper mDbHelper;
    String mUserId;
    String mUserName;
    PineTaskApplication mApplication;
    PrefsManager mPrefsManager;
    ActiveListManager mActiveListManager;
    Disposable mActiveListManagerSubscription;
    Disposable mUserNameSubscription;
    StartupMessage mStartupMessage;

    public MainActivityPresenterImpl(DbHelper dbHelper, String userId, PrefsManager prefsManager, PineTaskApplication application, ActiveListManager activeListManager)
    {
        logMsg("Creating MainActivityPresenter");
        mDbHelper = dbHelper;
        mUserId = userId;
        mPrefsManager = prefsManager;
        mApplication = application;
        mActiveListManager = activeListManager;

        // Subscribe to ActiveListManager to be notified of events when the active list is changed.
        mActiveListManagerSubscription = mActiveListManager.subscribe(this::handleActiveListEvent, ex -> logAndShowError(ex, mApplication.getString(R.string.error_processing_list_event)));

        // Subscribe to user name, so it can be displayed in the side navigation drawer.
        mUserNameSubscription = mDbHelper.getUserNameObservable(mUserId).subscribe(this::showUserName, ex -> logAndShowError(ex, mApplication.getString(R.string.error_getting_username)));

        // Check if there's a newer version of the startup message that the user hasn't seen yet. If so, display it.
        loadStartupMessage();
    }

    /** Process events that have been emitted from the ActiveListManager. **/
    private void handleActiveListEvent(ActiveListEvent activeListEvent)
    {
        logMsg("handleActiveListEvent: %s", activeListEvent.getClass().getSimpleName());

        if (activeListEvent instanceof ListLoadedEvent)
        {
            // A new list is active: refresh the name displayed on the list selector button.
            ListLoadedEvent listLoadedEvent = (ListLoadedEvent) activeListEvent;
            PineTaskList newList = listLoadedEvent.ActiveList;
            if (mView != null)
            {
                mView.showCurrentListName(newList.getName());
                mView.hideNoListsFoundMessage();
                mView.showBottomMenuBar();
            }
        }
        else if (activeListEvent instanceof ListLoadErrorEvent)
        {
            // Error occurred when loading list: log and show error.
            ListLoadErrorEvent listLoadErrorEvent = (ListLoadErrorEvent) activeListEvent;
            logAndShowError(listLoadErrorEvent.Error, mApplication.getString(R.string.error_loading_lists));
        }
        else if (activeListEvent instanceof NoListsAvailableEvent)
        {
            // The user's last list has been deleted - update list name selector button to indicate this.
            onNoListsAvailable();
        }
    }

    /** Unless this is the first app launch, query the server for the startup message and its version.  If the user hasn't seen it yet, display the message now. **/
    private void loadStartupMessage()
    {
        if (mPrefsManager.getIsFirstLaunch()) return;
        mDbHelper.getStartupMessageIfUnread(mUserId)
                .subscribe(startupMessage ->
                {
                    mStartupMessage = startupMessage;
                    showStartupMessage();
                }, ex ->
                {
                    logAndShowError(ex, mApplication.getString(R.string.error_loading_startup_message));
                });
    }

    @Override
    public void attach(MainActivityView view)
    {
        mView = view;
        PineTaskList activeList = mActiveListManager.getActiveList();
        if (mUserName != null) mView.showUserName(mUserName);
        if (activeList != null) mView.showCurrentListName(activeList.getName());
        if (mStartupMessage != null) showStartupMessage();
    }

    private void showStartupMessage()
    {
        if (mView != null)
        {
            logMsg("Showing startup message version %d", mStartupMessage.version);
            mView.showStartupMessage(mStartupMessage);
            mStartupMessage = null;
        }
    }

    @Override
    public void detach()
    {
        mView = null;
    }

    @Override
    public void shutdown()
    {
        logMsg("shutdown: shutting down subscriptions");
        if (mActiveListManagerSubscription != null) mActiveListManagerSubscription.dispose();
        if (mUserNameSubscription != null) mUserNameSubscription.dispose();
    }

    /** Load info for all lists that the user has access to, and display them in selector dialog for user to switch lists. **/
    @Override
    public void onListSelectorClicked()
    {
        mDbHelper.getListIdsForUser(mUserId).flatMapSingle(mDbHelper::getPineTaskList).toList().subscribe(lists ->
        {
            logMsg("onListSelectorClicked: loaded %s lists", lists.size());
            if ((lists.size() > 0) && (mView != null)) mView.showListChooser(lists);
        }, ex ->
        {
            logAndShowError(ex, "Error loading lists");
        });
    }

    @Override
    protected void showErrorMessage(String message, Object... args)
    {
        if (mView != null) mView.showError(message, args);
    }

    private void showUserName(String userName)
    {
        mUserName = userName;
        if (mView != null) mView.showUserName(userName);
    }

    @Override
    public void onListSelected(PineTaskList list)
    {
        mActiveListManager.setActiveList(list);
    }

    /** Set current list to null in shared prefs. Update UI state to indicate there is no current list. **/
    private void onNoListsAvailable()
    {
        if (mView != null)
        {
            mView.showCurrentListName(mApplication.getString(R.string.no_lists_found));
            mView.hideBottomMenuBar();
            mView.showNoListsFoundMessage();
            if (mPrefsManager.getIsFirstLaunch())
            {
                // If this is the first app launch, show "Add List" dialog and pass the flag so that it prompts the user to create their first list.
                logMsg("First launch: showing Add List dialog");
                mView.showAddListDialog();
                mPrefsManager.setIsFirstLaunch(false);
            }
        }
    }

    @Override
    public void purgeCompletedItems(String listId)
    {
        mDbHelper.purgeCompletedItems(listId).subscribe(() ->
        {
            logMsg("Purging completed items completed");
        }, ex ->
        {
            logAndShowError(ex, mApplication.getString(R.string.error_purging_completed_items));
        });
    }

    @Override
    public void onPurgeCompletedItemsSelected()
    {
        PineTaskList currentList = mActiveListManager.getActiveList();
        if (currentList != null)
        {
            logMsg("onPurgeCompletedItemsSelected: starting purge for list %s", currentList.getId());
            if (mView != null) mView.showPurgeCompletedItemsDialog(currentList.getId(), currentList.getName());
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }
    }
}
