package com.pinetask.app;

import java.util.List;

/** Interface for a callback to receive a result that is just a list of strings. **/
public interface StringListResultListener
{
    void onResult(List<String> data);
}
