package com.pinetask.app.chat;

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
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/** Fragment for the "chat" tab: shows chat messages for the currently selected list. **/
public class ChatFragment extends PineTaskFragment implements ChatView
{
    ChatMessagesAdapter mChatMessagesAdapter;
    LinearLayoutManager mLayoutManager;
    @Inject ChatPresenter mChatPresenter;

    @BindView(R.id.chatRecyclerView) RecyclerView mChatRecyclerView;
    @BindView(R.id.chatMessageEditText) EditText mChatMessageEditText;
    @BindView(R.id.sendMessageButton) FloatingActionButton mSendMessageButton;
    @BindView(R.id.chatLayout) RelativeLayout mChatLayout;

    public static ChatFragment newInstance()
    {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        logMsg("onCreateView");
        View view = inflater.inflate(R.layout.chat_fragment, container, false);
        ButterKnife.bind(this, view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mChatMessagesAdapter = new ChatMessagesAdapter(mLayoutManager);
        mChatRecyclerView.setAdapter(mChatMessagesAdapter);
        mChatRecyclerView.setLayoutManager(mLayoutManager);
        PineTaskApplication.getInstance().getUserComponent().inject(this);
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mChatPresenter.attachView(this);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mChatPresenter.detachView();
    }

    @OnClick(R.id.sendMessageButton)
    public void sendMessageButtonOnClick(View view)
    {
        String message = mChatMessageEditText.getText().toString();
        mChatMessageEditText.setText("");
        mChatPresenter.sendMessage(message);
        hideSoftKeyboard();
    }

    @Override
    public void showChatMessages(List<ChatMessage> messages)
    {
        mChatMessagesAdapter.showMessages(messages);
        mChatRecyclerView.postDelayed(mChatMessagesAdapter::scrollToBottom, 300);
    }

    @Override
    public void addChatMessage(ChatMessage chatMessage)
    {
        mChatMessagesAdapter.addMessage(chatMessage);
        mChatRecyclerView.postDelayed(mChatMessagesAdapter::scrollToBottom, 300);
    }

    @Override
    public void playNewMessageSound()
    {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
        r.play();
    }

    @Override
    public void clearChatMessages()
    {
        mChatMessagesAdapter.removeAll();
    }

    @Override
    public void showChatLayouts()
    {
        mChatLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideChatLayouts()
    {
        mChatLayout.setVisibility(View.GONE);
    }

    @Override
    public void showError(String message, Object... args)
    {
        showUserMessage(false, message, args);
    }

}
