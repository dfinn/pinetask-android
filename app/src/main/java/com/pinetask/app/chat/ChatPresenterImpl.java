package com.pinetask.app.chat;

import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.active_list_manager.ListLoadedEvent;
import com.pinetask.app.active_list_manager.NoListsAvailableEvent;
import com.pinetask.app.common.BasePresenter;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;
import com.squareup.otto.Bus;

import java.util.List;

import io.reactivex.disposables.Disposable;

public class ChatPresenterImpl extends BasePresenter implements ChatPresenter
{
    ChatView mChatView;
    String mUserId;
    ActiveListManager mActiveListManager;
    FirebaseDatabase mDatabase;
    DbHelper mDbHelper;
    ChatMessageLoader mChatMessageLoader;
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
        logMsg("Attaching view");
        mChatView = chatView;
        if (mChatMessageLoader != null) mChatView.showChatMessages(mChatMessageLoader.getChatMessages());
    }

    @Override
    public void detachView()
    {
        logMsg("Detaching view");
        mChatView = null;
    }

    @Override
    public void shutdown()
    {
        if (mActiveListManagerSubscription != null) mActiveListManagerSubscription.dispose();
        if (mChatMessageLoader != null) mChatMessageLoader.shutdown();
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

    /** Subscribe to chat messages in the list specified.  As chat messages are loaded, add them to the cached list (mChatMessages), and display them to the view.
     *  If it's a new message from a sender other than the current user, then play "new message" sound and post to eventbus (MainActivity will show a pop-up if Chat fragment not active).
     **/
    private void loadChatMessagesForList(PineTaskList pineTaskList)
    {
        logMsg("loadChatMessagesForList: %s (%s)", pineTaskList.getId(), pineTaskList.getName());

        if (mChatView != null)
        {
            mChatView.clearChatMessages();
            mChatView.showChatLayouts();
        }

        if (mChatMessageLoader != null)
        {
            logMsg("loadChatMessagesForList: Shutting down old mChatMessageLoader");
            mChatMessageLoader.shutdown();
        }

        mChatMessageLoader = new ChatMessageLoader(mDbHelper, pineTaskList, this::onInitialMessagesLoaded, this::onChatMessageAdded, this::onChatMessageLoadError);
    }

    private void onInitialMessagesLoaded(List<ChatMessage> messages)
    {
        logMsg("Finished initial load of %d messages", messages.size());
        if (mChatView != null) mChatView.showChatMessages(messages);
    }

    private void onChatMessageAdded(ChatMessage chatMessage)
    {
        mEventBus.post(chatMessage);
        if (mChatView != null)
        {
            mChatView.addChatMessage(chatMessage);
            if (!mUserId.equals(chatMessage.getSenderId())) mChatView.playNewMessageSound();
        }
    }

    private void onChatMessageLoadError(Throwable ex)
    {
        logError("Error in chat messages subscription");
        logException(ex);
    }
}
