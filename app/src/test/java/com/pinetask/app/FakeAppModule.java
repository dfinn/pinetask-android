package com.pinetask.app;

import com.pinetask.app.common.AppModule;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.db.DbHelper;

public class FakeAppModule extends AppModule
{
    public FakeAppModule(PineTaskApplication pineTaskApplication)
    {
        super(pineTaskApplication);
    }

    @Override
    public PineTaskApplication providesApplication()
    {
        return super.providesApplication();
    }

    @Override
    public DbHelper providesDbHelper()
    {
        return new FakeDbHelper();
    }
}
