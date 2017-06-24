package com.pinetask.app.chat;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;
import com.pinetask.app.db.UsesKeyIdentifier;

import org.joda.time.DateTime;

import java.util.Map;

/** Represents one chat message: a timestamp, a message string, and the ID of the user who sent it. **/
public class ChatMessage implements UsesKeyIdentifier
{
    /** Key identifying the item **/
    private String mKey;
    @Exclude    // No need to include this when writing the object to Firebase.
    public String getId() { return mKey; }
    public void setId(String key) { mKey = key; }

    /** Timestamp when the message was sent. **/
    long mCreatedAt;
    public Map<String, String> getCreatedAt() { return ServerValue.TIMESTAMP; }
    @Exclude
    public long getCreatedAtMs() { return mCreatedAt; }
    public void setCreatedAt(long createdAt) { mCreatedAt = createdAt; }

    /** Message text **/
    private String mMessage;
    public String getMessage() { return mMessage; }
    public void setMessage(String message) { mMessage = message; }

    /** User ID of the sender. **/
    private String mSenderId;
    public String getSenderId() { return mSenderId; }
    public void setSenderId(String senderId) { mSenderId = senderId; }

    /** Name of the sender. **/
    private String mName;
    @Exclude    // No need to include this when writing the object to Firebase.
    public String getSenderName() { return mName; }
    public void setSenderName(String name) { mName = name; }

    /** Indicates if the message is new (ie, has not yet been displayed to the user) **/
    private boolean mIsNewMessage;
    @Exclude    // No need to include this when writing the object to Firebase.
    public boolean getIsNewMessage() { return mIsNewMessage; }
    public void setIsNewMessage(boolean isNew) { mIsNewMessage = isNew; }

    public ChatMessage()
    {
    }

    public ChatMessage(String message, String senderId)
    {
        mMessage = message;
        mSenderId = senderId;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ChatMessage)
        {
            ChatMessage other = (ChatMessage) obj;
            return mKey.equals(other.getId());
        }
        else
        {
            return false;
        }
    }
}
