package com.pinetask.app.manage_lists;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.PineTaskListWithCollaborators;
import com.pinetask.app.R;
import com.pinetask.app.common.Logger;

import java.util.Collection;
import java.util.List;

/** Adapter for items displayed on the Manage Lists activity.  Shows list name, rename button, and delete button. Clicking list name will switch to that list. **/
public class ManageListsAdapter extends RecyclerView.Adapter<ManageListsAdapter.ListViewHolder>
{
    List<PineTaskListWithCollaborators> mPineTaskLists;
    ManageListsActivity mManageListsActity;
    String mUserId;

    public ManageListsAdapter(ManageListsActivity manageListsActivity, List<PineTaskListWithCollaborators> lists, String userId)
    {
        mManageListsActity = manageListsActivity;
        mPineTaskLists = lists;
        mUserId = userId;
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pinetask_list_list_row, parent, false);
        ListViewHolder viewHolder = new ListViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, int position)
    {
        final PineTaskList list = mPineTaskLists.get(position);
        holder.mListNameTextView.setText(list.getName());

        boolean renameAndDeleteEnabled = (list.getOwnerId().equals(mUserId));
        enableOrDisableButton(holder.mRenameButton, renameAndDeleteEnabled);
        enableOrDisableButton(holder.mDeleteImageButton, renameAndDeleteEnabled);
    }

    private void enableOrDisableButton(ImageButton button, boolean enabled)
    {
        button.setAlpha(enabled ? 1.0f : 0.5f);
        button.setEnabled(enabled);
    }

    @Override
    public int getItemCount()
    {
        return mPineTaskLists.size();
    }

    public class ListViewHolder extends RecyclerView.ViewHolder
    {
        ImageButton mRenameButton, mDeleteImageButton;
        TextView mListNameTextView;
        public ListViewHolder(View itemView)
        {
            super(itemView);
            mRenameButton = (ImageButton) itemView.findViewById(R.id.renameImageButton);
            mDeleteImageButton = (ImageButton) itemView.findViewById(R.id.deleteImageButton);
            mListNameTextView = (TextView) itemView.findViewById(R.id.listNameTextView);
            mRenameButton.setOnClickListener((View v) -> mManageListsActity.promptToRenameList(mPineTaskLists.get(getAdapterPosition())));
            mDeleteImageButton.setOnClickListener((View v) -> mManageListsActity.promptToDeleteList(mPineTaskLists.get(getAdapterPosition())));
            mListNameTextView.setOnClickListener((View v) -> mManageListsActity.switchToList(mPineTaskLists.get(getAdapterPosition())));
        }
    }

    public void add(PineTaskListWithCollaborators newList)
    {
        // If list already contains a PineTaskList with the same ID, then just ignore it.
        for (PineTaskList l : mPineTaskLists)
        {
            if (l.equals(newList))
            {
                logMsg("Ignoring add for duplicate list %s", newList.getKey());
                return;
            }
        }

        // Loop through all existing list items until we find the index of the one to insert.
        int pos=0;
        while (pos< mPineTaskLists.size())
        {
            PineTaskList listAtPos = mPineTaskLists.get(pos);
            // If comparator returns negative value, it means list is lexically before listAtPos, so exit the loop.
            int result = PineTaskList.NAME_COMPARATOR.compare(newList, listAtPos);
            if (result < 0) break;
            pos++;
        }
        // Insert at pos, pushing the existing item at that position down.
        logMsg("Adding list '%s' at position %d", newList.getKey(), pos);
        mPineTaskLists.add(pos, newList);
        notifyItemInserted(pos);
    }

    public void addAll(Collection<PineTaskListWithCollaborators> lists)
    {
        mPineTaskLists.addAll(lists);
        notifyDataSetChanged();
    }

    public void clear()
    {
        mPineTaskLists.clear();
        notifyDataSetChanged();
    }

    public void remove(String listId)
    {
        for (int i = 0; i< mPineTaskLists.size(); i++)
        {
            PineTaskList list = mPineTaskLists.get(i);
            if (list.getKey().equals(listId))
            {
                mPineTaskLists.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }
}
