package com.pinetask.app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pinetask.common.Logger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Action;

/** Observable wrapper for a Firebase database query for a single string value at a specified reference.  Value in onDataChange() will be provided in onNext().
 *  When observable is disposed, the listener is removed.
 **/
public class ObservableStringQuery
{
    ValueEventListener mValueEventListener;
    DatabaseReference mRef;
    String mOperationDescription;

    public ObservableStringQuery(DatabaseReference ref,  String operationDescription)
    {
        mRef = ref;
        mOperationDescription = operationDescription;
    }

    public static Observable<String> fromRef(DatabaseReference ref,  String operationDescription)
    {
        ObservableStringQuery observableStringQuery = new ObservableStringQuery(ref, operationDescription);
        return observableStringQuery.getObservable();
    }

    public Observable<String> getObservable()
    {
        return Observable.create(new ObservableOnSubscribe<String>()
        {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception
            {
                mValueEventListener = mRef.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        Object obj = dataSnapshot.getValue();
                        if (obj != null && !(obj instanceof String))
                        {
                            String msg = String.format("Unexpected data type '%s'", obj.getClass().getSimpleName());
                            emitter.onError(new DbException(mRef, mOperationDescription, msg));
                        }
                        else
                        {
                            // Null values not allowed in RxJava 2 - pass empty string instead.
                            String strVal = obj==null ? "" : (String)obj;
                            emitter.onNext(strVal);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        emitter.onError(new DbOperationCanceledException(mRef, databaseError, mOperationDescription));
                    }
                });
            }
        })
        .doOnDispose(new Action()
        {
            @Override
            public void run() throws Exception
            {
                Logger.logMsg(ObservableStringQuery.class, "doOnDispose: removing listener from %s", mRef.toString());
                if (mValueEventListener!=null) mRef.removeEventListener(mValueEventListener);
            }
        });
    }
}
