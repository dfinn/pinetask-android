package com.pinetask.app.chat;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ChatMessageLoader extends LoggingBase
{
    Long mOriginalMessageCount;
    Disposable mSubscription;
    List<ChatMessage> mChatMessages = new ArrayList<>();
    public List<ChatMessage> getChatMessages() { return mChatMessages; }

    /** Get count of existing chat messages and store it. Subscribe to chat messages in list. As soon as expected initial count has been loaded, notify
     *  the mInitialLoadCompletedListener.  Then, continue to emit subsequent messages to the mChatMessageAddedListener. **/
    public ChatMessageLoader(DbHelper dbHelper, PineTaskList list, Consumer<List<ChatMessage>> initialLoadCompleted, Consumer<ChatMessage> messageAdded, Consumer<Throwable> onError)
    {
        mSubscription = dbHelper.getChatMessageCount(list.getId())
                .map(messageCount -> mOriginalMessageCount = messageCount)
                .map(messageCount -> list.getId())
                .flatMapObservable(dbHelper::subscribeChatMessages)
                .filter(event -> event instanceof AddedEvent)
                .map(event -> (AddedEvent<ChatMessage>) event)
                .map(addedEvent -> addedEvent.Item)
                .flatMap(dbHelper::populateUserName)
                .map(chatMessage ->
                {
                    mChatMessages.add(chatMessage);
                    logMsg("loadChatMessages: mLoadedCount=%d, mOriginalMessageCount=%d", mChatMessages.size(), mOriginalMessageCount);
                    if (mChatMessages.size() == mOriginalMessageCount)
                    {
                        logMsg("Expected original message count (%d items) have loaded, notifying mInitialLoadCompletedListener", mOriginalMessageCount);
                        initialLoadCompleted.accept(mChatMessages);
                    }
                    return chatMessage;
                })
                .filter(chatMessage -> mChatMessages.size() > mOriginalMessageCount)
                .doOnDispose(() -> logMsg("loadChatMessages: disposing subscription"))
                .subscribe(messageAdded, onError);
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }
}
