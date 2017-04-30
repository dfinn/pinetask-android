package com.pinetask.app;

/** Represents a message (info or error) to be displayed to the user. **/
public class UserMessage
{
    public boolean IsError;
    public String Message;
    public UserMessage(boolean isError, String text, Object...args)
    {
        IsError = isError;
        Message = String.format(text,args);
    }
}
