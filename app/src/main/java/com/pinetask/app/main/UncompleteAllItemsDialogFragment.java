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

/** Dialog which prompts the user to uncomplete all completed items in the current list. **/
public class UncompleteAllItemsDialogFragment extends PineTaskDialogFragment
{
    public static String LIST_ID_KEY = "ListId";
    public static String LIST_NAME_KEY = "ListName";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    @Inject
    MainActivityPresenter mPresenter;

    public static UncompleteAllItemsDialogFragment newInstance(String listId, String listName)
    {
        Bundle args = new Bundle();
        args.putString(LIST_ID_KEY, listId);
        args.putString(LIST_NAME_KEY, listName);
        UncompleteAllItemsDialogFragment dialog = new UncompleteAllItemsDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.generic_dialog_with_title_ok_cancel, container, false);
        ButterKnife.bind(this, view);
        PineTaskApplication.getInstance().getUserComponent().inject(this);
        final String listId = getArguments().getString(LIST_ID_KEY);
        mTitleTextView.setText(String.format(getString(R.string.really_uncomplete_completed_items_in_list_x), getArguments().getString(LIST_NAME_KEY)));
        mOkButton.setText(R.string.uncomplete);

        mOkButton.setOnClickListener(__ ->
        {
            mPresenter.uncompleteAllItems(listId);
            dismiss();
        });

        mCancelButton.setOnClickListener(__ ->
        {
            logMsg("Uncomplete-all cancelled");
            dismiss();
        });

        // Prevent showing blank title area at the top of dialog (only affects older API versions)
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }
}
