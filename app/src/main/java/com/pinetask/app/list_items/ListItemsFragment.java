package com.pinetask.app.list_items;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskFragment;

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

    @Override
    public void onPause()
    {
        super.onPause();
        mPresenter.detachView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mPresenter.attachView(this);
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
    public void showEditDialog(PineTaskItemExt item)
    {
        AddOrEditItemDialog dialog = AddOrEditItemDialog.newInstance(item);
        dialog.setTargetFragment(this, -1);
        dialog.show(getFragmentManager(), AddOrEditItemDialog.class.getSimpleName());
    }

    @Override
    public void addItem(PineTaskItemExt item)
    {
        mItemsListAdapter.add(item);
        if (item.getIsNewItem())
        {
            Toast.makeText(getActivity(), String.format(getString(R.string.x_has_been_added), item.getItemDescription()), Toast.LENGTH_SHORT).show();
            item.setIsNewItem(false);
        }
    }

    @Override
    public void clearListItems()
    {
        mItemsListAdapter.clear();
    }

    @Override
    public void removeItem(String itemId)
    {
        mItemsListAdapter.remove(itemId);
    }

    @Override
    public void updateItem(PineTaskItemExt item)
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
