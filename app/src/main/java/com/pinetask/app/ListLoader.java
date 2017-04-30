package com.pinetask.app;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pinetask.common.LoggingBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Helper class to retrieve all lists owned by a specified user.  As each list is loaded, a user-provided callback is invoked.
 *  The user must call shutdown() when they no longer wish to receive callbacks about new lists being added. **/
class ListLoader extends LoggingBase
{
    interface ListLoadCallback
    {
        /** Called after initial load of all lists is completed. **/
        void onListsLoaded(List<PineTaskList> lists);

        /** Called if any loading error occurs.**/
        void onLoadError();

        /** Called when a list is deleted. **/
        void onListDeleted(String listId);

        /** Called when a list is added. **/
        void onListAdded(PineTaskList list);
    }

    private FirebaseDatabase mDatabase;
    private DatabaseReference mListsRef;
    private String mUserId;
    private ListLoadCallback mCallback;

    /** Will be true until after the initial list load has completed (the value event listener attached to /users/$userid/lists has fired) **/
    boolean mInitialLoadCompleted = false;

    /** Maintain reference to child and value event listeners attached to users/xxxx/lists so we can remove them when shut down. **/
    ChildEventListener mChildEventListener;
    ValueEventListener mValueEventListener;

    /** Maintain a list of value event listeners for all list_info nodes so we can remove them all when the ListLoader is shutdown.
     *  Map keys are listId, and value is the listener attached to the /list_info/$list_id node. **/
    private Map<String, ValueEventListener> mListInfoListenerMap = new HashMap<>();

    /** Expected number of lists to be loaded (found from the /users/$user_id/lists node) **/
    private long mNumberOfListsToLoad;

    /** List of the user's lists that have been loaded so far.  Since the name/owner of each one must be looked up with a separate async callback, we
     *  add each one to this list as its info is populated.  Then when the expected number of lists have been populated, the caller is provided this list. **/
    private List<PineTaskList> mLists;

    ListLoader(String userId, ListLoadCallback callback)
    {
        mDatabase = FirebaseDatabase.getInstance();
        mUserId = userId;
        mCallback = callback;
        mListsRef = mDatabase.getReference(DbHelper.USERS_NODE_NAME).child(mUserId).child(DbHelper.LISTS_NODE_NAME);
        logMsg("ListLoader: calling addActiveTask");
        PineTaskApplication.getInstance().addActiveTask();
        initChildEventListener();
        initValueEventListener();
    }

