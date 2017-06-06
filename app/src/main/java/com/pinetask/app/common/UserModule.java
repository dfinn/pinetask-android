package com.pinetask.app.common;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.chat.ChatPresenter;
import com.pinetask.app.chat.ChatPresenterImpl;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.list_members.MembersPresenter;
import com.pinetask.app.list_members.MembersPresenterImpl;
import com.pinetask.app.main.MainActivityPresenter;
import com.pinetask.app.main.MainActivityPresenterImpl;
import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/** Module for Dagger dependency injection: holds objects that are available while a user is logged in (MainActivity, etc) **/
@Module
public class UserModule
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
    @Named("user_id")
    public String providesUserId()
    {
        return mUserId;
    }

    @Provides
    public ActiveListManager providesActiveListManager(PrefsManager prefsManager, DbHelper dbHelper, @Named("user_id") String userId)
    {
        return new ActiveListManager(prefsManager, dbHelper, userId);
    }

    @Provides
    public MainActivityPresenter providesMainActivityPresenter(DbHelper dbHelper, @Named("user_id") String userId, PrefsManager prefsManager,
                                                               PineTaskApplication pineTaskApplication, ActiveListManager activeListManager)
    {
        return new MainActivityPresenterImpl(dbHelper, userId, prefsManager, pineTaskApplication, activeListManager);
    }

    @Provides
    public MembersPresenter providesMembersPresenter(DbHelper dbHelper, PineTaskApplication pineTaskApplication, ActiveListManager activeListManager, @Named("user_id") String userId)
    {
        return new MembersPresenterImpl(dbHelper, pineTaskApplication, activeListManager, userId);
    }

    @Provides
    public ChatPresenter providesChatPresenter(@Named("user_id") String userId, ActiveListManager activeListManager, FirebaseDatabase db, DbHelper dbHelper,
                                               Bus eventBus, PineTaskApplication application)
    {
        return new ChatPresenterImpl(userId, activeListManager, db, dbHelper, eventBus, application);
    }
}
