package com.pinetask.app;

import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.google.firebase.FirebaseApp;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.UserModule;
import com.pinetask.app.list_members.MembersFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

/** Unit test of the MembersFragment **/
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, application = FakePineTaskApplication.class)
public class MembersFragmentTest
{
    @Test
    public void addMemberButtonHiddenWhenFragmentCreated() throws Exception
    {
        PineTaskApplication.getInstance().createUserComponent(new FakeUserModule());
        MembersFragment membersFragment = MembersFragment.newInstance();
        startFragment(membersFragment);
        FloatingActionButton addMemberButton = (FloatingActionButton) membersFragment.getView().findViewById(R.id.addMemberButton);
        assertEquals(View.GONE, addMemberButton.getVisibility());
        membersFragment.setAddButtonVisible(true);
        assertEquals(View.VISIBLE, addMemberButton.getVisibility());
    }
}