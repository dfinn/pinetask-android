package com.pinetask.app.chat;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.common.LoggingBase;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

class ChatMessagesRepository extends LoggingBase
{
    private Long mOriginalMessageCount;
    private Disposable mSubscription;
    private List<ChatMessage> mChatMessages = new ArrayList<>();
    List<ChatMessage> getChatMessages() { return mChatMessages; }

    /** Get count of existing chat messages and store it. Subscribe to chat messages in list. As soon as expected initial count has been loaded, notify
     *  the mInitialLoadCompletedListener.  Then, continue to emit subsequent messages to the mChatMessageAddedListener. **/
    ChatMessagesRepository(DbHelper dbHelper, PineTaskList list, Consumer<List<ChatMessage>> initialLoadCompleted, Consumer<ChatMessage> messageAdded, Consumer<Throwable> onError)
    {
        mSubscription = dbHelper.getChatMessageCount(list.getId())
                .map(messageCount -> mOriginalMessageCount = messageCount)
                .doOnSuccess(messageCount -> logMsg("Initial message count is %d", messageCount))
                .doOnSuccess(messageCount -> { if (messageCount == 0) initialLoadCompleted.accept(mChatMessages); } )
                .map(__ -> list.getId())
                .flatMapObservable(dbHelper::subscribeChatMessages)
                .filter(event -> event instanceof AddedEvent)
                .map(event -> (AddedEvent<ChatMessage>) event)
                .map(addedEvent -> addedEvent.Item)
                .flatMap(dbHelper::populateUserName)
                .filter(chatMessage -> { boolean dup = mChatMessages.contains(chatMessage); if (dup) logMsg("Duplicate message %s received", chatMessage.getId()); return !dup; })
                .map(chatMessage ->
                {
                    mChatMessages.add(chatMessage);
                    String createdStr = new DateTime(chatMessage.getCreatedAtMs()).toString();
                    logMsg("loadChatMessages: message timestamp=%d (%s), mLoadedCount=%d, mOriginalMessageCount=%d", chatMessage.getCreatedAtMs(), createdStr, mChatMessages.size(), mOriginalMessageCount);
                    if (mChatMessages.size() == mOriginalMessageCount)
                    {
                        logMsg("Expected original message count (%d items) have loaded, notifying mInitialLoadCompletedListener", mOriginalMessageCount);
                        initialLoadCompleted.accept(mChatMessages);
                    }
                    return chatMessage;
                })
                .filter(chatMessage -> mChatMessages.size() > mOriginalMessageCount)
                .map(chatMessage -> { chatMessage.setIsNewMessage(true); return chatMessage; } )
                .doOnDispose(() -> logMsg("loadChatMessages: disposing subscription"))
                .subscribe(messageAdded, onError);
    }

    public void shutdown()
    {
        mSubscription.dispose();
    }
}
