package com.pinetask.app.list_items;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.UpdatedEvent;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ListItemsRepository extends LoggingBase
{
    List<PineTaskItemExt> mItems = new ArrayList<>();
    List<PineTaskItemExt> getItems() { return mItems; }
    Long mOriginalItemCount;
    Disposable mSubscription;
    private boolean mInitialLoadReported;
    Consumer<List<PineTaskItemExt>> mOnInitialLoadCompleted;

    public ListItemsRepository(DbHelper dbHelper, PineTaskList list, Consumer<List<PineTaskItemExt>> onInitialLoadCompleted, Consumer<ChildEventBase<PineTaskItemExt>> onChildEvent,
                               Consumer<Throwable> onError)
    {
        mOnInitialLoadCompleted = onInitialLoadCompleted;
        mSubscription = dbHelper.getListItemsCount(list.getId())
                .doOnSuccess(itemCount -> { mOriginalItemCount = itemCount; if (itemCount == 0) notifyInitialLoadCompleted(); } )
                .flatMapObservable(__ -> dbHelper.subscribeListItems(list.getId()))
                .doOnNext(childEvent -> childEvent.Item.setListId(list.getId()))
                .doOnNext(this::updateCache)
                .filter(this::reportInitialLoadAndFilterNewEvents)
                .doOnNext(childEvent -> childEvent.Item.setIsNewItem(true))
                .subscribe(onChildEvent, onError);
    }

    private void notifyInitialLoadCompleted() throws Exception
    {
        logMsg("Reporting initial load completed for %d items", mItems.size());
        mOnInitialLoadCompleted.accept(mItems);
        mInitialLoadReported = true;
    }

    /** If initial load not triggered yet and we've reached the expected number of items, invoke the onInitialLoadCompleted callback and set mInitialLoadReported flag to true.
     * @return True if this is a new item (after the initial load batch)
     */
    private boolean reportInitialLoadAndFilterNewEvents(ChildEventBase<PineTaskItemExt> childEvent) throws Exception
    {
        if (mInitialLoadReported)
        {
            return true;
        }
        else
        {
            if (mItems.size() == mOriginalItemCount) notifyInitialLoadCompleted();
            return false;
        }
    }

    /** Process added/deleted/uddated events and update mItems as needed. **/
    private void updateCache(ChildEventBase<PineTaskItemExt> childEvent)
    {
        if (childEvent instanceof DeletedEvent)
        {
            DeletedEvent<PineTaskItemExt> deletedEvent = (DeletedEvent<PineTaskItemExt>) childEvent;
            logMsg("DeletedEvent for %s", childEvent.Item.getId());
            mItems.remove(deletedEvent.Item);
        }
        else if (childEvent instanceof AddedEvent)
        {
            AddedEvent<PineTaskItemExt> addedEvent = (AddedEvent<PineTaskItemExt>) childEvent;
            mItems.add(addedEvent.Item);
        }
        else if (childEvent instanceof UpdatedEvent)
        {
            UpdatedEvent<PineTaskItemExt> updatedEvent = (UpdatedEvent<PineTaskItemExt>) childEvent;
            int i = mItems.indexOf(updatedEvent);
            if (i != -1) mItems.set(i, updatedEvent.Item);
        }
        logMsg("mLoadedCount=%d, mOriginalItemCount=%d", mItems.size(), mOriginalItemCount);
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }
}
