package com.pinetask.app;

import java.util.List;

public class PineTaskListWithCollaborators extends PineTaskList
{
    private List<String> mCollaboratorIds;
    public List<String> getCollaboratorIds() { return mCollaboratorIds; }
    public void setCollaboratorIds(List<String> collaboratorIds) { mCollaboratorIds = collaboratorIds; }

    public PineTaskListWithCollaborators()
    {
    }

    public PineTaskListWithCollaborators(PineTaskList pineTaskList, List<String> collaboratorIds)
    {
        mKey = pineTaskList.getKey();
        mOwnerId = pineTaskList.getOwnerId();
        mName = pineTaskList.getName();
        mCollaboratorIds = collaboratorIds;
    }
}
