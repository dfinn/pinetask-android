package com.pinetask.app;

import android.content.Context;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.Logger;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import static android.graphics.Paint.STRIKE_THRU_TEXT_FLAG;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestHelper
{
    public static String TEST_EMAIL_ADDRESS = "test@pinetask.com";
    public static String USERNAME = "Pinetask Tester";
    public static String PASSWORD = "Password";
    public static String LIST_NAME_1 = "***PineTaskTest List";
    public static String LIST_NAME_2 = "***PineTaskTest List 2";;
    public static String ITEM_1 = "Apples";
    public static String ITEM_2 = "Oranges";

    private Context mContext;

    public TestHelper(Context context)
    {
        mContext = context;
    }

    public void registerIdlingResource()
    {
        logMsg("Registering idling resource");
        Espresso.registerIdlingResources(new IdlingResource()
        {
            @Override
            public String getName()
            {
                return "PineTaskApplication Idling Resource";
            }

            @Override
            public boolean isIdleNow()
            {
                boolean isIdle = PineTaskApplication.getInstance().getActiveTasks()==0;
                logMsg("isIdleNow: %b", isIdle);
                return isIdle;
            }

            @Override
            public void registerIdleTransitionCallback(ResourceCallback callback)
            {
                logMsg("Registering idle transition callback");
                PineTaskApplication.getInstance().setOnIdleRunnable(new Runnable() {
                    @Override
                    public void run()
                    {
                        logMsg("Invoking callback.onTransitionToIdle()");
                        callback.onTransitionToIdle();
                    }
                });
            }
        });
    }

    public void firstLaunchCreateAnonymousAccountAndAddListItems()
    {
        skipIntroTutorial();
        chooseAnonymousLoginOption();
        enterUserName();
        firstLaunchCreateListAndAddItems();
    }

    /** Enter USERNAME into the "What is your name..." screen after the user chose "Sign up later" option.
     * Precondition: must be on AnonymousSetupActivity
     **/
    public void enterUserName()
    {
        onView(withText(R.string.what_is_your_name)).check(matches(isDisplayed()));
        onView(withId(R.id.nameEditText)).perform(replaceText(USERNAME));
        onView(withId(R.id.nextButton)).perform(click());
    }

    /** Verifies the "Welcome" message shows on the first tutorial slide, and presses "Skip" **/
    public void skipIntroTutorial()
    {
        onView(withText(R.string.welcome_title)).check(matches(isDisplayed()));
        onView(withText("SKIP")).perform(click());
    }

    /** Perform screen rotation to the right, then back to normal. **/
    public void rotateScreenRightAndBack() throws RemoteException
    {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.setOrientationRight();
        delay(500, "wait for setOrientationRight");
        mDevice.setOrientationNatural();
        delay(500, "wait for setOrientationNatural");
    }

    /** Verifies the SignupOrAnonymousLoginActivity displays correctly, and chooses the "Sign up later" option.
     *  Precondition: must be on SignupOrAnonymousLoginActivity**/
    public void chooseAnonymousLoginOption()
    {
        verifySignupOrAnonymousActivity();
        onView(withText(R.string.signup_later)).perform(click());
    }

    /** Verifies the SignupOrAnonymousLoginActivity displays correctly **/
    public void verifySignupOrAnonymousActivity()
    {
        onView(withText(R.string.signup_intro_text)).check(matches(isDisplayed()));
        onView(withText(R.string.login_or_signup)).check(matches(isDisplayed()));
        onView(withText(R.string.signup_later)).check(matches(isDisplayed()));
    }

    // When prompted at first launch, create a list, and add two items to it.
    public void firstLaunchCreateListAndAddItems()
    {
        onView(withText(R.string.please_create_first_list)).check(matches(isDisplayed()));
        onView(withId(R.id.listNameEditText)).perform(replaceText(LIST_NAME_1));
        closeSoftKeyboard();
        onView(withId(R.id.okButton)).perform(click());
        addListItem(ITEM_1);
        addListItem(ITEM_2);
        verifyListItemsShown(ITEM_1, ITEM_2);
    }

    /** Work around issue where test can intermittently fail due to error if soft keyboard is open when trying to click OK button on dialog.
     *  Need to close soft keyboard and give it enough time for the input window to move out of the way.
     *  (java.lang.SecurityException: Injecting to another application requires INJECT_EVENTS permission)
     **/
    public void closeSoftKeyboard()
    {
        Espresso.closeSoftKeyboard();
        delay(250, "wait for soft keyboard to close");
    }

    /**  Verify item 1 UI reflects unclaimed state. Click "Claim" button on item 1, verify UI state changes to show claimed state. Unclaim item, verify UI shows unclaimed state. **/
    public void claimAndUnclaimItem1()
    {
        verifyItemClaimedState(0, false);
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimImageButton)).perform(click());
        verifyItemClaimedState(0, true);
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.optionsImageButton)).perform(click());
        onView(withText("Unclaim")).perform(click());
        verifyItemClaimedState(0, false);
    }

    public void deleteListItem(int pos, String itemDescription)
    {
        onView(withRecyclerView(R.id.itemsRecyclerView).atPosition(pos)).check(matches(hasDescendant(withText(itemDescription))));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.optionsImageButton)).perform(click());
        onView(withText("Delete")).inRoot(isPlatformPopup()).perform(click());
        delay(500, "wait for UI update after deleting item");
        onView(withText(itemDescription)).check(doesNotExist());
    }

    private void verifyItemClaimedState(int pos, boolean isClaimed)
    {
        if (isClaimed)
        {
            // Claimed: Verify claimed button is hidden, and that first initial shows.
            String initial = USERNAME.substring(0, 1).toUpperCase();
            onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.claimImageButton)).check(matches(not(isDisplayed())));
            onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.claimedByTextView)).check(matches(isDisplayed()));
            onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.claimedByTextView)).check(matches(withText(initial)));
        }
        else
        {
            // Unclaimed: verify claimed button shows, and that textview is hidden.
            onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimImageButton)).check(matches(isDisplayed()));
            onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimedByTextView)).check(matches(not(isDisplayed())));
        }
    }

    /** Verify item 1 UI reflects uncompleted state. Click "Dismiss" button on item 1, verify UI state changes to show item completed.  Un-complete the item, verify UI state. **/
    public void completeAndUncompleteItem1()
    {
        completeListItem(0);
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.optionsImageButton)).perform(click());
        onView(withText("Uncomplete")).perform(click());
        verifyCompletedState(0, false);
    }

    public void completeListItem(int pos)
    {
        verifyCompletedState(pos, false);
        delay(500, "wait before clicking complete button");
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.completedImageButton)).perform(click());
        delay(500, "wait for list item completion update");
        verifyCompletedState(pos, true);
    }

    private void verifyCompletedState(int pos, boolean isCompleted)
    {
        // Verify "Complete" button only shows if item is uncompleted
        Matcher completeButtonMatcher = isCompleted ? not(isDisplayed()) : isDisplayed();
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.completedImageButton)).check(matches(completeButtonMatcher));

        // Verify completed items have strikethrough text style
        boolean expectedStrikeThrough = isCompleted ? true : false;
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.itemDescriptionTextView)).check(matches(withTextStrikethrough(expectedStrikeThrough)));

        // Verify completed items are faded (30% alpha)
        int expectedTextColor = isCompleted ? ContextCompat.getColor(mContext, R.color.black_30percent) : ContextCompat.getColor(mContext, R.color.black);
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(pos, R.id.itemDescriptionTextView)).check(matches(withTextColor(expectedTextColor)));
    }

    /** Open the toolbar menu, choose "Export List", and choose the "Messaging" destination. **/
    public void exportList() throws UiObjectNotFoundException
    {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Export List")).perform(click());
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject messagingItem = mDevice.findObject(new UiSelector().text("Messaging").className("android.widget.TextView"));
        messagingItem.click();
        UiObject messageTextItem = mDevice.findObject(new UiSelector().className("android.widget.EditText").resourceId("com.android.mms:id/embedded_text_editor"));
        String txt = messageTextItem.getText();
        Assert.assertEquals(LIST_NAME_1 + ":\n[X] Apples\n[ ] Oranges\n", txt);
        mDevice.pressBack();
        mDevice.pressBack();
        mDevice.findObject(By.clazz("android.widget.Button").text("OK")).click();
    }

    public void purgeCompletedItems()
    {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText("Purge Completed Items")).perform(click());
        onView(withId(R.id.okButton)).perform(click());
    }

    /** Open the side drawer, choose Settings, change user name, press back to save the name change. **/
    public void openSettingsActivityAndChangeName()
    {
        logMsg("openSettingsActivityAndChangeName");
        onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
        onView(withId(R.id.settingsTextView)).perform(click());
        onView(withId(R.id.nameEditText)).check(matches(withText(USERNAME)));
        onView(withText(R.string.account_setup_not_yet_completed)).check(matches(isDisplayed()));
        onView(withId(R.id.signUpOrLoginButton)).check(matches(isDisplayed()));
        onView(withId(R.id.nameEditText)).perform(replaceText(USERNAME +"_NewName"));
        Espresso.pressBack();
        delay(500, "temporary workaround to wait for navigation drawer to close");
    }

    /** Open the side drawer, open Help activity, verify some text, press back to exit. **/
    public void openAndCloseHelpActivity()
    {
        logMsg("openSettingsActivityAndChangeName");
        onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
        onView(withId(R.id.helpTextView)).perform(click());
        onView(withId(R.id.skip)).check(matches(isDisplayed()));
        onView(withId(R.id.next)).check(matches(isDisplayed()));
        Espresso.pressBack();
        delay(500, "temporary workaround to wait for navigation drawer to close");
    }

    /** Open the side drawer, open the About activity, verify some text, press back to exit. **/
    public void openAndCloseAboutActivity()
    {
        logMsg("openAndCloseAboutActivity");
        onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
        onView(withId(R.id.aboutTextView)).perform(click());
        onView(withId(R.id.copyrightTextView)).check(matches(withText(R.string.copyright_info)));
        Espresso.pressBack();
        delay(500, "temporary workaround to wait for navigation drawer to close");
    }

    public void sendChatMessage()
    {
        onView(withId(R.id.chatMenuItem)).perform(click());
        onView(withId(R.id.chatMessageEditText)).perform(replaceText("Chat test message"));
        onView(withId(R.id.sendMessageButton)).perform(click());
        onView(withId(R.id.listItemsMenuItem)).perform(click());
    }

    public void openManageListsActivity()
    {
        logMsg("openManageListsActivity");
        onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
        onView(withId(R.id.manageListsTextView)).perform(click());
    }

    public void renameList2()
    {
        onView(withRecyclerView(R.id.listsRecyclerView).atPositionOnView(1, R.id.renameImageButton)).perform(click());
        onView(withId(R.id.listNameEditText)).perform(replaceText(LIST_NAME_2 +"_RENAMED"));
        onView(withId(R.id.okButton)).perform(click());
        onView(withText(LIST_NAME_2+"_RENAMED")).check(matches(isDisplayed()));
    }

    /** Delete list at specified position in the recyclerview.
     *  Precondition: must be on Manage Lists activity **/
    public void deleteList(int pos, String listName)
    {
        onView(withRecyclerView(R.id.listsRecyclerView).atPositionOnView(pos, R.id.deleteImageButton)).perform(click());
        String titleStr = String.format("Do you really want to delete the list '%s'?", listName);
        onView(withId(R.id.titleTextView)).check(matches(withText(titleStr)));
        onView(withId(R.id.okButton)).perform(click());
        onView(withText(listName)).check(doesNotExist());
    }

    public void verifyNoListsFoundMessage()
    {
        onView(withId(R.id.noListsFoundTextView)).check(matches(isDisplayed()));
        onView(withId(R.id.noListsFoundTextView)).check(matches(withText("No lists found.")));
    }

    /** Add a second list named LIST_NAME_2.
     *  Precondition: must be on the Manage Lists activity. **/
    public void addAndSwitchToSecondList()
    {
        logMsg("addAndSwitchToSecondList");
        onView(withId(R.id.addListButton)).perform(click());
        onView(withId(R.id.listNameEditText)).perform(replaceText(LIST_NAME_2));
        onView(withId(R.id.okButton)).perform(click());
        onView(withRecyclerView(R.id.listsRecyclerView).atPosition(1)).check(matches(hasDescendant(withText(LIST_NAME_2))));
        openListFromManageListsActivity(1);
        onView(allOf(withId(android.R.id.text1), withParent(withId(R.id.listNameTextView)))).check(matches(withText(LIST_NAME_2)));
    }

    public void openListFromManageListsActivity(int pos)
    {
        onView(withRecyclerView(R.id.listsRecyclerView).atPositionOnView(pos, R.id.listNameTextView)).perform(click());
        delay(500, "temporary workaround to wait for navigation drawer to close");
    }

    // Helper to work around areas of the test that need a delay. TODO: Add idling resources as appropriate to remove this.
    public void delay(int ms, String description)
    {
        logMsg("Delay for %d ms: %s", ms, description);
        try
        {
            Thread.sleep(ms);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /** Click on the list selection spinner, and choose the first list. **/
    public void switchToFirstList()
    {
        logMsg("switchToFirstList");
        delay(2000, "wait for onActivityResult() to load new list contents");
        onView(withId(R.id.listNameTextView)).perform(click());
        onData(allOf(is(instanceOf(PineTaskList.class)), withListName(LIST_NAME_1))).perform(click());
        delay(500, "wait for spinner popup to open");
    }

    public void addListItem(String description)
    {
        onView(withId(R.id.addItemButton)).perform(click());
        onView(withText(R.string.add_item)).check(matches(isDisplayed()));
        onView(withId(R.id.descriptionEditText)).perform(replaceText(description));
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()));
        closeSoftKeyboard();
        onView(withId(R.id.okButton)).perform(click());
    }

    public void verifyListItemsShown(String... descriptions)
    {
        logMsg("verifyListItemsShown(%d)", descriptions.length);
        for (int i=0;i<descriptions.length;i++)
        {
            logMsg("Verifying item '%s'", descriptions[i]);
            onView(withRecyclerView(R.id.itemsRecyclerView).atPosition(i)).check(matches(hasDescendant(withText(descriptions[i]))));
            logMsg("Verified item '%s'", descriptions[i]);
        }
    }

    public void logMsg(String msg, Object... args)
    {
        Logger.logMsg(getClass(), "*** [TEST] *** " + msg, args);
    }

    private RecyclerViewMatcher withRecyclerView(int id)
    {
        return new RecyclerViewMatcher(id);
    }

    public Matcher<PineTaskList> withListName(final String listName)
    {
        return new BoundedMatcher<PineTaskList, PineTaskList>(PineTaskList.class) {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("with list name '"+listName+"'");
            }

            @Override
            protected boolean matchesSafely(PineTaskList list)
            {
                return listName.equals(list.getName());
            }
        };
    }

    public Matcher<View> withTextColor(final int textColor)
    {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("with text color " + textColor);
            }

            @Override
            protected boolean matchesSafely(TextView item)
            {
                return item.getCurrentTextColor() == textColor;
            }
        };
    }

    public Matcher<View> withTextStrikethrough(boolean strikeThrough)
    {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("with strikethrough="+strikeThrough);
            }

            @Override
            protected boolean matchesSafely(TextView item)
            {
                return ((item.getPaint().getFlags() & STRIKE_THRU_TEXT_FLAG) == STRIKE_THRU_TEXT_FLAG) == strikeThrough;
            }
        };
    }
}
