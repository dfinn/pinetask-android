package com.pinetask.app.launch;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskDialogFragment;
import com.pinetask.app.db.DbHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Dialog which displays a startup message and a checkbox "Don't show this again".  If the checkbox is checked upon clicking ok,
 *  then the user's "last read message version" is set to the version of the startup message displayed. **/
public class StartupMessageDialogFragment extends PineTaskDialogFragment
{
    public static String MESSAGE_KEY = "Message";
    public static String VERSION_KEY = "Version";
    public static String USER_ID_KEY = "UserId";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.dontShowAgainCheckBox) CheckBox mDontShowAgainCheckBox;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    public static StartupMessageDialogFragment newInstance(String message, int version, String userId)
    {
        Bundle args = new Bundle();
        args.putString(MESSAGE_KEY, message);
        args.putInt(VERSION_KEY, version);
        args.putString(USER_ID_KEY, userId);
        StartupMessageDialogFragment dialog = new StartupMessageDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.startup_message_dialog, container, false);
        ButterKnife.bind(this, view);
        mCancelButton.setVisibility(View.GONE);
        final String message = getArguments().getString(MESSAGE_KEY);
        mTitleTextView.setText(message);

        // Prevent showing blank title area at the top of dialog (only affects older API versions)
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    @OnClick(R.id.okButton)
    public void okButtonOnClick(View view)
    {
        if (mDontShowAgainCheckBox.isChecked())
        {
            String userId = getArguments().getString(USER_ID_KEY);
            int version = getArguments().getInt(VERSION_KEY);
            logMsg("Updating user %s last read startup message to version %d", userId, version);
            mDbHelper.setUserStartupMessageVersion(userId, version);
        }
        dismiss();
    }
}
