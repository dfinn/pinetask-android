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
                .map(itemCount -> mOriginalItemCount = itemCount)
                .map(itemCount -> list.getId())
                .flatMapObservable(dbHelper::subscribeListItems)
                .map(childEvent -> { childEvent.Item.setListId(list.getId()); return childEvent; } )
                .map(this::processChildEvent)
                .filter(this::reportInitialLoadAndFilterNewEvents)
                .map(childEvent -> { childEvent.Item.setIsNewItem(true); return childEvent; } )
                .subscribe(onChildEvent, onError);
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
            if (mItems.size() == mOriginalItemCount)
            {
                logMsg("Reporting initial load completed for %d items", mItems.size());
                mOnInitialLoadCompleted.accept(mItems);
                mInitialLoadReported = true;
            }
            return false;
        }
    }

    private ChildEventBase<PineTaskItemExt> processChildEvent(ChildEventBase<PineTaskItemExt> childEvent)
    {
        if (childEvent instanceof DeletedEvent)
        {
            DeletedEvent<PineTaskItemExt> deletedEvent = (DeletedEvent<PineTaskItemExt>) childEvent;
            logMsg("DeletedEvent for %s", childEvent.Item.getId());
            removeItem(deletedEvent.Item);
        }
        else if (childEvent instanceof AddedEvent)
        {
            AddedEvent<PineTaskItemExt> addedEvent = (AddedEvent<PineTaskItemExt>) childEvent;
            mItems.add(addedEvent.Item);
        }
        else if (childEvent instanceof UpdatedEvent)
        {
            UpdatedEvent<PineTaskItemExt> updatedEvent = (UpdatedEvent<PineTaskItemExt>) childEvent;
            updateItem(updatedEvent.Item);
        }
        logMsg("mLoadedCount=%d, mOriginalItemCount=%d", mItems.size(), mOriginalItemCount);
        return childEvent;
    }

    private void removeItem(PineTaskItemExt itemToRemove)
    {
        Iterator<PineTaskItemExt> iter = mItems.iterator();
        while (iter.hasNext())
        {
            PineTaskItemExt item = iter.next();
            if (item.getId().equals(itemToRemove.getId()))
            {
                iter.remove();
                break;
            }
        }
    }

    private void updateItem(PineTaskItemExt itemToUpdate)
    {
        for (int i=0 ; i<mItems.size() ; i++)
        {
            PineTaskItemExt item = mItems.get(i);
            if (item.getId().equals(itemToUpdate.getId()))
            {
                mItems.set(i, itemToUpdate);
                break;
            }
        }
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }

}
