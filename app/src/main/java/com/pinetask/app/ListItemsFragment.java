package com.pinetask.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Fragment for the "list items" tab: shows a list of items in the currently selected list. **/
public class ListItemsFragment extends PineTaskFragment
{
    FirebaseDatabase mDatabase;
    DatabaseReference mListItemsRef;
    ListItemAdapter mItemsListAdapter;
    StatefulChildListener mItemsListener;
    String mUserId;
    String mCurrentListId;
    @BindView(R.id.itemsRecyclerView) RecyclerView mItemsRecyclerView;
    @BindView(R.id.addItemButton) FloatingActionButton mAddItemButton;

    /** Name of a string argument specifying the user ID. **/
    public static String USER_ID_KEY = "UserId";

    public static ListItemsFragment newInstance(String userId)
    {
        ListItemsFragment fragment = new ListItemsFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID_KEY, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance();
        mUserId = getArguments().getString(USER_ID_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.list_items_fragment, container, false);
        ButterKnife.bind(this, view);
        initUI();
        return view;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        logMsg("onDestroy: shutting down event listeners");
        if (mItemsListener != null) mItemsListener.shutdown();
    }

    private void initUI()
    {
        // Initialize RecyclerView that will show the list of items.
        mItemsListAdapter = new ListItemAdapter(this, new ArrayList<PineTaskItem>());
        mItemsRecyclerView.setAdapter(mItemsListAdapter);
        mItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Get current list ID and display items in it.
        String listId = mPrefsManager.getCurrentListId();
        displayListItems(listId);
    }

    /** When "add" floating action button is clicked, open dialog for adding new item. **/
    @OnClick(R.id.addItemButton)
    public void onAddButtonClicked(View view)
    {
        AddOrEditItemDialog dialog = AddOrEditItemDialog.newInstance(null);
        dialog.setTargetFragment(this, -1);
        dialog.show(getFragmentManager(), AddOrEditItemDialog.class.getSimpleName());
    }

    /** Adds a new item to the database. **/
    public void addItem(String description)
    {
        PineTaskItem item = new PineTaskItem(null, description);
        mListItemsRef.push().setValue(item);
    }

    /** Updates the specified item in the database. **/
    public void updateItem(PineTaskItem item)
    {
        // Make async request to update item in Firebase DB
        mListItemsRef.child(item.getKey()).setValue(item);
        // Update item UI state in the RecyclerView
        mItemsListAdapter.update(item);
    }

    /** Deletes the specified item from the database. **/
    public void deleteItem(final PineTaskItem item)
    {
        logMsg("Deleting item: %s", item.getItemDescription());
        mListItemsRef.child(item.getKey()).removeValue((DatabaseError databaseError, DatabaseReference databaseReference) -> logMsg("onComplete: Item '%s' deleted", item.getItemDescription()));
    }

    /** Deletes all completed items in the current list. **/
    public void purgeCompletedItems()
    {
        logMsg("Deleting all completed items");

    }

    /** Sets the item to be claimed by the current user, and then updates the item in the database. **/
    public void claimItem(PineTaskItem item)
    {
        logMsg("Claiming item '%s'", item.getItemDescription());
        item.setClaimedBy(mUserId);
        updateItem(item);
    }

    /** Sets the item to be unclaimed (set claimed_by to null), and then updates the item in the database. **/
    public void unclaimItem(PineTaskItem item)
    {
        logMsg("Unclaiming item '%s'", item.getItemDescription());
        item.setClaimedBy(null);
        updateItem(item);
    }

    /** Sets the item to be uncompleted, and then updates the item in the database. **/
    public void uncompleteItem(PineTaskItem item)
    {
        logMsg("Uncompleting item '%s'", item.getItemDescription());
        item.setIsCompleted(false);
        updateItem(item);
    }

    /** Shows a dialog allowing the user to edit the description of the specified item. **/
    public void editItem(PineTaskItem item)
    {
        AddOrEditItemDialog dialog = AddOrEditItemDialog.newInstance(item);
        dialog.setTargetFragment(this, -1);
        dialog.show(getFragmentManager(), AddOrEditItemDialog.class.getSimpleName());
    }

