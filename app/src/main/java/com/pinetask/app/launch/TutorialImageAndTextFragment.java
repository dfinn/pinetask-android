package com.pinetask.app.launch;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Fragment used during the tutorial for playing a specified video. **/
public class TutorialImageAndTextFragment extends PineTaskFragment
{
    @BindView(R.id.imageView) ImageView mImageView;
    @BindView(R.id.titleTextView) TextView mTitleTextView;
    @BindView(R.id.descriptionTextView) TextView mDescriptionTextView;

    GlideDrawableImageViewTarget mTarget;

    /** Name of an integer argument specifying the resource ID of the image to show. **/
    public static String IMAGE_RES_ID_KEY = "ImageResId";

    /** Name of a string argument specifying the title text to show. **/
    public static String TITLE_KEY = "Title";

    /** Name of a string argument specifying the description text to show. **/
    public static String DESCRIPTION_KEY = "OperationDescription";

    public static TutorialImageAndTextFragment newInstance(int imageResId, String title, String description)
    {
        TutorialImageAndTextFragment fragment = new TutorialImageAndTextFragment();
        Bundle args = new Bundle();
        args.putInt(IMAGE_RES_ID_KEY, imageResId);
        args.putString(TITLE_KEY, title);
        args.putString(DESCRIPTION_KEY, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.tutorial_image_and_text_fragment, container, false);
        ButterKnife.bind(this, view);
        mTitleTextView.setText(getArguments().getString(TITLE_KEY));
        mDescriptionTextView.setText(getArguments().getString(DESCRIPTION_KEY));
        return view;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        logMsg("onPause(resid=%d)", getArguments().getInt(IMAGE_RES_ID_KEY));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        logMsg("onResume(resid=%d)", getArguments().getInt(IMAGE_RES_ID_KEY));
    }

    @Override
    public void onStart()
    {
        super.onStart();
        logMsg("onStart(resid=%d)", getArguments().getInt(IMAGE_RES_ID_KEY));
    }

    @Override
    public void onStop()
    {
        super.onStop();
        logMsg("onStop(resid=%d)", getArguments().getInt(IMAGE_RES_ID_KEY));
    }

    public void loadImage()
    {
        if (mTarget == null)
        {
            int imageResId = getArguments().getInt(IMAGE_RES_ID_KEY);
            logMsg("loadImage: showing image %d", imageResId);
            //Uri imageUri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + imageResId);
            //logMsg("Loading image from uri: %s", imageUri);
            //Glide.with(getActivity()).load(imageResId).into(new GlideDrawableImageViewTarget(mImageView));
            //Glide.with(this).load(imageResId).skipMemoryCache( true ).diskCacheStrategy( DiskCacheStrategy.SOURCE ).into(mTarget);
            mTarget = new GlideDrawableImageViewTarget(mImageView);
            //Glide.with(this).load(imageResId).diskCacheStrategy( DiskCacheStrategy.SOURCE ).fitCenter().into(mTarget);
            Glide.with(this).load(imageResId).diskCacheStrategy( DiskCacheStrategy.SOURCE ).into(mTarget);
        }
    }

    public void unloadImage()
    {
        if (mTarget != null)
        {
            logMsg("Unloading image %d", getArguments().getInt(IMAGE_RES_ID_KEY));
            Glide.clear(mTarget);
            mTarget = null;
        }
    }
}
