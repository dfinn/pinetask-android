package com.pinetask.app.list_items;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.UpdatedEvent;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

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
            int i = mItems.indexOf(updatedEvent);
            if (i != -1) mItems.set(i, updatedEvent.Item);
        }
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }
}
