package com.pinetask.app.db;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskInviteAlreadyUsedException;
import com.pinetask.app.list_items.PineTaskItem;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PineTaskListWithCollaborators;
import com.pinetask.app.main.InviteInfo;
import com.pinetask.app.manage_lists.StartupMessage;
import com.pinetask.common.Logger;
import com.squareup.otto.Bus;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static com.pinetask.app.db.KeyAddedOrDeletedObservable.getKeyAddedOrDeletedEventsAt;

@Singleton
public class DbHelper
{
    /** Name of node in the Firebase DB where list info is stored **/
    public static String LIST_INFO_NODE_NAME = "list_info";

    /** Name of node in the Firebase DB where list collaborators are stored **/
    final String LIST_COLLABORATORS_NODE_NAME = "list_collaborators";

    /** Key name under /list_collaborators/<listid>/<userid> that stores the ID of the invite from which the user got access to the list. **/
    final String INVITE_ID_KEY = "invite_id";

    /** Name of node in the Firebase DB where all list items are stored **/
    public static String LIST_ITEMS_NODE_NAME = "list_items";

    /** Name of node in the Firebase DB where all chat messages are stored. **/
    public static String CHAT_MESSAGES_NODE_NAME = "chat_messages";

    /** Name of node in the Firebase DB where all users are stored  **/
    public static String USERS_NODE_NAME = "users";

    /** Name of node in the Firebase DB where startup message is stored. **/
    final String STARTUP_MESSAGE_NODE = "startup_message";

    /** Name of a node in the FireBase DB where the user's name is stored. **/
    final String USERNAME_NODE_NAME = "userName";

    /** Name of a node in the Firebase DB under /users/$userId which stores a value indicating if the user is anonymous or not. **/
    final String IS_ANONYMOUS_NODE_NAME = "is_anonymous";

    /** Name of a node in the Firebase DB under /users/$userId which stores a value indicating the version of the last startup message the user read. **/
    final String USER_STARTUP_MESSAGE_VERSION = "startup_message_version";

    /** Name of the node where all list invites are stored. **/
    final String LIST_INVITES_NODE_NAME = "list_invites";

    /** Name of node in the Firebase DB where lists accessible to a certain user are stored (/users/<userid>/lists) **/
    public static String LISTS_NODE_NAME = "lists";

    /** Value for a list entry indicating the user is the owner of the list. **/
    final String OWNER = "owner";

    /** Value for a list entry indicating the user has write access to it. **/
    public static String WRITE = "write";

    /** Key name under /list_info/<listid>/ where the list name is stored. **/
    final String LIST_NAME_KEY = "name";

    /** Key name under /list_info/<listid>/ where the user ID of the list owner is stored. **/
    final String OWNER_ID_KEY = "ownerId";

    /** Key name under /list_invites/<listid>/<inviteid>/ that stores the timestamp when the invite was created. **/
    final String INVITE_CREATED_AT_KEY = "created_at";

    /** Key name under /list_items/<listid>/<itemid> which specifies if the item has been marked completed or not. **/
    final String IS_COMPLETED_KEY_NAME = "isCompleted";

    private PineTaskApplication mPineTaskApplication;
    private Bus mEventBus;
    private FirebaseDatabase mDb;

    @Inject
    public DbHelper(PineTaskApplication appContext, Bus eventBus, FirebaseDatabase db)
    {
        logMsg("Creating DbHelper");
        mPineTaskApplication = appContext;
        mEventBus = eventBus;
        mDb = db;
    }

    /** Returns a refernce to /users/$userId **/
    public DatabaseReference getUserRef(String userId)
    {
        return mDb.getReference(USERS_NODE_NAME).child(userId);
    }

    /** Returns a reference to /users/$userId/lists **/
    private DatabaseReference getUserListsRef(String userId)
    {
        return getUserRef(userId).child(LISTS_NODE_NAME);
    }

    /** Returns a reference to /users/$userId/userName **/
    private DatabaseReference getUserNameRef(String userId)
    {
        return getUserRef(userId).child(USERNAME_NODE_NAME);
    }

    /** Returns a reference to /users/$userId/is_anonymous **/
    private DatabaseReference getIsAnonymousReference(String userId)
    {
        return getUserRef(userId).child(IS_ANONYMOUS_NODE_NAME);
    }

