package com.pinetask.app.common;

import com.pinetask.app.active_list_manager.ActiveListEvent;
import com.pinetask.app.chat.ChatMessage;

/** Wrapper class indicating that a chat message was received for the active list. **/
public class ChatMessageEvent extends ActiveListEvent
{
    public ChatMessage Message;
    public ChatMessageEvent(ChatMessage message)
    {
        Message = message;
    }
}
