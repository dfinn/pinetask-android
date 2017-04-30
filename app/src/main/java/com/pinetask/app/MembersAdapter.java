package com.pinetask.app;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pinetask.common.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.pinetask.app.DbHelper.getUserNameSingle;
import static com.pinetask.app.DbHelper.singleObserver;

/** Adapter for the RecyclerView that displays members of a list. **/
public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder>
{
    /** User ID of the current user. **/
    String mCurrentUserId;

    /** ID of currently displayed list **/
    String mListId;
    public String getListId() { return mListId; }

    /** User ID of the owner of the current list. **/
    String mOwnerId;

    List<String> mUserIds = new ArrayList<>();
    MainActivity mActivity;

    class MemberViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.nameTextView) TextView NameTextView;
        @BindView(R.id.deleteImageButton) ImageButton DeleteImageButton;
        public MemberViewHolder(View itemView)
        {
            super(itemView);
            ButterKnife.bind(this, itemView);

            DeleteImageButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    String userId = mUserIds.get(getAdapterPosition());
                    String listId = PrefsManager.getInstance(mActivity).getCurrentListId();
                    RevokeListAccessDialogFragment dialog = RevokeListAccessDialogFragment.newInstance(listId, userId);
                    mActivity.getSupportFragmentManager().beginTransaction().add(dialog, RevokeListAccessDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
                }
            });
        }
    }

    public MembersAdapter(MainActivity activity, String listId, String currentUserId, String ownerId)
    {
        mListId = listId;
        mActivity = activity;
        mCurrentUserId = currentUserId;
        mOwnerId = ownerId;
    }

    @Override
    public MemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.members_list_row, parent, false);
        MemberViewHolder vh = new MemberViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(final MemberViewHolder holder, int position)
    {
        final String userId = mUserIds.get(position);

        // Only list owner is allowed to delete members. List owner cannot be deleted.  If disabled, show the delete button as faded (30% alpha).
        boolean showDelete = (mCurrentUserId.equals(mOwnerId)) && (! userId.equals(mOwnerId));
        holder.DeleteImageButton.setAlpha(showDelete ? 1.0f : 0.3f);
        holder.DeleteImageButton.setClickable(showDelete);

        // Set tag of the NameTextView to the user ID.  Then start async query to look up username for the userId.  Need to check that the
        // userId hasn't changed when the query returns before we set the text, since it's possible the view could have been recycled in between.
        holder.NameTextView.setTag(userId);
        getUserNameSingle(userId).subscribe(singleObserver((String data) ->
            {
                if (userId.equals(holder.NameTextView.getTag()))
                {
                    String userName = (data==null) ? "?" : data;
                    boolean isOwner = mOwnerId.equals(userId);
                    if (isOwner) userName += " " + mActivity.getString(R.string.owner);
                    holder.NameTextView.setTypeface(null, isOwner ? Typeface.BOLD : Typeface.NORMAL);
                    holder.NameTextView.setText(userName);
                }
            }));
    }

    @Override
    public int getItemCount()
    {
        return mUserIds.size();
    }

    public void addUser(String userId)
    {
        mUserIds.add(userId);
        notifyItemInserted(mUserIds.size()-1);
    }

    public void removeUser(String userId)
    {
        for (int pos=0;pos<mUserIds.size();pos++)
        {
            if (userId.equals(mUserIds.get(pos)))
            {
                mUserIds.remove(pos);
                notifyItemRemoved(pos);
                return;
            }
        }
    }

    /** To be called when a new list is being displayed. Clears existing list of user IDs.  Caller must provide the owner of the new list. **/
    public void onListChanged(String listId, String ownerId)
    {
        logMsg("onListChanged: new listId=%s, ownerId=%s", listId, ownerId);
        mListId = listId;
        mOwnerId = ownerId;
        mUserIds.clear();
        notifyDataSetChanged();
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }

}
