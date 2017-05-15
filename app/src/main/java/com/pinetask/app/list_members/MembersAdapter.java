package com.pinetask.app.list_members;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pinetask.app.main.MainActivity;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.R;
import com.pinetask.common.Logger;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.pinetask.app.db.DbHelper.getUserNameSingle;
import static com.pinetask.app.db.DbHelper.singleObserver;

/** Adapter for the RecyclerView that displays members of a list. **/
public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder>
{
    List<MemberInfo> mMembers = new ArrayList<>();
    MainActivity mActivity;

    class MemberViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.nameTextView) TextView NameTextView;
        @BindView(R.id.deleteImageButton) ImageButton DeleteImageButton;
        public MemberViewHolder(View itemView)
        {
            super(itemView);
            ButterKnife.bind(this, itemView);

            DeleteImageButton.setOnClickListener(__ ->
            {
                MemberInfo memberInfo = mMembers.get(getAdapterPosition());
                String userId = memberInfo.UserId;
                String listId = PrefsManager.getInstance(mActivity).getCurrentListId();
                RevokeListAccessDialogFragment dialog = RevokeListAccessDialogFragment.newInstance(listId, userId);
                mActivity.getSupportFragmentManager().beginTransaction().add(dialog, RevokeListAccessDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
            });
        }
    }

    public MembersAdapter(MainActivity activity)
    {
        mActivity = activity;
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
        MemberInfo memberInfo = mMembers.get(position);

        // Only list owner is allowed to delete members. List owner cannot be deleted.  If disabled, show the delete button as faded (30% alpha).
        boolean showDelete = memberInfo.IsOwner && memberInfo.IsCurrentUser;
        holder.DeleteImageButton.setAlpha(showDelete ? 1.0f : 0.3f);
        holder.DeleteImageButton.setClickable(showDelete);

        // Set username; if owner, show [owner] and use bold font
        String userName = memberInfo.Name;
        if (memberInfo.IsOwner) userName += " " + mActivity.getString(R.string.owner);
        holder.NameTextView.setTypeface(null, memberInfo.IsOwner ? Typeface.BOLD : Typeface.NORMAL);
        holder.NameTextView.setText(userName);
    }

    @Override
    public int getItemCount()
    {
        return mMembers.size();
    }

    public void addUser(MemberInfo memberInfo)
    {
        mMembers.add(memberInfo);
        notifyItemInserted(mMembers.size()-1);
    }

    public void removeUser(String userIdToRemove)
    {
        for (int pos=0;pos<mMembers.size();pos++)
        {
            String userId = mMembers.get(pos).UserId;
            if (userIdToRemove.equals(userId))
            {
                mMembers.remove(pos);
                notifyItemRemoved(pos);
                return;
            }
        }
    }

    public void clear()
    {
        mMembers.clear();
        notifyDataSetChanged();
    }

    protected void logMsg(String msg, Object...args)
    {
        Logger.logMsg(getClass(), msg, args);
    }
}
