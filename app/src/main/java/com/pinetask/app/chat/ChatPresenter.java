package com.pinetask.app.chat;

public interface ChatPresenter
{
    void attachView(ChatView chatView);
    void detachView();
    void shutdown();
    void sendMessage(String messageText);
}
