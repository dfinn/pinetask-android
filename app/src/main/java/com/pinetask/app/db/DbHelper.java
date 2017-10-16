package com.pinetask.app.db;

import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PineTaskListWithCollaborators;
import com.pinetask.app.common.UserMessageListener;
import com.pinetask.app.list_items.PineTaskItemExt;
import com.pinetask.app.main.InviteInfo;
import com.pinetask.app.manage_lists.StartupMessage;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface DbHelper
{

    Completable deleteList(String listId);

    Observable<String> getListCollaborators(String listId);

    Single<String> getUserNameSingle(String userId);

    Observable<ChatMessage> populateUserName(ChatMessage chatMessage);

    Observable<String> getUserNameObservable(String userId);

    Completable setUserName(String userId, String newUserName);

    Completable setIsAnonymous(String userId, boolean isAnonymous);

    Single<Boolean> getIsAnonymous(String userId);

    Observable<String> getListIdsForUser(String userId);

    Single<Boolean> canAccessList(String userId, String listId);

    Observable<ChildEventBase<String>> getListAddedOrDeletedEvents(String userId);

    Observable<ChildEventBase<String>> subscribeMembersAddedOrDeletedEvents(String listId);

    Single<String> getListName(String listId);

    void createInvite(String listId, String inviteId);

    Completable verifyInviteExists(InviteInfo inviteInfo);

    Completable deleteInvite(InviteInfo inviteInfo);

    Completable addUserAsCollaboratorToList(InviteInfo inviteInfo, String userId);

    Completable addListToUserLists(String listId, String userId, String accessType);

    Completable revokeAccessToList(String listId, String userId);

    Completable createList(String ownerId, String listName);

    abstract Single<PineTaskList> getPineTaskList(String listId);

    Observable<PineTaskList> tryGetPineTaskList(String listId);

    Single<PineTaskListWithCollaborators> getPineTaskListWithCollaborators(PineTaskList list);

    Observable<PineTaskList> subscribeListInfo(String listId);

    Single<Long> getChatMessageCount(String listId);

    Observable<ChildEventBase<ChatMessage>> subscribeChatMessages(String listId);

    void sendChatMessage(String listId, ChatMessage chatMessage);

    Completable renameList(String listId, String newName);

    Single<Long> getLastListItemTimestamp(String listId);

    Observable<ChildEventBase<PineTaskItemExt>> subscribeListItems(String listId);

    void updateItem(PineTaskItemExt item, UserMessageListener userMessageListener);

    void deleteItem(PineTaskItemExt item, UserMessageListener userMessageListener);

    Completable addPineTaskItem(PineTaskItemExt item);

    Single<String> getListAsString(String listId);

    Single<Integer> getUserStartupMessageVersion(String userId);

    void setUserStartupMessageVersion(String userId, int version);

    Single<StartupMessage> getStartupMessage();

    Maybe<StartupMessage> getStartupMessageIfUnread(String userId);

    Single<PineTaskList> acceptInvite(InviteInfo inviteInfo, String userId);

    Completable purgeCompletedItems(String listId);

    void setShoppingTripForActiveList(String listId, boolean isActive);

    Observable<Boolean> subscribeToShoppingTripActiveEventsForList(String listId);
}