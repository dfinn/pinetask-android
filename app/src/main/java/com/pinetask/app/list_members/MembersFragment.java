package com.pinetask.app.list_members;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskFragment;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.main.InviteManager;
import com.pinetask.app.main.MainActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Fragment which shows all members of the current list.
 *  - The list owner is shown first and identified as the owner.
 *  - Other list members have a 'Delete' button next to them, to revoke their access to the list.
 *  - Pressing the floating action button will begin the process to add a new list member (sending an invite)
 **/
public class MembersFragment extends PineTaskFragment implements MembersView
{
    @Inject MembersPresenter mPresenter;
    @BindView(R.id.membersRecyclerView) RecyclerView mMembersRecyclerView;
    @BindView(R.id.addMemberButton) FloatingActionButton mAddMemberButton;
    MembersAdapter mAdapter;

    public static MembersFragment newInstance()
    {
        MembersFragment membersFragment = new MembersFragment();
        Bundle args = new Bundle();
        membersFragment.setArguments(args);
        return membersFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.members_fragment, container, false);
        ButterKnife.bind(this, view);
        PineTaskApplication.getInstance().getUserComponent().inject(this);
        logMsg("onCreateView: creating membersAdapter");
        mAdapter = new MembersAdapter(getActivity());
        mMembersRecyclerView.setAdapter(mAdapter);
        mMembersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mPresenter.attachView(this);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mPresenter.detachView();
    }

    @Override
    public void addListMember(MemberInfo memberInfo)
    {
        mAdapter.addUser(memberInfo);
    }

    @Override
    public void removeListMember(String userId)
    {
        mAdapter.removeUser(userId);
    }

    @Override
    public void clearListDisplay()
    {
        mAdapter.clear();
    }

    @Override
    public void setListVisible(boolean visible)
    {
        mMembersRecyclerView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setAddButtonVisible(boolean visible)
    {
        mAddMemberButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.addMemberButton)
    public void addMemberButtonOnClick(View __)
    {
        mPresenter.onAddMemberButtonClicked();
    }

    @Override
    public void launchInviteProcess(PineTaskList pineTaskList)
    {
        MainActivity mainActivity = (MainActivity) getActivity();
        InviteManager inviteManager = mainActivity.getInviteManager();
        inviteManager.sendInvite(pineTaskList);
    }

    @Override
    public void showError(String message, Object... args)
    {
        showUserMessage(false, message, args);
    }

}
