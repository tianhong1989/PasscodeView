package com.kevalpatel.passcodeview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Created by Keval on 07-Apr-17.
 *
 * @author 'https://github.com/kevalpatel2106'
 */

class BoxKeypad extends Box {
    static final int KEY_TYPE_CIRCLE = 0;
    static final int KEY_TYPE_RECT = 1;
    static final float KEY_BOARD_BOTTOM_WEIGHT = 0.14F;
    static final float KEY_BOARD_TOP_WEIGHT = 0.2F;
    private static final int NO_OF_COLUMNS = 3;
    private static final int NO_OF_ROWS = 4;
    private static final String[] KEY_VALUES = new String[]{"1", "4", "7", "", "2", "5", "8", "0", "3", "6", "9", "-1"};

    //Theme params
    private boolean mIsOneHandOperation = false;    //Bool to set true if you want to display one hand key board.
    @Dimension
    private float mKeyPadding;                      //Surround padding to each single key
    @Dimension
    private float mKeyTextSize;                     //Surround padding to each single key
    @Dimension
    private float mKeyStrokeWidth;                   //Surround padding to each single key
    @ColorInt
    private int mKeyStrokeColor;                    //KeyCircle background stroke color
    @ColorInt
    private int mKeyTextColor;                      //KeyCircle text color
    private int mKeyShape = KEY_TYPE_CIRCLE;

    private ArrayList<Key> mKeys;

    //Paint
    private Paint mKeyPaint;
    private TextPaint mKeyTextPaint;
    private boolean isFingerPrintEnable;

    private Rect mKeyBoxBound = new Rect();

    /**
     * Public constructor
     *
     * @param pinView {@link PinView} in which box will be displayed.
     */
    BoxKeypad(@NonNull PinView pinView) {
        super(pinView);
        mKeyPadding = getContext().getResources().getDimension(R.dimen.key_padding);
        isFingerPrintEnable = FingerPrintUtils.isFingerPrintEnrolled(getContext());
    }

    /**
     * Measure and display the keypad box.
     * |------------------------|=|
     * |                        | |
     * |                        | | => The title and the indicator. ({@link BoxTitleIndicator#measure(Rect)})
     * |                        | |
     * |                        | |
     * |------------------------|=| => {@link #KEY_BOARD_TOP_WEIGHT} of the total height.
     * |                        | |
     * |                        | |
     * |                        | |
     * |                        | |
     * |                        | |
     * |                        | | => Keypad height.
     * |                        | |
     * |                        | |
     * |                        | |
     * |                        | |
     * |                        | |
     * |------------------------|=|=> {@link #KEY_BOARD_BOTTOM_WEIGHT} of the total weight if the fingerprint is available. Else it touches to the bottom of the main view.
     * |                        | |
     * |                        | |=> Section for fingerprint. If the fingerprint is enabled. Otherwise keyboard streaches to the bottom of the root view.
     * |------------------------|=|
     * Don't change until you know what you are doing. :-)
     *
     * @param rootViewBound bound of the main view.
     */
    @Override
    void measure(@NonNull Rect rootViewBound) {
        mKeyBoxBound.left = mIsOneHandOperation ? (int) (rootViewBound.width() * 0.3) : 0;
        mKeyBoxBound.right = rootViewBound.width();
        mKeyBoxBound.top = (int) (rootViewBound.top + (rootViewBound.height() * KEY_BOARD_TOP_WEIGHT));
        mKeyBoxBound.bottom = (int) (rootViewBound.bottom -
                rootViewBound.height() * (isFingerPrintEnable ? KEY_BOARD_BOTTOM_WEIGHT : 0));

        float singleKeyHeight = mKeyBoxBound.height() / NO_OF_ROWS;
        float singleKeyWidth = mKeyBoxBound.width() / NO_OF_COLUMNS;

        mKeys = new ArrayList<>();
        for (int colNo = 0; colNo < NO_OF_COLUMNS; colNo++) {

            for (int rowNo = 0; rowNo < NO_OF_ROWS; rowNo++) {
                Rect keyBound = new Rect();
                keyBound.left = (int) ((colNo * singleKeyWidth) + mKeyBoxBound.left);
                keyBound.right = (int) (keyBound.left + singleKeyWidth);
                keyBound.top = (int) ((rowNo * singleKeyHeight) + mKeyBoxBound.top);
                keyBound.bottom = (int) (keyBound.top + singleKeyHeight);

                switch (mKeyShape) {
                    case KEY_TYPE_CIRCLE:
                        mKeys.add(new KeyCircle(getRootView(), KEY_VALUES[mKeys.size()], keyBound, mKeyPadding));
                        break;
                    case KEY_TYPE_RECT:
                        mKeys.add(new KeyRect(getRootView(), KEY_VALUES[mKeys.size()], keyBound, mKeyPadding));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid key shape.");
                }
            }
        }
    }

