package com.pinetask.app;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

/** Wrapper for a DatabaseError returned by a Firebase database operation. **/
public class DbException extends PineTaskException
{
    public DatabaseReference Reference;
    public String OperationDescription;
    public String Message;

    public DbException(DatabaseReference reference, String operationDescription, String message)
    {
        Reference = reference;
        OperationDescription = operationDescription;
        Message = String.format("Error in operation '%s' for node %s: %s", OperationDescription, Reference.toString(), message);
    }

    @Override
    public String getMessage()
    {
        return Message;
    }
}
