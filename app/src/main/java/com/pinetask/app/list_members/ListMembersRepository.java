package com.pinetask.app.list_members;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.common.LoggingBase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ListMembersRepository extends LoggingBase
{
    private List<MemberInfo> mCurrentListMembers;
    public List<MemberInfo> getListMembers() { return mCurrentListMembers; }
    private Disposable mSubscription;

    /** Attach listener to get user IDs for collaborators of the specified list, emitting added/deleted events for MemberInfo objects which are then passed to
     *  the view (if still attached) to add or remove the member from the displayed list. **/
    ListMembersRepository(DbHelper dbHelper, PineTaskList pineTaskList, String currentUserId, Consumer<ChildEventBase<MemberInfo>> onChildEvent, Consumer<Throwable> onError)
    {
        mCurrentListMembers = new ArrayList<>();

        mSubscription = dbHelper.subscribeMembersAddedOrDeletedEvents(pineTaskList.getId())
                .flatMapSingle(addedOrDeletedEvent -> getMemberInfoForUserId(dbHelper, addedOrDeletedEvent, currentUserId, pineTaskList.getOwnerId()))
                .filter(event -> !mCurrentListMembers.contains(event.Item))
                .doOnNext(event ->
                {
                    if (event instanceof AddedEvent)
                    {
                        mCurrentListMembers.add(event.Item);
                    }
                    else
                    {
                        mCurrentListMembers.remove(event.Item);
                    }
                })
                .doOnSubscribe(__ -> logMsg("Subscription created to member added/deleted events for list %s", pineTaskList.getId()))
                .subscribe(onChildEvent, onError);
    }

    /** Look up username for the specified userId, and convert the "user ID added or deleted" event into a "MemberInfo added or deleted" event. **/
    private Single<ChildEventBase<MemberInfo>> getMemberInfoForUserId(DbHelper dbHelper, ChildEventBase<String> userAddedOrDeletedEvent, String currentUserId, String currentListOwnerId)
    {
        String userId = userAddedOrDeletedEvent.Item;
        return dbHelper.getUserNameSingle(userId).map(userName ->
        {
            // The member can only be deleted if the current user is the list owner.  Owner can never be deleted.
            boolean canBeDeleted = (currentListOwnerId.equals(currentUserId)) && (!userId.equals(currentListOwnerId));
            MemberInfo memberInfo = new MemberInfo(userName, userId, (userId.equals(currentListOwnerId)), canBeDeleted);
            if (userAddedOrDeletedEvent instanceof AddedEvent) return new AddedEvent<>(memberInfo);
            else return new DeletedEvent<>(memberInfo);
        });
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }
}
