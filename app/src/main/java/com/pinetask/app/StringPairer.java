package com.pinetask.app;

import io.reactivex.functions.BiFunction;

public class StringPairer implements BiFunction<String,String,StringPair>
{
    @Override
    public StringPair apply(String s, String s2) throws Exception
    {
        return new StringPair(s, s2);
    }
}
