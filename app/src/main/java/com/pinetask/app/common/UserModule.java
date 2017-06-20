package com.pinetask.app.common;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.chat.ChatPresenter;
import com.pinetask.app.chat.ChatPresenterImpl;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.list_items.ListItemsPresenter;
import com.pinetask.app.list_items.ListItemsPresenterImpl;
import com.pinetask.app.list_members.MembersPresenter;
import com.pinetask.app.list_members.MembersPresenterImpl;
import com.pinetask.app.main.MainActivityPresenter;
import com.pinetask.app.main.MainActivityPresenterImpl;
import com.pinetask.common.Logger;
import com.pinetask.common.LoggingBase;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/** Module for Dagger dependency injection: holds objects that are available while a user is logged in (MainActivity, etc) **/
@Module
public class UserModule extends LoggingBase
{
    private String mUserId;

    public UserModule(String userId)
    {
        Logger.logMsg(getClass(), "Creating UserModule for user ID %s", userId);
        mUserId = userId;

        // Set user information for Crashlytics
        Crashlytics.setUserIdentifier(mUserId);
    }

    /** Provides the ID of the currently logged in user. **/
    @Provides
    @UserScope
    @Named("user_id")
    public String providesUserId()
    {
        return mUserId;
    }

    @Provides
    @UserScope
    public ActiveListManager providesActiveListManager(PrefsManager prefsManager, DbHelper dbHelper, @Named("user_id") String userId)
    {
        logMsg("creating ActiveListManager");
        return new ActiveListManager(prefsManager, dbHelper, userId);
    }

    @Provides
    @UserScope
    public MainActivityPresenter providesMainActivityPresenter(DbHelper dbHelper, @Named("user_id") String userId, PrefsManager prefsManager,
                                                               PineTaskApplication pineTaskApplication, ActiveListManager activeListManager)
    {
        logMsg("creating MainActivityPresenter");
        return new MainActivityPresenterImpl(dbHelper, userId, prefsManager, pineTaskApplication, activeListManager);
    }

    @Provides
    @UserScope
    public ListItemsPresenter providesListItemsPresenter(PineTaskApplication application, DbHelper dbHelper, ActiveListManager activeListManager, @Named("user_id") String userId)
    {
        return new ListItemsPresenterImpl(application, dbHelper, activeListManager, userId);
    }

    @Provides
    @UserScope
    public MembersPresenter providesMembersPresenter(DbHelper dbHelper, PineTaskApplication pineTaskApplication, ActiveListManager activeListManager, @Named("user_id") String userId)
    {
        logMsg("Creating MembersPresenter");
        return new MembersPresenterImpl(dbHelper, pineTaskApplication, activeListManager, userId);
    }

    @Provides
    @UserScope
    public ChatPresenter providesChatPresenter(@Named("user_id") String userId, ActiveListManager activeListManager, FirebaseDatabase db, DbHelper dbHelper, PineTaskApplication application)
    {
        logMsg("Creating ChatPresenter");
        return new ChatPresenterImpl(userId, activeListManager, db, dbHelper, application);
    }
}
