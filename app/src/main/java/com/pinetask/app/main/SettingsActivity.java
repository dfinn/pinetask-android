package com.pinetask.app.main;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskActivity;
import com.pinetask.app.common.PineTaskUser;
import com.pinetask.app.databinding.SettingsActivityBinding;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.launch.SignupOrAnonymousLoginActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends PineTaskActivity
{
    /** Name of string extra passed in launch intent specifying the user ID. **/
    public static String USER_ID_KEY = "UserId";

    /** Request code for launching the firebase authentication activity. **/
    public static int FIREBASE_UI_AUTH_REQUEST_CODE = 0;

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.nameEditText) EditText mNameEditText;
    @BindView(R.id.anonymousLayout) LinearLayout mAnonymousLayout;

    SettingsActivityBinding mBinding;
    PineTaskUser mUser;

    public static Intent getLaunchIntent(Context context, String userId)
    {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(USER_ID_KEY, userId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.settings_activity);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Lookup username in database and perform UI binding
        String userId = getIntent().getStringExtra(USER_ID_KEY);
        DbHelper.getUserNameSingle(userId).subscribe(DbHelper.singleObserver((String userName) -> {
            mUser = new PineTaskUser(userId, userName);
            mBinding.setUser(mUser);
            mNameEditText.setEnabled(true);
        }));

        // If user has not yet signed up for an account, show the layout with explanation text and a "Sign up / Login" button.
        mAnonymousLayout.setVisibility(View.GONE);
        DbHelper.getIsAnonymous(userId).subscribe(DbHelper.singleObserver((Boolean isAnonymous) ->
        {
            if (isAnonymous) mAnonymousLayout.setVisibility(View.VISIBLE);
        }));
    }

    @OnClick(R.id.signUpOrLoginButton)
    public void signUpOrLoginButtonOnClick(View view)
    {
        logMsg("Starting Firebase UI auth activity");
        Intent intent = SignupOrAnonymousLoginActivity.getFirebaseAuthIntent();
        startActivityForResult(intent, FIREBASE_UI_AUTH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FIREBASE_UI_AUTH_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                logMsg("onActivityResult: Firebase auth returned successfully.  Removing 'anonymous' account flag.");
                String userId = getIntent().getStringExtra(USER_ID_KEY);
                DbHelper.setIsAnonymous(userId, false).subscribe(activityObserver("remove anonymous status"));
            }
            else
            {
                logError("onActivityResult: Firebase auth activity returned error %d", resultCode);
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mUser != null)
        {
            logMsg("Saving changes (username=%s)", mUser.getUserName());
            DbHelper.setUserName(mUser.getUserId(), mUser.getUserName()).subscribe(activityObserver("change username"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