    /** Returns a reference to /users/$userId/startup_message_version **/
    private DatabaseReference getUserStartupMessageVersionRef(String userId)
    {
        return getUserRef(userId).child(USER_STARTUP_MESSAGE_VERSION);
    }

    /** Returns a reference to /list_info/$listId **/
    private DatabaseReference getListInfoReference(String listId)
    {
        return mDb.getReference(LIST_INFO_NODE_NAME).child(listId);
    }

    /** Returns a reference to /list_info/$listId/name **/
    private DatabaseReference getListNameReference(String listId)
    {
        return getListInfoReference(listId).child(LIST_NAME_KEY);
    }

    /** Returns a reference to /list_collaborators/$listId **/
    private DatabaseReference getListCollaboratorsReference(String listId)
    {
        return mDb.getReference(LIST_COLLABORATORS_NODE_NAME).child(listId);
    }

    /** Returns a reference to /list_items/$listId **/
    private DatabaseReference getListItemsRef(String listId)
    {
        return mDb.getReference(LIST_ITEMS_NODE_NAME).child(listId);
    }

    /** Returns a reference to /startup_message **/
    private DatabaseReference getStartupMessageRef()
    {
        return mDb.getReference(STARTUP_MESSAGE_NODE);
    }

    /** Returns a reference to /chat_messages/$list_id **/
    private DatabaseReference getChatMessagesRef(String listId)
    {
        return mDb.getReference(CHAT_MESSAGES_NODE_NAME).child(listId);
    }

    private void deleteListItem(DatabaseReference listItemsRef, final String itemId)
    {
        listItemsRef.child(itemId).removeValue((DatabaseError databaseError, DatabaseReference databaseReference) ->
                logDbOperationResult("Deleting list item " + itemId, databaseError, databaseReference));
    }

    /** Returns a Completable that will delete all list collaborators for the specified list, followed by all other related list nodes:
     *  /list_invites/$listId
     *  /list_collaborators/$listId
     *  /list_items/$listId
     *  /chat_messages/$listId
     *  /list_info/$listId
     **/
    public Completable deleteList(final String listId)
    {
        logMsg("deleteList: Deleting list %s", listId);

        Map<String,Object> updates = new HashMap<>();
        updates.put("/" + LIST_INVITES_NODE_NAME + "/" + listId, null);
        updates.put("/" + LIST_COLLABORATORS_NODE_NAME + "/" + listId, null);
        updates.put("/" + LIST_ITEMS_NODE_NAME + "/" + listId, null);
        updates.put("/" + CHAT_MESSAGES_NODE_NAME + "/" + listId, null);
        updates.put("/" + LIST_INFO_NODE_NAME + "/" + listId, null);

        return getListCollaborators(listId)
                .flatMapCompletable(userId -> revokeAccessToList(listId, userId))
                .andThen(updateChildren(mDb.getReference(), updates, "remove nodes related to list"));
    }

    /** Returns an Observable that emits all user IDs who are collaborators of the specified list, and then completes. **/
    public Observable<String> getListCollaborators(String listId)
    {
        return getKeysAt(getListCollaboratorsReference(listId), "get userIds of list collaborators");
    }

    public void logDbOperationResult(String description, DatabaseError databaseError, Query databaseReference)
    {
        if (databaseError==null)
        {
            logMsg("%s [success]", description);
        }
        else
        {
            logDbError(description, databaseError, databaseReference);
        }
    }

    public void logDbError(String description, DatabaseError databaseError, Query databaseReference)
    {
        logError("Error in operation '%s' for node %s (error=%s)", description, databaseReference.toString(), databaseError);
    }

    /** Looks up the username for the userId specified. **/
    public Single<String> getUserNameSingle(final String userId)
    {
        return getItem(String.class, getUserNameRef(userId), "get user name");
    }

    /** Looks up the name of the user based on the ChatMessage's senderId, and populates the username field. **/
    public Observable<ChatMessage> populateUserName(ChatMessage chatMessage)
    {
        return getUserNameSingle(chatMessage.getSenderId()).map(userName ->
        {
            chatMessage.setSenderName(userName);
            return chatMessage;
        }).toObservable();
    }

