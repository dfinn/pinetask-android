package com.pinetask.app.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/** Scope for Dagger dependency injection to hold objects that are available while a user is logged in (ie, on the MainActivity and its ListItems/Chat/Members fragments) **/
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface UserScope
{
}
