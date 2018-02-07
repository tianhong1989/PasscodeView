/*
 * Copyright 2017 Keval Patel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kevalpatel.passcodeview.internal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import com.kevalpatel.passcodeview.PasscodeViewLifeCycle;
import com.kevalpatel.passcodeview.PatternView;
import com.kevalpatel.passcodeview.PinView;
import com.kevalpatel.passcodeview.R;
import com.kevalpatel.passcodeview.Utils;
import com.kevalpatel.passcodeview.interfaces.AuthenticationListener;
import com.kevalpatel.passcodeview.internal.box.BoxFingerprint;

/**
 * Created by Keval Patel on 18/04/17.
 * A base class to implement the view for authentication like {@link PinView} and {@link PatternView}.
 * This class will set up finger print reader and the indicator boxes.
 *
 * @author 'https://github.com/kevalpatel2106'
 * @see PatternView
 * @see PinView
 */

public abstract class BasePasscodeView extends View implements PasscodeViewLifeCycle {

    /**
     * Context of the view.
     */
    protected final Context mContext;

    /**
     * Bounds of the whole {@link BasePasscodeView}.
     */
    protected Rect mRootViewBound = new Rect();             //Bounds for the root view

    /**
     * A listener to notify the user when the authentication successful or ffailed.
     *
     * @see AuthenticationListener
     */
    protected AuthenticationListener mAuthenticationListener;

    /**
     * Finger print box.
     */
    private BoxFingerprint mBoxFingerprint;               //Fingerprint box

    //Title divider
    @ColorInt
    private int mDividerColor;                              //Horizontal divider color
    private Paint mDividerPaint;                            //Horizontal divider paint color
    private Rect mDividerBound = new Rect();                //Divider bound
    private boolean mIsTactileFeedbackEnabled = true;       //Bool to indicate weather to enable tactile feedback

    ///////////////////////////////////////////////////////////////
    //                  CONSTRUCTORS
    ///////////////////////////////////////////////////////////////

    public BasePasscodeView(Context context) {
        super(context);
        mContext = context;
        mBoxFingerprint = new BoxFingerprint(this);

        init(null);
    }

    public BasePasscodeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mBoxFingerprint = new BoxFingerprint(this);

