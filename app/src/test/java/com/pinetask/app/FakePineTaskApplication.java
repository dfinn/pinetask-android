package com.pinetask.app;

import com.pinetask.app.common.AppModule;
import com.pinetask.app.common.DaggerAppComponent;
import com.pinetask.app.common.PineTaskApplication;

public class FakePineTaskApplication extends PineTaskApplication
{
    @Override
    protected void createAppModule()
    {
        logMsg("Creating FakeAppModule");
        mAppComponent = DaggerAppComponent.builder().appModule(new FakeAppModule(this)).build();
    }
}
