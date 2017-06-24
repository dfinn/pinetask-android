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
    private DbHelper mDbHelper;
    private PineTaskApplication mApplication;
    private Disposable mActiveListManagerSubscription;
    private ActiveListManager mActiveListManager;
    private ListMembersRepository mListMembersRepository;

    public MembersPresenterImpl(DbHelper dbHelper, PineTaskApplication pineTaskApplication, ActiveListManager activeListManager, @Named("user_id") String userId)
    {
        mDbHelper = dbHelper;
        mApplication = pineTaskApplication;
        mCurrentUserId = userId;
        mActiveListManager = activeListManager;
        mActiveListManagerSubscription = activeListManager.subscribe(this::handleListLoadEvent, ex -> logError("ActiveListManager reported error event: %s", ex.getMessage()));
    }

    @Override
    public void attachView(MembersView view)
    {
        logMsg("Attaching view");
        mView = view;
        mView.clearListDisplay();
        if (mListMembersRepository != null) for (MemberInfo m : mListMembersRepository.getListMembers()) mView.addListMember(m);
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
        if (mListMembersRepository != null) mListMembersRepository.shutdown();
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
        if (mListMembersRepository != null) mListMembersRepository.shutdown();
        if (mView != null)
        {
            mView.clearListDisplay();
            mView.setAddButtonVisible(false);
        }
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
        mListMembersRepository = new ListMembersRepository(mDbHelper, list, mCurrentUserId, this::onMemberAddedOrDeletedEvent, this::onLoadError);
    }

    private void onMemberAddedOrDeletedEvent(ChildEventBase<MemberInfo> event)
    {
        if (event instanceof AddedEvent)
        {
            if (mView != null) mView.addListMember(event.Item);
        }
        else
        {
            if (mView != null) mView.removeListMember(event.Item.UserId);
        }
    }

    private void onLoadError(Throwable ex)
    {
        logError("Error loading list members");
        logException(ex);
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