    synchronized void shutdown()
    {
        logMsg("Shutting down ListLoader");

        // Shut down listeners attached to /users/$userid/lists node.
        if (mValueEventListener != null)
        {
            logMsg("--- Removing /users/%s/lists value event listener", mUserId);
            mListsRef.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
        if (mChildEventListener != null)
        {
            logMsg("--- Removing /users/%s/lists child event listener", mUserId);
            mListsRef.removeEventListener(mChildEventListener);
            mChildEventListener= null;
        }

        // Shut down listeners attached to /list_info/$listid/ nodes.
        for (String listId : mListInfoListenerMap.keySet())
        {
            logMsg("--- Removing /list_info/%s value event listener", listId);
            ValueEventListener listener = mListInfoListenerMap.get(listId);
            mDatabase.getReference(DbHelper.LIST_INFO_NODE_NAME).child(listId).removeEventListener(listener);
        }
        mListInfoListenerMap.clear();
    }

    private void initValueEventListener()
    {
        mValueEventListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                mNumberOfListsToLoad = dataSnapshot.getChildrenCount();
                mLists = new ArrayList<>();
                logMsg("onDataChange: user owns %d lists. Setting up child event listener.", mNumberOfListsToLoad);

                if (dataSnapshot.getChildrenCount() == 0)
                {
                    // User has no lists: return (empty) result array immediately
                    mInitialLoadCompleted = true;
                    notifyListsLoaded();
                }
                else
                {
                    // For each list the user owns, start a query that will look up the list name. When all lists have been looked up, the callback will be invoked.
                    for (DataSnapshot ds : dataSnapshot.getChildren()) getListInfo(ds.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                logError("ListLoader::onCancelled: %s", databaseError.getMessage());
                notifyLoadError();
            }
        };

        mListsRef.addListenerForSingleValueEvent(mValueEventListener);
    }

    private void initChildEventListener()
    {
        mChildEventListener = new ChildEventListener()
        {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                if (mLists!=null)
                {
                    logMsg("onChildAdded: %s -- querying list info", dataSnapshot.getKey());
                    getListInfo(dataSnapshot.getKey());
                }
                else
                {
                    logMsg("onChildAdded: %s -- initial load not complete, ignoring", dataSnapshot.getKey());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                logMsg("onChildChanged: %s", dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot)
            {
                String listId = dataSnapshot.getKey();

                // If valueEventListener is attached to /list_info/$list_id, then remove it.
                ValueEventListener listener = mListInfoListenerMap.get(listId);
                if (listener != null)
                {
                    logMsg("--- Removing /list_info/%s value event listener", listId);
                    mDatabase.getReference(DbHelper.LIST_INFO_NODE_NAME).child(listId).removeEventListener(listener);
                    mListInfoListenerMap.remove(listId);
                }

                if (mLists!=null)
                {
                    logMsg("onChildRemoved: %s -- calling onListDeleted callback and posting ListDeletedEvent", listId);
                    mCallback.onListDeleted(dataSnapshot.getKey());
                    PineTaskApplication.getEventBus().post(new ListDeletedEvent(listId));
                }
                else
                {
                    logMsg("onChildRemoved: %s -- initial load not complete, ignoring", listId);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s)
            {
                logMsg("onChildMoved: %s", dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                logError("onCancelled: %s", databaseError.getMessage());
            }
        };

        mListsRef.addChildEventListener(mChildEventListener);
    }

    /** Queries /lists/<listId>/ to get the name and owner of the list with the ID specified, populating a PineTaskList object and adding it to mLists.
     *  Once mLists contains the expected number of lists to be loaded, the callback is invoked to provide mLists to the caller. **/
    private synchronized void getListInfo(final String listId)
    {
        logMsg("getPineTaskList: loading list info for listId '%s'", listId);

        ValueEventListener listInfoValueEventListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                PineTaskList list = dataSnapshot.getValue(PineTaskList.class);
                if (list == null)
                {
                    logMsg("getPineTaskList: dataSnapshot is null for listId %s, list has been deleted", listId);
                    return;
                }

                list.setKey(listId);
                logMsg("Loaded list '%s' [%s]", list.getName(), list.getKey());

                if (mInitialLoadCompleted)
                {
                    mCallback.onListAdded(list);
                }
                else
                {
                    mLists.add(list);
                    if (mLists.size() == mNumberOfListsToLoad)
                    {
                        // All lists have been loaded: sort them by name, and return the list of lists to the caller.
                        logMsg("getPineTaskList: finished loading %d lists", mNumberOfListsToLoad);
                        mInitialLoadCompleted = true;
                        Collections.sort(mLists, PineTaskList.NAME_COMPARATOR);
                        notifyListsLoaded();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                if (isListActive(listId))
                {
                    logError("getPineTaskList(listId=%s): error=%s", listId, databaseError.getMessage());
                    PineTaskApplication.getInstance().endActiveTask();
                    notifyLoadError();
                }
                else
                {
                    // Suppress reporting error message: works around issue where callback might get fired soon after list was deleted (even if callback was removed)
                    logMsg("getPineTaskList(listId=%s): suppressing error, list no longer active", listId);
                }
            }
        };

        // Add listener to map so we can remove it at shutdown time.
        mListInfoListenerMap.put(listId, listInfoValueEventListener);

        mDatabase.getReference(DbHelper.LIST_INFO_NODE_NAME).child(listId).addValueEventListener(listInfoValueEventListener);
    }

    /** Returns true if the list is in the map of lists with active listeners attached for their /list_info/$list_id node.
     *  Will be false for lists not yet loaded, or for lists which have been deleted. **/
    public synchronized boolean isListActive(String listid)
    {
        return mListInfoListenerMap.containsKey(listid);
    }

    private void notifyListsLoaded()
    {
        logMsg("notifyListsLoaded: calling endActiveTask");
        PineTaskApplication.getInstance().endActiveTask();
        mCallback.onListsLoaded(mLists);
    }

    private void notifyLoadError()
    {
        PineTaskApplication.getInstance().endActiveTask();
        mCallback.onLoadError();
    }
}
