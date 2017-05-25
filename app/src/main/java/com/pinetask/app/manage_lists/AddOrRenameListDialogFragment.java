package com.pinetask.app.manage_lists;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pinetask.app.common.PineTaskDialogFragment;
import com.pinetask.app.R;
import com.pinetask.app.db.DbHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;

/** Dialog for adding or renaming a list. **/
public class AddOrRenameListDialogFragment extends PineTaskDialogFragment
{
    /** Name of a string argument specifying the current user ID **/
    public static String USER_ID_KEY = "UserId";

    /** Name of a string argument specifying the ID of the existing list to rename, or null if adding a new list. **/
    public static String LIST_ID_KEY = "ListId";

    /** Name of a string argument specifying the old name of the existing list to rename, or null if adding a new list. **/
    public static String OLD_NAME_KEY = "OldName";

    /** Name of a boolean argument specifying whether the dialog was launched automatically for the user to fromRef their first list. **/
    public static String IS_FIRST_LIST_KEY = "IsFirstList";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.listNameEditText) EditText mListNameEditText;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    /** Constructs a new instance of the dialog for renaming an existing list. **/
    public static AddOrRenameListDialogFragment newInstanceRenameMode(String userId, String listId, String oldListName)
    {
        Bundle args = new Bundle();
        args.putString(USER_ID_KEY, userId);
        args.putString(LIST_ID_KEY, listId);
        args.putString(OLD_NAME_KEY, oldListName);
        AddOrRenameListDialogFragment dialog = new AddOrRenameListDialogFragment();
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    /** Constructs a new instance of the dialog for adding a new list. **/
    public static AddOrRenameListDialogFragment newInstanceAddMode(String userId, boolean isFirstList)
    {
        Bundle args = new Bundle();
        args.putString(USER_ID_KEY, userId);
        args.putBoolean(IS_FIRST_LIST_KEY, isFirstList);
        AddOrRenameListDialogFragment dialog = new AddOrRenameListDialogFragment();
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.add_or_rename_list_dialog, container, false);
        ButterKnife.bind(this, view);

        // Hide OK button and input field until after we verify network connection.
        mOkButton.setVisibility(View.INVISIBLE);
        mListNameEditText.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Make sure network connection is online. If not, show error message.
        mDbHelper.isConnected().observeOn(AndroidSchedulers.mainThread()).subscribe(isConnected -> {
            if (isConnected) showCreateOrRenamePrompt();
            else showError(R.string.no_network_connection_try_again_later);
        }, ex -> {
            showError(R.string.no_network_connection_try_again_later);
        });
    }

    @OnClick(R.id.cancelButton)
    public void cancelOnClick(View view)
    {
        dismiss();
    }

    @OnClick(R.id.okButton)
    public void okButtonOnClick(View view)
    {
        String newName = mListNameEditText.getText().toString();
        final String listId = getArguments().getString(LIST_ID_KEY);
        final String oldName = getArguments().getString(OLD_NAME_KEY);
        final String userId = getArguments().getString(USER_ID_KEY);
        if (listId != null)
        {
            // Renaming an existing list.
            logMsg("Renaming list '%s' to '%s", oldName, newName);
            mDbHelper.renameList(listId, newName).subscribe(activityObserver("rename list", () -> logMsg("Rename list completed")));
        }
        else
        {
            // Add new list
            mDbHelper.createList(userId, newName).subscribe(activityObserver("create list", () -> logMsg("Create list completed")));
        }
        dismiss();
    }

    private void showCreateOrRenamePrompt()
    {
        mListNameEditText.setVisibility(View.VISIBLE);
        mOkButton.setVisibility(View.VISIBLE);

        // If renaming an existing list, populate the old name.
        final String oldName = getArguments().getString(OLD_NAME_KEY);
        if (oldName != null) mListNameEditText.setText(oldName);

        if (getArguments().getBoolean(IS_FIRST_LIST_KEY))
        {
            // If dialog was launched automatically for user to create their first list, show a helpful title message.
            mTitleTextView.setText(R.string.please_create_first_list);
        }
        else
        {
            // Show title as either "Add List" or "Rename List"
            mTitleTextView.setText(oldName == null ? R.string.add_list : R.string.rename_list);
        }

        // Show soft keyboard when dialog opens.
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
        mListNameEditText.requestFocus();
    }

    private void showError(int stringResId)
    {
        mListNameEditText.setVisibility(View.INVISIBLE);
        mTitleTextView.setText(stringResId);
    }
}
