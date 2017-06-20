package com.pinetask.app.db;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.ChildEventBase;
import com.pinetask.app.common.DeletedEvent;
import com.pinetask.app.common.UpdatedEvent;
import com.pinetask.common.LoggingBase;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

/** RxJava wrapper which creates an Observable to emit events for items being added, updated, or deleted at the specified database location.
 *  When the subscription is disposed, the Firebase ChildEventListener is detached. **/
public class ChildEventObservable<T> extends LoggingBase
{
    ChildEventListener mListener;
    DatabaseReference mDbRef;
    String mOperationDescription;
    Class<T> mClass;

    public ChildEventObservable(Class<T> cl, DatabaseReference dbRef, String operationDescription)
    {
        mDbRef = dbRef;
        mOperationDescription = operationDescription;
        mClass = cl;
    }

    public Observable<ChildEventBase<T>> attachListener()
    {
        return Observable.create((ObservableEmitter<ChildEventBase<T>> emitter) ->
        {
            mListener = mDbRef.addChildEventListener(new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    logMsg("onChildAdded(%s): %s", mDbRef, dataSnapshot.getKey());
                    T value = DbHelper.getValueFromSnapshot(dataSnapshot, mClass);
                    if (! emitter.isDisposed()) emitter.onNext(new AddedEvent<>(value));
                    else logError("onChildAdded -- observable has been disposed, won't call onNext");
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s)
                {
                    T value = DbHelper.getValueFromSnapshot(dataSnapshot, mClass);
                    if (! emitter.isDisposed()) emitter.onNext(new UpdatedEvent<>(value));
                    else logError("onChildChanged -- observable has been disposed, won't call onNext");
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot)
                {
                    T value = DbHelper.getValueFromSnapshot(dataSnapshot, mClass);
                    if (! emitter.isDisposed()) emitter.onNext(new DeletedEvent<>(value));
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
