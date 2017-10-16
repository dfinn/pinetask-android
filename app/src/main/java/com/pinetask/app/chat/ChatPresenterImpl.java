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

import java.util.List;

import io.reactivex.disposables.Disposable;

public class ChatPresenterImpl extends BasePresenter implements ChatPresenter
{
    ChatView mChatView;
    String mUserId;
    ActiveListManager mActiveListManager;
    DbHelper mDbHelper;
    ChatMessagesRepository mChatMessagesRepository;
    Disposable mActiveListManagerSubscription;
    PineTaskApplication mApplication;

    public ChatPresenterImpl(String userId, ActiveListManager activeListManager, DbHelper dbHelper, PineTaskApplication application)
    {
        mUserId = userId;
        mActiveListManager = activeListManager;
        mDbHelper = dbHelper;
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
            resetState();
        }
    }

    @Override
    public void attachView(ChatView chatView)
    {
        logMsg("Attaching view");
        mChatView = chatView;
        if (mChatMessagesRepository != null)
        {
            mChatView.showChatLayouts();
            mChatView.showChatMessages(mChatMessagesRepository.getChatMessages());
        }
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
        if (mChatMessagesRepository != null) mChatMessagesRepository.shutdown();
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

    private void resetState()
    {
        if (mChatView != null)
        {
            mChatView.clearChatMessages();
            mChatView.hideChatLayouts();
        }

        if (mChatMessagesRepository != null)
        {
            logMsg("loadChatMessagesForList: Shutting down old mChatMessagesRepository");
            mChatMessagesRepository.shutdown();
        }
    }

    /** Clear layout if visible, shut down previous chat repository if active, and then subscribe to chat messages in the list specified. **/
    private void loadChatMessagesForList(PineTaskList pineTaskList)
    {
        logMsg("loadChatMessagesForList: %s (%s)", pineTaskList.getId(), pineTaskList.getName());
        resetState();
        mChatMessagesRepository = new ChatMessagesRepository(mDbHelper, pineTaskList, this::onInitialMessagesLoaded, this::onChatMessageAdded, this::onChatMessageLoadError);
    }

    private void onInitialMessagesLoaded(List<ChatMessage> messages)
    {
        logMsg("Finished initial load of %d messages", messages.size());
        if (mChatView != null)
        {
            mChatView.showChatLayouts();
            mChatView.showChatMessages(messages);
        }
    }

    /** Post "chat message received" event so that MainActivityPresenter can be notified -- if the chat tab isn't active, chat message will show as a toast. If app is in the
     *  background, a system notification will be raised.  Add chat message to ChatFragment if view is attached.  If message is from another sender, play sound. **/
    private void onChatMessageAdded(ChatMessage chatMessage)
    {
        mActiveListManager.notifyChatMessageReceived(chatMessage);
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
