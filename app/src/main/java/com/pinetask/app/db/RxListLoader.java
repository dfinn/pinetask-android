package com.pinetask.app.db;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PineTaskListWithCollaborators;
import com.pinetask.app.common.LoggingBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

/** Provides callbacks for events related to the PineTaskLists owned by a specified user:
 *   - All accessible lists (ie, owned by the user or shared with them)
 *   - List added events
 *   - List deleted events
 *   - List updated (ie, renamed) events
 * Caller must invoke shutdown() in order to dispose of subscriptions.
 */
public class RxListLoader extends LoggingBase
{
    String mUserId;
    RxListLoaderCallbacks mCallback;
    Disposable mAddedOrDeletedSubscription;
    List<Disposable> mListInfoSubscriptions = new ArrayList<>();
    List<PineTaskListWithCollaborators> mLists;
    @Inject DbHelper mDbHelper;

    /** Load all PineTaskLists that the user has access to, and then invoke callback::onListsLoaded().
     * Then, subscribe to rename/add/delete events - caller must then invoke shutdown() when no longer needed. **/
    public RxListLoader(String userId, RxListLoaderCallbacks callback)
    {
        mUserId = userId;
        PineTaskApplication.getInstance().getAppComponent().inject(this);
        mCallback = callback;

        mDbHelper.getListIdsForUser(userId)
                .flatMap(mDbHelper::tryGetPineTaskList)
                .flatMapSingle(mDbHelper::getPineTaskListWithCollaborators)
                .toSortedList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lists ->
                {
                    logMsg("Finished load of %d lists", lists.size());
                    mLists = lists;
                    mCallback.onListsLoaded(lists);
                    subscribeToChanges(lists);
                }, ex ->
                {
                    logError("Error loading lists");
                    logException(ex);
                    callback.onError(ex);
                } );
    }

    /** Return list of all PineTaskLists that have been loaded.  May be null if initial load not done yet.  List will be updated in response to add/remove/update events. **/
    public List<PineTaskListWithCollaborators> getLists()
    {
        return mLists;
    }

    /** Set up subscription to list renamed/added/deleted events.  Caller is responsible to call shutdown() when subscription no longer needed. **/
    private void subscribeToChanges(List<PineTaskListWithCollaborators> lists)
    {
        /** Subscribe to changes when a list is renamed. **/
        for (PineTaskListWithCollaborators list : lists)
        {
            subscribeToListInfoChanges(list.getKey());
        }

        // Build stream that will emit both list added / list deleted events.
        ConnectableObservable<ChildEventBase<String>> connection = mDbHelper.getListAddedOrDeletedEvents(mUserId).observeOn(AndroidSchedulers.mainThread()).publish();

        // Subscribe to stream filtered to only AddedEvents.
        connection.filter(event -> event instanceof AddedEvent)
                .map(event -> event.Item)
                .flatMap(mDbHelper::tryGetPineTaskList)
                .flatMapSingle(mDbHelper::getPineTaskListWithCollaborators)
                .subscribe(this::onListAdded, mCallback::onError);

        // Subscribe to stream filtered to only DeletedEvents.
        connection.filter(event -> event instanceof DeletedEvent)
                .map(event -> event.Item)
                .subscribe(this::onListDeleted, mCallback::onError);

        // Connect subscription and save reference so we can dispose later.
        mAddedOrDeletedSubscription = connection.connect();
    }

    public void onListAdded(PineTaskListWithCollaborators list)
    {
        // If mLists already has list with the same ID, then ignore.
        for (PineTaskListWithCollaborators l : mLists)
        {
            if (l.getKey().equals(list.getKey()))
            {
                logMsg("onListAdded: duplicate list %s, ignoring", list.getKey());
                return;
            }
        }

        logMsg("onListAdded: adding list %s", list.getKey());
        mLists.add(list);
        Collections.sort(mLists);
        subscribeToListInfoChanges(list.getId());
        mCallback.onListAdded(list);
    }

    public void onListDeleted(String listId)
    {
        logMsg("List deleted: %s", listId);
        Iterator<PineTaskListWithCollaborators> iter = mLists.iterator();
        while (iter.hasNext())
        {
            PineTaskList list = iter.next();
            if (list.getId().equals(listId))
            {
                iter.remove();
                break;
            }
        }
        mCallback.onListDeleted(listId);
    }

    private void onListUpdated(PineTaskListWithCollaborators updatedList)
    {
        logMsg("List updated: %s (name=%s)", updatedList.getKey(), updatedList.getName());
        for (int i=0;i<mLists.size();i++)
        {
            PineTaskListWithCollaborators list  = mLists.get(i);
            if (list.equals(updatedList))
            {
                // Event will always fire initially due to how Firebase's ValueEventListener works.  Only call onListUpdated callback if list info has actually changed.
                if (list.getName().equals(updatedList.getName()) && list.getOwnerId().equals(updatedList.getOwnerId()))
                {
                    logMsg("-- onListUpdated: list is unchanged, won't fire onListUpdated");
                }
                else
                {
                    mLists.set(i, updatedList);
                    mCallback.onListUpdated(updatedList);
                }
                break;
            }
        }
    }

    /** Subscribe to any changes to the PineTaskList with the ID provided, so we notify the callback of "list renamed" events. **/
    private void subscribeToListInfoChanges(String listId)
    {
        logMsg("subscribeToListInfoChanges for list %s", listId);
        Disposable disposable = mDbHelper.subscribeListInfo(listId)
                .flatMapSingle(mDbHelper::getPineTaskListWithCollaborators)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onListUpdated, mCallback::onError);
        mListInfoSubscriptions.add(disposable);
    }

    /** Must be called by the user when loader is no longer needed.
     * Dispose of subscription for list added / list deleted events, and dispose of subscription for "list info" for each of the user's lists. **/
    public void shutdown()
    {
        if (mAddedOrDeletedSubscription != null)
        {
            logMsg("shutdown: disposing mAddedOrDeletedSubscription");
            mAddedOrDeletedSubscription.dispose();
            mAddedOrDeletedSubscription=null;
        }
        logMsg("shutdown: disposing %d list info subscriptions", mListInfoSubscriptions.size());
        for (Disposable d : mListInfoSubscriptions) d.dispose();
        if (mLists!=null) mLists.clear();
    }
}
