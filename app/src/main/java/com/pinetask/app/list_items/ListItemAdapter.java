package com.pinetask.app.list_items;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.hints.HintManager;
import com.pinetask.app.common.Logger;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.hints.HintType;

import java.text.NumberFormat;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ListItemAdapter extends RecyclerView.Adapter<ListItemAdapter.ItemViewHolder>
{
    private List<PineTaskItemExt> mItems;
    private ListItemsFragment mListItemsFragment;
    private boolean mShowCostField;
    @Inject DbHelper mDbHelper;
    @Inject ListItemsPresenter mListItemsPresenter;
    @Inject PrefsManager mPrefsManager;
    @Inject HintManager mHintManager;

    // IDs for pop-up menu items
    private final int MENU_ITEM_DELETE = 0;
    private final int MENU_ITEM_UNCLAIM = 1;
    private final int MENU_ITEM_UNCOMPLETE = 2;
    private final int MENU_ITEM_EDIT = 3;

    public ListItemAdapter(ListItemsFragment listItemsFragment, List<PineTaskItemExt> items)
    {
        mListItemsFragment = listItemsFragment;
        mItems = items;
        PineTaskApplication.getInstance().getUserComponent().inject(this);
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.claimImageButton) ImageButton mClaimImageButton;
        @BindView(R.id.completedImageButton) ImageButton mCompletedImageButton;
        @BindView(R.id.itemCostTextView) TextView mItemCostTextView;
        @BindView(R.id.itemDescriptionTextView) TextView mItemDescriptionTextView;
        @BindView(R.id.claimedByTextView) TextView mClaimedByTextView;
        ViewGroup mMainLayout;

        public ItemViewHolder(View itemView)
        {
            super(itemView);
            mMainLayout = (ViewGroup) itemView.findViewById(R.id.mainLayout);
            ButterKnife.bind(this, mMainLayout);
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
        logMsg("onBindViewHolder for item %s (%s)", item.getId(), item.getItemDescription());

        // Show item description text.  If marked as completed, show text with strikethrough and use lighter color.
        holder.mItemDescriptionTextView.setText(item.getItemDescription());
        holder.mItemDescriptionTextView.getPaint().setStrikeThruText(item.getIsCompleted());
        holder.mItemDescriptionTextView.setTextColor(ContextCompat.getColor(mListItemsFragment.getActivity(), item.getIsCompleted() ? R.color.black_30percent : R.color.black));

        // Configure pop-up menu attached to the description textview. It will show different options depending on the state of the item.
        final PopupMenu popupMenu;
        popupMenu = new PopupMenu(mListItemsFragment.getActivity(), holder.mItemDescriptionTextView);
        popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, R.string.edit);
        popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, R.string.delete);
        if (item.getIsCompleted()) popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_UNCOMPLETE, Menu.NONE, R.string.uncomplete);
        else if (item.getClaimedBy()!=null) popupMenu.getMenu().add(Menu.NONE, MENU_ITEM_UNCLAIM, Menu.NONE, R.string.unclaim);
        popupMenu.setOnMenuItemClickListener(menuItem ->
        {
            if (menuItem.getItemId() == MENU_ITEM_DELETE) mListItemsPresenter.deleteItem(item);
            else if (menuItem.getItemId() == MENU_ITEM_UNCLAIM) mListItemsPresenter.unclaimItem(item);
            else if (menuItem.getItemId() == MENU_ITEM_UNCOMPLETE) mListItemsPresenter.setCompletedStatus(item, false);
            else if (menuItem.getItemId() == MENU_ITEM_EDIT) mListItemsFragment.showEditDialog(item);
            return true;
        });

        // Configure the background layout to show the pop-up menu when clicked.
        holder.mMainLayout.setOnClickListener(__ -> popupMenu.show());

        // Set "claim" button to claim the item.
        holder.mClaimImageButton.setOnClickListener(__ -> mListItemsPresenter.claimItem(item));

        // Only show the "completed" check box if item not yet complete
        holder.mCompletedImageButton.setVisibility((item.getIsCompleted() == false) ? View.VISIBLE : View.INVISIBLE);

        // Set "complete" (checkbox) button to mark the item as completed. It will be claimed by the current user if not already claimed by them.
        holder.mCompletedImageButton.setOnClickListener(__ -> mListItemsPresenter.setCompletedStatus(item, true));

        // If claimed, show the initials of the person who has claimed the item.
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

        // Show pop-up hints if this is the first list item that has been added.
        if (!mHintManager.isHintDisplayed(HintType.FIRST_LIST_ITEM_ADDED) && mHintManager.isHintDisplayed(HintType.FIRST_LIST_ADDED)) showFirstListItemHints(holder);

        // Show cost field if shopping trip mode is active.
        if (mShowCostField)
        {
            holder.mItemCostTextView.setVisibility(View.VISIBLE);
            String costStr = (item.getCost() == null) ? "" : NumberFormat.getCurrencyInstance().format(item.getCost());
            holder.mItemCostTextView.setText(costStr);
        }
        else
        {
            holder.mItemCostTextView.setVisibility(View.GONE);
        }
    }

    /** If this is the first time an item has been added, show tips popup. **/
    private void showFirstListItemHints(ItemViewHolder holder)
    {
        if ( (holder.mClaimImageButton.getVisibility() != View.VISIBLE) || (holder.mCompletedImageButton.getVisibility() != View.VISIBLE) ) return;

        holder.mMainLayout.postDelayed(() ->
        {
            View rootView = holder.mMainLayout.getRootView();
            mHintManager.showTip(mListItemsFragment.getActivity(), R.string.item_hand_icon_hint, holder.mClaimImageButton, rootView, true, () ->
            {
                mHintManager.showTip(mListItemsFragment.getActivity(), R.string.item_checkbox_icon_hint, holder.mCompletedImageButton, rootView, true, () ->
                {
                    mHintManager.showTip(mListItemsFragment.getActivity(), R.string.item_other_options_hint, holder.mItemDescriptionTextView, rootView, false, () ->
                    {
                        mHintManager.setHintDisplayed(HintType.FIRST_LIST_ITEM_ADDED);
                    });
                });
            });
        }, 300);
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

    public void clear()
    {
        mItems.clear();
        notifyDataSetChanged();
    }

    /** Add the item if not already in the list **/
    public void add(PineTaskItemExt item)
    {
        logMsg("Adding item %s (%s)", item.getId(), item.getItemDescription());
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
        logMsg("Removing item %s", key);
        int i = findItem(key);
        if (i != -1)
        {
            mItems.remove(i);
            notifyItemRemoved(i);
        }
    }

    public void update(PineTaskItemExt updatedItem)
    {
        logMsg("Updating item %s (%s)", updatedItem.getId(), updatedItem.getItemDescription());
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

    public void setShowCostField(boolean show)
    {
        mShowCostField = show;
        notifyDataSetChanged();
    }
}
