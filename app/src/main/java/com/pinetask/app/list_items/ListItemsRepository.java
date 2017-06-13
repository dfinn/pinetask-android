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
    List<PineTaskItem> mItems = new ArrayList<>();
    List<PineTaskItem> getItems() { return mItems; }
    Long mOriginalItemCount;
    Disposable mSubscription;

    public ListItemsRepository(DbHelper dbHelper, PineTaskList list, Consumer<List<PineTaskItem>> initialLoadCompleted, Consumer<ChildEventBase<PineTaskItem>> onChildEvent,
                               Consumer<Throwable> onError)
    {
        mSubscription = dbHelper.getListItemsCount(list.getId())
                .map(itemCount -> mOriginalItemCount = itemCount)
                .map(itemCount -> list.getId())
                .flatMapObservable(dbHelper::subscribeListItems)
                .map(this::processChildEvent)
                .map(childEvent -> { if (mItems.size() == mOriginalItemCount) initialLoadCompleted.accept(mItems); return childEvent; })
                .filter(childEvent -> mItems.size() > mOriginalItemCount)
                .subscribe(onChildEvent, onError);
    }

    private ChildEventBase<PineTaskItem> processChildEvent(ChildEventBase<PineTaskItem> childEvent)
    {
        if (childEvent instanceof DeletedEvent)
        {
            DeletedEvent<PineTaskItem> deletedEvent = (DeletedEvent<PineTaskItem>) childEvent;
            removeItem(deletedEvent.Item);
        }
        else if (childEvent instanceof AddedEvent)
        {
            AddedEvent<PineTaskItem> addedEvent = (AddedEvent<PineTaskItem>) childEvent;
            mItems.add(addedEvent.Item);
        }
        else if (childEvent instanceof UpdatedEvent)
        {
            UpdatedEvent<PineTaskItem> updatedEvent = (UpdatedEvent<PineTaskItem>) childEvent;
            updateItem(updatedEvent.Item);
        }
        logMsg("mLoadedCount=%d, mOriginalItemCount=%d", mItems.size(), mOriginalItemCount);
        return childEvent;
    }

    private void removeItem(PineTaskItem itemToRemove)
    {
        Iterator<PineTaskItem> iter = mItems.iterator();
        while (iter.hasNext())
        {
            PineTaskItem item = iter.next();
            if (item.getKey().equals(itemToRemove.getKey()))
            {
                iter.remove();
                break;
            }
        }
    }

    private void updateItem(PineTaskItem itemToUpdate)
    {
        for (int i=0 ; i<mItems.size() ; i++)
        {
            PineTaskItem item = mItems.get(i);
            if (item.getKey().equals(itemToUpdate.getKey()))
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
