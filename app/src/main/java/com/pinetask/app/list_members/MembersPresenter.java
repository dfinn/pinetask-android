package com.pinetask.app.list_members;

import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.common.ListDeletedEvent;
import com.pinetask.app.common.ListSelectedEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.ChildEventListenerWrapper;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.main.InviteManager;
import com.pinetask.app.main.MainActivity;
import com.pinetask.common.LoggingBase;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import static com.pinetask.app.db.DbHelper.getListOwner;
import static com.pinetask.app.db.DbHelper.singleObserver;

/** Presenter for the MembersFragment. **/
public class MembersPresenter extends LoggingBase implements MembersContract.IMembersPresenter
{
    MembersContract.IMembersView mView;
    FirebaseDatabase mDatabase;
    ChildEventListenerWrapper mChildEventListener;
    DatabaseReference mDatabaseRef;
    Bus mEventBus;
    PrefsManager mPrefsManager;
    String mCurrentDisplayedListId;
    String mCurrentUserId;

    @Override
    public void attachView(MembersContract.IMembersView view, String currentUserId)
    {
        mCurrentUserId = currentUserId;
        mView = view;
        mPrefsManager = PrefsManager.getInstance(PineTaskApplication.getInstance());
        mDatabase = FirebaseDatabase.getInstance();
        mEventBus = PineTaskApplication.getEventBus();
        mEventBus.register(this);
        requestLoadListMembers(mPrefsManager.getCurrentListId());
    }

    @Override
    public void detachView()
    {
        mView = null;
        if (mDatabaseRef != null & mChildEventListener != null) mChildEventListener.shutdown();
        mEventBus.unregister(this);
    }

    /** Initiate async load of list members.   As each one is loaded, it will be added to the view. **/
    private void requestLoadListMembers(final String listId)
    {
        // If list ID is null, clear list display (no currently selected list - ie, user just deleted their last one)
        // If request if for the list already loaded (or being loaded), just ignore it.
        // If for any other list, clear the current display, set new list ID, and start async load.
        if (listId == null)
        {
            logMsg("requestLoadListMembers: list ID is null, clearing list display");
            mCurrentDisplayedListId = null;
            if (mChildEventListener != null) mChildEventListener.shutdown();
            mView.clearListDisplay();
            mView.setListVisible(false);
            mView.setAddButtonVisible(false);
        }
        else if (listId.equals(mCurrentDisplayedListId))
        {
            logMsg("requestLoadListMembers: ignoring request, already displaying list %s", mCurrentDisplayedListId);
        }
        else
        {
            logMsg("requestLoadListMembers: starting load of members in list %s", listId);
            mCurrentDisplayedListId = listId;
            if (mChildEventListener != null) mChildEventListener.shutdown();
            mView.clearListDisplay();
            mView.setListVisible(true);
            mView.setAddButtonVisible(false);

            // TODO: Get list owner, show "Add" button if current user. Attach observer for add/remove events for current list items.
            // As observer emits items, convert them to MemberInfo objects and then pass them to mView.add() and mView.remove()


            //getListOwner(listId).subscribe(singleObserver((String ownerId) -> initRecyclerView(listId, ownerId)));

        /*
        getUserNameSingle(userId).subscribe(singleObserver((String data) ->
            {
                if (userId.equals(holder.NameTextView.getTag()))
                {
                    String userName = (data==null) ? "?" : data;
                    boolean isOwner = mOwnerId.equals(userId);
                    if (isOwner) userName += " " + mActivity.getString(R.string.owner);
                    holder.NameTextView.setTypeface(null, isOwner ? Typeface.BOLD : Typeface.NORMAL);
                    holder.NameTextView.setText(userName);
                }
            }));
            */
        }

    }



    private void initRecyclerView(String listId, String ownerId)
    {

        mDatabaseRef = mDatabase.getReference(DbHelper.LIST_COLLABORATORS_NODE_NAME).child(listId);

        mChildEventListener = new ChildEventListenerWrapper(mDatabaseRef, new ChildEventListenerWrapper.Callback()
        {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                logMsg("onChildAdded: %s", dataSnapshot.getKey());
                mAdapter.addUser(dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot)
            {
                logMsg("onChildRemoved: %s", dataSnapshot.getKey());
                mAdapter.removeUser(dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                DbHelper.logDbOperationResult("get list members", databaseError, mDatabaseRef);
            }
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
        displayListMembers(event.ListId);
    }

    /** Called by the event bus when a list has been deleted. **/
    @Subscribe
    public void onListDeleted(ListDeletedEvent event)
    {
        logMsg("ListDeletedEvent for list %s", event.ListId);
        String currentListId = mPrefsManager.getCurrentListId();
        if (event.ListId.equals(currentListId))
        {
            logMsg("onListDeleted: disconnecting mChildEventListener, current list %s was deleted", currentListId);
            displayListMembers(null);
        }
    }
}
