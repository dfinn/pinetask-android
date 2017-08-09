package com.pinetask.app.common;

public class PineTaskUtil
{
    /** Return true if both objects are null, or if they are equal.
     *  Note: Objects.equals() not available below API level 19, or we'd use that instead.
     **/
    public static boolean equalsOrNull(Object obj1, Object obj2)
    {
        if (obj1 == null && obj2 == null) return true;
        else return (obj1 != null && obj1.equals(obj2));
    }
}
