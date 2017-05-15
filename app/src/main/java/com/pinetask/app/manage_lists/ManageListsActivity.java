package com.pinetask.app.manage_lists;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.pinetask.app.common.PineTaskActivity;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PineTaskListWithCollaborators;
import com.pinetask.app.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Activity for managing lists: creating new lists, deleting or renaming existing lists. **/
public class ManageListsActivity extends PineTaskActivity implements ManageListsContract.IManageListsView
{
    /** Name of a string extra passed in the launch intent specifying the user's ID. **/
    public static String USER_ID_KEY = "UserId";

    ManageListsAdapter mListAdapter;
    ManageListsContract.IManageListsPresenter mPresenter;

    @BindView(R.id.listsRecyclerView) RecyclerView mListsRecyclerView;
    @BindView(R.id.statusTextView) TextView mStatusTextView;

    /** Activity result code indicating that a new list was selected by the user. **/
    public static int NEW_LIST_SELECTED_RESULT_CODE = 100;

    /** Name of a serializable (PineTaskList) extra returned in the result intent with the new list that was selected by the user, if applicable. **/
    public static String LIST_KEY = "List";

    public static Intent buildLaunchIntent(Context context, String userId)
    {
        Intent i = new Intent(context, ManageListsActivity.class);
        i.putExtra(USER_ID_KEY, userId);
        return i;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_lists_activity);
        ButterKnife.bind(this);

        // Set the Toolbar as the app bar for the activity.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.manage_lists));
        setSupportActionBar(toolbar);

        // Set up RecyclerView
        String userId = getIntent().getStringExtra(USER_ID_KEY);
        mListAdapter = new ManageListsAdapter(this, new ArrayList<>(), userId);
        mListsRecyclerView.setAdapter(mListAdapter);
        mListsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Attach or re-attach presenter and start data load.
        mPresenter = (ManageListsContract.IManageListsPresenter) getLastCustomNonConfigurationInstance();
        if (mPresenter == null) mPresenter = new ManageListsPresenter(userId);
        mPresenter.attachView(this);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance()
    {
        return mPresenter;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mPresenter != null) mPresenter.detachView(isFinishing());
    }

    /** Handler for floating action button: open the "add list" dialog **/
    @OnClick(R.id.addListButton)
    public void addListButtonOnClick(View view)
    {
        String userId = getIntent().getStringExtra(USER_ID_KEY);
        AddOrRenameListDialogFragment dialog = AddOrRenameListDialogFragment.newInstanceAddMode(userId, false);
        dialog.show(getSupportFragmentManager(), AddOrRenameListDialogFragment.class.getSimpleName());
    }

    /** Show a dialog allowing the user to rename the list. **/
    public void promptToRenameList(PineTaskList list)
    {
        String userId = getIntent().getStringExtra(USER_ID_KEY);
        AddOrRenameListDialogFragment dialog = AddOrRenameListDialogFragment.newInstanceRenameMode(userId, list.getKey(), list.getName());
        dialog.show(getSupportFragmentManager(), AddOrRenameListDialogFragment.class.getSimpleName());
    }

    /** Show a dialog asking if the user really wants to delete the specified list. **/
    public void promptToDeleteList(PineTaskList list)
    {
        DeleteListDialogFragment deleteDialog = DeleteListDialogFragment.newInstance(list.getKey(), list.getName());
        deleteDialog.show(getSupportFragmentManager(), DeleteListDialogFragment.class.getSimpleName());
    }

    /** Called from PineTaskListAdapter if user clicks list name. Switch to the specified list and finish this activity. **/
    public void switchToList(PineTaskList list)
    {
        Intent data = new Intent();
        data.putExtra(LIST_KEY, list);
        setResult(NEW_LIST_SELECTED_RESULT_CODE, data);
        finish();
    }

    @Override
    public void showLists(List<PineTaskListWithCollaborators> lists)
    {
        mListAdapter.clear();
        mListAdapter.addAll(lists);
    }

    @Override
    public void addList(PineTaskListWithCollaborators list)
    {
        // If the mListAdapter already has a list with the same ID, it will get ignored.
        mListAdapter.add(list);
    }

    @Override
    public void removeList(String listId)
    {
        mListAdapter.remove(listId);
    }

    @Override
    public void updateList(PineTaskListWithCollaborators list)
    {
        mListAdapter.remove(list.getId());
        mListAdapter.add(list);
    }

    @Override
    public void onLoadError()
    {
        showUserMessage(true, getString(R.string.error_loading_lists));
    }

    @Override
    public void showLoadStatus(int stringResId)
    {
        mStatusTextView.setText(stringResId);
        mStatusTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoadStatus()
    {
        mStatusTextView.setVisibility(View.GONE);
    }
}
