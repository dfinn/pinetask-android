package com.pinetask.app.list_items;

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
import com.pinetask.app.common.ListDeletedEvent;
import com.pinetask.app.common.ListSelectedEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskFragment;
import com.pinetask.app.R;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.db.StatefulChildListener;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Fragment for the "list items" tab: shows a list of items in the currently selected list. **/
public class ListItemsFragment extends PineTaskFragment implements ListItemsView
{
    ListItemAdapter mItemsListAdapter;

    @BindView(R.id.itemsRecyclerView) RecyclerView mItemsRecyclerView;
    @BindView(R.id.addItemButton) FloatingActionButton mAddItemButton;

    @Inject ListItemsPresenter mPresenter;

    public static ListItemsFragment newInstance()
    {
        ListItemsFragment fragment = new ListItemsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.list_items_fragment, container, false);
        ButterKnife.bind(this, view);
        mItemsListAdapter = new ListItemAdapter(this, new ArrayList<>());
        mItemsRecyclerView.setAdapter(mItemsListAdapter);
        mItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        PineTaskApplication.getInstance().getUserComponent().inject(this);
        return view;
    }

    /** When "add" floating action button is clicked, open dialog for adding new item. **/
    @OnClick(R.id.addItemButton)
    public void onAddButtonClicked(View view)
    {
        AddOrEditItemDialog dialog = AddOrEditItemDialog.newInstance(null);
        dialog.setTargetFragment(this, -1);
        dialog.show(getFragmentManager(), AddOrEditItemDialog.class.getSimpleName());
    }

    /** Shows a dialog allowing the user to edit the description of the specified item. **/
    public void showEditDialog(PineTaskItem item)
    {
        AddOrEditItemDialog dialog = AddOrEditItemDialog.newInstance(item);
        dialog.setTargetFragment(this, -1);
        dialog.show(getFragmentManager(), AddOrEditItemDialog.class.getSimpleName());
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

    @Override
    public void showListItems(List<PineTaskItem> items)
    {
        mItemsListAdapter.showItems(items);
    }

    @Override
    public void addItem(PineTaskItem item)
    {
        mItemsListAdapter.add(item);
    }

    @Override
    public void removeItem(String itemId)
    {
        mItemsListAdapter.remove(itemId);
    }

    @Override
    public void updateItem(PineTaskItem item)
    {
        mItemsListAdapter.update(item);
    }

    @Override
    public void showListItemsLayouts()
    {
        mItemsRecyclerView.setVisibility(View.VISIBLE);
        mAddItemButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideListItemsLayouts()
    {
        mItemsRecyclerView.setVisibility(View.GONE);
        mAddItemButton.setVisibility(View.GONE);
    }

    @Override
    public void showError(String message, Object... args)
    {
        showUserMessage(false, message, args);
    }
}
