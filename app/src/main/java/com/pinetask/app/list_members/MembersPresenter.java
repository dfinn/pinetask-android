package com.pinetask.app.list_members;

public interface MembersPresenter
{
    void attachView(MembersView view);
    void detachView();
    void shutdown();
    void onAddMemberButtonClicked();
}