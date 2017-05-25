package com.pinetask.app.chat;

import android.content.Context;
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
    List<ChatMessage> mChatMessages = new ArrayList<>();
    @Inject PineTaskApplication mAppContext;
    @Inject DbHelper mDbHelper;

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

    public ChatMessagesAdapter()
    {
        PineTaskApplication.getInstance().getAppComponent().inject(this);
    }

    public void addMessage(ChatMessage chatMessage)
    {
        mChatMessages.add(chatMessage);
        notifyItemInserted(mChatMessages.size()-1);
    }

    public void removeAll()
    {
        mChatMessages.clear();
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

        // Show abbreviated timestamp indicating age of the message
        DateTime timestamp = new DateTime(chatMessage.getCreatedAt());
        String timeAbbrevStr = getAbbreviatedDurationString(timestamp);

        // Start async request to populate sender name
        if (chatMessage.getSenderId()!=null)
        {
            mDbHelper.getUserNameSingle(chatMessage.getSenderId()).subscribe(mDbHelper.singleObserver((String userName)
                    -> holder.NameAndTimestampTextView.setText((userName != null ? userName : "?") + "\n" + timeAbbrevStr)));
        }
        else
        {
            holder.NameAndTimestampTextView.setText("?\n"+timeAbbrevStr);
            logError("Warning: senderId for chat message %s is null", chatMessage.getKey());
        }

        // Show message text
        holder.MessageTextView.setText(chatMessage.getMessage());
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
