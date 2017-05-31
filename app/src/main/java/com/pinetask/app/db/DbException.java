package com.pinetask.app.db;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.pinetask.app.common.PineTaskException;

/** Wrapper for a DatabaseError returned by a Firebase database operation. **/
public class DbException extends PineTaskException
{
    public Query Reference;
    public String OperationDescription;
    public String Message;

    public DbException(Query reference, String operationDescription, String message)
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
