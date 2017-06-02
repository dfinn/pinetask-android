package com.pinetask.app.list_members;

import com.pinetask.app.common.PineTaskList;

public interface MembersView
{
    /** Add the person to the displayed list. **/
    void addListMember(MemberInfo memberInfo);

    /** Remove the person from the displayed list. **/
    void removeListMember(String userId);

    /** Remove all people from the displayed list **/
    void clearListDisplay();

    /** Show or hide the list **/
    void setListVisible(boolean visible);

    /** Show or hide the "Add" button **/
    void setAddButtonVisible(boolean visible);

    /** Start the invite process for the list id specified **/
    void launchInviteProcess(PineTaskList list);

    /** Display error message to the user. **/
    void showError(String message, Object... args);
}