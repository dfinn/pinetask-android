package com.pinetask.app;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import com.pinetask.app.launch.LaunchActivity;
import com.pinetask.app.common.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static com.pinetask.app.TestHelper.ITEM_1;
import static com.pinetask.app.TestHelper.ITEM_2;
import static com.pinetask.app.TestHelper.PASSWORD;
import static com.pinetask.app.TestHelper.TEST_EMAIL_ADDRESS;
import static com.pinetask.app.TestHelper.USERNAME;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/** Test scenarios for user signup/onboarding flow.  Note that these are intended to be run from the command line script launchers, as they are dependent
 *  on app uninstall/reinstall, Firebase auth account deletion, and ordering.
 **/
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnboardingTests
{
    TestHelper mTestHelper;

    @Rule
    public ActivityTestRule<LaunchActivity> mActivityRule = new ActivityTestRule<>(LaunchActivity.class);

    @Before
    public void testSetup()
    {
        mTestHelper = new TestHelper(mActivityRule.getActivity());
        mTestHelper.registerIdlingResource();
    }

    /** Log in to a fresh install of the app, choose "Sign up later" to make an anonymous account.  Create a list called "Grocery Shopping", and add "Apples" and "Oranges" to it.
     *  Preconditions: must be fresh app install (first run).
     *  Postcondition: new anonymous account will have been created with one list and two list items. **/
    @Test
    public void firstLaunchCreateAnonymousAccountAndAddListItems()
    {
        mTestHelper.firstLaunchCreateAnonymousAccountAndAddListItems();
    }

    /** Log in to fresh install of app, choose "Sign up / Sign in", create new account with email/password, make a list called "Grocery Shopping", add "Apples" and "Oranges" to it.
     *  Preconditions: must be fresh app install (first run), must not be an existing account with the email address.
     *  Postcondition: account has been created with email address, which has one list with two items.
     **/
    @Test
    public void firstLaunchSignUpWithEmailAndAddListItems()
    {
        signUpOrLoginWithEmail();
        onView(withId(R.id.email)).check(matches(withText(TEST_EMAIL_ADDRESS)));
        onView(withId(R.id.name)).perform(replaceText(USERNAME));
        onView(withId(R.id.password)).perform(replaceText(PASSWORD));
        onView(withId(R.id.button_create)).perform(click());
        mTestHelper.firstLaunchCreateListAndAddItems();
    }

    /** Goes to the Settings screen and chooses "Sign up", and creates a login account using TEST_EMAIL_ADDRESS.
     * Preconditions:  On previous app launch, user signed in with an anonymous account. There must not be a Firebase auth account using email address TEST_EMAIL_ADDRESS.
     * Poscondition: Anonymous account has been linked with the email address.
     **/
    @Test
    public void signupFromSettingsScreenWhenAnonymous()
    {
        // SignupOrAnonymousLoginActivity: choose anonymous login option
        mTestHelper.chooseAnonymousLoginOption();

        // Go to settings activity and start signup process.
        onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
        onView(withId(R.id.settingsTextView)).perform(click());
        onView(withText(R.string.account_setup_not_yet_completed)).check(matches(isDisplayed()));
        onView(withId(R.id.signUpOrLoginButton)).perform(click());

        // Registration activity: enter email address, click next, verify that email and name show. Enter password and click next.
        onView(withId(R.id.email)).perform(replaceText(TEST_EMAIL_ADDRESS));
        onView(withId(R.id.button_next)).perform(click());
        onView(withId(R.id.email)).check(matches(withText(TEST_EMAIL_ADDRESS)));
        onView(withId(R.id.name)).check(matches(withText(USERNAME)));
        onView(withId(R.id.password)).perform(replaceText(PASSWORD));
        onView(withId(R.id.button_create)).perform(click());

        // Settings activity has resumed. Verify that "Sign Up" message and button don't appear anymore.
        onView(withText(R.string.account_setup_not_yet_completed)).check(matches(not(isDisplayed())));
        onView(withId(R.id.signUpOrLoginButton)).check(matches(not(isDisplayed())));

        // Press back to return to main activity.  Verify that the list items added earlier still appear.
        Espresso.pressBack();
        mTestHelper.verifyListItemsShown(ITEM_1, ITEM_2);
    }

    /** Launches the app and verifies the the user is presented immediately with the list items created previously. **/
    @Test
    public void verifyListItemsExist()
    {
        logMsg("Verifying list items after initial launch");
        mTestHelper.verifyListItemsShown(ITEM_1, ITEM_2);
    }


    /** Log in to fresh install of the app, using an email address which already has an existing PineTask user account.  Verify list items created previously are shown. **/
    @Test
    public void firstLaunchLoginWithExistingEmailAndVerifyItems()
    {
        signUpOrLoginWithEmail();
        onView(withText("Welcome back!")).check(matches(isDisplayed()));
        onView(withId(R.id.password)).perform(replaceText(PASSWORD));
        onView(withId(R.id.button_done)).perform(click());
        mTestHelper.verifyListItemsShown(ITEM_1, ITEM_2);
    }

    /** After prior launch where user created anonymous account, re-launch the app and choose "Sign In / Sign Up" from the launch screen.  Sign up with email account, verify
     *  previously created list items show.
     *  Preconditions: user signed in anonymously previously; an account doens't yet exist with the email address.
     *  Post condition: anonymous account has been convered to email signup; user still retains previous list items.
     **/
    @Test
    public void signupFromLaunchScreenWhenAnonymous()
    {
        signUpOrLoginWithEmail();
        onView(withId(R.id.email)).check(matches(withText(TEST_EMAIL_ADDRESS)));
        onView(withId(R.id.name)).check(matches(withText(USERNAME)));
        onView(withId(R.id.password)).perform(replaceText(PASSWORD));
        onView(withId(R.id.button_create)).perform(click());
        mTestHelper.verifyListItemsShown(ITEM_1, ITEM_2);
    }

    /** After previous app launch where user created anonymous account, choose "Sign up / Log in" but try to use an existing email address. Verify appropriate error is shown. **/
    @Test
    public void errorShowsWithExistingEmailSignupAttemptWhenAnonymous()
    {
        signUpOrLoginWithEmail();
        onView(withText("An account already exists with that email address.")).inRoot(withDecorView(not(is(getActiveActivity().getWindow().getDecorView())))).check(matches(isDisplayed()));
    }

    // ************************************************************************************************************************
    // Begin helper methods
    // ************************************************************************************************************************

    @After
    public void afterTest() throws InterruptedException
    {
        logMsg("Test completed");
        //Thread.sleep(3000);
    }

    /** Verifies the SignupOrAnonymousLoginActivity displays correctly, and chooses the "Sign up / Log In" option. **/
    private void signUpOrLoginWithEmail()
    {
        mTestHelper.verifySignupOrAnonymousActivity();
        onView(withText(R.string.login_or_signup)).perform(click());
        onView(withId(R.id.email)).perform(replaceText(TEST_EMAIL_ADDRESS));
        onView(withId(R.id.button_next)).perform(click());
    }

    public Activity getActiveActivity()
    {
        final Activity[] currentActivity = {null};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED);
                if (resumedActivities.iterator().hasNext())
                {
                    currentActivity[0] = resumedActivities.iterator().next();
                }
            }
        });

        return currentActivity[0];
    }

    private void logMsg(String msg, Object... args)
    {
        Logger.logMsg(getClass(), "*** [TEST] *** " + msg, args);
    }

    private static RecyclerViewMatcher withRecyclerView(int id)
    {
        return new RecyclerViewMatcher(id);
    }
}
