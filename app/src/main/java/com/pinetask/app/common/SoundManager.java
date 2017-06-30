package com.pinetask.app.common;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.pinetask.app.R;

import javax.inject.Inject;

/** Manage playback of sound effects.   Must call shutdown() when done to release resources. **/
@UserScope
public class SoundManager extends LoggingBase
{
    private SoundPool mSoundPool;
    private int mItemAddedSoundId;
    private int mItemCompletedSoundId;

    @SuppressWarnings("deprecation")
    @Inject
    public SoundManager(PineTaskApplication applicationContext)
    {
        logMsg("Creating SoundPool");
        mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        mItemAddedSoundId = mSoundPool.load(applicationContext, R.raw.item_added_sound, 1);
        mItemCompletedSoundId = mSoundPool.load(applicationContext, R.raw.item_completed_sound, 1);
        logMsg("SoundPool created");
    }

    public void shutdown()
    {
        if (mSoundPool != null)
        {
            logMsg("Releasing sound pool");
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    public void playItemAddedSound()
    {
        if (mSoundPool != null) mSoundPool.play(mItemAddedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public void playItemCompletedSound()
    {
        if (mSoundPool != null) mSoundPool.play(mItemCompletedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

}
