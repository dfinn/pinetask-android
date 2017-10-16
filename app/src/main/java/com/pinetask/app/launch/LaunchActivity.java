package com.pinetask.app.launch;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pinetask.app.main.MainActivity;
import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskActivity;

public class LaunchActivity extends PineTaskActivity
{
    /** Name of an Intent action for launching the app and automatically adding a specified item to the active list.  Must also include String extra (LIST_ITEM_KEY). **/
    public static String ACTION_ADD_LIST_ITEM = "com.pinetask.app.ADD_LIST_ITEM";

    /** Name of an Extra (used with ADD_LIST_ITEM action) specifying the item to add. **/
    public static String EXTRA_LIST_ITEM = "com.pinetask.app.LIST_ITEM";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_activity);
        checkIntentAndDoStartup();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        logMsg("onNewIntent: %s", intent.toString());
        checkIntentAndDoStartup();
    }

    private void checkIntentAndDoStartup() {
        // If launched with "add list item" intent, store the item temporarily to shared prefs until it can be added.
        if (getIntent().getAction().equals(ACTION_ADD_LIST_ITEM)) {
            String itemToAdd = getIntent().getStringExtra(EXTRA_LIST_ITEM);
            logMsg("onCreate: processing ADD_LIST_ITEM Intent, will add item '%s'", itemToAdd);
            mPrefsManager.setListItemToAdd(itemToAdd);
        }

        launchNextStep();
    }

    /** Launch the next step to be completed:
     *  1) First-launch tutorial
     *  2) Sign in / sign up
     *  3) Main activity
     **/
    private void launchNextStep()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null)
        {
            // First launch: start activity prompting to setup an account or choose anonymous login.
            logMsg("onCreate: FirebaseAuth reports user is not signed in. Starting sign up / anonymous chooser activity.");
            launchSignupOrAnonyousLoginActivity();
        }
        else
        {
            // Check if signed in user is anonymous, or has completed account setup. If anonymous, launch activity asking if they want to complete account setup now.
            FirebaseUser user = auth.getCurrentUser();
            mDbHelper.getIsAnonymous(user.getUid()).subscribe(singleObserver((Boolean isAnonymous) ->
            {
                if (isAnonymous)
                {
                    logMsg("onCreate: FirebaseAuth reports anonymous user %s is signed in.  Launching SignupOrAnonymousLoginActivity.", user.getUid());
                    launchSignupOrAnonyousLoginActivity();
                }
                else
                {
                    logMsg("onCreate: FirebaseAuth reports user %s is already signed in, launching MainActivity", user.getUid());
                    Intent intent = MainActivity.buildLaunchIntent(this, user.getUid(), null);
                    startActivity(intent);
                    finish();
                }
            }));
        }
    }

    private void launchSignupOrAnonyousLoginActivity()
    {
        Intent intent = new Intent(this, SignupOrAnonymousLoginActivity.class);
        startActivity(intent);
        finish();
    }
}
