package com.pinetask.app.list_members;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskDialogFragment;
import com.pinetask.app.common.PineTaskList;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Dialog which prompts the user to confirm deleting a member from a list.  If they choose yes, initiates delete request. **/
public class RevokeListAccessDialogFragment extends PineTaskDialogFragment
{
    public static String LIST_KEY = "List";
    public static String USER_ID_KEY = "UserId";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    public static RevokeListAccessDialogFragment newInstance(PineTaskList pineTaskList, String userId)
    {
        Bundle args = new Bundle();
        args.putSerializable(LIST_KEY, pineTaskList);
        args.putString(USER_ID_KEY, userId);
        RevokeListAccessDialogFragment dialog = new RevokeListAccessDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.basic_dialog, container, false);
        ButterKnife.bind(this, view);
        mOkButton.setText(R.string.delete);

        PineTaskList pineTaskList = (PineTaskList) getArguments().getSerializable(LIST_KEY);
        final String userId = getArguments().getString(USER_ID_KEY);

        // Look up list name and username, and then populate dialog text.
        mDbHelper.getUserNameSingle(userId).subscribe(userName ->
        {
            mTitleTextView.setText(String.format(getString(R.string.really_revoke_access_to_list_x_for_user_y), pineTaskList.getName(), userName));
        }, ex ->
        {
            logException(ex);
            dismiss();
            Toast.makeText(getActivity(), R.string.error_getting_username, Toast.LENGTH_LONG).show();
        });

        mOkButton.setOnClickListener((View __) ->
        {
            mDbHelper.revokeAccessToList(pineTaskList.getId(), userId).subscribe(activityObserver("revoke access to list " + pineTaskList.getId()));
            dismiss();
        });

        mCancelButton.setOnClickListener((View __) ->
        {
            logMsg("Revoke operation cancelled");
            dismiss();
        });

        // Prevent showing blank title area at the top of dialog (only affects older API versions)
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }
}
