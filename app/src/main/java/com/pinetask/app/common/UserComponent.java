package com.pinetask.app.common;

import com.pinetask.app.main.MainActivity;
import com.pinetask.app.main.PurgeCompletedItemsDialogFragment;

import dagger.Subcomponent;

/** Component for Dagger dependency injection: holds objects that are available while a user is logged in (MainActivity, etc) **/
@UserScope
@Subcomponent(modules={UserModule.class})
public interface UserComponent
{
    void inject(MainActivity target);
    void inject(PurgeCompletedItemsDialogFragment target);
}
