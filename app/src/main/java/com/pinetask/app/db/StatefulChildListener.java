package com.pinetask.app.db;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pinetask.common.LoggingBase;

/** Helper class for wrapping a ChildEventListener to a Firebase reference which will also maintain the state of whether or not the initial data load has completed.
 *  To do this, it will add a ChildEventListener and also a ValueEventListener for the specified database reference.  After all onChildAdded events have been
 *  called for existing data, the ValueEventListener's onDataChange will fire.  At that time it will set a flag indicating that initial data load has completed. **/

public class StatefulChildListener extends LoggingBase
{
    public interface StatefulChildListenerCallbacks
    {
        void onInitialLoadCompleted();
        void onChildAdded(DataSnapshot dataSnapshot, boolean isInitialDataLoaded);
        void onChildChanged(DataSnapshot dataSnapshot, boolean isInitialDataLoaded);
        void onChildRemoved(DataSnapshot dataSnapshot, boolean isInitialDataLoaded);
        void onCancelled(DatabaseError error);
    }

    boolean mIsInitialDataLoaded;
    DatabaseReference mRef;
    ChildEventListener mChildEventListener;
    ValueEventListener mValueEventListener;

    public StatefulChildListener(final DatabaseReference ref, final StatefulChildListenerCallbacks callbacks)
    {
        mRef = ref;

        mChildEventListener = new ChildEventListener()
        {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName)
            {
                callbacks.onChildAdded(dataSnapshot, mIsInitialDataLoaded);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                callbacks.onChildChanged(dataSnapshot, mIsInitialDataLoaded);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot)
            {
                callbacks.onChildRemoved(dataSnapshot, mIsInitialDataLoaded);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s)
            {
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                callbacks.onCancelled(databaseError);
            }
        };

        mValueEventListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                logMsg("Initial data load completed for reference '%s'", ref.getKey());
                mIsInitialDataLoaded = true;
                callbacks.onInitialLoadCompleted();
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                logError("StatefulChildListener::onCancelled: %s", databaseError.getMessage());
            }
        };

        mRef.addChildEventListener(mChildEventListener);
        mRef.addListenerForSingleValueEvent(mValueEventListener);
    }

    /** Detaches the ChildEventListener / ValueEventListener. **/
    public void shutdown()
    {
        mRef.removeEventListener(mChildEventListener);
        mRef.removeEventListener(mValueEventListener);
    }
}
