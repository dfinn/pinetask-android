package com.pinetask.app;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.R;
import com.squareup.otto.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Fragment for the "chat" tab: shows chat messages for the currently selected list. **/
public class ChatFragment extends PineTaskFragment
{
    FirebaseDatabase mDatabase;
    DatabaseReference mChatMessagesRef;
    ChatMessagesAdapter mChatMessagesAdapter;
    StatefulChildListener mChatMessagesListener;
    String mUserId, mCurrentListId;

    @BindView(R.id.chatRecyclerView) RecyclerView mChatRecyclerView;
    @BindView(R.id.chatMessageEditText) EditText mChatMessageEditText;
    @BindView(R.id.sendMessageButton) FloatingActionButton mSendMessageButton;
    @BindView(R.id.chatLayout) RelativeLayout mChatLayout;

    /** Name of a string argument specifying the user ID. **/
    public static String USER_ID_KEY = "UserId";

    public static ChatFragment newInstance(String userId)
    {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID_KEY, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.chat_fragment, container, false);
        ButterKnife.bind(this, view);

        // Initialize RecyclerView that will show chat messages.
        mDatabase = FirebaseDatabase.getInstance();
        mUserId = getArguments().getString(USER_ID_KEY);
        mChatMessagesAdapter = new ChatMessagesAdapter(getActivity());
        mChatRecyclerView.setAdapter(mChatMessagesAdapter);
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @OnClick(R.id.sendMessageButton)
    public void sendMessageButtonOnClick(View view)
    {
        String message = mChatMessageEditText.getText().toString();
        mChatMessageEditText.setText("");
        ChatMessage chatMessage = new ChatMessage(message, mUserId);
        logMsg("Sending chat message: %s", message.replace("%","%%"));
        mChatMessagesRef.push().setValue(chatMessage);

        // Hide soft keyboard
        View focusedView = getActivity().getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    /** Called by the event bus when the user has selected a new list. **/
    @Subscribe
    public void onListSelected(ListSelectedEvent event)
    {
        logMsg("onListSelected: listId = %s", event.ListId);

        // Shut down old listener if there was a previous list.  Clear existing chat messages from mListAdapter.
        if (mChatMessagesListener != null) mChatMessagesListener.shutdown();
        mChatMessagesAdapter.removeAll();

        mCurrentListId = event.ListId;

        if (mCurrentListId==null)
        {
            // All lists have been deleted. Disable chat EditText / send button.
            logMsg("onListSelected: listId is null, returning");
            mChatMessageEditText.setVisibility(View.GONE);
            mSendMessageButton.setVisibility(View.GONE);
            return;
        }

        // Enable chat edittext and "send" button
        mChatMessageEditText.setVisibility(View.VISIBLE);
        mSendMessageButton.setVisibility(View.VISIBLE);

        mChatMessagesRef = mDatabase.getReference(DbHelper.CHAT_MESSAGES_NODE_NAME).child(mCurrentListId);

        mChatMessagesListener = new StatefulChildListener(mChatMessagesRef, new StatefulChildListener.StatefulChildListenerCallbacks()
        {
            @Override
            public void onInitialLoadCompleted()
            {
            }

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, boolean isInitialDataLoaded)
            {
                ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                chatMessage.setKey(dataSnapshot.getKey());
                mChatMessagesAdapter.addMessage(chatMessage);
                mChatRecyclerView.smoothScrollToPosition(mChatMessagesAdapter.getItemCount()-1);

                // Play notification sound and post message to event bus when new messages are received (ie, not from the current user).
                if (isInitialDataLoaded && !mUserId.equals(chatMessage.getSenderId()))
                {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
                    r.play();
                    mEventBus.post(chatMessage);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, boolean isInitialDataLoaded)
            {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot, boolean isInitialDataLoaded)
            {
            }

            @Override
            public void onCancelled(DatabaseError error)
            {
                logError("initChatMessagesListener: onCancelled: %s", error.getMessage());
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        logMsg("onDestroy: shutting down event listeners");
        if (mChatMessagesListener != null) mChatMessagesListener.shutdown();
    }
}
