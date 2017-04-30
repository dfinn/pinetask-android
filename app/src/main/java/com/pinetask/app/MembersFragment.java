package com.pinetask.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.otto.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.pinetask.app.DbHelper.getListOwner;
import static com.pinetask.app.DbHelper.singleObserver;

/** Fragment which shows all members of the current list.
 *  - The list owner is shown first and identified as the owner.
 *  - Other list members have a 'Delete' button next to them, to revoke their access to the list.
 *  - Pressing the floating action button will begin the process to add a new list member (sending an invite)
 **/
public class MembersFragment extends PineTaskFragment
{
    FirebaseDatabase mDatabase;
    ChildEventListenerWrapper mChildEventListener;
    DatabaseReference mDatabaseRef;
    MembersAdapter mAdapter;

    @BindView(R.id.membersRecyclerView) RecyclerView mMembersRecyclerView;
    @BindView(R.id.addMemberButton) FloatingActionButton mAddMemberButton;

    /** Name of a string argument specifying the user ID. **/
    public static String USER_ID_KEY = "UserId";

    public static MembersFragment newInstance(String userId)
    {
        MembersFragment membersFragment = new MembersFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID_KEY, userId);
        membersFragment.setArguments(args);
        return membersFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.members_fragment, container, false);
        ButterKnife.bind(this, view);
        mDatabase = FirebaseDatabase.getInstance();
        displayListMembers(mPrefsManager.getCurrentListId());
        return view;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mDatabaseRef != null & mChildEventListener != null) mChildEventListener.shutdown();
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

    /** Called by the event bus when the user has selected a new list. **/
    @Subscribe
    public void onListSelected(ListSelectedEvent event)
    {
        logMsg("onListSelected: listId = %s", event.ListId);
        displayListMembers(event.ListId);
    }

    private void displayListMembers(final String listId)
    {
        if (mAdapter != null && mAdapter.getListId().equals(listId))
        {
            logMsg("displayListMembers: already displaying list %s, returning", listId);
            return;
        }

        if (mChildEventListener != null)
        {
            mChildEventListener.shutdown();
        }

        if (listId==null)
        {
            logMsg("displayListMembers: no current list, will not display members");
            mMembersRecyclerView.setVisibility(View.GONE);
            mAddMemberButton.setVisibility(View.GONE);
        }
        else
        {
            // Make async request to get userId of this list's owner.  When retrieved, initialize the RecyclerView.
            getListOwner(listId).subscribe(singleObserver((String ownerId) -> initRecyclerView(listId, ownerId)));
        }
    }

    private void initRecyclerView(String listId, String ownerId)
    {
        logMsg("initRecyclerView: owner id is %s", ownerId);
        String currentUserId = getArguments().getString(USER_ID_KEY);
        if (mAdapter == null)
        {
            mAdapter = new MembersAdapter((MainActivity)getActivity(), listId, currentUserId, ownerId);
            mMembersRecyclerView.setAdapter(mAdapter);
            mMembersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        else
        {
            mAdapter.onListChanged(listId, ownerId);
        }

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

        mMembersRecyclerView.setVisibility(View.VISIBLE);

        // Only show "add member" button if current user is the list owner.
        mAddMemberButton.setVisibility(currentUserId.equals(ownerId) ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.addMemberButton)
    public void addMemberButtonOnClick(View view)
    {
        String currentListId = mPrefsManager.getCurrentListId();
        MainActivity mainActivity = (MainActivity) getActivity();
        InviteManager inviteManager = mainActivity.getInviteManager();
        inviteManager.sendInvite(currentListId);
    }
}
