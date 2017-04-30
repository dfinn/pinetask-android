package com.pinetask.app;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pinetask.common.LoggingBase;

/** Wrapper for firebase ChildEventListener with special handling to suppress any errors that occur after the listener has been detached.  User must call shutdown() to detach. **/
public class ChildEventListenerWrapper extends LoggingBase implements ChildEventListener
{
    interface Callback
    {
        void onChildAdded(DataSnapshot dataSnapshot, String s);
        void onChildRemoved(DataSnapshot dataSnapshot);
        void onCancelled(DatabaseError databaseError);
    }

    DatabaseReference mDbRef;
    Callback mCallback;
    boolean mIsActive;

    public ChildEventListenerWrapper(DatabaseReference dbReference, Callback callback)
    {
        mDbRef = dbReference;
        mCallback = callback;
        mIsActive = true;
        mDbRef.addChildEventListener(this);
    }

    public void shutdown()
    {
        logMsg("Shutting down child event listener for %s", mDbRef);
        mIsActive = false;
        mDbRef.removeEventListener(this);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s)
    {
        mCallback.onChildAdded(dataSnapshot, s);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s)
    {
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot)
    {
        mCallback.onChildRemoved(dataSnapshot);
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s)
    {
    }

    @Override
    public void onCancelled(DatabaseError databaseError)
    {
        if (mIsActive)
        {
            mCallback.onCancelled(databaseError);
        }
        else
        {
            logMsg("onCancelled: listener has been shutdown, ignoring error");
        }
    }
}
