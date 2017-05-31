package com.pinetask.app.common;

import com.pinetask.app.chat.ChatMessagesAdapter;
import com.pinetask.app.db.ListLoader;
import com.pinetask.app.db.RxListLoader;
import com.pinetask.app.launch.TutorialActivity;
import com.pinetask.app.list_items.ListItemAdapter;
import com.pinetask.app.list_members.MembersAdapter;
import com.pinetask.app.list_members.MembersPresenter;
import com.pinetask.app.main.InviteManager;
import com.pinetask.app.main.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

/** Dagger2 component for application-wide dependencies. **/
@Singleton
@Component(modules={AppModule.class})
public interface AppComponent
{
    /** UserComponent/Module are scoped to exist only while a user is logged in (MainActivity, etc) **/
    UserComponent userComponent(UserModule userModule);

    void inject(PineTaskActivity target);
    void inject(PineTaskFragment target);
    void inject(PineTaskDialogFragment target);
    void inject(ChatMessagesAdapter target);
    void inject(ListItemAdapter target);
    void inject(TutorialActivity target);
    void inject(ListLoader target);
    void inject(RxListLoader target);
    void inject(InviteManager target);
    void inject(MembersAdapter target);
    void inject(MembersPresenter target);
}
