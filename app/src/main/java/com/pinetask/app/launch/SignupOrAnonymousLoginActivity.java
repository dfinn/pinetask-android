package com.pinetask.app.launch;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskActivity;
import com.pinetask.app.main.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Offers the user two choices:
 *  - Try PineTask without logging in: perform anonymous Firebase authentication process to make a temp account.
 *  - Sign in / sign up: launch Firebase-UI auth process to log in with existing or new account.
 **/
public class SignupOrAnonymousLoginActivity extends PineTaskActivity
{
    /** Request code for launching the firebase authentication activity. **/
    public static int FIREBASE_UI_AUTH_REQUEST_CODE = 0;

    /** Request code for launching the anonymous account setup activity. **/
    public static int ANONYMOUS_AUTH_REQUEST_CODE = 1;

    @BindView(R.id.introMessageTextView) TextView mIntroMessageTextView;
    @BindView(R.id.signUpOrLoginButton) Button mSignUpOrLoginButton;
    @BindView(R.id.signUpLaterButton) Button mSignUpLaterButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_or_anonymous_activity);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.signUpOrLoginButton)
    public void signUpOrLoginButtonClick(View view)
    {
        logMsg("Starting Firebase-UI sign up / login flow");
        startActivityForResult(getFirebaseAuthIntent(), FIREBASE_UI_AUTH_REQUEST_CODE);
    }

    /** Returns an intent for launching the Firebase UI auth flow. **/
    public static Intent getFirebaseAuthIntent()
    {
        return AuthUI.getInstance().createSignInIntentBuilder().setTheme(R.style.AppTheme).build();
    }

    @OnClick(R.id.signUpLaterButton)
    public void signUpLaterButtonOnClick(View view)
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null)
        {
            logMsg("Starting anonymous account setup activity");
            Intent intent = new Intent(this, AnonymousSetupActivity.class);
            startActivityForResult(intent, ANONYMOUS_AUTH_REQUEST_CODE);
        }
        else
        {
            logMsg("User has already completed anonymous setup on prior launch: going to MainActivity");
            MainActivity.launch(this);
            finish();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FIREBASE_UI_AUTH_REQUEST_CODE)
        {
            // Firebase-UI sign up / sign in flow has completed.
            if (resultCode == RESULT_OK)
            {
                logMsg("onActivityResult: Sign in successful, creating new user.");
                mSignUpOrLoginButton.setVisibility(View.GONE);
                mSignUpLaterButton.setVisibility(View.GONE);
                mIntroMessageTextView.setText(R.string.finishing_account_setup);
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser user = auth.getCurrentUser();
                logMsg("createNewUser: FirebaseUser is: uid=%s, name=%s, email=%s, photoUri=%s", user.getUid(), user.getDisplayName(), user.getEmail(), user.getPhotoUrl());
                mDbHelper.setUserName(user.getUid(), user.getDisplayName()).andThen(mDbHelper.setIsAnonymous(user.getUid(), false)).subscribe(activityObserver("setup new user", ()->
                {
                    MainActivity.launch(SignupOrAnonymousLoginActivity.this);
                    finish();
                }));
            }
            else
            {
                logError("onActivityResult: AUTH_REQUEST_CODE failed with error %d", resultCode);
                finish();
            }
        }
        else if (requestCode == ANONYMOUS_AUTH_REQUEST_CODE)
        {
            // Anonymous account setup activity has returned
            if (resultCode == RESULT_OK)
            {
                logMsg("onActivityResult: ANONYMOUS_AUTH_REQUEST_CODE returned ok");
                MainActivity.launch(SignupOrAnonymousLoginActivity.this);
                finish();
            }
            else
            {
                logError("onActivityResult: ANONYMOUS_AUTH_REQUEST_CODE returned error %d", resultCode);
                finish();
            }
        }
    }
}
