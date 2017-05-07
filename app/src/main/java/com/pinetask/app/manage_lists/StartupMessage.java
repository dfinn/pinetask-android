package com.pinetask.app.manage_lists;

/** Represents an informational message to be displayed to the user when the app starts up. **/
public class StartupMessage
{
    public String text;
    public int version;

    @Override
    public String toString()
    {
        return String.format("version=%d, text=%s", version, text);
    }
}
