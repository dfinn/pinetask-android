package com.pinetask.app.list_items;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.Logger;

import java.util.List;

import javax.inject.Inject;

public class ListItemAdapter extends RecyclerView.Adapter<ListItemAdapter.ItemViewHolder>
{
    List<PineTaskItemExt> mItems;
    ListItemsFragment mListItemsFragment;
    @Inject DbHelper mDbHelper;
    @Inject ListItemsPresenter mListItemsPresenter;

    // IDs for pop-up menu items
    public static int MENU_ITEM_DELETE = 0;
    public static int MENU_ITEM_UNCLAIM = 1;
    public static int MENU_ITEM_UNCOMPLETE = 2;

    public ListItemAdapter(ListItemsFragment listItemsFragment, List<PineTaskItemExt> items)
    {
        mListItemsFragment = listItemsFragment;
        mItems = items;
        PineTaskApplication.getInstance().getUserComponent().inject(this);
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder
    {
        ImageButton mOptionsImageButton, mClaimImageButton, mCompletedImageButton;
        TextView mItemDescriptionTextView, mClaimedByTextView;

        public ItemViewHolder(View itemView)
        {
            super(itemView);
            mItemDescriptionTextView = (TextView) itemView.findViewById(R.id.itemDescriptionTextView);
            mOptionsImageButton = (ImageButton) itemView.findViewById(R.id.optionsImageButton);
            mClaimImageButton = (ImageButton) itemView.findViewById(R.id.claimImageButton);
            mClaimedByTextView = (TextView) itemView.findViewById(R.id.claimedByTextView);
            mCompletedImageButton = (ImageButton) itemView.findViewById(R.id.completedImageButton);
        }
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_row, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position)
    {
        final PineTaskItemExt item = mItems.get(position);

        // Show item description text.  If marked as completed, show text with strikethrough and use lighter color.
        holder.mItemDescriptionTextView.setText(item.getItemDescription());
        holder.mItemDescriptionTextView.getPaint().setStrikeThruText(item.getIsCompleted());
        holder.mItemDescriptionTextView.setTextColor(ContextCompat.getColor(mListItemsFragment.getActivity(), item.getIsCompleted() ? R.color.black_30percent : R.color.black));

        // Configure pop-up menu attached to the "..." button. It will show different options depending on the state of the item.
        final PopupMenu popupMenu;
        popupMenu = new PopupMenu(mListItemsFragment.getActivity(), holder.mOptionsImageButton);
        popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, R.string.delete);
        if (item.getIsCompleted())
        {
            popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_UNCOMPLETE, Menu.NONE, R.string.uncomplete);
        }
        else
        {
            if (item.getClaimedBy()!=null) popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_UNCLAIM, Menu.NONE, R.string.unclaim);
        }
        popupMenu.setOnMenuItemClickListener(menuItem ->
        {
            if (menuItem.getItemId() == MENU_ITEM_DELETE) mListItemsPresenter.deleteItem(item);
            else if (menuItem.getItemId() == MENU_ITEM_UNCLAIM) mListItemsPresenter.unclaimItem(item);
            else if (menuItem.getItemId() == MENU_ITEM_UNCOMPLETE) mListItemsPresenter.setCompletedStatus(item, false);
            return true;
        });

        // If the user clicks the item text, open the edit dialog.
        holder.mItemDescriptionTextView.setOnClickListener(__ -> mListItemsFragment.showEditDialog(item));

        // Configure the "..." button to show the pop-up menu when clicked.
        holder.mOptionsImageButton.setOnClickListener(__ -> popupMenu.show());

        // Set "claim" button to claim the item.
        holder.mClaimImageButton.setOnClickListener(__ -> mListItemsPresenter.claimItem(item));

        // Only show the "completed" check box if item not yet complete
        holder.mCompletedImageButton.setVisibility((item.getIsCompleted() == false) ? View.VISIBLE : View.INVISIBLE);

        // Set "complete" (checkbox) button to mark the item as completed. It will be claimed by the current user if not already claimed by them.
        holder.mCompletedImageButton.setOnClickListener(__ -> mListItemsPresenter.setCompletedStatus(item, true));

        // If claimed, show the initials of the person who has claimed the item.
        // TODO: show avatar of the person who has claimed the item.
        if (item.getClaimedBy() != null)
        {
            holder.mClaimImageButton.setVisibility(View.GONE);
            holder.mClaimedByTextView.setVisibility(View.VISIBLE);
            populateClaimedBy(holder, item.getClaimedBy());
        }
        else
        {
            holder.mClaimImageButton.setVisibility(View.VISIBLE);
            holder.mClaimedByTextView.setVisibility(View.GONE);
        }
    }

    /** Initiate async query to find out the name of the user with the specified username.  When query returns, populates the "claimed by" textview in the holder provided. **/
    private void populateClaimedBy(final ItemViewHolder holder, final String claimedBy)
    {
        mDbHelper.getUserNameSingle(claimedBy).subscribe(data ->
        {
            holder.mClaimedByTextView.setText((data != null && data.length() > 0) ? data.substring(0, 1) : "?");
        }, ex ->
        {
            Logger.logErrorAndException(getClass(), ex, "Error getting user name for user %s", claimedBy);
        });
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    public void showItems(List<PineTaskItemExt> items)
    {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    /** Add the item if not already in the list **/
    public void add(PineTaskItemExt item)
    {
        if (findItem(item.getId()) == -1)
        {
            mItems.add(item);
            notifyItemInserted(mItems.size() - 1);
        }
    }

    /** Return the index of the item, or -1 if not found **/
    private int findItem(String key)
    {
        for (int i = 0; i< mItems.size(); i++)
        {
            PineTaskItemExt item = mItems.get(i);
            if (item.getId().equals(key)) return i;
        }
        return -1;
    }

    /** Removes the item with the key provided. **/
    public void remove(String key)
    {
        int i = findItem(key);
        if (i != -1)
        {
            mItems.remove(i);
            notifyItemRemoved(i);
        }
    }

    public void update(PineTaskItemExt updatedItem)
    {
        int i = findItem(updatedItem.getId());
        if (i != -1)
        {
            PineTaskItem oldItem = mItems.get(i);
            oldItem.updateFrom(updatedItem);
            notifyItemChanged(i);
        }
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }
    protected void logError(String msg, Object...args)
    {
        Logger.logError(getClass(), msg, args);
    }

}
