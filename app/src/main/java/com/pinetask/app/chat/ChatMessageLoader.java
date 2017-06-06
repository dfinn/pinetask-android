package com.pinetask.app.chat;

import com.pinetask.app.common.AddedEvent;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.LoggingBase;

import io.reactivex.Observable;

public class ChatMessageLoader extends LoggingBase
{
    DbHelper mDbHelper;
    PineTaskList mList;
    Long mOriginalMessageCount;
    Long mLoadedCount=0L;

    public ChatMessageLoader(DbHelper dbHelper, PineTaskList list)
    {
        mDbHelper = dbHelper;
        mList = list;
    }

    /** Get the current number of chat messages in the specified list and store as mOriginalMessageCount.
     *  Then, attach a child event listener to emit added/deleted events for chat messages in the list.
     *  For added events:  if more than mOriginalCount message have been loaded, then set flag indicating the message is new (arrived after initial load).
     *  Deleted events are ignored. **/
    public Observable<AddedEvent<ChatMessage>> loadChatMessages()
    {
        return mDbHelper.getChatMessageCount(mList.getId())
                .flatMapObservable(messageCount ->
                {
                    logMsg("loadChatMessages: original message count is %d", messageCount);
                    mOriginalMessageCount = messageCount;
                    return mDbHelper.subscribeChatMessages(mList.getId());
                })
                .filter(event -> event instanceof AddedEvent)
                .map(event -> (AddedEvent<ChatMessage>) event)
                .map(addedEvent ->
                {
                    mLoadedCount++;
                    logMsg("loadChatMessages: mLoadedCount=%d, mOriginalMessageCount=%d", mLoadedCount, mOriginalMessageCount);
                    if (mLoadedCount > mOriginalMessageCount) addedEvent.setIsNew(true);
                    return addedEvent;
                });
    }
}
