package com.tbocek.android.combatmap.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.tbocek.android.combatmap.model.primitives.CoordinateTransformer;
import com.tbocek.android.combatmap.model.primitives.Shape;
import com.tbocek.android.combatmap.model.primitives.Units;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tbocek on 11/4/14.
 */
public class Selection {
    private RectF mWorldSpaceSelection;
    private List<Shape> mSelectedShapes;
    private LineCollection mManagedCollection;

    private Paint mSelectionInteriorPaint;
    private Paint mSelectionExteriorPaint;

    public Selection() {
        createPaints();
    }

    private void createPaints() {
        mSelectionInteriorPaint = new Paint();
        mSelectionInteriorPaint.setColor(Color.argb(64, 128, 128, 255));
        mSelectionInteriorPaint.setStyle(Paint.Style.FILL);

        mSelectionExteriorPaint = new Paint();
        mSelectionExteriorPaint.setColor(Color.argb(255, 64, 64, 255));
        mSelectionExteriorPaint.setStyle(Paint.Style.STROKE);
        mSelectionExteriorPaint.setStrokeWidth(Units.dpToPx(2));
        mSelectionExteriorPaint.setPathEffect(
                new DashPathEffect(new float[]{Units.dpToPx(4.0f), Units.dpToPx(8.0f)}, 0));
    }

    public void setRectangle(RectF rect) {
        mWorldSpaceSelection = rect;
    }

    public void draw(Canvas c, CoordinateTransformer wsTransformer) {
        // Draw in screen space so that the border doesn't get scaled.
        Rect rectScreenSpace = wsTransformer.worldSpaceToScreenSpace(
                mWorldSpaceSelection);
        c.drawRect(rectScreenSpace, mSelectionInteriorPaint);
        c.drawRect(rectScreenSpace, mSelectionExteriorPaint);
    }

    public void finalizeSelection(LineCollection shapes) {
        mManagedCollection = shapes;
        mSelectedShapes = new ArrayList<Shape>();

        // Figure out which shapes are included.
        // TODO: Better logic behind which shapes are included.
        for (Shape s: shapes.allShapes()) {
            if (s.getBoundingRectangle().toRectF().intersect(mWorldSpaceSelection)) {
                mSelectedShapes.add(s);
            }
        }
    }
}
