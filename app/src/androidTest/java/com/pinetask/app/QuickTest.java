package com.pinetask.app;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.pinetask.app.TestHelper.USERNAME;

/** Test of basic functionality:
 *  - log in to fresh install of app
 *  - choose "Sign Up Later"
 *  - create first list when prompted
 *  - add some list items
 *  - claim, complete, delete a list item
 **/
@RunWith(AndroidJUnit4.class)
@LargeTest
public class QuickTest
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

    @Test
    public void runQuickTest()
    {
        mTestHelper.viewAllTutorialSlides();
        mTestHelper.chooseAnonymousLoginOption();
        mTestHelper.enterUserName();
        mTestHelper.firstLaunchCreateListAndAddItems();
        mTestHelper.claimItem1();
        mTestHelper.dismissItem1();

    }
}
