package com.pinetask.app;

import android.content.Context;
import android.graphics.Paint;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pinetask.common.Logger;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static android.graphics.Paint.STRIKE_THRU_TEXT_FLAG;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

public class TestHelper
{
    public static String TEST_EMAIL_ADDRESS = "test@pinetask.com";
    public static String USERNAME = "Pinetask Tester";
    public static String PASSWORD = "Password";
    public static String LIST_NAME = "***PineTaskTest List***";
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
                logMsg("Regisering idle transition callback");
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

    /** Verifies the "Welcome" message shows on the first tutorial slide, clicks the right arrow 7 times to view all slides, then clicks Done. **/
    public void viewAllTutorialSlides()
    {
        for (int i=0; i<8; i++) onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.done)).perform(click());
    }

    /** Verifies the SignupOrAnonymousLoginActivity displays correctly, and chooses the "Sign up later" option. **/
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
        onView(withId(R.id.listNameEditText)).perform(replaceText(LIST_NAME));
        onView(withId(R.id.okButton)).perform(click());
        addListItem(ITEM_1);
        addListItem(ITEM_2);
        verifyListItemsShown(ITEM_1, ITEM_2);
    }

    /**  Verify item 1 UI reflects unclaimed state. Click "Claim" button on item 1, verify UI state changes to show claimed state. **/
    public void claimItem1()
    {
        String initial = USERNAME.substring(0, 1).toUpperCase();
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimImageButton)).check(matches(isDisplayed()));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimedByTextView)).check(matches(not(isDisplayed())));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimImageButton)).perform(click());
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimedByTextView)).check(matches(isDisplayed()));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimedByTextView)).check(matches(withText(initial)));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.claimImageButton)).check(matches(not(isDisplayed())));
    }

    /** Verify item 1 UI reflects uncompleted state. Click "Dismiss" button on item 1, verify UI state changes to show item completed. **/
    public void dismissItem1()
    {
        int normalColor = ContextCompat.getColor(mContext, R.color.black);
        int completedColor = ContextCompat.getColor(mContext, R.color.black_30percent);
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.completedImageButton)).check(matches(isDisplayed()));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.itemDescriptionTextView)).check(matches(withTextStrikethrough(false)));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.itemDescriptionTextView)).check(matches(withTextColor(normalColor)));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.completedImageButton)).perform(click());
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.completedImageButton)).check(matches(not(isDisplayed())));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.itemDescriptionTextView)).check(matches(withTextStrikethrough(true)));
        onView(withRecyclerView(R.id.itemsRecyclerView).atPositionOnView(0, R.id.itemDescriptionTextView)).check(matches(withTextColor(completedColor)));
    }

    public void addListItem(String description)
    {
        onView(withId(R.id.addItemButton)).perform(click());
        onView(withText(R.string.add_item)).check(matches(isDisplayed()));
        onView(withId(R.id.descriptionEditText)).perform(replaceText(description));
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()));
        onView(withId(R.id.okButton)).perform(click());
    }

    public void verifyListItemsShown(String... descriptions)
    {
        for (int i=0;i<descriptions.length;i++)
        {
            logMsg("Verifying item '%s'", descriptions[i]);
            onView(withRecyclerView(R.id.itemsRecyclerView).atPosition(i)).check(matches(hasDescendant(withText(descriptions[i]))));
            logMsg("Verified item '%s'", descriptions[i]);
        }
    }


    private void logMsg(String msg, Object... args)
    {
        Logger.logMsg(getClass(), "*** [TEST] *** " + msg, args);
    }

    private RecyclerViewMatcher withRecyclerView(int id)
    {
        return new RecyclerViewMatcher(id);
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
