package com.pinetask.app.chat;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.db.DbHelper;
import com.pinetask.common.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/** Adapter for RecyclerView that shows a list of chat messages. **/
public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ChatMessageViewHolder>
{
    private List<ChatMessage> mChatMessages = new ArrayList<>();
    private LinearLayoutManager mLayoutManager;
    @Inject PineTaskApplication mAppContext;
    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder
    {
        TextView NameAndTimestampTextView, MessageTextView;
        public ChatMessageViewHolder(View itemView)
        {
            super(itemView);
            NameAndTimestampTextView = (TextView) itemView.findViewById(R.id.nameAndTimestampTextView);
            MessageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
        }
    }

    public ChatMessagesAdapter(LinearLayoutManager linearLayoutManager)
    {
        mLayoutManager = linearLayoutManager;
        PineTaskApplication.getInstance().getAppComponent().inject(this);
    }

    public void addMessage(ChatMessage chatMessage)
    {
        mChatMessages.add(chatMessage);
        notifyItemInserted(mChatMessages.size()-1);
    }

    public void scrollToBottom()
    {
        mLayoutManager.scrollToPosition(mChatMessages.size()-1);
    }

    public void removeAll()
    {
        mChatMessages.clear();
        notifyDataSetChanged();
    }

    public void showMessages(List<ChatMessage> messages)
    {
        mChatMessages.clear();
        mChatMessages.addAll(messages);
        notifyDataSetChanged();
    }

    @Override
    public ChatMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_message, parent, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatMessageViewHolder holder, int position)
    {
        ChatMessage chatMessage = mChatMessages.get(position);
        DateTime timestamp = new DateTime(chatMessage.getCreatedAtMs());
        String timeAbbrevStr = getAbbreviatedDurationString(timestamp);
        holder.NameAndTimestampTextView.setText(chatMessage.getSenderName() + "\n" + timeAbbrevStr);
        holder.MessageTextView.setText(chatMessage.getMessage());

        if (chatMessage.getIsNewMessage())
        {
            Drawable drawable = holder.MessageTextView.getBackground();
            int highlightColor = ContextCompat.getColor(mAppContext, R.color.highlighted_background);
            int whiteColor = ContextCompat.getColor(mAppContext, R.color.white);
            ObjectAnimator.ofObject(holder.MessageTextView, "backgroundColor", new ArgbEvaluator(), highlightColor, whiteColor, highlightColor, whiteColor).setDuration(1500).start();
            chatMessage.setIsNewMessage(false);
            mLayoutManager.scrollToPosition(position);
        }
    }

    /**
     * Returns an abbreviated string representing the duration.
     * Durations greater than 72 hours will be represented in days, durations greater than 1 hour will use hours, else use minutes.
     * Examples:
     * 7d = 7 days
     * 14h = 14 hours
     * 10m = 10 minutes
     */
    public String getAbbreviatedDurationString(DateTime timestamp)
    {
        Duration duration = new Duration(timestamp, DateTime.now());
        if (duration.getStandardHours() > 72)
        {
            return duration.getStandardDays() +  mAppContext.getString(R.string.days_abbreviation);
        }
        else if (duration.getStandardHours() >= 1)
        {
            return duration.getStandardHours() + mAppContext.getString(R.string.hours_abbreviation);
        }
        else
        {
            return duration.getStandardMinutes() + mAppContext.getString(R.string.minutes_abbreviation);
        }
    }

    @Override
    public int getItemCount()
    {
        return mChatMessages.size();
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
