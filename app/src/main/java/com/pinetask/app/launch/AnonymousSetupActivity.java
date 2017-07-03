package com.pinetask.app.launch;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseUser;
import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Prompts user for their name, and then creates an anonymous Firebase account. **/
public class AnonymousSetupActivity extends PineTaskActivity
{
    @BindView(R.id.nameEditText) EditText mNameEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anonymous_setup_activity);
        ButterKnife.bind(this);
        showSoftKeyboard(mNameEditText);
    }

    @OnClick(R.id.nextButton)
    public void nextButtonOnClick(View view)
    {
        String name = mNameEditText.getText().toString();
        mNameEditText.setEnabled(false);

        /** Start async process to setup anonymous account. When done, set username for the new FirebaseUser, and set the is_anonymous flag to true. **/
        AnonymousAccountCreator.createAnonymousAccount()
            .flatMapCompletable((FirebaseUser user) -> mDbHelper.setUserName(user.getUid(), name).andThen(mDbHelper.setIsAnonymous(user.getUid(), true)))
            .subscribe(activityObserver("create anonymous user", () -> setResultAndFinish(RESULT_OK)));
    }

}
