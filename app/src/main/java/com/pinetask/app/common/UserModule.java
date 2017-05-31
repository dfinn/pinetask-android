package com.pinetask.app.common;

import com.crashlytics.android.Crashlytics;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.main.MainActivityContract;
import com.pinetask.app.main.MainActivityPresenter;
import com.pinetask.common.Logger;

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

    /** Provides the MainActivityPresenter **/
    @Provides
    public MainActivityContract.IMainActivityPresenter providesMainActivityPresenter(DbHelper dbHelper, @Named("user_id") String userId, PrefsManager prefsManager)
    {
        return new MainActivityPresenter(dbHelper, userId, prefsManager);
    }
}
