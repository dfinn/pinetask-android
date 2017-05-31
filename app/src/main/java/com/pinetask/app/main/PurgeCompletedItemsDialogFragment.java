package com.pinetask.app.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskDialogFragment;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Dialog which prompts the user to purge all completed items in the current list. **/
public class PurgeCompletedItemsDialogFragment extends PineTaskDialogFragment
{
    public static String LIST_ID_KEY = "ListId";
    public static String LIST_NAME_KEY = "ListName";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    @Inject
    MainActivityContract.IMainActivityPresenter mPresenter;

    public static PurgeCompletedItemsDialogFragment newInstance(String listId, String listName)
    {
        Bundle args = new Bundle();
        args.putString(LIST_ID_KEY, listId);
        args.putString(LIST_NAME_KEY, listName);
        PurgeCompletedItemsDialogFragment dialog = new PurgeCompletedItemsDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.purge_completed_items_dialog, container, false);
        ButterKnife.bind(this, view);
        PineTaskApplication.getInstance().getUserComponent().inject(this);
        final String listId = getArguments().getString(LIST_ID_KEY);
        mTitleTextView.setText(String.format(getString(R.string.really_purge_completed_items_in_list_x), getArguments().getString(LIST_NAME_KEY)));
        mOkButton.setText(R.string.delete);

        mOkButton.setOnClickListener(__ ->
        {
            mPresenter.purgeCompletedItems(listId);
            dismiss();
        });

        mCancelButton.setOnClickListener(__ ->
        {
            logMsg("Purge completed items cancelled");
            dismiss();
        });

        // Prevent showing blank title area at the top of dialog (only affects older API versions)
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }
}
