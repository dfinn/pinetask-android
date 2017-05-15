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
import com.pinetask.app.common.PineTaskFragment;
import com.pinetask.app.main.InviteManager;
import com.pinetask.app.main.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Fragment which shows all members of the current list.
 *  - The list owner is shown first and identified as the owner.
 *  - Other list members have a 'Delete' button next to them, to revoke their access to the list.
 *  - Pressing the floating action button will begin the process to add a new list member (sending an invite)
 **/
public class MembersFragment extends PineTaskFragment implements MembersContract.IMembersView
{
    MembersPresenter mPresenter;
    MembersAdapter mAdapter;

    public static String USER_ID_KEY = "UserId";

    @BindView(R.id.membersRecyclerView) RecyclerView mMembersRecyclerView;
    @BindView(R.id.addMemberButton) FloatingActionButton mAddMemberButton;

    public static MembersFragment newInstance(String userId)
    {
        MembersFragment membersFragment = new MembersFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID_KEY, userId);
        membersFragment.setArguments(args);
        return membersFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.members_fragment, container, false);
        ButterKnife.bind(this, view);
        mAdapter = new MembersAdapter((MainActivity)getActivity());
        mMembersRecyclerView.setAdapter(mAdapter);
        mMembersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        String userId = getArguments().getString(USER_ID_KEY);
        mPresenter.attachView(this, userId);
        return view;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
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
    public void addMemberButtonOnClick(View view)
    {
        mPresenter.onAddMemberButtonClicked();
    }

    @Override
    public void launchInviteProcess(String listId)
    {
        MainActivity mainActivity = (MainActivity) getActivity();
        InviteManager inviteManager = mainActivity.getInviteManager();
        inviteManager.sendInvite(listId);
    }

}
