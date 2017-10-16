package com.pinetask.app.active_list_manager;

import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChatMessageEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.common.LoggingBase;
import com.pinetask.app.hints.HintManager;
import com.pinetask.app.list_items.PineTaskItemExt;

import javax.inject.Named;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

/** Keeps track of the currently active list, notifying subscribers of events for when the list was changed, or if no list is currently available. **/
public class ActiveListManager extends LoggingBase
{
    PineTaskList mCurrentList;
    boolean mListLoadInProgress;
    PrefsManager mPrefsManager;
    DbHelper mDbHelper;
    String mUserId;
    Disposable mListsAddedOrDeletedSubscription;
    private HintManager mHintManager;
    private Disposable mShoppingTripActiveSubscription;
    private boolean mShoppingTripActive;

    public PineTaskList getActiveList()
    {
        return mCurrentList;
    }
    public boolean isShoppingTripActive() { return mShoppingTripActive; }

    /** Use a BehaviorSubject so that subscribers will get the most recent event, plus all subsequent events. **/
    BehaviorSubject<ActiveListEvent> mSubject;

    public ActiveListManager(PrefsManager prefsManager, DbHelper dbHelper, @Named("user_id") String userId, HintManager hintManager)
    {
        mPrefsManager = prefsManager;
        mDbHelper = dbHelper;
        mUserId = userId;
        mSubject = BehaviorSubject.create();
        mHintManager = hintManager;
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

    private void onListAddedOrDeleted(ChildEventBase<String> event)
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
            if (mCurrentList == null && !mListLoadInProgress)
            {
                logMsg("onListAddedOrDeleted: no current list, and a list was added - loading it");
                mListLoadInProgress = true;
                mDbHelper.getPineTaskList(listId).subscribe(this::setActiveList, this::onListLoadError);
            }
        }
    }

    public Disposable subscribe(Consumer<ActiveListEvent> onNext, Consumer<Throwable> onError)
    {
        return mSubject.subscribe(onNext, onError);
    }

    public void notifyChatMessageReceived(ChatMessage chatMessage)
    {
        mSubject.onNext(new ChatMessageEvent(chatMessage));
    }

    /** Set mCurrentList, set current list ID in shared prefs, and notify listeners. **/
    public void setActiveList(PineTaskList list)
    {
        logMsg("onListSelected: setting current list to %s (%s)", list.getKey(), list.getName());

        // If this is the first app launch, and the user has at least one existing list, assume it's a returning user, so they won't want to see any of the startup hints again.
        if (mPrefsManager.getIsFirstLaunch())
        {
            logMsg("onListSelected: first app launch, assuming existing user and setting all 'hint shown' flags");
            mHintManager.setAllHintsDisplayed();
            mPrefsManager.setIsFirstLaunch(false);
        }

        mListLoadInProgress = false;
        mCurrentList = list;
        mPrefsManager.setCurrentListId(list.getKey());
        mSubject.onNext(new ListLoadedEvent(list));
        if (mListsAddedOrDeletedSubscription != null) initListsAddedOrDeletedSubscription();
        initShoppingTripActiveSubscription(list);
    }

    private void initShoppingTripActiveSubscription(PineTaskList list)
    {
        if (mShoppingTripActiveSubscription != null) mShoppingTripActiveSubscription.dispose();
        mSubject.onNext(new ShoppingTripEndedEvent());
        mShoppingTripActiveSubscription = mDbHelper.subscribeToShoppingTripActiveEventsForList(list.getId()).subscribe(shoppingTripActive ->
        {
            mShoppingTripActive = shoppingTripActive;
            logMsg("subscribeToShoppingTripActiveEventsForList: shoppingTripActive=%b", shoppingTripActive);
            if (shoppingTripActive) mSubject.onNext(new ShoppingTripStartedEvent());
            else mSubject.onNext(new ShoppingTripEndedEvent());
        }, ex ->
        {
            logError("Error in subscription for shopping trip active events for list %s", list.getId());
            logException(ex);
        });
    }

    private void initListsAddedOrDeletedSubscription()
    {
        if (mListsAddedOrDeletedSubscription != null) mListsAddedOrDeletedSubscription.dispose();
        logMsg("initListsAddedOrDeletedSubscription is running");
        mListsAddedOrDeletedSubscription = mDbHelper.getListAddedOrDeletedEvents(mUserId)
                    .subscribe(this::onListAddedOrDeleted, ex -> logErrorAndException(ex, "Error getting added/deleted events"));
    }

    private void onListLoadError(Throwable ex)
    {
        logError("onListLoadError: %s", ex.getMessage());
        mListLoadInProgress = false;
        logException(ex);
        mSubject.onNext(new ListLoadErrorEvent(ex));
        initListsAddedOrDeletedSubscription();
    }

    /** Set current list to null, and notify listeners that no list is available (the user has deleted their last list) **/
    private void onNoListsAvailable()
    {
        logMsg("onNoListsAvailable: setting current list to null");
        mPrefsManager.setCurrentListId(null);
        mCurrentList = null;
        mSubject.onNext(new NoListsAvailableEvent());
        initListsAddedOrDeletedSubscription();
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
