package com.pinetask.app.chat;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.active_list_manager.ListLoadedEvent;
import com.pinetask.app.active_list_manager.NoListsAvailableEvent;
import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;
import com.squareup.otto.Bus;

import io.reactivex.disposables.Disposable;

public class ChatPresenterImpl extends BasePresenter implements ChatPresenter
{
    ChatView mChatView;
    String mUserId;
    ActiveListManager mActiveListManager;
    FirebaseDatabase mDatabase;
    DbHelper mDbHelper;
    Disposable mChatMessagesSubscription;
    Disposable mActiveListManagerSubscription;
    Bus mEventBus;
    PineTaskApplication mApplication;

    public ChatPresenterImpl(String userId, ActiveListManager activeListManager, FirebaseDatabase db, DbHelper dbHelper, Bus eventBus, PineTaskApplication application)
    {
        mUserId = userId;
        mActiveListManager = activeListManager;
        mDatabase = db;
        mDbHelper = dbHelper;
        mEventBus = eventBus;
        mApplication = application;
        mActiveListManagerSubscription = activeListManager.subscribe(this::processActiveListEvent, ex -> logError("Error from activeListManager: %s", ex.getMessage()));
    }

    private void processActiveListEvent(ActiveListEvent activeListEvent)
    {
        if (activeListEvent instanceof ListLoadedEvent)
        {
            ListLoadedEvent listLoadedEvent = (ListLoadedEvent) activeListEvent;
            loadChatMessagesForList(listLoadedEvent.ActiveList);
        }
        else if (activeListEvent instanceof NoListsAvailableEvent)
        {
            if (mChatView != null)
            {
                mChatView.clearChatMessages();
                mChatView.hideChatLayouts();
            }
        }
    }

    @Override
    public void attachView(ChatView chatView)
    {
        mChatView = chatView;
    }

    @Override
    public void detachView()
    {
        mChatView = null;
    }

    @Override
    public void shutdown()
    {
        if (mActiveListManagerSubscription != null) mActiveListManagerSubscription.dispose();
        if (mChatMessagesSubscription != null) mChatMessagesSubscription.dispose();
    }

    @Override
    public void sendMessage(String message)
    {
        PineTaskList activeList = mActiveListManager.getActiveList();
        if (activeList != null)
        {
            ChatMessage chatMessage = new ChatMessage(message, mUserId);
            logMsg("Sending chat message: %s", message.replace("%", "%%"));
            mDbHelper.sendChatMessage(activeList.getId(), chatMessage);
        }
        else
        {
            showErrorMessage(mApplication.getString(R.string.error_no_current_list));
        }
    }

    @Override
    protected void showErrorMessage(String message, Object... args)
    {
        if (mChatView != null) mChatView.showError(message, args);
    }

    private void loadChatMessagesForList(PineTaskList pineTaskList)
    {
        logMsg("loadChatMessagesForList: %s (%s)", pineTaskList.getId(), pineTaskList.getName());

        if (mChatView != null)
        {
            mChatView.clearChatMessages();
            mChatView.showChatLayouts();
        }

        if (mChatMessagesSubscription != null)
        {
            logMsg("Disposing of old subscription");
            mChatMessagesSubscription.dispose();
        }

        ChatMessageLoader chatMessageLoader = new ChatMessageLoader(mDbHelper, pineTaskList);
        mChatMessagesSubscription = chatMessageLoader.loadChatMessages().subscribe(addedEvent ->
        {
            if (mChatView != null)
            {
                mChatView.showChatMessage(addedEvent.Item);
                // If it's a new message (received after the initial load, and not from the current user): play notification sound and post to event bus.
                if (addedEvent.getIsNew() && !mUserId.equals(addedEvent.Item.getSenderId()))
                {
                    mChatView.playNewMessageSound();
                    mEventBus.post(addedEvent.Item);
                }
            }
        }, ex ->
        {
            logError("Error in chat messages subscription");
            logException(ex);
        });
    }
}
