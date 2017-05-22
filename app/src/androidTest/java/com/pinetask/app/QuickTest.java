package com.pinetask.app;

import android.os.RemoteException;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;

import com.pinetask.app.launch.LaunchActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.pinetask.app.TestHelper.ITEM_1;
import static com.pinetask.app.TestHelper.ITEM_2;
import static com.pinetask.app.TestHelper.LIST_NAME_1;
import static com.pinetask.app.TestHelper.LIST_NAME_2;

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
    public void runQuickTest() throws UiObjectNotFoundException, RemoteException
    {
        mTestHelper.rotateScreenRightAndBack();
        mTestHelper.viewAllTutorialSlides();
        mTestHelper.rotateScreenRightAndBack();
        mTestHelper.chooseAnonymousLoginOption();
        mTestHelper.rotateScreenRightAndBack();
        mTestHelper.enterUserName();
        mTestHelper.firstLaunchCreateListAndAddItems();
        mTestHelper.rotateScreenRightAndBack();
        mTestHelper.sendChatMessage();
        mTestHelper.claimAndUnclaimItem1();
        mTestHelper.completeAndUncompleteItem1();
        mTestHelper.openManageListsActivity();
        mTestHelper.rotateScreenRightAndBack();
        mTestHelper.addAndSwitchToSecondList();
        mTestHelper.switchToFirstList();
        mTestHelper.verifyListItemsShown(ITEM_1, ITEM_2);
        mTestHelper.openManageListsActivity();
        mTestHelper.renameList2();
        mTestHelper.deleteList(1, LIST_NAME_2+"_RENAMED");
        mTestHelper.openListFromManageListsActivity(0);
        mTestHelper.deleteListItem(0, ITEM_1);
        mTestHelper.completeListItem(0);
        mTestHelper.purgeCompletedItems();
        mTestHelper.openManageListsActivity();
        mTestHelper.deleteList(0, LIST_NAME_1);
        Espresso.pressBack();
        mTestHelper.verifyNoListsFoundMessage();
    }
}
