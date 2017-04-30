package com.pinetask.app;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pinetask.common.LoggingBase;

import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;

/** Handles the flow for setting up an anonymous Firebase account.  Emits the FirebaseUser for the new user when done. **/
public class AnonymousAccountCreator extends LoggingBase
{
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;

    public static Single<FirebaseUser> createAnonymousAccount()
    {
        AnonymousAccountCreator anonymousAccountCreator = new AnonymousAccountCreator();
        return anonymousAccountCreator.getCompletable();
    }

    public Single<FirebaseUser> getCompletable()
    {
        mAuth = FirebaseAuth.getInstance();

        return Single.create((SingleEmitter<FirebaseUser> emitter) ->
            {
                logMsg("Registering AuthStateListener");
                mAuthStateListener = authStateListener(emitter);
                mAuth.addAuthStateListener(mAuthStateListener);
                logMsg("Calling signInAnonymously");
                mAuth.signInAnonymously().addOnCompleteListener(anonymousOnCompleteListener(emitter));
            })
            .doOnDispose(() ->
            {
                logMsg("onDispose: shutting down mAuthStateListener");
                if (mAuthStateListener != null) mAuth.removeAuthStateListener(mAuthStateListener);
            });
    }

    /** Returns a listener to monitor the Firebase authentication callback.  Emits onComplete() when onAuthStateChange() is called and getCurrentUser() reports a signed in user.
     *  Note: emitter onError() will only happen in anonymousOnCompleteListener()
     **/
    private FirebaseAuth.AuthStateListener authStateListener(SingleEmitter<FirebaseUser> emitter)
    {
        return (@NonNull FirebaseAuth firebaseAuth) ->
            {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null)
                {
                    logMsg("onAuthStateChanged: User is signed in - anonymous account %s setup completed", user.getUid());
                    emitter.onSuccess(user);
                }
                else
                {
                    logMsg("onAuthStateChanged: User is signed out");
                }
            };
    }

    /** Listens to the completion state for Firebase anonymous account setup call: emits onError() if the task did not complete successfully.
     *  Note: emitter onComplete() will only happen in authStateListener()
     **/
    private OnCompleteListener<AuthResult> anonymousOnCompleteListener(SingleEmitter<FirebaseUser> emitter)
    {
        return (@NonNull Task<AuthResult> task) ->
            {
                if (task.isSuccessful())
                {
                    logMsg("anonymousOnCompleteListener: success");
                }
                else
                {
                    logError("anonymousOnCompleteListener: error");
                    logException(AnonymousAccountCreator.class, task.getException());
                    emitter.onError(task.getException());
                }
            };
    }
}
