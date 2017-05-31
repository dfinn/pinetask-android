package com.pinetask.app.db;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/** Wrapper for a DatabaseError returned by a Firebase database operation. **/
public class DbOperationCanceledException extends DbException
{
    public DatabaseError Error;

    public DbOperationCanceledException(Query reference, DatabaseError error, String operationDescription)
    {
        super(reference, operationDescription, error.getMessage());
        Error = error;
    }
}
