package com.pinetask.app.common;

import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.chat.ChatFragment;
import com.pinetask.app.chat.ChatPresenter;
import com.pinetask.app.list_items.AddOrEditItemDialog;
import com.pinetask.app.list_items.ListItemAdapter;
import com.pinetask.app.list_items.ListItemsFragment;
import com.pinetask.app.list_items.ListItemsPresenter;
import com.pinetask.app.list_members.MembersAdapter;
import com.pinetask.app.list_members.MembersFragment;
import com.pinetask.app.list_members.MembersPresenter;
import com.pinetask.app.main.CostInputDialogFragment;
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
    ListItemsPresenter listItemsPresenter();
    ChatPresenter chatPresenter();
    MembersPresenter membersPresenter();
    SoundManager soundManager();

    void inject(MainActivity target);
    void inject(PurgeCompletedItemsDialogFragment target);
    void inject(MembersFragment target);
    void inject(ListItemsFragment target);
    void inject(ChatFragment target);
    void inject(MembersAdapter target);
    void inject(AddOrEditItemDialog target);
    void inject(ListItemAdapter target);
    void inject(CostInputDialogFragment target);
}
