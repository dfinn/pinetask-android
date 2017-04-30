package com.pinetask.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Dialog which prompts the user to purge all completed items in the current list. **/
public class PurgeCompletedItemsDialogFragment extends PineTaskDialogFragment
{
    public static String LIST_ID_KEY = "ListId";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    public static PurgeCompletedItemsDialogFragment newInstance(String listId)
    {
        Bundle args = new Bundle();
        args.putString(LIST_ID_KEY, listId);
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
        final String listId = getArguments().getString(LIST_ID_KEY);

        DbHelper.getListName(listId).subscribe(DbHelper.singleObserver(new DbCallback<String>()
        {
            @Override
            public void onResult(String listName)
            {
                mTitleTextView.setText(String.format(getString(R.string.really_purge_completed_items_in_list_x), listName));
            }
        }));

        mOkButton.setText(R.string.delete);
        mOkButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                DbHelper.purgeCompletedItems(listId);
                dismiss();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                logMsg("Purge completed items cancelled");
                dismiss();
            }
        });

        // Prevent showing blank title area at the top of dialog (only affects older API versions)
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }
}
