package com.pinetask.app.list_items;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.UpdatedEvent;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.common.LoggingBase;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ListItemsRepository extends LoggingBase
{
    List<PineTaskItemExt> mItems = new ArrayList<>();
    List<PineTaskItemExt> getItems() { return mItems; }
    Long mLastItemTimestamp;
    Disposable mSubscription;

    public ListItemsRepository(DbHelper dbHelper, PineTaskList list, Consumer<ChildEventBase<PineTaskItemExt>> onChildEvent, Consumer<Throwable> onError)
    {
        mSubscription = dbHelper.getLastListItemTimestamp(list.getId())
                .doOnSubscribe(__ -> logMsg("Subscription has been created"))
                .doOnSuccess(lastItemTimestamp -> logMsg("Last item's timestamp for list %s is %s", list.getId(), getTimestamp(lastItemTimestamp)))
                .doOnSuccess(lastOpenTimestamp -> mLastItemTimestamp = lastOpenTimestamp)
                .flatMapObservable(__ -> dbHelper.subscribeListItems(list.getId()))
                .doOnNext(childEvent -> childEvent.Item.setListId(list.getId()))
                .filter(event -> event.Item.getId() != null)
                .filter(event -> !((event instanceof AddedEvent) && (mItems.contains(event.Item))))
                .filter(event -> !((event instanceof UpdatedEvent) && containsIdenticalItem(event.Item)))
                .doOnNext(this::updateCache)
                .doOnNext(childEvent -> childEvent.Item.setIsNewItem(childEvent.Item.getCreatedAtMs() > mLastItemTimestamp))
                .doOnNext(childEvent -> logMsg("Loaded item %s, createdAt=%s, isNew=%b", childEvent.Item.getId(), getTimestamp(childEvent.Item.getCreatedAtMs()), childEvent.Item.getIsNewItem()))
                .subscribe(onChildEvent, onError);
    }

    private String getTimestamp(Long ms)
    {
        return String.format("%d (%s)", ms, new DateTime(ms).toString(DateTimeFormat.shortDateTime()));
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
            int i = mItems.indexOf(updatedEvent.Item);
            if (i != -1) mItems.set(i, updatedEvent.Item);
        }
    }

    /** Returns true if the list contains an item with the same ID and content as the specified item. **/
    private boolean containsIdenticalItem(PineTaskItemExt item)
    {
        int i = mItems.indexOf(item);
        if (i != -1)
        {
            PineTaskItemExt existingItem = mItems.get(i);
            return item.exactlyEqual(existingItem);
        }
        else
        {
            return false;
        }
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }
}
