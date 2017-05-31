package com.pinetask.app.main;

import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;
import com.squareup.otto.Bus;

import javax.inject.Inject;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** Manages the process of sending or accepting invitations to shared lists. **/
public class InviteManager extends LoggingBase
{
    MainActivity mMainActivity;
    GoogleApiClient mGoogleApiClient;
    String mUserId;

    @Inject DbHelper mDbHelper;
    @Inject PrefsManager mPrefsManager;
    @Inject Bus mBus;

    /** ID of the list to which an invite is being accepted. **/
    String mAcceptedInviteListId;
    public String getAcceptedInviteListId() { return mAcceptedInviteListId; }
    public void setAcceptedInviteListId(String id) { mAcceptedInviteListId = id; }

    /** Base URL used to represent a link to a list (ex: pinetask://lists/12345) **/
    public static String PINETASK_URL_LIST_BASE = "pinetask://lists";

    public InviteManager(MainActivity mainActivity, GoogleApiClient googleApiClient, String userId)
    {
        mMainActivity = mainActivity;
        mGoogleApiClient = googleApiClient;
        mUserId = userId;
        PineTaskApplication.getInstance().getAppComponent().inject(this);
    }

    /** Starts an activity from which the user can select contacts to send an invite.  The invite will be for the list with the ID specified. **/
    public void sendInvite(final String listId, Consumer<Throwable> onError)
    {
        mDbHelper.getListName(listId).subscribe(listName ->
        {
            String urlStr = PINETASK_URL_LIST_BASE + "/" + listId;
            logMsg("Starting invite intent with URL '%s'", urlStr);
            String message = String.format("I'd like to share my list '%s' with you using PineTask.", listName);
            Intent intent = new AppInviteInvitation.IntentBuilder("Share List")
                    .setMessage(message)
                    .setDeepLink(Uri.parse(urlStr))
                    .setCallToActionText("Join List")
                    .build();
            mMainActivity.startActivityForResult(intent, MainActivity.SEND_INVITE_REQUEST_CODE);
        }, ex ->
        {
            onError()
            showUserMessage(true, getString(R.string.error_getting_list_name));
        });
    }

    /** Should be called after the invite sending activity has returned successfully.  Processes the list of IDs for invites that were sent, creating the
     *  appropriate /list_invites/$list_id/$invite_id nodes.
     **/
    public void onInvitesSent(int resultCode, Intent data)
    {
        // Get the list ID of the current list.
        final String listId = mPrefsManager.getCurrentListId();

        // Get the invitation IDs of all sent messages.  Then, for each one, add an entry under /list_invites/<list_id>/<invite_id>.
        // After the invite gets accepted, the recipient will delete this node.
        String[] inviteIds = AppInviteInvitation.getInvitationIds(resultCode, data);
        logMsg("%d invites were sent", inviteIds.length);
        for (final String inviteId : inviteIds) mDbHelper.createInvite(listId, inviteId);
    }

    /** Checks if any Firebase invites are pending.  If so, processes each one, accepting the invite to the shared list. **/
    public void checkForInvites()
    {
        logMsg("Setting up callback to receive invites");

        AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, mMainActivity, false).setResultCallback(result ->
        {
            logMsg("getInvitation:onResult:" + result.getStatus());
            if (result.getStatus().isSuccess()) {
                Intent intent = result.getInvitationIntent();
                String deepLink = AppInviteReferral.getDeepLink(intent);
                String invitationId = AppInviteReferral.getInvitationId(intent);
                logMsg("deepLink: %s", deepLink);
                logMsg("invitationId: %s", invitationId);
                if (deepLink.startsWith(PINETASK_URL_LIST_BASE))
                {
                    acceptInvite(invitationId, deepLink);
                }
                else
                {
                    logError("Deep link does not appear to be a valid PineTask list resource, ignoring");
                }
            }
        });
    }

    /** Accepts an invitation that was received to grant the current user access to the shared list.
     *  If successful, shows a message "You've been granted access to list ..."
     *  If an error occurs at any stage in the process, abort and show an error to the user.
     **/
    private void acceptInvite(final String invitationId, String deepLink)
    {
        final String listId = deepLink.substring(PINETASK_URL_LIST_BASE.length()+1);
        mDbHelper.acceptInvite(listId, mUserId, invitationId).subscribe(listName ->
        {
            logMsg("addUserAsCollaboratorToList: setting mAcceptedInviteListId to %s", listId);
            mAcceptedInviteListId = listId;
            logMsg("Notifying user of access granted to list '%s'", listId);
            showUserMessage(false, getString(R.string.access_granted_to_list_x), listName);
        }, ex->
        {
            logError("addUserAsCollaboratorToList/onError: %s", ex.getMessage());
            mAcceptedInviteListId = null;
            showUserMessage(true, ex.getMessage());
        });
    }

    private String getString(int id)
    {
        return mMainActivity.getString(id);
    }
}
