package com.pinetask.app.db;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.AddedOrDeletedEvent;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.common.LoggingBase;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

/** RxJava wrapper which creates an Observable to emit events for keys being added or deleted at the specified database location.
 *  When the subscription is disposed, the Firebase ChildEventListener is detached. **/
public class KeyAddedOrDeletedObservable extends LoggingBase
{
    ChildEventListener mListener;
    DatabaseReference mDbRef;
    String mOperationDescription;

    public static Observable<AddedOrDeletedEvent<String>> getKeyAddedOrDeletedEventsAt(DatabaseReference ref, String operationDescription)
    {
        KeyAddedOrDeletedObservable keyAddedOrDeletedObservable = new KeyAddedOrDeletedObservable(ref, operationDescription);
        return keyAddedOrDeletedObservable.attachListener();
    }

    public KeyAddedOrDeletedObservable(DatabaseReference dbRef, String operationDescription)
    {
        mDbRef = dbRef;
        mOperationDescription = operationDescription;
    }

    private Observable<AddedOrDeletedEvent<String>> attachListener()
    {
        return Observable.create((ObservableEmitter<AddedOrDeletedEvent<String>> emitter) ->
        {
            mListener = mDbRef.addChildEventListener(new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    logMsg("onChildAdded(%s): %s", mDbRef, dataSnapshot.getKey());
                    emitter.onNext(new AddedEvent<>(dataSnapshot.getKey()));
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s)
                {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot)
                {
                    emitter.onNext(new DeletedEvent<>(dataSnapshot.getKey()));
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s)
                {
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                    emitter.onError(new DbOperationCanceledException(mDbRef, databaseError, mOperationDescription));
                }
            });
        })
        .doOnDispose(() ->
        {
            if (mListener != null)
            {
                logMsg("Shutting down ChildEventListener at %s", mDbRef);
                mDbRef.removeEventListener(mListener);
            }
        });
    }
}