        init(attrs);
    }

    public BasePasscodeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mBoxFingerprint = new BoxFingerprint(this);

        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BasePasscodeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        mBoxFingerprint = new BoxFingerprint(this);

        init(attrs);
    }

    ///////////////////////////////////////////////////////////////
    //                  SET THEME PARAMS INITIALIZE
    ///////////////////////////////////////////////////////////////

    /**
     * Initialize the view. This will set {@link BoxFingerprint} and parse {@link TypedArray} to
     * read all the parameters added in xml file.
     * <p>
     * If you wan to enable customized parameters, override {@link #init()}  method and initialize the
     * parameters. This method will call before parsing the {@link TypedArray}.
     * <p>
     * If you want to parse view specific XML parameters, override {@link #parseTypeArr(AttributeSet)}
     * and parse the {@link TypedArray}. This method will only call if there is any custom parameters
     * defined in XML.
     * <p>
     * You can set default theme parameters by overriding {@link #setDefaults()} if there are no
     * parameters defined in XML layout.
     *
     * @param attrs {@link AttributeSet}
     */
    private void init(@Nullable AttributeSet attrs) {
        mBoxFingerprint.init();
        init();

        if (attrs != null) {    //Parse all the params from the arguments.
            TypedArray a = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.BasePasscodeView, 0, 0);
            try {
                mIsTactileFeedbackEnabled = a.getBoolean(R.styleable.BasePasscodeView_giveTactileFeedback, true);

                //Parse divider params
                mDividerColor = a.getColor(R.styleable.BasePasscodeView_dividerColor,
                        Utils.getColorCompat(mContext, R.color.lib_divider_color));

                //Fet fingerprint params
                mBoxFingerprint.parseTypeArr(attrs);

                parseTypeArr(attrs);
            } finally {
                a.recycle();
            }
        } else {        //Nothing's provided in XML. Set default for now.
            setDividerColor(Utils.getColorCompat(getContext(), R.color.lib_divider_color));

            //Set every thing to defaults.
            mBoxFingerprint.setDefaults();
            setDefaults();
        }

        //Prepare paints.
        prepareDividerPaint();
        mBoxFingerprint.preparePaint();
        preparePaint();
    }


    /**
     * Create the paint to drawText divider.
     */
    private void prepareDividerPaint() {
        mDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDividerPaint.setColor(mDividerColor);
    }

    ///////////////////////////////////////////////////////////////
    //                  VIEW MEASUREMENT
    ///////////////////////////////////////////////////////////////

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);

        mRootViewBound.left = 0;
        mRootViewBound.right = mRootViewBound.left + viewWidth;
        mRootViewBound.top = 0;
        mRootViewBound.bottom = mRootViewBound.left + viewHeight;

        measureDivider();

        //Measure the finger print
        mBoxFingerprint.measureView(mRootViewBound);

        //Pass it to the implementation box
        measureView(mRootViewBound);

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Measure horizontal divider bounds.
     * Don't change until you know what you are doing. :-)
     */
    private void measureDivider() {
        mDividerBound.left = (int) (mRootViewBound.left
                + mContext.getResources().getDimension(R.dimen.lib_divider_horizontal_margin));
        mDividerBound.right = (int) (mRootViewBound.right
                - mContext.getResources().getDimension(R.dimen.lib_divider_horizontal_margin));
        mDividerBound.top = (int) (mRootViewBound.top + (mRootViewBound.height() * Constants.KEY_BOARD_TOP_WEIGHT)
                - mContext.getResources().getDimension(R.dimen.lib_divider_vertical_margin));
        mDividerBound.bottom = (int) (mRootViewBound.top + (mRootViewBound.height() * Constants.KEY_BOARD_TOP_WEIGHT)
                - mContext.getResources().getDimension(R.dimen.lib_divider_vertical_margin));
    }

    ///////////////////////////////////////////////////////////////
    //                  VIEW DRAW
    ///////////////////////////////////////////////////////////////


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDivider(canvas);

        //Draw the finger print box
        mBoxFingerprint.drawView(canvas);

        //Pass it to the implementation
        drawView(canvas);
    }

    private void drawDivider(Canvas canvas) {
        canvas.drawLine(mDividerBound.left,
                mDividerBound.top,
                mDividerBound.right,
                mDividerBound.bottom,
                mDividerPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //Stop scanning fingerprint
        mBoxFingerprint.reset();
    }

    @Override
    @CallSuper
    public void onAuthenticationSuccess() {
        if (mAuthenticationListener != null) mAuthenticationListener.onAuthenticationSuccessful();
    }

    @Override
    @CallSuper
    public void onAuthenticationFail() {
        if (mAuthenticationListener != null) mAuthenticationListener.onAuthenticationFailed();
    }

    @Override
    @CallSuper
    public void reset() {
        mBoxFingerprint.reset();
    }

    ///////////////////////////////////////////////////////////////
    //                  GETTERS/SETTERS
    ///////////////////////////////////////////////////////////////


    public void setAuthenticationListener(@NonNull AuthenticationListener authenticationListener) {
        mAuthenticationListener = authenticationListener;
    }

    /**
     * Get the colors of the dividers.
     */
    @ColorInt
    public int getDividerColor() {
        return mDividerColor;
    }

    public void setDividerColor(@ColorInt int dividerColor) {
        mDividerColor = dividerColor;
        prepareDividerPaint();
        invalidate();
    }

    public void setDividerColorRes(@ColorRes int dividerColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setDividerColor(mContext.getColor(dividerColor));
        } else {
            setDividerColor(mContext.getResources().getColor(dividerColor));
        }
    }

    public boolean isTactileFeedbackEnable() {
        return mIsTactileFeedbackEnabled;
    }

    public void setTactileFeedback(boolean enable) {
        mIsTactileFeedbackEnabled = enable;
    }


    public Boolean isFingerPrintEnable() {
        return mBoxFingerprint.isFingerPrintEnable();
    }

    public void setIsFingerPrintEnable(boolean isEnable) {
        mBoxFingerprint.setFingerPrintEnable(isEnable);
        requestLayout();
        invalidate();
    }

    @NonNull
    public String getFingerPrintStatusText() {
        return mBoxFingerprint.getStatusText();
    }

    public void setFingerPrintStatusText(@NonNull String statusText) {
        mBoxFingerprint.setStatusText(statusText);
        invalidate();
    }

    public int getFingerPrintStatusTextColor() {
        return mBoxFingerprint.getStatusTextColor();
    }

    public void setFingerPrintStatusTextColor(@ColorInt int statusTextColor) {
        mBoxFingerprint.setStatusTextColor(statusTextColor);
        invalidate();
    }

    public void setFingerPrintStatusTextColorRes(@ColorRes int statusTextColor) {
        mBoxFingerprint.setStatusTextColor(mContext.getResources().getColor(statusTextColor));
        invalidate();
    }

    public float getFingerPrintStatusTextSize() {
        return mBoxFingerprint.getStatusTextSize();
    }

    public void setFingerPrintStatusTextSize(@DimenRes int statusTextSize) {
        mBoxFingerprint.setStatusTextSize(getResources().getDimension(statusTextSize));
        invalidate();
    }

    public void setFingerPrintStatusTextSize(@Dimension float statusTextSize) {
        mBoxFingerprint.setStatusTextSize(statusTextSize);
        invalidate();
    }
}
