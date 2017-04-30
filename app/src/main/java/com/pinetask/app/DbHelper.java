package com.pinetask.app;

import android.support.v4.util.Pair;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.pinetask.common.Logger;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;

import static com.pinetask.app.KeyAddedOrDeletedObservable.getKeyAddedOrDeletedEventsAt;

public class DbHelper
{
    /** Name of node in the Firebase DB where list info is stored **/
    public static String LIST_INFO_NODE_NAME = "list_info";

    /** Name of node in the Firebase DB where list collaborators are stored **/
    public static String LIST_COLLABORATORS_NODE_NAME = "list_collaborators";

    /** Key name under /list_collaborators/<listid>/<userid> that stores the ID of the invite from which the user got access to the list. **/
    public static String INVITE_ID_KEY = "invite_id";

    /** Name of node in the Firebase DB where all list items are stored **/
    public static String LIST_ITEMS_NODE_NAME = "list_items";

    /** Name of node in the Firebase DB where all chat messages are stored. **/
    public static String CHAT_MESSAGES_NODE_NAME = "chat_messages";

    /** Name of node in the Firebase DB where all users are stored  **/
    public static String USERS_NODE_NAME = "users";

    /** Name of a node in the FireBase DB where the user's name is stored. **/
    public static String USERNAME_NODE_NAME = "userName";

    /** Name of a node in the Firebaes DB under /users/$userId which stores a value indicating if the user is anonymous or not. **/
    public static String IS_ANONYMOUS_NODE_NAME = "is_anonymous";

    /** Name of the node where all list invites are stored. **/
    public static String LIST_INVITES_NODE_NAME = "list_invites";

    /** Name of node in the Firebase DB where lists accessible to a certain user are stored (/users/<userid>/lists) **/
    public static String LISTS_NODE_NAME = "lists";

    /** Value for a list entry indicating the user is the owner of the list. **/
    public static String OWNER = "owner";

    /** Value for a list entry indicating the user has write access to it. **/
    public static String WRITE = "write";

    /** Key name under /list_info/<listid>/ where the list name is stored. **/
    public static String LIST_NAME_KEY = "name";

    /** Key name under /list_info/<listid>/ where the user ID of the list owner is stored. **/
    public static String OWNER_ID_KEY = "ownerId";

    /** Key name under /list_invites/<listid>/<inviteid>/ that stores the timestamp when the invite was created. **/
    public static String INVITE_CREATED_AT_KEY = "created_at";

    /** Key name under /list_items/<listid>/<itemid> which specifies if the item has been marked completed or not. **/
    public static String IS_COMPLETED_KEY_NAME = "isCompleted";

    public static FirebaseDatabase getDb()
    {
        return FirebaseDatabase.getInstance();
    }

    /** Returns a reference to /users/$userId/lists **/
    private static DatabaseReference getUserListsRef(String userId)
    {
        return getDb().getReference(USERS_NODE_NAME).child(userId).child(LISTS_NODE_NAME);
    }

    /** Returns a reference to /users/$userId/userName **/
    private static DatabaseReference getUserNameRef(String userId)
    {
        return FirebaseDatabase.getInstance().getReference(DbHelper.USERS_NODE_NAME).child(userId).child(DbHelper.USERNAME_NODE_NAME);
    }

    /** Returns a reference to /users/$userId/is_anonymous **/
    private static DatabaseReference getIsAnonymousReference(String userId)
    {
        return FirebaseDatabase.getInstance().getReference(USERS_NODE_NAME).child(userId).child(IS_ANONYMOUS_NODE_NAME);
    }

    /** Returns a reference to /list_info/$listId **/
    private static DatabaseReference getListInfoReference(String listId)
    {
        return getDb().getReference(DbHelper.LIST_INFO_NODE_NAME).child(listId);
    }

    /** Returns a reference to /list_info/$listId/name **/
    private static DatabaseReference getListNameReference(String listId)
    {
        return getListInfoReference(listId).child(DbHelper.LIST_NAME_KEY);
    }