    /** Sets up event listener to query the username for the userId specified.  Must be disposed of when not needed any longer to detach the listener. **/
    public Observable<String> getUserNameObservable(final String userId)
    {
        return ObservableStringQuery.fromRef(getUserNameRef(userId), "get user name");
    }

    /** Updates the username for the user specified. **/
    public Completable setUserName(String userId, String newUserName)
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
        builder.setDisplayName(newUserName);
        user.updateProfile(builder.build());
        return setValueRx(getUserNameRef(userId), newUserName, "change username");
    }

    /** Sets the "is anonymous user" flag for the current user.  This is a database node under /users/$userId/isAnonymous.
     *  Note: this is only necessary as a workaround since FirebaseAuth.getCurrentUser().isAnonymous() doesn't seem to work correctly.
     **/
    public Completable setIsAnonymous(String userId, boolean isAnonymous)
    {
        return setValueRx(getIsAnonymousReference(userId), isAnonymous, "set is_anonymous");
    }

    public Single<Boolean> getIsAnonymous(String userId)
    {
        return getItem(Boolean.class, getIsAnonymousReference(userId), "get is_anonymous");
    }

    /** Initiate async request to get the user ID of the owner of the list specified. When query returns, invoke the callback provided.
        If any error occurs, logs the error and raises a user message. **/
    public Single<String> getListOwner(String listId)
    {
        final DatabaseReference ref = getListInfoReference(listId).child(OWNER_ID_KEY);
        return getItem(String.class, ref, "get list owner");
    }

    /** Returns an Observable that will emit IDs for all lists that the user has access to, and then completes. **/
    public Observable<String> getListIdsForUser(String userId)
    {
        return getKeysAt(getUserListsRef(userId), "get user lists");
    }

    /** Emits true if the user has access to the list specifed, of false if the user doesn't have access or the list doesn't exist. **/
    public Single<Boolean> canAccessList(String userId, String listId)
    {
        DatabaseReference ref = getUserListsRef(userId).child(listId);
        logMsg("canAccessList: checking if user %s can access list %s", userId, listId);
        return doesKeyExist(ref);
    }

    /** Returns an Observable that will emit notifications of list added / list deleted events for lists that the specified user has access to. **/
    public Observable<ChildEventBase<String>> getListAddedOrDeletedEvents(String userId)
    {
        return getKeyAddedOrDeletedEventsAt(getUserListsRef(userId), "get list added/deleted events");
    }

    /** Returns an Observable that will emit AddedEvent or DeletedEvent for member IDs of the list specified. **/
    public Observable<ChildEventBase<String>> getMembersAddedOrDeletedEvents(String listId)
    {
        return getKeyAddedOrDeletedEventsAt(getListCollaboratorsReference(listId), "get list members added/deleted events");
    }

    /** Returns a Single that, when subscribed to, will look up the name of the specified user in the database. **/
    public Single<String> getListName(final String listId)
    {
        return getItem(String.class, getListNameReference(listId), "get list name");
    }

    /** Creates the database node for a list invite that has been sent (/list_invites/<listid>/<invite_id>) with the "created_at" value set to the current time. **/
    public void createInvite(final String listId, final String inviteId)
    {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIST_INVITES_NODE_NAME).child(listId).child(inviteId).child(INVITE_CREATED_AT_KEY);
        String timestamp = DateTime.now().toString(ISODateTimeFormat.basicDateTime());
        logMsg("Creating invite for list %s with timestamp %s", listId, timestamp);
        ref.setValue(timestamp, (dbErr, errRef) -> logDbOperationResult("create invite", dbErr, errRef));
    }

    /** Returns a Completable that, when subscribed to, checks if the invite for the specified list exists.  If it does, invokes onComplete().
     *  If it doesn't exists, or if an error occurs when checking, calls onError(). **/
    public Completable verifyInviteExists(InviteInfo inviteInfo)
    {
        return Completable.create((final CompletableEmitter emitter) ->
        {
            logMsg("verifyInviteExists: %s", inviteInfo);
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIST_INVITES_NODE_NAME).child(inviteInfo.ListId).child(inviteInfo.InviteId).child(INVITE_CREATED_AT_KEY);
            ref.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    logMsg("verifyInviteExists: onDataChanged, value=%s", dataSnapshot.getValue());
                    if (dataSnapshot.getValue() != null)
                    {
                        logMsg("verifyInviteExists/onDataChange: snapshot value non-null, calling onComplete");
                        emitter.onComplete();
                    }
                    else
                    {
                        logMsg("verifyInviteExists/onDataChange: snapshot value is null, calling onError(invite doesn't exist)");
                        emitter.onError(new PineTaskInviteAlreadyUsedException());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                    logMsg("verifyInviteExists: onCancelled, error=%s", databaseError.getMessage());
                    emitter.onError(new DbOperationCanceledException(ref, databaseError, "verify invite exists"));
                }
            });
        });
    }

    /** Returns a Completable that, when subscribed to, will delete the invite specified. **/
    public Completable deleteInvite(InviteInfo inviteInfo)
    {
        logMsg("Deleting invite: %s", inviteInfo);
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIST_INVITES_NODE_NAME).child(inviteInfo.ListId).child(inviteInfo.InviteId);
        return setValueRx(ref, null, "delete invite");
    }

    /** Returns a Completable that, when subscribed to, will add the user as a collaborator to the list specified by adding the node:
     *   /list_collaborators/<listid>/<userid>/invite_id with the ID of the invite from which access was given. **/
    public Completable addUserAsCollaboratorToList(InviteInfo inviteInfo, final String userId)
    {
        logMsg("--- Creating node %s/%s/%s with payload invite_id=%s", LIST_COLLABORATORS_NODE_NAME, inviteInfo.ListId, userId, inviteInfo.InviteId);
        final DatabaseReference ref = getListCollaboratorsReference(inviteInfo.ListId).child(userId).child(INVITE_ID_KEY);
        return setValueRx(ref, inviteInfo.InviteId, "add user as list collaborator");
    }

    /** Returns a Completable that when subscribed to adds the list ID to the /lists node of the current user (so it shows up in the list of lists this user has access to).
     *  The accessType value can be either OWNER or WRITE. **/
    public Completable addListToUserLists(final String listId, final String userId, String accessType)
    {
        logMsg("Adding list %s to /users/%s/lists", listId, userId);
        final DatabaseReference ref = getUserListsRef(userId).child(listId);
        return setValueRx(ref, accessType, "add list to user's lists");
    }

    /** Revokes access to the list specified for the userId specified.  On error, logs the error and raises a user message.
     * The following nodes are deleted:
     *    /users/<userid>/lists/<listid>
     *    /list_collaborators/<listid>/<userid>
     **/
    public Completable revokeAccessToList(final String listId, final String userId)
    {
        DatabaseReference userListsRef = getUserListsRef(userId).child(listId);
        DatabaseReference collaboratorsRef = getListCollaboratorsReference(listId).child(userId);
        return removeNode(userListsRef).andThen(removeNode(collaboratorsRef));
    }

    /** General purpose Completable wrapper to set the value at a specified location in the database. Starts async request and then Completes immediately. **/
    public Completable setValueRx(final DatabaseReference ref, final Object value, final String operationDescription)
    {
        return setValueRx(ref, value, operationDescription, false);
    }

    /** General purpose Completable wrapper to set the value at a specified location in the database.
     *  If waitForCompletion is true, onComplete is only called after setValue() completed callback occurs.
     *  If waitForCompletion if false, onComplete is called immediately after setValue() is called. **/
    public Completable setValueRx(final DatabaseReference ref, final Object value, final String operationDescription, boolean waitForCompletion)
    {
        return Completable.create((CompletableEmitter emitter) ->
        {
            logMsg("setValue(%s) to '%s'", ref, value);
            ref.setValue(value, (DatabaseError databaseError, DatabaseReference databaseReference) ->
            {
                if (databaseError != null)
                {
                    logDbOperationResult(operationDescription, databaseError, ref);
                    if (!emitter.isDisposed()) emitter.onError(new DbOperationCanceledException(ref, databaseError, operationDescription));
                }
                else
                {
                    logMsg("setValue(%s) completed", ref);
                    if (waitForCompletion && !emitter.isDisposed()) emitter.onComplete();
                }
            });
            if (!waitForCompletion && !emitter.isDisposed()) emitter.onComplete();
        });
    }

    /** Make async call to set the value at a specified location in the database, and return immediately. **/
    public void setValue(final DatabaseReference ref, final Object value, final String operationDescription)
    {
        logMsg("setValue(%s) to '%s'", ref, value);
        ref.setValue(value, (DatabaseError databaseError, DatabaseReference databaseReference) ->
        {
            if (databaseError != null)
            {
                logDbOperationResult(operationDescription, databaseError, ref);
            }
            else
            {
                logMsg("setValue(%s) completed", ref);
            }
        });
    }

    /** Add a new list.  The following nodes are created:
     *    /list_info/<listid>
     *    /list_info/<listid>/name
     *    /list_info/<listid>/ownerId
     *    /users/<userId>/lists/<listid> = "owner"
     *    /list_collaborators/<listid>/<userid> = "owner"
     **/
    public Completable createList(final String ownerId, String listName)
    {
        Logger.logMsg(DbHelper.class, "Adding new list '%s' with owner %s", listName, ownerId);
        PineTaskList newList = new PineTaskList(null, listName, ownerId);
        final DatabaseReference listInfoRef = FirebaseDatabase.getInstance().getReference(LIST_INFO_NODE_NAME).push();
        String listId = listInfoRef.getKey();
        //final DatabaseReference collaboratorsRef = getListCollaboratorsReference(listId).child(ownerId);
        final DatabaseReference collaboratorsRef = getListCollaboratorsReference(listId);

        Map<String,String> collaboratorsMap = new HashMap<>();
        collaboratorsMap.put(ownerId, OWNER);

        return setValueRx(listInfoRef, newList, "create list info node")
            .andThen(addListToUserLists(listId, ownerId, OWNER))
                .andThen(setValueRx(collaboratorsRef, collaboratorsMap, "add owner as collaborator"));
    }

    /** Returns a single that will emit the populated PineTaskList object for the list ID provided. **/
    public Single<PineTaskList> getPineTaskList(String listId)
    {
        return getItem(PineTaskList.class, getListInfoReference(listId), "get list info");
    }

    /** Looks up all collaborators for the list specified, and returns a new populated PineTaskListWithCollaborators object. **/
    public Single<PineTaskListWithCollaborators> getPineTaskListWithCollaborators(PineTaskList list)
    {
        return getListCollaborators(list.getId()).toList().map(collaboratorIds -> new PineTaskListWithCollaborators(list, collaboratorIds));
    }

    /** Returns an Observable that will emit any changes to the PineTaskList with the ID provided.  Caller must dispose Observable when no longer needed. **/
    public Observable<PineTaskList> subscribeListInfo(String listId)
    {
        return subscribeValueEvents(PineTaskList.class, getListInfoReference(listId), "subscribe to list info");
    }

    /** Returns a Single that emits the count of chat messages in the list specified. **/
    public Single<Long> getChatMessageCount(String listId)
    {
        return getNodeCount(getChatMessagesRef(listId));
    }

    /** Returns an observable that emits added/deleted events for chat messages in the list specified. **/
    public Observable<ChildEventBase<ChatMessage>> subscribeChatMessages(String listId)
    {
        ChildEventObservable<ChatMessage> o = new ChildEventObservable(ChatMessage.class, getChatMessagesRef(listId), "subscribe to chat messages");
        return o.attachListener();
    }

    public void sendChatMessage(String listId, ChatMessage chatMessage)
    {
        DatabaseReference dbRef = getChatMessagesRef(listId);
        dbRef.push().setValue(chatMessage);
    }

    /** Rename the specified list **/
    public Completable renameList(String listId, String newName)
    {
        return setValueRx(getListNameReference(listId), newName, "rename list");
    }

    /** Returns a Single that emits the count of list items in the list specified. **/
    public Single<Long> getListItemsCount(String listId)
    {
        return getNodeCount(getListItemsRef(listId));
    }

    /** Returns an observable that emits added/deleted events for items in the list specified. **/
    public Observable<ChildEventBase<PineTaskItem>> subscribeListItems(String listId)
    {
        ChildEventObservable<PineTaskItem> o = new ChildEventObservable(PineTaskItem.class, getListItemsRef(listId), "subscribe to list items");
        return o.attachListener();
    }

    /** Make async call to update PineTaskItem in the database. **/
    public void updateItem(String listId, PineTaskItem item)
    {
        DatabaseReference dbRef = getListItemsRef(listId).child(item.getKey());
        setValue(dbRef, item, "update item");
    }

    public Completable deleteItem(String listId, PineTaskItem item)
    {
        DatabaseReference dbRef = getListItemsRef(listId).child(item.getKey());
        return removeNode(dbRef);
    }

    private void logMsg(String msg, Object...args)
    {
        Logger.logMsg(DbHelper.class, msg, args);
    }

    private void logError(String msg, Object...args)
    {
        Logger.logError(DbHelper.class, msg, args);
    }

    /** Returns an Observable that emits the list of keys at the database reference specified, and then completes. **/
    public Observable<String> getKeysAt(Query ref, String operationDescription)
    {
        return Observable.create((ObservableEmitter<String> emitter) ->
            {
                logMsg("Getting keys at %s", ref);
                ref.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        logMsg("Got %d keys at %s", dataSnapshot.getChildrenCount(), ref);
                        for (DataSnapshot child : dataSnapshot.getChildren())
                        {
                            emitter.onNext(child.getKey());
                        }
                        emitter.onComplete();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        logDbOperationResult("Get keys at " + ref, databaseError, ref);
                        emitter.onError(new DbOperationCanceledException(ref, databaseError, operationDescription));
                    }
                });
            });
    }

    public <T> Single<T> getItem(final Class T, final DatabaseReference ref, final String operationDescription)
    {
        return getItem(T, ref, operationDescription, null);
    }

    /** Emits true if the specified database node exists, false otherwise. **/
    public Single<Boolean> doesKeyExist(DatabaseReference ref)
    {
        return Single.create(new SingleOnSubscribe<Boolean>()
        {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception
            {
                ref.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.getValue()!=null) emitter.onSuccess(true);
                        else emitter.onSuccess(false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        emitter.onError(new DbOperationCanceledException(ref, databaseError, "check if node exists: " + ref));
                    }
                });
            }
        });
    }

    /** Returns a single that emits the count of child nodes at the database reference specified.
     *  TODO: using dataSnapshot.getChildrenCount() is a rather expensive operation, as it retrieves all data including sub-nodes in order to count it client side.
     *       Revisit this code if Firebase adds a more efficient method to query for this, such as support for the "shallow" argument.
     *       See: https://stackoverflow.com/questions/41590730/firebase-shallow-query-parameter-for-android **/
    public Single<Long> getNodeCount(DatabaseReference dbRef)
    {
        return Single.create(emitter ->
        {
            dbRef.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    long childCount = dataSnapshot.getChildrenCount();
                    emitter.onSuccess(childCount);
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                    logDbError("get node count", databaseError, dbRef);
                    emitter.onError(new DbOperationCanceledException(dbRef, databaseError, "get node count"));
                }
            });
        });
    }

    /** Returns a Single that emits the object at the specified database location, deserialized based on the objClass provided.
     *  If the value is null, error is emitted.
     **/
    public <T> Single<T> getItem(final Class cl, final DatabaseReference ref, final String operationDescription, final T defaultValue)
    {
        return Single.create(new SingleOnSubscribe<T>()
        {
            @Override
            public void subscribe(final SingleEmitter<T> emitter) throws Exception
            {
                logMsg("getItem(%s) making request", ref);
                ref.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        Object obj = dataSnapshot.getValue(cl);
                        logMsg("getItem(%s) onDataChange: %s", ref, obj);
                        if (obj instanceof UsesKeyIdentifier)
                        {
                            // Object uses the database key as an identifier, so populate it in the return object.
                            UsesKeyIdentifier keyIdentifier = (UsesKeyIdentifier) obj;
                            keyIdentifier.setId(dataSnapshot.getKey());
                        }
                        if (obj != null)
                        {
                            emitter.onSuccess((T) obj);
                        }
                        else if (defaultValue != null)
                        {
                            // Return value was null but default value was provided: return defaultValue
                            emitter.onSuccess(defaultValue);
                        }
                        else
                        {
                            // Return value was null, and no default value -- error -- rxJava2 does not allow emitting null values.
                            emitter.onError(new DbException(ref, operationDescription, "Value is null"));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        logMsg("getItem(%s) onCancelled", ref);
                        emitter.onError(new DbOperationCanceledException(ref, databaseError, operationDescription));
                    }
                });
            }
        });
    }

    /** Returns an Observable that emits the object at the specified database location, deserialized based on the type provided.
     *  Continues to emit items via onNext() whenever data at dbRef changes.  When the Observable is disposed, the ValueEventListener is disconnected.
     **/
    public <T> Observable<T> subscribeValueEvents(final Class T, final DatabaseReference ref, final String operationDescription)
    {
        ObjectWrapper<ValueEventListener> eventListenerWrapper = new ObjectWrapper<>();
        ObjectWrapper<Boolean> dataReturnedWrapper = new ObjectWrapper<>(false);
        return Observable.create((ObservableEmitter<T> emitter) ->
        {
            eventListenerWrapper.Item = ref.addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    Object obj = dataSnapshot.getValue(T);
                    if (obj instanceof UsesKeyIdentifier)
                    {
                        // Object uses the database key as an identifier, so populate it in the return object.
                        UsesKeyIdentifier keyIdentifier = (UsesKeyIdentifier) obj;
                        keyIdentifier.setId(dataSnapshot.getKey());
                    }
                    if (obj==null)
                    {
                        // Null value means the database location was deleted.
                        logMsg("onDataChange: null value at %s, calling onComplete", ref);
                        emitter.onComplete();
                    }
                    else
                    {
                        logMsg("subscribeValueEvents(%s): onNext", ref);
                        emitter.onNext((T) obj);
                        dataReturnedWrapper.Item=true;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                    // Workaround issue where onCancelled will get called when addValueEventListener was created while offline (ex: create a list offline; ListLoader calls getPineTaskList()).
                    // Check if onDataChange() has previously been called successfully, and if so, just log the error and call onComplete.
                    if (dataReturnedWrapper.Item)
                    {
                        logError("Error in subscribeValueEvents(%s), but dataReturned=true. Error=%s", ref, databaseError);
                        emitter.onComplete();
                    }
                    else
                    {
                        emitter.onError(new DbOperationCanceledException(ref, databaseError, operationDescription));
                    }
                }
            });
        })
        .doOnDispose(() ->
        {
            if (eventListenerWrapper.Item != null) ref.removeEventListener(eventListenerWrapper.Item);
        });
    }

    public class ObjectWrapper<T>
    {
        public T Item;
        public ObjectWrapper() { }
        public ObjectWrapper(T item) { Item = item; }
    }

    /** Returns a Completable that will perform updateChildren() on the database reference provided, using the values in the updates map. **/
    public Completable updateChildren(DatabaseReference ref, Map updates, String operationDescription)
    {
        return Completable.create((CompletableEmitter emitter) ->
        {
            ref.updateChildren(updates, (DatabaseError databaseError, DatabaseReference databaseReference) ->
            {
                if (databaseError==null) emitter.onComplete();
                else emitter.onError(new DbOperationCanceledException(ref, databaseError, operationDescription));
            });
        });
    }

    public Completable removeNode(DatabaseReference dbRef)
    {
        String operationDescription = "Remove node " + dbRef;
        return Completable.create(emitter ->
        {
            logMsg("Removing node %s", dbRef);
            dbRef.removeValue((DatabaseError databaseError, DatabaseReference databaseReference) ->
            {
                if (databaseError == null)
                {
                    logMsg("Successfully removed node %s", dbRef);
                    emitter.onComplete();
                }
                else
                {
                    logDbOperationResult(operationDescription, databaseError, dbRef);
                    emitter.onError(new DbOperationCanceledException(dbRef, databaseError, operationDescription));
                }
            });
        });
    }

    /** Emits a value indicating whether currently connected to the Firebase server. **/
    public Single<Boolean> isConnected()
    {
        return Single.create(emitter ->
        {
            DatabaseReference connectedRef = mDb.getReference(".info/connected");
            connectedRef.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot snapshot)
                {
                    boolean connected = snapshot.getValue(Boolean.class);
                    emitter.onSuccess(connected);
                }

                @Override
                public void onCancelled(DatabaseError error)
                {
                    emitter.onError(new DbOperationCanceledException(connectedRef, error, "get connected state"));
                }
            });
        });
    }

    /** Enumerate items at the specified database reference, and return each one deserialized as an item of the class specified. **/
    public <T> Observable<T> getItemsOfType(Class T, DatabaseReference dbRef)
    {
        return Observable.create(new ObservableOnSubscribe<T>()
        {
            @Override
            public void subscribe(ObservableEmitter<T> emitter) throws Exception
            {
                dbRef.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        for (DataSnapshot ds : dataSnapshot.getChildren())
                        {
                            T item = (T) ds.getValue(T);
                            if (item instanceof UsesKeyIdentifier)
                            {
                                // Object uses the database key as an identifier, so populate it in the return object.
                                UsesKeyIdentifier keyIdentifier = (UsesKeyIdentifier) item;
                                keyIdentifier.setId(ds.getKey());
                            }
                            emitter.onNext(item);
                        }
                        emitter.onComplete();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        emitter.onError(new DbOperationCanceledException(dbRef, databaseError, "get item"));
                    }
                });
            }
        });
    }

    /** Returns a string representation of the list with the ID specified. String will include the list name, followed by each list item description. Example:
     *  Grocery Shopping:
     *  [ ] Apples
     *  [X] Oranges
     *  [ ] Bread
     **/
    public Single<String> getListAsString(String listId)
    {
        return getListName(listId)
                .map(listName -> listName + ":")
                .toObservable()
                .concatWith( getItemsOfType(PineTaskItem.class, getListItemsRef(listId)).map(item -> item.toString()) )
                .toList()
                .map(strList ->
                {
                    StringBuilder sb = new StringBuilder();
                    for (String str : strList) sb.append(str+"\n");
                    return sb.toString();
                });
    }

    /** Get version number of most recent startup message the specified user has read. **/
    public Single<Integer> getUserStartupMessageVersion(String userId)
    {
        return getItem(Integer.class, getUserStartupMessageVersionRef(userId), "get startup message version", -1).doOnSuccess(v -> logMsg("getUserStartupMessageVersion: %d", v));
    }

    /** Sets the version number of the last read startup message for the user specified. **/
    public void setUserStartupMessageVersion(String userId, int version)
    {
        setValue(getUserStartupMessageVersionRef(userId), version, "set user startup message version");
    }

    /** Get startup message (text and version) **/
    public Single<StartupMessage> getStartupMessage()
    {
        Single<StartupMessage> source = getItem(StartupMessage.class, getStartupMessageRef(), "get startup message");
        return source.doOnSuccess(s -> { getStartupMessageRef().keepSynced(false); logMsg("getStartupMessage: %s", s); });
    }

    /** Emits the startup message to be read by the user, unless the user has already read the most recent version of the message. **/
    public Maybe<StartupMessage> getStartupMessageIfUnread(String userId)
    {
        DatabaseReference refreshRef = getStartupMessageRef().child("refresh");
        return getStartupMessage().delay(1, TimeUnit.SECONDS).toCompletable().andThen(setValueRx(refreshRef, DateTime.now().getMillis(), "startup message refresh workaround", true))
                .andThen(getUserStartupMessageVersion(userId)).flatMapMaybe(userVersion -> getStartupMessage().filter(startupMessage -> startupMessage.version>userVersion));
    }

    /** Accepts an invitation that was received to grant the current user access to the shared list by doing the following:
     *  - Makes sure the invite exists (hasn't already been used)
     *  - Adds the current user as a collaborator for the specified list so the user will have access to it.
     *  - Deletes the invitation so it can't be used again.
     *  - Adds the list ID to the current user's list of accessible lists.
     *  - Looks up the list's name and returns it
     **/
    public Single<PineTaskList> acceptInvite(InviteInfo inviteInfo, String userId)
    {
        return verifyInviteExists(inviteInfo)
                .andThen(addUserAsCollaboratorToList(inviteInfo, userId))
                .andThen(deleteInvite(inviteInfo))
                .andThen(addListToUserLists(inviteInfo.ListId, userId, DbHelper.WRITE))
                .andThen(getPineTaskList(inviteInfo.ListId));
    }

    /** Delete all completed items in the list with the ID specified. **/
    public Completable purgeCompletedItems(String listId)
    {
        logMsg("Purging completed items from list %s", listId);
        DatabaseReference listItemsRef = FirebaseDatabase.getInstance().getReference(LIST_ITEMS_NODE_NAME).child(listId);
        Query query = listItemsRef.orderByChild(IS_COMPLETED_KEY_NAME).equalTo(true);
        return getKeysAt(query, "get list items to purge").flatMapCompletable(itemId -> removeNode(listItemsRef.child(itemId)));
    }
}
