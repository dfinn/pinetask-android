package com.pinetask.app.list_members;

import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.active_list_manager.ListLoadedEvent;
import com.pinetask.app.active_list_manager.NoListsAvailableEvent;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

/** Presenter for the MembersFragment. **/
public class MembersPresenterImpl extends BasePresenter implements MembersPresenter
{
    private MembersView mView;
    private String mCurrentUserId;
    private Disposable mMembersAddedDeletedSubscription;
    private DbHelper mDbHelper;
    private PineTaskApplication mApplication;
    private Disposable mActiveListManagerSubscription;
    private List<MemberInfo> mCurrentListMembers;
    private ActiveListManager mActiveListManager;

    public MembersPresenterImpl(DbHelper dbHelper, PineTaskApplication pineTaskApplication, ActiveListManager activeListManager, @Named("user_id") String userId)
    {
        mDbHelper = dbHelper;
        mApplication = pineTaskApplication;
        mCurrentUserId = userId;
        mActiveListManager = activeListManager;
        mCurrentListMembers = new ArrayList<>();
        mActiveListManagerSubscription = activeListManager.subscribe(this::handleListLoadEvent, ex -> logError("ActiveListManager reported error event: %s", ex.getMessage()));
    }

    @Override
    public void attachView(MembersView view)
    {
        logMsg("Attaching view");
        mView = view;
        mView.clearListDisplay();
        for (MemberInfo m : mCurrentListMembers) mView.addListMember(m);
        showOrHideAddMembersButton(mActiveListManager.getActiveList());
    }

    @Override
    public void detachView()
    {
        logMsg("Detaching view");
        mView = null;
    }

    @Override
    public void shutdown()
    {
        if (mMembersAddedDeletedSubscription != null) mMembersAddedDeletedSubscription.dispose();
        if (mActiveListManagerSubscription != null) mActiveListManagerSubscription.dispose();
    }

    /** Process events emitted by the ActiveListManager. **/
    private void handleListLoadEvent(ActiveListEvent event)
    {
        if (event instanceof ListLoadedEvent)
        {
            ListLoadedEvent listLoadedEvent = (ListLoadedEvent) event;
            resetState();
            loadListMembers(listLoadedEvent.ActiveList);
        }
        else if (event instanceof NoListsAvailableEvent)
        {
            resetState();
        }
    }

    /** Unsubscribe from member add/remove subscription if active, clear the list display, and hide the "Add Member" button. **/
    private void resetState()
    {
        mCurrentListMembers.clear();
        if (mMembersAddedDeletedSubscription != null) mMembersAddedDeletedSubscription.dispose();
        if (mView != null) mView.clearListDisplay();
        if (mView != null) mView.setAddButtonVisible(false);
    }

    /** If a list is currently active and the current user is its owner, then show the "Add Members" button, else hide it. **/
    private void showOrHideAddMembersButton(PineTaskList activeList)
    {
        if (mView != null) mView.setAddButtonVisible((activeList == null) ? false : activeList.getOwnerId().equalsIgnoreCase(mCurrentUserId));
    }

    /** Load and display the members list, then make async request to get list owner.  If current user is owner, "Add Member" button will show.
        Then, subscribe to events for members added/deleted and update the view as they come in. **/
    private void loadListMembers(PineTaskList list)
    {
        logMsg("loadListMembers: list=%s", list==null ? null : list.getId());
        if (mView != null) mView.setListVisible(true);
        showOrHideAddMembersButton(list);
        mMembersAddedDeletedSubscription = subscribeToMemberAddedDeletedEvents(list);
    }

    /** Attach listener to get user IDs for collaborators of the specified list, emitting added/deleted events for MemberInfo objects which are then passed to
     *  the view (if still attached) to add or remove the member from the displayed list. **/
    private Disposable subscribeToMemberAddedDeletedEvents(PineTaskList pineTaskList)
    {
        return mDbHelper.getMembersAddedOrDeletedEvents(pineTaskList.getId())
            .flatMapSingle(addedOrDeletedEvent -> getMemberInfoForUserId(addedOrDeletedEvent, mCurrentUserId, pineTaskList.getOwnerId()))
            .subscribe(memberAddedOrDeletedEvent ->
            {
                if (memberAddedOrDeletedEvent instanceof AddedEvent)
                {
                    mCurrentListMembers.add(memberAddedOrDeletedEvent.Item);
                    if (mView != null) mView.addListMember(memberAddedOrDeletedEvent.Item);
                }
                else
                {
                    mCurrentListMembers.remove(memberAddedOrDeletedEvent.Item);
                    if (mView != null) mView.removeListMember(memberAddedOrDeletedEvent.Item.UserId);
                }
            }, ex ->
            {
                logError("subscribeToMemberAddedDeletedEvents: error getting member added/deleted events");
                logException(ex);
            });
    }

    /** Look up username for the specified userId, and convert the "user ID added or deleted" event into a "MemberInfo added or deleted" event. **/
    private Single<ChildEventBase<MemberInfo>> getMemberInfoForUserId(ChildEventBase<String> userAddedOrDeletedEvent, String currentUserId, String currentListOwnerId)
    {
        String userId = userAddedOrDeletedEvent.Item;
        return mDbHelper.getUserNameSingle(userId).map(userName ->
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
        PineTaskList activeList = mActiveListManager.getActiveList();
        if (activeList != null) mView.launchInviteProcess(activeList);
        else mView.showError(mApplication.getString(R.string.error_no_current_list));
    }

    @Override
    protected void showErrorMessage(String message, Object... args)
    {
        if (mView != null) mView.showError(message, args);
    }
}
