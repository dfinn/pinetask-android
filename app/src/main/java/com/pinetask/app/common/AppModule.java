package com.pinetask.app.common;

import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

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
    PineTaskApplication providesApplication() { return mPineTaskApplication; }

    @Provides
    @Singleton
    Bus providesEventBus()
    {
        Logger.logMsg(getClass(), "Creating new event bus");
        return new Bus();
    }

    @Provides
    @Singleton
    FirebaseDatabase providesFirebaseDb()
    {
        return FirebaseDatabase.getInstance();
    }
}
