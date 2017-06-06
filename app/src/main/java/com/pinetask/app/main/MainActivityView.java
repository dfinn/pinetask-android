package com.pinetask.app.main;

import com.pinetask.app.common.PineTaskList;

import java.util.List;

public interface MainActivityView
{
    void showError(String message, Object... args);
    void showErrorAndExit(String message, Object... args);
    void showCurrentListName(String listName);
    void showListChooser(List<PineTaskList> lists);
    void showUserName(String userName);
    void showStartupMessage(String text, int versionNumber);
    void showAddListDialog();
    void showBottomMenuBar();
    void hideBottomMenuBar();
    void showNoListsFoundMessage();
    void hideNoListsFoundMessage();
    void showPurgeCompletedItemsDialog(String listId, String listName);
}