    /** Sets the "is completed" status for the item to the state specified, and then updates the item in the database. **/
    public void setCompletedStatus(PineTaskItem item, boolean isCompleted)
    {
        logMsg("Setting completion status for '%s' to %b", item.getItemDescription(), isCompleted);
        item.setClaimedBy(mUserId);
        item.setIsCompleted(isCompleted);
        updateItem(item);
    }

    /** Called by the event bus when a list has been deleted. **/
    @Subscribe
    public void onListDeleted(ListDeletedEvent event)
    {
        logMsg("ListDeletedEvent for list %s", event.ListId);
        if (event.ListId.equals(mCurrentListId))
        {
            logMsg("onListDeleted: disconnecting mItemsListener, current list %s was deleted", mCurrentListId);
            // Disconnect listener for previously selected list, if any.
            if (mItemsListener != null) mItemsListener.shutdown();
            mCurrentListId = null;
        }
    }

    /** Called by the event bus when the user has selected a new list. **/
    @Subscribe
    public void onListSelected(ListSelectedEvent event)
    {
        logMsg("onListSelected: listId = %s", event.ListId);
        displayListItems(event.ListId);
    }

    /** Clears the currently displayed list, if any, and disconnects the list items listener.
     * If listId is non-null, starts async load of all items in the list with the ID specified and displays them to the RecyclerView. **/
    private void displayListItems(final String listId)
    {
        logMsg("displayListItems: listid=%s, mCurrentListId=%s", listId, mCurrentListId);
        if (mCurrentListId != null && mCurrentListId.equals(listId))
        {
            logMsg("displayListItems: Already displaying list %s, returning.", mCurrentListId);
            return;
        }

        logMsg("Loading items in list %s", listId);
        mCurrentListId = listId;
        mItemsListAdapter.clear();

        // Disconnect listener for previously selected list, if any.
        if (mItemsListener != null) mItemsListener.shutdown();

        if (listId==null)
        {
            // Disable the "add item" button
            mAddItemButton.setVisibility(View.GONE);
            logMsg("displayListItems: listId is null, returning");
            return;
        }

        // Enable the "add item" button
        mAddItemButton.setVisibility(View.VISIBLE);

        PineTaskApplication.getInstance().addActiveTask();
        mListItemsRef = mDatabase.getReference(DbHelper.LIST_ITEMS_NODE_NAME).child(listId);
        mItemsListener = new StatefulChildListener(mListItemsRef, new StatefulChildListener.StatefulChildListenerCallbacks()
        {
            @Override
            public void onInitialLoadCompleted()
            {
                logMsg("onInitialLoadCompleted");
                PineTaskApplication.getInstance().endActiveTask();
            }

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, boolean isInitialDataLoaded)
            {
                logMsg("onChildAdded: %s=%s", dataSnapshot.getKey(), dataSnapshot.getValue());
                PineTaskItem item = dataSnapshot.getValue(PineTaskItem.class);
                item.setKey(dataSnapshot.getKey());
                mItemsListAdapter.add(item);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, boolean isInitialDataLoaded)
            {
                logMsg("onChildChanged: %s=%s", dataSnapshot.getKey(), dataSnapshot.getValue());
                PineTaskItem item = dataSnapshot.getValue(PineTaskItem.class);
                item.setKey(dataSnapshot.getKey());
                mItemsListAdapter.update(item);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot, boolean isInitialDataLoaded)
            {
                logMsg("onChildRemoved: %s=%s", dataSnapshot.getKey(), dataSnapshot.getValue());
                PineTaskItem item = dataSnapshot.getValue(PineTaskItem.class);
                mItemsListAdapter.remove(dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError error)
            {
                PineTaskApplication.getInstance().endActiveTask();
                // Only show error if a list is currently active. If list was just deleted, mCurrentListId will be null so we can ignore this error.
                if (mCurrentListId != null)
                {
                    logError("displayListItems: onCancelled: %s", error.getMessage());
                }
                else
                {
                    // Suppress reporting error message: works around issue where callback might get fired soon after list was deleted (even if callback was removed)
                    logMsg("displayListItems(listId=%s): suppressing error, list no longer active", listId);
                }
            }
        });
    }
}
