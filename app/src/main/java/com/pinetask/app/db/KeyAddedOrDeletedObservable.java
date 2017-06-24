package com.pinetask.app.db;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
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

    public static Observable<ChildEventBase<String>> subscribeKeyAddedOrDeletedEventsAt(DatabaseReference ref, String operationDescription)
    {
        KeyAddedOrDeletedObservable keyAddedOrDeletedObservable = new KeyAddedOrDeletedObservable(ref, operationDescription);
        return keyAddedOrDeletedObservable.attachListener();
    }

    public KeyAddedOrDeletedObservable(DatabaseReference dbRef, String operationDescription)
    {
        mDbRef = dbRef;
        mOperationDescription = operationDescription;
    }

    private Observable<ChildEventBase<String>> attachListener()
    {
        return Observable.create((ObservableEmitter<ChildEventBase<String>> emitter) ->
        {
            mListener = mDbRef.addChildEventListener(new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    logMsg("onChildAdded(%s): %s", mDbRef, dataSnapshot.getKey());
                    if (! emitter.isDisposed()) emitter.onNext(new AddedEvent<>(dataSnapshot.getKey()));
                    else logError("onChildAdded -- observable has been disposed, won't call onNext");
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s)
                {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot)
                {
                    if (! emitter.isDisposed()) emitter.onNext(new DeletedEvent<>(dataSnapshot.getKey()));
                    else logError("onChildRemoved -- observable has been disposed, won't call onNext");
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s)
                {
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                    if (!emitter.isDisposed())
                    {
                        emitter.onError(new DbOperationCanceledException(mDbRef, databaseError, mOperationDescription));
                    }
                    else
                    {
                        logError("onCancelled: observable has been disposed, won't invoke onError (ref=%s, error=%s)", mDbRef, databaseError);
                    }
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
