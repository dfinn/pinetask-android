package com.pinetask.app;

import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.chat.ChatPresenter;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.common.SoundManager;
import com.pinetask.app.common.UserModule;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.hints.HintManager;
import com.pinetask.app.list_items.ListItemsPresenter;
import com.pinetask.app.list_members.MembersPresenter;
import com.pinetask.app.main.MainActivityPresenter;

import javax.inject.Named;

/** UserModule replacement for use in testing. **/
public class FakeUserModule extends UserModule
{
    public FakeUserModule()
    {
        super("12345");
    }

    @Override
    public String providesUserId()
    {
        return super.providesUserId();
    }

    @Override
    public ActiveListManager providesActiveListManager(PrefsManager prefsManager, DbHelper dbHelper, @Named("user_id") String userId, HintManager hintManager)
    {
        return super.providesActiveListManager(prefsManager, dbHelper, userId, hintManager);
    }

    @Override
    public MainActivityPresenter providesMainActivityPresenter(DbHelper dbHelper, @Named("user_id") String userId, PrefsManager prefsManager, PineTaskApplication pineTaskApplication, ActiveListManager activeListManager)
    {
        return super.providesMainActivityPresenter(dbHelper, userId, prefsManager, pineTaskApplication, activeListManager);
    }

    @Override
    public ListItemsPresenter providesListItemsPresenter(PineTaskApplication application, DbHelper dbHelper, ActiveListManager activeListManager, @Named("user_id") String userId, SoundManager soundManager)
    {
        return super.providesListItemsPresenter(application, dbHelper, activeListManager, userId, soundManager);
    }

    @Override
    public MembersPresenter providesMembersPresenter(DbHelper dbHelper, PineTaskApplication pineTaskApplication, ActiveListManager activeListManager, @Named("user_id") String userId)
    {
        return new FakeMembersPresenter();
    }

    @Override
    public ChatPresenter providesChatPresenter(@Named("user_id") String userId, ActiveListManager activeListManager, DbHelper dbHelper, PineTaskApplication application)
    {
        return super.providesChatPresenter(userId, activeListManager, dbHelper, application);
    }
}