    /** Returns a reference to /list_collaborators/$listId **/
    private static DatabaseReference getListCollaboratorsReference(String listId)
    {
        return getDb().getReference(LIST_COLLABORATORS_NODE_NAME).child(listId);
    }

    public static void purgeCompletedItems(String listId)
    {
        logMsg("Purging completed items from list %s", listId);
        final DatabaseReference listItemsRef = FirebaseDatabase.getInstance().getReference(LIST_ITEMS_NODE_NAME).child(listId);
        Query query = listItemsRef.orderByChild(IS_COMPLETED_KEY_NAME).equalTo(true);
        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                logMsg("purgeCompletedItems::onDataChange: dataSnapshot has %d children", dataSnapshot.getChildrenCount());
                for (DataSnapshot ds : dataSnapshot.getChildren())
                {
                    deleteListItem(listItemsRef, ds.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                logError("purgeCompletedItems::onCancelled: %s", databaseError.getMessage());
                PineTaskApplication.raiseUserMsg(true, R.string.error_getting_completed_items);
            }
        });
    }

    private static void deleteListItem(DatabaseReference listItemsRef, final String itemId)
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
    public static Completable deleteList(final String listId)
    {
        logMsg("deleteList: Deleting list %s", listId);

        Map<String,Object> updates = new HashMap<>();
        updates.put("/" + DbHelper.LIST_INVITES_NODE_NAME + "/" + listId, null);
        updates.put("/" + DbHelper.LIST_COLLABORATORS_NODE_NAME + "/" + listId, null);
        updates.put("/" + DbHelper.LIST_ITEMS_NODE_NAME + "/" + listId, null);
        updates.put("/" + DbHelper.CHAT_MESSAGES_NODE_NAME + "/" + listId, null);
        updates.put("/" + DbHelper.LIST_INFO_NODE_NAME + "/" + listId, null);

        return getListCollaborators(listId)
                .flatMapCompletable(userId -> revokeAccessToList(listId, userId))
                .andThen(updateChildren(getDb().getReference(), updates, "remove nodes related to list"));
    }

    /** Returns an Observable that emits all user IDs who are collaborators of the specified list, and then completes. **/
    public static Observable<String> getListCollaborators(String listId)
    {
        return getKeysAt(getListCollaboratorsReference(listId), "get userIds of list collaborators");
    }

    public static void logDbOperationResult(String description, DatabaseError databaseError, DatabaseReference databaseReference)
    {
        if (databaseError==null)
        {
            logMsg("%s [success]", description);
        }
        else
        {
            logDbError(description, databaseError, databaseReference);
            PineTaskApplication.raiseUserMsg(true, R.string.error_in_operation_x_msg_x, description, databaseError);
        }
    }

    public static void logDbError(String description, DatabaseError databaseError, DatabaseReference databaseReference)
    {
        logError("Error in operation '%s' for node %s (error=%s)", description, databaseReference.toString(), databaseError);
    }

    /** Looks up the username for the userId specified. **/
    public static Single<String> getUserNameSingle(final String userId)
    {
        return getItem(String.class, getUserNameRef(userId), "get user name");
    }

    /** Sets up event listener to query the username for the userId specified.  Must be disposed of when not needed any longer to detach the listener. **/
    public static Observable<String> getUserNameObservable(final String userId)
    {
        return ObservableStringQuery.fromRef(getUserNameRef(userId), "get user name");
    }

    /** Updates the username for the user specified. **/
    public static Completable setUserName(String userId, String newUserName)
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
        builder.setDisplayName(newUserName);
        user.updateProfile(builder.build());
        return setValue(getUserNameRef(userId), newUserName, "change username");
    }

    /** Sets the "is anonymous user" flag for the current user.  This is a database node under /users/$userId/isAnonymous.
     *  Note: this is only necessary as a workaround since FirebaseAuth.getCurrentUser().isAnonymous() doesn't seem to work correctly.
     **/
    public static Completable setIsAnonymous(String userId, boolean isAnonymous)
    {
        return setValue(getIsAnonymousReference(userId), isAnonymous, "set is_anonymous");
    }

    public static Single<Boolean> getIsAnonymous(String userId)
    {
        return getItem(Boolean.class, getIsAnonymousReference(userId), "get is_anonymous");
    }

    /** Initiate async request to get the user ID of the owner of the list specified. When query returns, invoke the callback provided.
        If any error occurs, logs the error and raises a user message. **/
    public static Single<String> getListOwner(String listId)
    {
        final DatabaseReference ref = getListInfoReference(listId).child(DbHelper.OWNER_ID_KEY);
        return getItem(String.class, ref, "get list owner");
    }

    /** Returns an Observable that will emit IDs for all lists that the user has access to, and then completes. **/
    public static Observable<String> getListIdsForUser(String userId)
    {
        return getKeysAt(getUserListsRef(userId), "get user lists");
    }

    /** Returns an Observable that will emit notifications of list added / list deleted events for lists that the specified user has access to. **/
    public static Observable<AddedOrDeletedEvent<String>> getListAddedOrDeletedEvents(String userId)
    {
        return getKeyAddedOrDeletedEventsAt(getUserListsRef(userId), "get list added/deleted events");
    }

    /** Returns a Single that, when subscribed to, will look up the name of the specified user in the database. **/
    public static Single<String> getListName(final String listId)
    {
        return getItem(String.class, getListNameReference(listId), "get list name");
    }

    /** Creates the database node for a list invite that has been sent (/list_invites/<listid>/<invite_id>) with the "created_at" value set to the current time. **/
    public static void createInvite(final String listId, final String inviteId)
    {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(DbHelper.LIST_INVITES_NODE_NAME).child(listId).child(inviteId).child(INVITE_CREATED_AT_KEY);
        String timestamp = DateTime.now().toString(ISODateTimeFormat.basicDateTime());
        logMsg("Creating invite for list %s with timestamp %s", listId, timestamp);
        ref.setValue(timestamp, (DatabaseError databaseError, DatabaseReference databaseReference) -> logDbOperationResult("fromRef invite", databaseError, ref));
    }

    /** Returns a Completable that, when subscribed to, checks if the invite for the specified list exists.  If it does, invokes onComplete().
     *  If it doesn't exists, or if an error occurs when checking, calls onError(). **/
    public static Completable verifyInviteExists(final String listId, final String inviteId)
    {
        return Completable.create((final CompletableEmitter emitter) ->
            {
                logMsg("verifyInviteExists: listId=%s, inviteId=%s", listId, inviteId);
                final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIST_INVITES_NODE_NAME).child(listId).child(inviteId).child(INVITE_CREATED_AT_KEY);
                ref.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        logMsg("verifyInviteExists: onDataChanged, value=%s", dataSnapshot.getValue());
                        if (dataSnapshot.getValue()!=null)
                        {
                            logMsg("verifyInviteExists/onDataChange: snapshot value non-null, calling onComplete");
                            emitter.onComplete();
                        }
                        else
                        {
                            logMsg("verifyInviteExists/onDataChange: snapshot value is null, calling onError(invite doesn't exist)");
                            emitter.onError(new PineTaskException(PineTaskApplication.getInstance().getString(R.string.invite_already_used)));
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
    public static Completable deleteInvite(final String listId, final String inviteId)
    {
        logMsg("Deleting invite %s for list %s", inviteId, listId);
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIST_INVITES_NODE_NAME).child(listId).child(inviteId);
        return setValue(ref, null, "delete invite");
    }

    /** Returns a Completable that, when subscribed to, will add the user as a collaborator to the list specified by adding the node:
     *   /list_collaborators/<listid>/<userid>/invite_id with the ID of the invite from which access was given. **/
    public static Completable addUserAsCollaboratorToList(final String listId, final String invitationId, final String userId)
    {
        logMsg("--- Creating node %s/%s/%s with payload invite_id=%s", DbHelper.LIST_COLLABORATORS_NODE_NAME, listId, userId, invitationId);
        final DatabaseReference ref = getListCollaboratorsReference(listId).child(userId).child(DbHelper.INVITE_ID_KEY);
        return setValue(ref, invitationId, "add user as list collaborator");
    }

    /** Returns a Completable that when subscribed to adds the list ID to the /lists node of the current user (so it shows up in the list of lists this user has access to).
     *  The accessType value can be either OWNER or WRITE. **/
    public static Completable addListToUserLists(final String listId, final String userId, String accessType)
    {
        logMsg("Adding list %s to /users/%s/lists", listId, userId);
        final DatabaseReference ref = getUserListsRef(userId).child(listId);
        return setValue(ref, accessType, "add list to user's lists");
    }

    /** Revokes access to the list specified for the userId specified.  On error, logs the error and raises a user message.
     * The following nodes are deleted:
     *    /users/<userid>/lists/<listid>
     *    /list_collaborators/<listid>/<userid>
     **/
    public static Completable revokeAccessToList(final String listId, final String userId)
    {
        DatabaseReference userListsRef = getUserListsRef(userId).child(listId);
        DatabaseReference collaboratorsRef = getListCollaboratorsReference(listId).child(userId);
        //return setValue(userListsRef, null, "delete from user's lists").andThen(setValue(collaboratorsRef, null, "delete collaborator entry"));
        return removeNode(userListsRef).andThen(removeNode(collaboratorsRef));
    }

    /** General purpose Completable wrapper to set the value at a specified location in the database. **/
    public static Completable setValue(final DatabaseReference ref, final Object value, final String operationDescription)
    {
        return Completable.fromAction(() ->
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
        });
    }

    /** Observer for a single result that will just pass the result to a callback when available.
     *  If an error occurs, logs the exception and raises a user message. **/
    public static <T> SingleObserver<T> singleObserver(final DbCallback<T> callback)
    {
        return new SingleObserver<T>()
        {
            @Override
            public void onSubscribe(Disposable d)
            {
            }

            @Override
            public void onSuccess(T t)
            {
                callback.onResult(t);
            }

            @Override
            public void onError(Throwable ex)
            {
                Logger.logException(getClass(), ex);
                PineTaskApplication.raiseUserMsg(true, ex.getMessage());
            }
        };
    }

    /** Observer that will just pass the result to a callback when available. If an error occurs, logs the exception and raises a user message. **/
    public static <T> Observer<T> genericObserver(final DbCallback<T> callback)
    {
        return new Observer<T>()
        {
            @Override
            public void onSubscribe(Disposable d)
            {
            }

            @Override
            public void onNext(T t)
            {
                callback.onResult(t);
            }

            @Override
            public void onError(Throwable ex)
            {
                Logger.logException(getClass(), ex);
                PineTaskApplication.raiseUserMsg(true, ex.getMessage());
            }

            @Override
            public void onComplete()
            {
            }
        };
    }

    /** Add a new list.  The following nodes are created:
     *    /list_info/<listid>
     *    /list_info/<listid>/name
     *    /list_info/<listid>/ownerId
     *    /users/<userId>/lists/<listid> = "owner"
     *    /list_collaborators/<listid>/<userid> = "owner"
     **/
    public static Completable createList(final String ownerId, String listName)
    {
        Logger.logMsg(DbHelper.class, "Adding new list '%s' with owner %s", listName, ownerId);
        PineTaskList newList = new PineTaskList(null, listName, ownerId);
        final DatabaseReference listInfoRef = FirebaseDatabase.getInstance().getReference(LIST_INFO_NODE_NAME).push();
        String listId = listInfoRef.getKey();
        //final DatabaseReference collaboratorsRef = getListCollaboratorsReference(listId).child(ownerId);
        final DatabaseReference collaboratorsRef = getListCollaboratorsReference(listId);

        Map<String,String> collaboratorsMap = new HashMap<>();
        collaboratorsMap.put(ownerId, OWNER);

        return setValue(listInfoRef, newList, "create list info node")
            .andThen(addListToUserLists(listId, ownerId, OWNER))
                .andThen(setValue(collaboratorsRef, collaboratorsMap, "add owner as collaborator"));
    }

    /** Returns a single that will emit the populated PineTaskList object for the list ID provided. **/
    public static Single<PineTaskList> getPineTaskList(String listId)
    {
        return getItem(PineTaskList.class, getListInfoReference(listId), "get list info");
    }

    /** Looks up all collaborators for the list specified, and returns a new populated PineTaskListWithCollaborators object. **/
    public static Single<PineTaskListWithCollaborators> getPineTaskListWithCollaborators(PineTaskList list)
    {
        return getListCollaborators(list.getId()).toList().map(collaboratorIds -> new PineTaskListWithCollaborators(list, collaboratorIds));
    }

    /** Returns an Observable that will emit any changes to the PineTaskList with the ID provided.  Caller must dispose Observable when no longer needed. **/
    public static Observable<PineTaskList> subscribeListInfo(String listId)
    {
        return subscribeItem(PineTaskList.class, getListInfoReference(listId), "subscribe to list info");
    }

    /** Rename the specified list **/
    public static Completable renameList(String listId, String newName)
    {
        return setValue(getListNameReference(listId), newName, "rename list");
    }

    private static void logMsg(String msg, Object...args)
    {
        Logger.logMsg(DbHelper.class, msg, args);
    }

    private static void logError(String msg, Object...args)
    {
        Logger.logError(DbHelper.class, msg, args);
    }

    /** Returns an Observable that emits the list of keys at the database reference specified, and then completes. **/
    public static Observable<String> getKeysAt(DatabaseReference ref, String operationDescription)
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

    /** Returns a Single that emits the object at the specified database location, deserialized based on the objClass provided.
     *  If the value is null, error is emitted.
     **/
    public static <T> Single<T> getItem(final Class T, final DatabaseReference ref, final String operationDescription)
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
                        logMsg("getItem(%s) onDataChange", ref);
                        Object obj = dataSnapshot.getValue(T);
                        if (obj instanceof UsesKeyIdentifier)
                        {
                            // Object uses the database key as an identifier, so populate it in the return object.
                            UsesKeyIdentifier keyIdentifier = (UsesKeyIdentifier) obj;
                            keyIdentifier.setId(dataSnapshot.getKey());
                        }
                        if (obj==null) emitter.onError(new DbException(ref, operationDescription, "Value is null"));  // rxJava2 does not allow emitting null values.
                        emitter.onSuccess((T) obj);
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
    public static <T> Observable<T> subscribeItem(final Class T, final DatabaseReference ref, final String operationDescription)
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
                        logMsg("subscribeItem(%s): onNext", ref);
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
                        logError("Error in subscribeItem(%s), but dataReturned=true. Error=%s", ref, databaseError);
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

    public static class ObjectWrapper<T>
    {
        public T Item;
        public ObjectWrapper() { }
        public ObjectWrapper(T item) { Item = item; }
    }

    /** Returns a Completable that will perform updateChildren() on the database reference provided, using the values in the updates map. **/
    public static Completable updateChildren(DatabaseReference ref, Map updates, String operationDescription)
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

    public static Completable removeNode(DatabaseReference dbRef)
    {
        String operationDescription = "Remove node " + dbRef;
        return Completable.create(emitter ->
        {
            logMsg("Removing node %s", dbRef);
            dbRef.removeValue((DatabaseError databaseError, DatabaseReference databaseReference) ->
            {
                if (databaseError == null)
                {
                    logMsg("Succssfully removed node %s", dbRef);
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
    public static Single<Boolean> isConnected()
    {
        return Single.create(emitter ->
        {
            DatabaseReference connectedRef = getDb().getReference(".info/connected");
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
}
