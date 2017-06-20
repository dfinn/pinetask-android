package com.pinetask.app.common;

import com.google.firebase.database.FirebaseDatabase;

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
    FirebaseDatabase providesFirebaseDb()
    {
        return FirebaseDatabase.getInstance();
    }
}
