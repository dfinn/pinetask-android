package com.pinetask.app.chat;

import java.util.List;

public interface ChatView
{
    /** Shows all chat messages in the list, replacing any current contents. **/
    void showChatMessages(List<ChatMessage> messages);

    /** Adds the specified chat message to the bottom of the list. **/
    void addChatMessage(ChatMessage chatMessage);

    /** Plays a sound to indicate a new chat message arrived. **/
    void playNewMessageSound();

    /** Clears all displayed chat messages. **/
    void clearChatMessages();

    /** Show the message list, input EditText, and "send" button. **/
    void showChatLayouts();

    /** Hide the message list, input EditText, and "send" button. **/
    void hideChatLayouts();

    void showError(String message, Object... args);
}
