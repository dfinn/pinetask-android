package com.pinetask.app.manage_lists;

import com.pinetask.app.PineTaskList;
import com.pinetask.app.PineTaskListWithCollaborators;
import com.pinetask.app.R;
import com.pinetask.app.RxListLoader;
import com.pinetask.app.RxListLoaderCallbacks;
import com.pinetask.common.LoggingBase;

import java.util.List;

public class ManageListsPresenter extends LoggingBase implements ManageListsContract.IManageListsPresenter, RxListLoaderCallbacks
{
    ManageListsContract.IManageListsView mView;
    RxListLoader mListLoader;

    public ManageListsPresenter(String userId)
    {
        if (mView != null) mView.showLoadStatus(R.string.loading_lists);
        mListLoader = new RxListLoader(userId, this);
    }

    /**** Begin implementation of RxListLoaderCallbacks. ********/
    @Override
    public void onListsLoaded(List<PineTaskListWithCollaborators> lists)
    {
        if (mView!=null) mView.showLists(lists);
        onListCountUpdated();
    }

    @Override
    public void onListAdded(PineTaskListWithCollaborators list)
    {
        if (mView != null) mView.addList(list);
        onListCountUpdated();
    }

    @Override
    public void onListDeleted(String listId)
    {
        if (mView!=null) mView.removeList(listId);
    }

    @Override
    public void onListUpdated(PineTaskListWithCollaborators list)
    {
        if (mView != null) mView.updateList(list);
    }

    @Override
    public void onError(Throwable ex)
    {
        logException(getClass(), ex);
        if (mView != null) mView.onLoadError();
    }

    /**** End implementation of RxListLoaderCallbacks. ********/



    private void onListCountUpdated()
    {
        if (mView != null)
        {
            if (mListLoader.getLists().size()==0) mView.showLoadStatus(R.string.no_lists_found);
            else mView.hideLoadStatus();
        }
    }

    @Override
    public void attachView(ManageListsContract.IManageListsView view)
    {
        logMsg("Attaching view");
        mView = view;
        if (mListLoader.getLists()!=null) onListsLoaded(mListLoader.getLists());
    }

    @Override
    public void detachView(boolean isFinishing)
    {
        logMsg("Detaching view");
        mView = null;
        if (isFinishing)
        {
            logMsg("detachView: shutting down RxListLoader");
            mListLoader.shutdown();
        }
    }
}
