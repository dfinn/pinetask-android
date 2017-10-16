package com.pinetask.app.common;

import com.pinetask.app.db.DbHelper;
import com.pinetask.app.db.DbHelperImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** Dagger2 module for application-wide dependencies. **/
@Module
public class AppModule
{
    PineTaskApplication mPineTaskApplication;

    public AppModule(PineTaskApplication pineTaskApplication)
    {
        mPineTaskApplication = pineTaskApplication;
    }

    @Provides
    @Singleton
    public PineTaskApplication providesApplication() { return mPineTaskApplication; }

    @Provides
    @Singleton
    public DbHelper providesDbHelper() { return new DbHelperImpl(); }
}
