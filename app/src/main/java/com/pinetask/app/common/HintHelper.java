package com.pinetask.app.common;

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

/** Helper class for displaying pop-up tips to new users. **/
public class HintHelper
{
    public static void showTip(Activity activity, int stringResId, View targetView, View rootView, boolean circularShape)
    {
        showTip(activity, stringResId, targetView, rootView, circularShape, null);
    }

    public static void showTip(Activity activity, int stringResId, View targetView, View rootView, boolean circularShape, HintCase.OnClosedListener onClosedListener)
    {
        if (activity == null)
        {
            Logger.logMsg(HintHelper.class, "Activity is null, won't show tip");
            return;
        }

        Logger.logMsg(HintHelper.class, "Showing hint '%s'", activity.getString(stringResId));

        SimpleHintContentHolder hintBlock = new SimpleHintContentHolder.Builder(activity)
                .setContentText(stringResId)
                .setContentStyle(R.style.HintTextStyle)
                .setMarginByResourcesId(R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin, R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin)
                .build();

        Shape shape = circularShape ? new CircularShape() : new RectangularShape();
        ShapeAnimator revealShapeAnimator = circularShape ? new RevealCircleShapeAnimator() : new RevealRectangularShapeAnimator();
        ShapeAnimator unRevealShapeAnimator = circularShape ? new UnrevealCircleShapeAnimator() : new UnrevealRectangularShapeAnimator();

        Logger.logMsg(HintHelper.class, "Hint '%s' using root view %s (%X)", activity.getString(stringResId), rootView.getClass().getSimpleName(), rootView.getId());
        HintCase hintCase = new HintCase(rootView)
                .setTarget(targetView, shape, HintCase.TARGET_IS_NOT_CLICKABLE)
                .setShapeAnimators(revealShapeAnimator, unRevealShapeAnimator)
                .setHintBlock(hintBlock);
        if (onClosedListener != null)
        {
            hintCase.setOnClosedListener(() ->
            {
                Logger.logMsg(HintHelper.class, "Hint '%s' has been closed", activity.getString(stringResId));
                onClosedListener.onClosed();
            });
        }
        hintCase.show();
    }
}
