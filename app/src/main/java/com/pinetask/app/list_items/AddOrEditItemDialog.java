package com.pinetask.app.list_items;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskDialogFragment;

import javax.inject.Inject;

/** Dialog for adding a new item or editing an existing one. **/
public class AddOrEditItemDialog extends PineTaskDialogFragment
{
    /** Name of a serializable argument that specifies the PineTaskItem to edit (or null if adding a new item) **/
    public static String ITEM_KEY = "PineTaskItem";

    @Inject
    ListItemsPresenter mListItemsPresenter;

    public static AddOrEditItemDialog newInstance(PineTaskItemExt item)
    {
        AddOrEditItemDialog dialog = new AddOrEditItemDialog();
        Bundle args = new Bundle();
        args.putSerializable(ITEM_KEY, item);
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.add_or_edit_item_dialog, container, false);
        TextView titleTextView = (TextView) view.findViewById(R.id.titleTextView);
        final EditText descriptionEditText = (EditText) view.findViewById(R.id.descriptionEditText);
        Button okButton = (Button) view.findViewById(R.id.okButton);
        Button cancelButton = (Button) view.findViewById(R.id.cancelButton);

        final PineTaskItemExt item = (PineTaskItemExt) getArguments().getSerializable(ITEM_KEY);

        PineTaskApplication.getInstance().getUserComponent().inject(this);

        // If adding an item, show "Add Item", otherwise show "Edit Item"
        titleTextView.setText((item == null) ? R.string.add_item : R.string.edit_item);

        // Show exiting description if editing an item.
        if (item != null) descriptionEditText.setText(item.getItemDescription());

        // OK button will add/update the item in the database.
        okButton.setOnClickListener(__ ->
        {
            ListItemsFragment listItemsFragment = (ListItemsFragment) getTargetFragment();
            String description = descriptionEditText.getText().toString();
            if (item != null)
            {
                // Update existing item
                logMsg("Updating item '%s' to '%s'", item.getItemDescription(), description);
                item.setItemDescription(description);
                mListItemsPresenter.updateItem(item);
            }
            else
            {
                // Add new item
                logMsg("Adding new item '%s'", description);
                mListItemsPresenter.addItem(description);
            }
            hideSoftKeyboard();
            dismiss();
        });

        // Cancel button closes dialog without saving
        cancelButton.setOnClickListener(__ ->
        {
            hideSoftKeyboard();
            dismiss();
        });

        // Show soft keyboard when dialog opens.
        showSoftKeyboard(descriptionEditText);

        return view;
    }
}
