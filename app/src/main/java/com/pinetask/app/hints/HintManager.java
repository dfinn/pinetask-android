package com.pinetask.app.hints;

import android.app.Activity;
import android.view.View;

import com.joanfuentes.hintcase.HintCase;
import com.joanfuentes.hintcase.RectangularShape;
import com.joanfuentes.hintcase.Shape;
import com.joanfuentes.hintcase.ShapeAnimator;
import com.joanfuentes.hintcaseassets.hintcontentholders.SimpleHintContentHolder;
import com.joanfuentes.hintcaseassets.shapeanimators.RevealCircleShapeAnimator;
import com.joanfuentes.hintcaseassets.shapeanimators.RevealRectangularShapeAnimator;
import com.joanfuentes.hintcaseassets.shapeanimators.UnrevealCircleShapeAnimator;
import com.joanfuentes.hintcaseassets.shapeanimators.UnrevealRectangularShapeAnimator;
import com.joanfuentes.hintcaseassets.shapes.CircularShape;
import com.pinetask.app.R;
import com.pinetask.app.common.Logger;
import com.pinetask.app.common.LoggingBase;
import com.pinetask.app.common.PrefsManager;
import com.pinetask.app.common.UserScope;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/** Helper class for displaying pop-up tips to new users. **/
@UserScope
public class HintManager extends LoggingBase
{
    PrefsManager mPrefsManager;

    private Map<HintType, Boolean> mDisplayedHintsMap = new HashMap<>();

    @Inject
    public HintManager(PrefsManager prefsManager)
    {
        mPrefsManager = prefsManager;
    }

    public boolean isHintDisplayed(HintType hintType)
    {
        Boolean displayed = mDisplayedHintsMap.get(hintType);
        if (displayed == null)
        {
            displayed = mPrefsManager.isTipShown(hintType.getKey());
            mDisplayedHintsMap.put(hintType, displayed);
        }
        return displayed;
    }

    public void setHintDisplayed(HintType hintType)
    {
        mDisplayedHintsMap.put(hintType, true);
        mPrefsManager.setTipShown(hintType.getKey());
    }

    public void setAllHintsDisplayed()
    {
        for (HintType h : HintType.values()) setHintDisplayed(h);
    }

    public void showTip(Activity activity, int stringResId, View targetView, View rootView, boolean circularShape)
    {
        showTip(activity, stringResId, targetView, rootView, circularShape, null);
    }

    public void showTip(Activity activity, int stringResId, View targetView, View rootView, boolean circularShape, HintCase.OnClosedListener onClosedListener)
    {
        if (activity == null)
        {
            Logger.logMsg(HintManager.class, "Activity is null, won't show tip");
            return;
        }

        Logger.logMsg(HintManager.class, "Showing hint '%s'", activity.getString(stringResId));

        SimpleHintContentHolder hintBlock = new SimpleHintContentHolder.Builder(activity)
                .setContentText(stringResId)
                .setContentStyle(R.style.HintTextStyle)
                .setMarginByResourcesId(R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin, R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin)
                .build();

        Shape shape = circularShape ? new CircularShape() : new RectangularShape();
        ShapeAnimator revealShapeAnimator = circularShape ? new RevealCircleShapeAnimator() : new RevealRectangularShapeAnimator();
        ShapeAnimator unRevealShapeAnimator = circularShape ? new UnrevealCircleShapeAnimator() : new UnrevealRectangularShapeAnimator();

        Logger.logMsg(HintManager.class, "Hint '%s' using root view %s (%X)", activity.getString(stringResId), rootView.getClass().getSimpleName(), rootView.getId());
        HintCase hintCase = new HintCase(rootView)
                .setTarget(targetView, shape, HintCase.TARGET_IS_NOT_CLICKABLE)
                .setShapeAnimators(revealShapeAnimator, unRevealShapeAnimator)
                .setHintBlock(hintBlock);
        if (onClosedListener != null)
        {
            hintCase.setOnClosedListener(() ->
            {
                Logger.logMsg(HintManager.class, "Hint '%s' has been closed", activity.getString(stringResId));
                onClosedListener.onClosed();
            });
        }
        hintCase.show();
    }
}
