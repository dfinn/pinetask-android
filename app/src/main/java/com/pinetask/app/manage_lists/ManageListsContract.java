package com.pinetask.app.manage_lists;

import com.pinetask.app.PineTaskList;
import com.pinetask.app.PineTaskListWithCollaborators;

import java.util.List;

public class ManageListsContract
{
    interface IManageListsPresenter
    {
        void attachView(IManageListsView view);
        void detachView(boolean isFinishing);
    }

    interface IManageListsView
    {
        void showLists(List<PineTaskListWithCollaborators> lists);
        void addList(PineTaskListWithCollaborators list);
        void removeList(String listId);
        void updateList(PineTaskListWithCollaborators list);
        void onLoadError();
        void showLoadStatus(int stringResIds);
        void hideLoadStatus();
    }
}
