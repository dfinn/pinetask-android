package com.pinetask.app.manage_lists;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskDialogFragment;
import com.pinetask.app.db.DbHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;

/** Dialog which prompts the user to confirm deleting a specified list.  If they choose yes, initiates Firebase DB requests to delete all nodes related to the list. **/
public class DeleteListDialogFragment extends PineTaskDialogFragment
{
    public static String LIST_ID_KEY = "ListId";
    public static String LIST_NAME_KEY = "ListName";

    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;
    @BindView(R.id.titleTextView) TextView mTitleTextView;

    public static DeleteListDialogFragment newInstance(String listId, String listName)
    {
        Bundle args = new Bundle();
        args.putString(LIST_ID_KEY, listId);
        args.putString(LIST_NAME_KEY, listName);
        DeleteListDialogFragment dialog = new DeleteListDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.basic_dialog, container, false);
        ButterKnife.bind(this, view);

        // OK Button will be hidden until we verify online status.
        mOkButton.setText(R.string.delete);
        mOkButton.setVisibility(View.INVISIBLE);

        // Prevent showing blank title area at the top of dialog (only affects older API versions)
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Make sure network connection is online. If not, show error message.
        mDbHelper.isConnected().observeOn(AndroidSchedulers.mainThread()).subscribe(isConnected -> {
            if (isConnected) showDeletePrompt();
            else showError(R.string.no_network_connection_try_again_later);
        }, ex -> {
            showError(R.string.no_network_connection_try_again_later);
        });
    }

    @OnClick(R.id.cancelButton)
    public void cancelOnClick(View view)
    {
        logMsg("Delete cancelled");
        dismiss();
    }

    @OnClick(R.id.okButton)
    public void okButtonOnClick(View view)
    {
        final String listId = getArguments().getString(LIST_ID_KEY);
        final String listName = getArguments().getString(LIST_NAME_KEY);
        logMsg("Deleting list '%s' (%s)", listName, listId);
        mDbHelper.deleteList(listId).subscribe(activityObserver("delete list", () -> logMsg("Delete completed")));
        dismiss();
    }

    private void showDeletePrompt()
    {
        mOkButton.setVisibility(View.VISIBLE);
        final String listName = getArguments().getString(LIST_NAME_KEY);
        mTitleTextView.setText(String.format(getString(R.string.really_delete_list_x), listName));
    }

    private void showError(int stringResId)
    {
        mTitleTextView.setText(stringResId);
    }
}
