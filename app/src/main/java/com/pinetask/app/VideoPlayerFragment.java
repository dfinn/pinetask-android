package com.pinetask.app;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Fragment used during the tutorial for playing a specified video. **/
public class VideoPlayerFragment extends PineTaskFragment
{
    @BindView(R.id.videoView) VideoView mVideoView;
    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.descriptionTextView) TextView mDescriptionTextView;

    /** Name of an integer argument specifying the resource ID of the video to play. **/
    public static String VIDEO_RES_ID_KEY = "VideoResId";

    /** Name of a string argument specifying the title text to show. **/
    public static String TITLE_KEY = "Title";

    /** Name of a string argument specifying the description text to show. **/
    public static String DESCRIPTION_KEY = "OperationDescription";

    public static VideoPlayerFragment newInstance(int videoResId, String title, String description)
    {
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        Bundle args = new Bundle();
        args.putInt(VIDEO_RES_ID_KEY, videoResId);
        args.putString(TITLE_KEY, title);
        args.putString(DESCRIPTION_KEY, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.video_player_fragment, container, false);
        ButterKnife.bind(this, view);
        mTitleTextView.setText(getArguments().getString(TITLE_KEY));
        mDescriptionTextView.setText(getArguments().getString(DESCRIPTION_KEY));
        int videoResId = getArguments().getInt(VIDEO_RES_ID_KEY);
        Uri videoUri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + videoResId);
        logMsg("Setting video uri to: %s", videoUri);
        mVideoView.setVideoURI(videoUri);
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    public void startPlayback()
    {
        logMsg("Starting video playback");
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                mVideoView.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mVideoView.start();
                    }
                }, 2000);
            }
        });
        mVideoView.start();
    }
}
