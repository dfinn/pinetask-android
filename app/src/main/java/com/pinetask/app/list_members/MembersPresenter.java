package com.pinetask.app.list_members;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.AddedOrDeletedEvent;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.ListDeletedEvent;
import com.pinetask.app.common.ListSelectedEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import static com.pinetask.app.db.DbHelper.getListOwner;
import static com.pinetask.app.db.DbHelper.getUserNameSingle;
import static com.pinetask.app.db.DbHelper.singleObserver;

/** Presenter for the MembersFragment. **/
public class MembersPresenter extends LoggingBase implements MembersContract.IMembersPresenter
{
    private MembersContract.IMembersView mView;
    private Bus mEventBus;
    private PrefsManager mPrefsManager;
    private String mCurrentDisplayedListId;
    private String mCurrentUserId;
    private Disposable mSubscription;

    @Override
    public void attachView(MembersContract.IMembersView view, String currentUserId)
    {
        mCurrentUserId = currentUserId;
        mView = view;
        mPrefsManager = PrefsManager.getInstance(PineTaskApplication.getInstance());
        mEventBus = PineTaskApplication.getEventBus();
        mEventBus.register(this);
        requestLoadListMembers(mPrefsManager.getCurrentListId());
    }

    @Override
    public void detachView()
    {
        mView = null;
        if (mSubscription != null)
        {
            mSubscription.dispose();
            mSubscription=null;
        }
        mEventBus.unregister(this);
    }

    /** Initiate async load of list members.   As each one is loaded, it will be added to the view. **/
    private void requestLoadListMembers(final String listId)
    {
        logMsg("requestLoadListMembers: listId=%s (mCurrentListId=%s)", listId, mCurrentDisplayedListId);

        if ((listId==null && mCurrentDisplayedListId==null) || (listId!=null && listId.equals(mCurrentDisplayedListId)))
        {
            // If request if for the list already loaded (or being loaded), just ignore it.
            logMsg("requestLoadListMembers: ignoring request, already displaying list %s", mCurrentDisplayedListId);
        }
        else
        {
            // Dispose of old subscription if there is one
            if (mSubscription != null)
            {
                logMsg("Existing subscription - disposing");
                mSubscription.dispose();
                mSubscription = null;
            }

            // Set the new list ID to display (could be null if no list), clear list display, hide "Add Member" button.
            mCurrentDisplayedListId = listId;
            mView.clearListDisplay();
            mView.setAddButtonVisible(false);

            // If list ID is null, clear list display but don't load a new one (no currently selected list - ie, user just deleted their last one)
            if (listId == null)
            {
                logMsg("requestLoadListMembers: list ID is null, clearing list display");
                mView.setListVisible(false);
            }
            else
            {
                // Loading a new list: show the members list, then make async request to get list owner.  If current user is owner, "Add Member" button will show.
                // Then, get events for members added/deleted to display in the view.
                logMsg("requestLoadListMembers: starting load of members in list %s", listId);
                mView.setListVisible(true);
                getListOwnerAndSubscribeToMemberAddedOrDeletedEvents(listId);
            }
        }
    }

    /** Make async request to look up owner of specified list ID.  Then, set up an Observable to emit added/deleted events for members of the specified list.
     *  As MemberInfo objects are emitted, it will call mView.addListMember() or mView.removeListMember() if the view is still attached.
     **/
    private void getListOwnerAndSubscribeToMemberAddedOrDeletedEvents(String listId)
    {
        // Get list owner, and show the "Add" button if it's the current user.
        getListOwner(listId).subscribe(singleObserver(ownerId ->
        {
            // If list has changed, abort loading list members.
            if (! listId.equalsIgnoreCase(mCurrentDisplayedListId))
            {
                logMsg("getListOwnerAndSubscribeToMemberAddedOrDeletedEvents: list has changed, aborting");
                return;
            }

            logMsg("requestLoadListMembers: List owner=%s, currentUser=%s", ownerId, mCurrentUserId);
            if (mView != null)
            {
                mView.setAddButtonVisible(ownerId.equalsIgnoreCase(mCurrentUserId));
            }

            // Attach observer for add/remove events for current list members. As observer emits items, convert them to add/remove events for MemberInfo objects and
            // then pass them to mView.add() and mView.remove().
            mSubscription = subscribeToMemberAddedDeletedEvents(listId, ownerId);
        }));
    }

    /** Attach listener to get user IDs for collaborators of the specified list, emitting added/deleted events for MemberInfo objects which are then passed to
     *  the view (if still attached) to add or remove the member from the displayed list. **/
    private Disposable subscribeToMemberAddedDeletedEvents(String listId, String ownerId)
    {
        return DbHelper.getMembersAddedOrDeletedEvents(listId)
                .flatMapSingle(addedOrDeletedEvent -> getMemberInfoForUserId(addedOrDeletedEvent, mCurrentUserId, ownerId))
                .subscribe(memberAddedOrDeletedevent ->
                {
                    // If list has changed, abort loading list members.
                    if (! listId.equalsIgnoreCase(mCurrentDisplayedListId))
                    {
                        logMsg("getListOwnerAndSubscribeToMemberAddedOrDeletedEvents: list has changed, aborting");
                        return;
                    }

                    if (mView != null)
                    {
                        if (memberAddedOrDeletedevent instanceof AddedEvent) mView.addListMember(memberAddedOrDeletedevent.Item);
                        else mView.removeListMember(memberAddedOrDeletedevent.Item.UserId);
                    }
                }, ex ->
                {
                    logError("getListOwnerAndSubscribeToMemberAddedOrDeletedEvents: error getting member added/deleted events");
                    logException(ex);
                });
    }

    /** Look up username for the specified userId, and convert the "user ID added or deleted" event into a "MemberInfo added or deleted" event. **/
    private Single<AddedOrDeletedEvent<MemberInfo>> getMemberInfoForUserId(AddedOrDeletedEvent<String> userAddedOrDeletedEvent, String currentUserId, String currentListOwnerId)
    {
        String userId = userAddedOrDeletedEvent.Item;
        return getUserNameSingle(userId).map(userName ->
        {
            // The member can only be deleted if the current user is the list owner.  Owner can never be deleted.
            boolean canBeDeleted = (currentListOwnerId.equals(currentUserId)) && (!userId.equals(currentListOwnerId));
            MemberInfo memberInfo = new MemberInfo(userName, userId, (userId.equals(currentListOwnerId)), canBeDeleted);
            if (userAddedOrDeletedEvent instanceof AddedEvent) return new AddedEvent<>(memberInfo);
            else return new DeletedEvent<>(memberInfo);
        });
    }

    @Override
    public void onAddMemberButtonClicked()
    {
        String currentListId = mPrefsManager.getCurrentListId();
        mView.launchInviteProcess(currentListId);
    }

    /** Called by the event bus when the user has selected a new list. **/
    @Subscribe
    public void onListSelected(ListSelectedEvent event)
    {
        logMsg("onListSelected: listId = %s", event.ListId);
        requestLoadListMembers(event.ListId);
    }

    /** Called by the event bus when a list has been deleted. **/
    @Subscribe
    public void onListDeleted(ListDeletedEvent event)
    {
        logMsg("ListDeletedEvent for list %s", event.ListId);
        if (event.ListId.equals(mCurrentDisplayedListId))
        {
            logMsg("onListDeleted: current list %s has been deleted, clearing list display", mCurrentDisplayedListId);
            requestLoadListMembers(null);
        }
    }
}
