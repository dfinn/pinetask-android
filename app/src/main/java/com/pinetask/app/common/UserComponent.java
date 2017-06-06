package com.pinetask.app.common;

import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.chat.ChatFragment;
import com.pinetask.app.chat.ChatPresenter;
import com.pinetask.app.list_members.MembersAdapter;
import com.pinetask.app.list_members.MembersFragment;
import com.pinetask.app.list_members.MembersPresenter;
import com.pinetask.app.main.MainActivity;
import com.pinetask.app.main.MainActivityPresenter;
import com.pinetask.app.main.PurgeCompletedItemsDialogFragment;

import dagger.Subcomponent;

/** Component for Dagger dependency injection: holds objects that are available while a user is logged in (MainActivity, etc) **/
@UserScope
@Subcomponent(modules={UserModule.class})
public interface UserComponent
{
    ActiveListManager activeListManager();
    MainActivityPresenter mainActivityPresenter();
    ChatPresenter chatPresenter();
    MembersPresenter membersPresenter();

    void inject(MainActivity target);
    void inject(PurgeCompletedItemsDialogFragment target);
    void inject(MembersFragment target);
    void inject(MembersAdapter target);
    void inject(ChatFragment target);
}
