package com.pinetask.app;

import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PineTaskListWithCollaborators;
import com.pinetask.app.common.UserMessageListener;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.list_items.PineTaskItemExt;
import com.pinetask.app.main.InviteInfo;
import com.pinetask.app.manage_lists.StartupMessage;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public class FakeDbHelper implements DbHelper
{
    @Override
    public Completable deleteList(String listId)
    {
        return null;
    }

    @Override
    public Observable<String> getListCollaborators(String listId)
    {
        return null;
    }

    @Override
    public Single<String> getUserNameSingle(String userId)
    {
        return null;
    }

    @Override
    public Observable<ChatMessage> populateUserName(ChatMessage chatMessage)
    {
        return null;
    }

    @Override
    public Observable<String> getUserNameObservable(String userId)
    {
        return null;
    }

    @Override
    public Completable setUserName(String userId, String newUserName)
    {
        return null;
    }

    @Override
    public Completable setIsAnonymous(String userId, boolean isAnonymous)
    {
        return null;
    }

    @Override
    public Single<Boolean> getIsAnonymous(String userId)
    {
        return null;
    }

    @Override
    public Observable<String> getListIdsForUser(String userId)
    {
        return Observable.empty();
    }

    @Override
    public Single<Boolean> canAccessList(String userId, String listId)
    {
        return null;
    }

    @Override
    public Observable<ChildEventBase<String>> getListAddedOrDeletedEvents(String userId)
    {
        return null;
    }

    @Override
    public Observable<ChildEventBase<String>> subscribeMembersAddedOrDeletedEvents(String listId)
    {
        return null;
    }

    @Override
    public Single<String> getListName(String listId)
    {
        return null;
    }

    @Override
    public void createInvite(String listId, String inviteId)
    {

    }

    @Override
    public Completable verifyInviteExists(InviteInfo inviteInfo)
    {
        return null;
    }

    @Override
    public Completable deleteInvite(InviteInfo inviteInfo)
    {
        return null;
    }

    @Override
    public Completable addUserAsCollaboratorToList(InviteInfo inviteInfo, String userId)
    {
        return null;
    }

    @Override
    public Completable addListToUserLists(String listId, String userId, String accessType)
    {
        return null;
    }

    @Override
    public Completable revokeAccessToList(String listId, String userId)
    {
        return null;
    }

    @Override
    public Completable createList(String ownerId, String listName)
    {
        return null;
    }

    @Override
    public Single<PineTaskList> getPineTaskList(String listId)
    {
        return null;
    }

    @Override
    public Observable<PineTaskList> tryGetPineTaskList(String listId)
    {
        return null;
    }

    @Override
    public Single<PineTaskListWithCollaborators> getPineTaskListWithCollaborators(PineTaskList list)
    {
        return null;
    }

    @Override
    public Observable<PineTaskList> subscribeListInfo(String listId)
    {
        return null;
    }

    @Override
    public Single<Long> getChatMessageCount(String listId)
    {
        return null;
    }

    @Override
    public Observable<ChildEventBase<ChatMessage>> subscribeChatMessages(String listId)
    {
        return null;
    }

    @Override
    public void sendChatMessage(String listId, ChatMessage chatMessage)
    {

    }

    @Override
    public Completable renameList(String listId, String newName)
    {
        return null;
    }

    @Override
    public Single<Long> getLastListItemTimestamp(String listId)
    {
        return null;
    }

    @Override
    public Observable<ChildEventBase<PineTaskItemExt>> subscribeListItems(String listId)
    {
        return null;
    }

    @Override
    public void updateItem(PineTaskItemExt item, UserMessageListener userMessageListener)
    {

    }

    @Override
    public void deleteItem(PineTaskItemExt item, UserMessageListener userMessageListener)
    {

    }

    @Override
    public Completable addPineTaskItem(PineTaskItemExt item)
    {
        return null;
    }

    @Override
    public Single<String> getListAsString(String listId)
    {
        return null;
    }

    @Override
    public Single<Integer> getUserStartupMessageVersion(String userId)
    {
        return null;
    }

    @Override
    public void setUserStartupMessageVersion(String userId, int version)
    {

    }

    @Override
    public Single<StartupMessage> getStartupMessage()
    {
        return null;
    }

    @Override
    public Maybe<StartupMessage> getStartupMessageIfUnread(String userId)
    {
        return null;
    }

    @Override
    public Single<PineTaskList> acceptInvite(InviteInfo inviteInfo, String userId)
    {
        return null;
    }

    @Override
    public Completable purgeCompletedItems(String listId)
    {
        return null;
    }

    @Override
    public void setShoppingTripForActiveList(String listId, boolean isActive)
    {

    }

    @Override
    public Observable<Boolean> subscribeToShoppingTripActiveEventsForList(String listId)
    {
        return null;
    }
}
