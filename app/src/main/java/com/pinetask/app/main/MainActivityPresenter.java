package com.pinetask.app.main;

import com.pinetask.app.R;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;

import io.reactivex.Maybe;

public class MainActivityPresenter extends BasePresenter implements MainActivityContract.IMainActivityPresenter
{
    MainActivityContract.IMainActivityView mView;
    DbHelper mDbHelper;
    String mUserId;
    String mUserName;
    PrefsManager mPrefsManager;
    PineTaskList mCurrentList;
    PineTaskApplication mApplication;

    public MainActivityPresenter(DbHelper dbHelper, String userId, PrefsManager prefsManager, PineTaskApplication application)
    {
        logMsg("Creating MainActivityPresenter");
        mDbHelper = dbHelper;
        mUserId = userId;
        mPrefsManager = prefsManager;
        mApplication = application;

        // Subscribe to user name, so it can be displayed in the side navigation drawer.
        observe(mDbHelper.getUserNameObservable(mUserId), this::showUserName);

        // Determine the user's current list: previously used list if still available, otherwise their first list, otherwise none. If first launch, show "Add List" dialog.
        // Then notify the view so it can display the list name on the list selector button.
        getListToUse().subscribe(this::onListSelected, ex -> logAndShowError(ex, "Error getting list to use"), this::onNoListsAvailable);
    }

    @Override
    public void attach(MainActivityContract.IMainActivityView view)
    {
        mView = view;

        if (mUserName != null) mView.showUserName(mUserName);
        if (mCurrentList != null) mView.showCurrentListName(mCurrentList.getName());

        // Create presenters for ListItemsFragment, ChatFragment, and MembersFragment. Then, tell the view to initialize the ViewPager.
        // TODO

        // Subscribe to list deleted events: if the current list gets deleted, try switching to the first available list if there is one.
        // TODO

        // Check for new startup message the user hasn't seen yet, and display them to the user.
        // TODO



    }

    @Override
    public void detach(boolean isFinishing)
    {
        mView = null;
        if (isFinishing) shutdown();
    }

    /** Load info for all lists that the user has access to, and display them in selector dialog for use to switch lists. **/
    @Override
    public void onListSelectorClicked()
    {
        mDbHelper.getListIdsForUser(mUserId).flatMapSingle(mDbHelper::getPineTaskList).toList().subscribe(lists ->
        {
            logMsg("onListSelectorClicked: loaded %s lists", lists.size());
            if (mView != null) mView.showListChooser(lists);
        }, ex ->
        {
            logAndShowError(ex, "Error loading lists");
        });
    }

    /** Set current list ID in shared prefs.   Look up the name of the list with the ID specified, and display it on the list selector button.  **/
    @Override
    public void onListSelected(PineTaskList list)
    {
        logMsg("onListSelected: setting current list to %s (%s)", list.getKey(), list.getName());
        mCurrentList = list;
        mPrefsManager.setCurrentListId(list.getKey());
        if (mView != null) mView.showCurrentListName(list.getName());
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

    /** Look up the ID of the user's previously selected list. If it still exists, emit it.
     *  Otherwise, if the user has at least one list, emit the first list ID.
     *  Otherwise, emit empty.
     **/
    Maybe<PineTaskList> getListToUse()
    {
        return getPreviousListIdIfExists().switchIfEmpty(getFirstListIdIfExists()).flatMap(listId -> mDbHelper.getPineTaskList(listId).toMaybe());
    }

    /** Emits the previously used list ID if non-null and it still exists, otherwise empty. **/
    Maybe<String> getPreviousListIdIfExists()
    {
        String previousListId = mPrefsManager.getCurrentListId();
        if (previousListId == null) return Maybe.empty();
        else return mDbHelper.canAccessList(mUserId, previousListId).flatMapMaybe(canAccess -> canAccess ? Maybe.just(previousListId) : Maybe.empty());
    }

    /** If the user has any lists, emit their first list ID. Otherwise, emit empty result. **/
    Maybe<String> getFirstListIdIfExists()
    {
        return mDbHelper.getListIdsForUser(mUserId).toList().flatMapMaybe(listIds ->
        {
            if (listIds != null && listIds.size() > 0) return Maybe.just(listIds.get(0));
            return Maybe.empty();
        });
    }

    private void logAndShowError(Throwable ex, String message, Object... args)
    {
        logError(message);
        logException(ex);
        if (mView != null) mView.showError(message, args);
    }

    /** Set current list to null in shared prefs. Update UI state to indicate there is no current list. **/
    private void onNoListsAvailable()
    {
        logMsg("onNoListsAvailable: setting current list to null");
        mPrefsManager.setCurrentListId(null);
        if (mView != null)
        {
            mView.showCurrentListName("");
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
        PineTaskList currentList = mCurrentList;
        if (currentList != null)
        {
            logMsg("onPurgeCompletedItemsSelected: starting purge for list %s", currentList.getId());
            mDbHelper.getListName(currentList.getId()).subscribe(listName ->
            {
                if (mView != null) mView.showPurgeCompletedItemsDialog(currentList.getId(), currentList.getName());
            }, ex ->
            {
                logAndShowError(ex, mApplication.getString(R.string.error_getting_list_name));
            });
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }

    }


}
