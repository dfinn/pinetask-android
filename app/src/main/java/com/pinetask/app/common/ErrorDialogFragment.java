package com.pinetask.app.common;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.pinetask.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Dialog which displays an error message to the user. **/
public class ErrorDialogFragment extends PineTaskDialogFragment
{
    public static String MESSAGE_KEY = "Message";

    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.okButton) Button mOkButton;
    @BindView(R.id.cancelButton) Button mCancelButton;

    public static ErrorDialogFragment newInstance(String message)
    {
        Bundle args = new Bundle();
        args.putString(MESSAGE_KEY, message);
        ErrorDialogFragment dialog = new ErrorDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.basic_dialog, container, false);
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
        dismiss();
    }
}
