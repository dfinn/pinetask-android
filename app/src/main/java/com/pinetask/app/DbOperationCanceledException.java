package com.pinetask.app;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

/** Wrapper for a DatabaseError returned by a Firebase database operation. **/
public class DbOperationCanceledException extends DbException
{
    public DatabaseError Error;

    public DbOperationCanceledException(DatabaseReference reference, DatabaseError error, String operationDescription)
    {
        super(reference, operationDescription, error.getMessage());
        Error = error;
    }
}
