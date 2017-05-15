package com.pinetask.app.launch;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.github.paolorotolo.appintro.AppIntro;
import com.pinetask.app.R;
import com.pinetask.app.common.PrefsManager;

public class TutorialActivity extends AppIntro
{
    public static void launch(Context context)
    {
        Intent i = new Intent(context, TutorialActivity.class);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        PrefsManager prefsManager = PrefsManager.getInstance(this);

        if (! prefsManager.getTutorialCompleted())
        {
            // Tutorial is showing because it's the first launch: show the "Welcome to PineTask" slide.
            addSlide(TutorialImageAndTextFragment.newInstance(R.raw.welcome_to_pinetask, getString(R.string.welcome_title), getString(R.string.welcome_description)));
        }

        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.adding_lists, getString(R.string.adding_lists_title), getString(R.string.adding_lists_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.adding_deleting_items, getString(R.string.adding_deleting_items_title), getString(R.string.adding_deleting_items_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.claiming_unclaiming_items, getString(R.string.claiming_unclaiming_items_title), getString(R.string.claiming_unclaiming_items_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.completing_uncompleting_items, getString(R.string.completing_uncompleting_items_title), getString(R.string.completing_uncompleting_items_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.sharing_lists, getString(R.string.sharing_lists_title), getString(R.string.sharing_lists_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.renaming_deleting_lists, getString(R.string.renaming_deleting_lists_title), getString(R.string.renaming_deleting_lists_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.chat, getString(R.string.chat_title), getString(R.string.chat_description)));
        addSlide(TutorialImageAndTextFragment.newInstance(R.raw.purge_completed_items, getString(R.string.purge_completed_items_title), getString(R.string.purge_completed_items_description)));

        setBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        setSeparatorColor(Color.parseColor("#2196F3"));
        showSkipButton(true);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment)
    {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment)
    {
        super.onDonePressed(currentFragment);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment)
    {
        super.onSlideChanged(oldFragment, newFragment);
        if (newFragment instanceof TutorialImageAndTextFragment)
        {
            TutorialImageAndTextFragment tutorialImageAndTextFragment = (TutorialImageAndTextFragment) newFragment;
            tutorialImageAndTextFragment.loadImage();
        }

        if (oldFragment instanceof TutorialImageAndTextFragment)
        {
            TutorialImageAndTextFragment tutorialImageAndTextFragment = (TutorialImageAndTextFragment) oldFragment;
            tutorialImageAndTextFragment.unloadImage();
        }
    }
}
