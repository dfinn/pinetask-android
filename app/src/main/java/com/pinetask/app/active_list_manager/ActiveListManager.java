package com.pinetask.app.active_list_manager;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.AddedOrDeletedEvent;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;

import javax.inject.Named;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

/** Keeps track of the currently active list, notifying subscribers of events for when the list was changed, or if no list is currently available. **/
public class ActiveListManager extends LoggingBase
{
    PineTaskList mCurrentList;
    PrefsManager mPrefsManager;
    DbHelper mDbHelper;
    String mUserId;
    Disposable mListsAddedOrDeletedSubscription;

    /** Use a BehaviorSubject so that subscribers will get the most recent event, plus all subsequent events. **/
    BehaviorSubject<ActiveListEvent> mSubject;

    public ActiveListManager(PrefsManager prefsManager, DbHelper dbHelper, @Named("user_id") String userId)
    {
        mPrefsManager = prefsManager;
        mDbHelper = dbHelper;
        mUserId = userId;
        mSubject = BehaviorSubject.create();
        mListsAddedOrDeletedSubscription = mDbHelper.getListAddedOrDeletedEvents(mUserId)
                                                    .subscribe(this::onListAddedOrDeleted, ex -> logErrorAndException(ex, "Error getting added/deleted events"));
        determineListToUse();
    }

    public void shutdown()
    {
        logMsg("ActiveListManager is shutting down");
        if (mListsAddedOrDeletedSubscription != null) mListsAddedOrDeletedSubscription.dispose();
    }

    /** Determine the user's current list: previously used list if still available, otherwise their first list, otherwise none. Then, store active list and notify listeners. **/
    private void determineListToUse()
    {
        logMsg("determineListToUse is starting");
        getInitialListToUse().subscribe(this::setActiveList, this::onListLoadError, this::onNoListsAvailable);
    }

    private void onListAddedOrDeleted(AddedOrDeletedEvent<String> event)
    {
        if (event instanceof DeletedEvent)
        {
            DeletedEvent<String> deletedEvent = (DeletedEvent<String>) event;
            String listId = deletedEvent.Item;
            if (mCurrentList != null && mCurrentList.getId().equals(listId))
            {
                logMsg("onListAddedOrDeleted: current list has been deleted");
                mPrefsManager.setCurrentListId(null);
                mSubject.onNext(new ActiveListDeletedEvent(mCurrentList.getName()));
                determineListToUse();
            }
        }
        else if (event instanceof AddedEvent)
        {
            AddedEvent<String> addedEvent = (AddedEvent<String>) event;
            String listId = addedEvent.Item;
            if (mCurrentList == null)
            {
                logMsg("onListAddedOrDeleted: no current list, and a list was added - loading it");
                mDbHelper.getPineTaskList(listId).subscribe(this::setActiveList, this::onListLoadError);
            }
        }
    }

    public Disposable subscribe(Consumer<ActiveListEvent> onNext, Consumer<Throwable> onError)
    {
        return mSubject.subscribe(onNext, onError);
    }

    public PineTaskList getActiveList()
    {
        return mCurrentList;
    }

    /** Set mCurrentList, set current list ID in shared prefs, and notify listeners. **/
    public void setActiveList(PineTaskList list)
    {
        logMsg("onListSelected: setting current list to %s (%s)", list.getKey(), list.getName());
        mCurrentList = list;
        mPrefsManager.setCurrentListId(list.getKey());
        mSubject.onNext(new ListLoadedEvent(list));
    }

    private void onListLoadError(Throwable ex)
    {
        logError("onListLoadError: %s", ex.getMessage());
        logException(ex);
        mSubject.onNext(new ListLoadErrorEvent(ex));
    }

    /** Set current list to null, and notify listeners that no list is available (the user has deleted their last list) **/
    private void onNoListsAvailable()
    {
        logMsg("onNoListsAvailable: setting current list to null");
        mPrefsManager.setCurrentListId(null);
        mCurrentList = null;
        mSubject.onNext(new NoListsAvailableEvent());
    }

    /** Look up the ID of the user's previously selected list. If it still exists, emit it.
     *  Otherwise, if the user has at least one list, emit the first list ID.
     *  Otherwise, emit empty.
     **/
    Maybe<PineTaskList> getInitialListToUse()
    {
        logMsg("getInitialListToUse is starting");
        return getPreviousListIdIfExists().switchIfEmpty(getFirstListIdIfExists()).flatMap(listId ->
        {
            logMsg("getInitialListToUse: calling getPineTaskList(%s)", listId);
            return mDbHelper.getPineTaskList(listId).toMaybe();
        });
    }

    /** Emits the previously used list ID if non-null and it still exists, otherwise empty. **/
    Maybe<String> getPreviousListIdIfExists()
    {
        logMsg("getPreviousListIdIfExists is starting");
        String previousListId = mPrefsManager.getCurrentListId();
        if (previousListId == null) return Maybe.empty();
        else return mDbHelper.canAccessList(mUserId, previousListId).flatMapMaybe(canAccess -> canAccess ? Maybe.just(previousListId) : Maybe.empty());
    }

    /** If the user has any lists, emit their first list ID. Otherwise, emit empty result. **/
    Maybe<String> getFirstListIdIfExists()
    {
        logMsg("getFirstListIdIfExists is starting");
        return mDbHelper.getListIdsForUser(mUserId).toList().flatMapMaybe(listIds ->
        {
            if (listIds != null && listIds.size() > 0) return Maybe.just(listIds.get(0));
            return Maybe.empty();
        });
    }



}