    @Override
    void preparePaint() {
        //Set the keyboard paint
        mKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mKeyPaint.setStyle(Paint.Style.STROKE);
        mKeyPaint.setColor(mKeyStrokeColor);
        mKeyPaint.setTextSize(mKeyTextSize);
        mKeyPaint.setStrokeWidth(mKeyStrokeWidth);

        //Set the keyboard text paint
        mKeyTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mKeyTextPaint.setColor(mKeyTextColor);
        mKeyTextPaint.setTextSize(mKeyTextSize);
        mKeyTextPaint.setFakeBoldText(true);
        mKeyTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    void onValueEntered(@NonNull String newValue) {
        //Do nothing
    }

    /**
     * Set the default theme parameters.
     */
    @SuppressWarnings("deprecation")
    @Override
    void setDefaults() {
        mKeyTextColor = getContext().getResources().getColor(R.color.key_default_color);
        mKeyStrokeColor = getContext().getResources().getColor(R.color.key_background_color);
        mKeyTextSize = getContext().getResources().getDimension(R.dimen.key_text_size);
        mKeyStrokeWidth = getContext().getResources().getDimension(R.dimen.key_stroke_width);
    }

    @Override
    void onAuthenticationFail() {
        //Vibrate all the keys.
        for (Key key : mKeys) key.playError();
    }

    @Override
    void onAuthenticationSuccess() {
        //DO nothing
    }

    /**
     * Draw keyboard on the canvas. This will draw all the {@link #KEY_VALUES} on the canvas.
     *
     * @param canvas canvas on which the keyboard will be drawn.
     */
    @Override
    void draw(@NonNull Canvas canvas) {
        for (Key key : mKeys) {
            if (key.getDigit().isEmpty()) continue; //Don't draw the empty button
            key.draw(canvas, mKeyPaint, mKeyTextPaint);
        }
    }

    /**
     * Find which key is pressed based on the ACTION_DOWN and ACTION_UP coordinates.
     *
     * @param downEventX ACTION_DOWN event X coordinate
     * @param downEventY ACTION_DOWN event Y coordinate
     * @param upEventX   ACTION_UP event X coordinate
     * @param upEventY   ACTION_UP event Y coordinate
     */
    @Nullable
    String findKeyPressed(float downEventX, float downEventY, float upEventX, float upEventY) {
        //figure out down key.
        for (Key key : mKeys) {

            //Update the typed passcode
            if (key.checkKeyPressed(downEventX, downEventY, upEventX, upEventY)) {
                key.playClickAnimation();
                return key.getDigit();
            }
        }
        return null;
    }

    ///////////////// SETTERS/GETTERS //////////////

    ArrayList<Key> getKeys() {
        return mKeys;
    }

    Rect getBounds() {
        return mKeyBoxBound;
    }

    int getKeyBackgroundColor() {
        return mKeyStrokeColor;
    }

    void setKeyBackgroundColor(@ColorInt int keyBackgroundColor) {
        mKeyStrokeColor = keyBackgroundColor;
    }

    int getKeyTextColor() {
        return mKeyTextColor;
    }

    void setKeyTextColor(@ColorInt int keyTextColor) {
        mKeyTextColor = keyTextColor;
    }

    float getKeyPadding() {
        return mKeyPadding;
    }

    void setKeyPadding(float keyPadding) {
        mKeyPadding = keyPadding;
    }

    void setKeyTextSize(float keyTextSize) {
        mKeyTextSize = keyTextSize;
    }

    void setKeyStrokeWidth(float keyStrokeWidth) {
        mKeyStrokeWidth = keyStrokeWidth;
    }

    boolean isOneHandOperation() {
        return mIsOneHandOperation;
    }

    void setOneHandOperation(boolean oneHandOperation) {
        mIsOneHandOperation = oneHandOperation;
    }

    @KeyShapes
    int getKeyShape() {
        return mKeyShape;
    }

    void setKeyShape(@KeyShapes int keyShape) {
        mKeyShape = keyShape;
    }

    void setFingerPrintEnable(boolean fingerPrintEnable) {
        isFingerPrintEnable = fingerPrintEnable && FingerPrintUtils.isFingerPrintEnrolled(getContext());
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({KEY_TYPE_CIRCLE, KEY_TYPE_RECT})
    @interface KeyShapes {
    }
}
