package com.pinetask.app;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LaunchActivity extends PineTaskActivity
{
    /** Request code for luanching tutorial activity. **/
    public static int TUTORIAL_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_activity);
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

        if (! mPrefsManager.getTutorialCompleted())
        {
            // User hasn't completed the startup tutorial yet: launch tutorial activity.
            logMsg("Starting first-launch tutorial");
            Intent tutorialIntent = new Intent(this, TutorialActivity.class);
            startActivityForResult(tutorialIntent, TUTORIAL_REQUEST_CODE);
        }
        else if (auth.getCurrentUser() == null)
        {
            // First launch: start activity prompting to setup an account or choose anonymous login.
            logMsg("onCreate: FirebaseAuth reports user is not signed in. Starting sign up / anonymous chooser activity.");
            launchSignupOrAnonyousLoginActivity();
        }
        else
        {
            // Check if signed in user is anonymous, or has completed account setup. If anonymous, launch activity asking if they want to complete account setup now.
            FirebaseUser user = auth.getCurrentUser();
            DbHelper.getIsAnonymous(user.getUid()).subscribe(singleObserver((Boolean isAnonymous) ->
            {
                if (isAnonymous)
                {
                    logMsg("onCreate: FirebaseAuth reports anonymous user %s is signed in.  Launching SignupOrAnonymousLoginActivity.", user.getUid());
                    launchSignupOrAnonyousLoginActivity();
                }
                else
                {
                    logMsg("onCreate: FirebaseAuth reports user %s is already signed in, launching MainActivity", user.getUid());
                    MainActivity.launch(this);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TUTORIAL_REQUEST_CODE)
        {
            logMsg("Launch tutorial has finished, setting 'tutorial completed' flag in shared prefs");
            mPrefsManager.setTutorialCompleted(true);
            launchNextStep();
        }
    }
}
