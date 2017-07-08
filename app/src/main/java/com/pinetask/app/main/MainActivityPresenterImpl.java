package com.pinetask.app.main;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;

import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListDeletedEvent;
import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.active_list_manager.ListLoadErrorEvent;
import com.pinetask.app.active_list_manager.ListLoadedEvent;
import com.pinetask.app.active_list_manager.NoListsAvailableEvent;
import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.ChatMessageEvent;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.hints.HintManager;
import com.pinetask.app.launch.StartupMessageDialogFragment;
import com.pinetask.app.manage_lists.StartupMessage;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class MainActivityPresenterImpl extends BasePresenter implements MainActivityPresenter
{
    private MainActivityView mView;
    private DbHelper mDbHelper;
    private String mUserId;
    private String mUserName;
    private PineTaskApplication mApplication;
    private PrefsManager mPrefsManager;
    private ActiveListManager mActiveListManager;
    private Disposable mActiveListManagerSubscription;
    private Disposable mUserNameSubscription;
    private StartupMessage mStartupMessage;

    public MainActivityPresenterImpl(DbHelper dbHelper, String userId, PrefsManager prefsManager, PineTaskApplication application, ActiveListManager activeListManager)
    {
        logMsg("Creating MainActivityPresenter");
        mDbHelper = dbHelper;
        mUserId = userId;
        mPrefsManager = prefsManager;
        mApplication = application;
        mActiveListManager = activeListManager;

        // Subscribe to ActiveListManager to be notified of events when the active list is changed.
        mActiveListManagerSubscription = mActiveListManager.subscribe(this::handleActiveListEvent, ex -> logAndShowError(ex, mApplication.getString(R.string.error_processing_list_event)));

        // Subscribe to user name, so it can be displayed in the side navigation drawer.
        mUserNameSubscription = mDbHelper.getUserNameObservable(mUserId).subscribe(this::showUserName, ex -> logAndShowError(ex, mApplication.getString(R.string.error_getting_username)));

        // Check if there's a newer version of the startup message that the user hasn't seen yet. If so, display it.
        loadStartupMessage();
    }

    /** Process events that have been emitted from the ActiveListManager. **/
    private void handleActiveListEvent(ActiveListEvent activeListEvent)
    {
        logMsg("handleActiveListEvent: %s", activeListEvent.getClass().getSimpleName());

        if (activeListEvent instanceof ListLoadedEvent)
        {
            // A new list is active: refresh the name displayed on the list selector button.
            ListLoadedEvent listLoadedEvent = (ListLoadedEvent) activeListEvent;
            PineTaskList newList = listLoadedEvent.ActiveList;
            if (mView != null)
            {
                mView.showCurrentListName(newList.getName());
                mView.hideNoListsFoundMessage();
                mView.showBottomMenuBar();
            }
        }
        else if (activeListEvent instanceof ListLoadErrorEvent)
        {
            // Error occurred when loading list: log and show error.
            ListLoadErrorEvent listLoadErrorEvent = (ListLoadErrorEvent) activeListEvent;
            logAndShowError(listLoadErrorEvent.Error, mApplication.getString(R.string.error_loading_lists));
        }
        else if (activeListEvent instanceof ActiveListDeletedEvent)
        {
            // Show user a message indicating their active list was deleted.
            ActiveListDeletedEvent activeListDeletedEvent = (ActiveListDeletedEvent) activeListEvent;
            showErrorMessage(mApplication.getString(R.string.current_list_x_deleted_switching_to_first_available), activeListDeletedEvent.DeletedListName);
        }
        else if (activeListEvent instanceof NoListsAvailableEvent)
        {
            // The user's last list has been deleted - update list name selector button to indicate this.
            onNoListsAvailable();
        }
        else if (activeListEvent instanceof ChatMessageEvent)
        {
            // A chat message was received in the active list
            ChatMessageEvent chatMessageEvent = (ChatMessageEvent) activeListEvent;
            notifyChatMessage(chatMessageEvent.Message);
        }
    }

    private void notifyChatMessage(ChatMessage chatMessage)
    {
        if (mView != null && mView.isVisible())
        {
            // If app is active but MainActivity is not on the chat tab, show message as a Toast.
            mView.notifyOfChatMessage(chatMessage);
        }
        else
        {
            // Raise system notification if app is currently in the background.  Clicking the notification will open the app and go to the chat tab.
            logMsg("onChatMessageReceived: building pending intent for chat message %s", chatMessage.getId());
            Intent intent = MainActivity.buildLaunchIntent(mApplication, mUserId, chatMessage.getId());
            PendingIntent pendingIntent = PendingIntent.getActivity(mApplication, 0, intent, 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mApplication)
                    .setSmallIcon(R.drawable.launcher_icon)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[] {0, 500, 500, 500})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentTitle(String.format(mApplication.getString(R.string.message_from_x), chatMessage.getSenderName()))
                    .setContentText(chatMessage.getMessage());
            NotificationManager mNotificationManager = (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }
    }

    /** Unless this is the first app launch, query the server for the startup message and its version.  If the user hasn't seen it yet, display the message now. **/
    private void loadStartupMessage()
    {
        if (mPrefsManager.getIsFirstLaunch()) return;
        mDbHelper.getStartupMessageIfUnread(mUserId)
                .subscribe(startupMessage ->
                {
                    if (startupMessage.version > 0)
                    {
                        mStartupMessage = startupMessage;
                        showStartupMessage();
                    }
                }, ex ->
                {
                    logAndShowError(ex, mApplication.getString(R.string.error_loading_startup_message));
                });
    }

    @Override
    public void attach(MainActivityView view)
    {
        mView = view;
        PineTaskList activeList = mActiveListManager.getActiveList();
        if (mUserName != null) mView.showUserName(mUserName);
        if (activeList != null) mView.showCurrentListName(activeList.getName());
        if (mStartupMessage != null) showStartupMessage();
    }

    private void showStartupMessage()
    {
        if (mView != null)
        {
            logMsg("Showing startup message version %d", mStartupMessage.version);
            mView.showStartupMessage(mStartupMessage);
            mStartupMessage = null;
        }
    }

    @Override
    public void detach()
    {
        mView = null;
    }

    @Override
    public void shutdown()
    {
        logMsg("shutdown: shutting down subscriptions");
        if (mActiveListManagerSubscription != null) mActiveListManagerSubscription.dispose();
        if (mUserNameSubscription != null) mUserNameSubscription.dispose();
    }

    /** Load info for all lists that the user has access to, and display them in selector dialog for user to switch lists.
     *  If any particular list fails to load, log exception detail but proceed loading the other lists. **/
    @Override
    public void onListSelectorClicked()
    {
        mDbHelper.getListIdsForUser(mUserId)
                .flatMap(mDbHelper::tryGetPineTaskList)
                .toList()
                .subscribe(lists ->
                {
                    logMsg("onListSelectorClicked: loaded %s lists", lists.size());
                    if ((lists.size() > 0) && (mView != null)) mView.showListChooser(lists);
                }, ex ->
                {
                    logAndShowError(ex, "Error loading lists");
                });
    }

    @Override
    protected void showErrorMessage(String message, Object... args)
    {
        if (mView != null) mView.showError(message, args);
    }

    private void showUserName(String userName)
    {
        mUserName = userName;
        if (mView != null) mView.showUserName(userName);
    }

    @Override
    public void onListSelected(PineTaskList list)
    {
        mActiveListManager.setActiveList(list);
    }

    /** Set current list to null in shared prefs. Update UI state to indicate there is no current list. **/
    private void onNoListsAvailable()
    {
        if (mView != null)
        {
            mView.showCurrentListName(mApplication.getString(R.string.no_lists_found));
            mView.hideBottomMenuBar();
            mView.showNoListsFoundMessage();
            if (mPrefsManager.getIsFirstLaunch())
            {
                // If this is the first app launch, show "Add List" dialog and pass the flag so that it prompts the user to create their first list.
                logMsg("First launch: showing Add List dialog");
                mView.showAddListDialog();
                mPrefsManager.setIsFirstLaunch(false);
            }
        }
    }

    @Override
    public void purgeCompletedItems(String listId)
    {
        mDbHelper.purgeCompletedItems(listId).subscribe(() ->
        {
            logMsg("Purging completed items completed");
        }, ex ->
        {
            logAndShowError(ex, mApplication.getString(R.string.error_purging_completed_items));
        });
    }

    @Override
    public void onPurgeCompletedItemsSelected()
    {
        PineTaskList currentList = mActiveListManager.getActiveList();
        if (currentList != null)
        {
            logMsg("onPurgeCompletedItemsSelected: starting purge for list %s", currentList.getId());
            if (mView != null) mView.showPurgeCompletedItemsDialog(currentList.getId(), currentList.getName());
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }
    }

    @Override
    public void startShoppingTrip()
    {
        PineTaskList currentList = mActiveListManager.getActiveList();
        if (currentList != null)
        {
            logMsg("startShoppingTrip: start shopping trip for list %s", currentList.getId());
            mDbHelper.setShoppingTripForActiveList(currentList.getId(), true);
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }
    }

    @Override
    public void stopShoppingTrip()
    {
        PineTaskList currentList = mActiveListManager.getActiveList();
        if (currentList != null)
        {
            logMsg("stopShoppingTrip: end shopping trip for list %s", currentList.getId());
            mDbHelper.setShoppingTripForActiveList(currentList.getId(), false);
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }
    }
}
