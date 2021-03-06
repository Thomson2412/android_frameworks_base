/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NumPadKey extends ViewGroup {
    // list of "ABC", etc per digit, starting with '0'
    static String sKlondike[];
    private static List<Integer> sDigits = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    private static int sCount = 0;
    private static boolean sShuffled;
    private int mDigit = -1;
    private int mTextViewResId;
    private PasswordTextView mTextView;
    private TextView mDigitText;
    private TextView mKlondikeText;
    private boolean mEnableHaptics;
    private PowerManager mPM;

    private TypedArray mStyleable;

    private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View thisView) {
            if (mTextView == null && mTextViewResId > 0) {
                final View v = NumPadKey.this.getRootView().findViewById(mTextViewResId);
                if (v != null && v instanceof PasswordTextView) {
                    mTextView = (PasswordTextView) v;
                }
            }
            if (mTextView != null && mTextView.isEnabled()) {
                mTextView.append(Character.forDigit(mDigit, 10));
            }
            userActivity();
            doHapticKeyClick();
        }
    };

    public void userActivity() {
        mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    public NumPadKey(Context context) {
        this(context, null);
    }

    public NumPadKey(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumPadKey(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);

        mStyleable = context.obtainStyledAttributes(attrs, R.styleable.NumPadKey);

        mTextViewResId = mStyleable.getResourceId(R.styleable.NumPadKey_textView, 0);

        setOnClickListener(mListener);
        setOnHoverListener(new LiftToActivateListener(context));
        setAccessibilityDelegate(new ObscureSpeechDelegate(context));

        mEnableHaptics = new LockPatternUtils(context).isTactileFeedbackEnabled();

        mPM = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.keyguard_num_pad_key, this, true);

        mDigitText = (TextView) findViewById(R.id.digit_text);
        mKlondikeText = (TextView) findViewById(R.id.klondike_text);

        createNumKeyPad(false);
    }

    public void createNumKeyPad(boolean enableRandom) {
        if (enableRandom) {
            if (!sShuffled) {
                Collections.shuffle(sDigits);
                sShuffled = true;
            }
            mDigit = sDigits.get(sCount);
        } else {
            mDigit = mStyleable.getInt(R.styleable.NumPadKey_digit, mDigit);
        }

        mDigitText.setText(Integer.toString(mDigit));

        if (mDigit >= 0) {
            if (sKlondike == null) {
                sKlondike = getResources().getStringArray(R.array.lockscreen_num_pad_klondike);
            }
            if (sKlondike != null && sKlondike.length > mDigit) {
                String klondike = sKlondike[mDigit];
                final int len = klondike.length();
                if (len > 0) {
                    mKlondikeText.setText(klondike);
                } else {
                    mKlondikeText.setVisibility(View.INVISIBLE);
                }
            }
        }
        sCount++;

        setBackground(mContext.getDrawable(R.drawable.ripple_drawable));
        setContentDescription(mDigitText.getText().toString());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Reset the "announced headset" flag when detached.
        ObscureSpeechDelegate.sAnnouncedHeadset = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int digitHeight = mDigitText.getMeasuredHeight();
        int klondikeHeight = mKlondikeText.getMeasuredHeight();
        int totalHeight = digitHeight + klondikeHeight;
        int top = getHeight() / 2 - totalHeight / 2;
        int centerX = getWidth() / 2;
        int left = centerX - mDigitText.getMeasuredWidth() / 2;
        int bottom = top + digitHeight;
        mDigitText.layout(left, top, left + mDigitText.getMeasuredWidth(), bottom);
        top = (int) (bottom - klondikeHeight * 0.35f);
        bottom = top + klondikeHeight;

        left = centerX - mKlondikeText.getMeasuredWidth() / 2;
        mKlondikeText.layout(left, top, left + mKlondikeText.getMeasuredWidth(), bottom);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // Cause a VIRTUAL_KEY vibration
    public void doHapticKeyClick() {
        if (mEnableHaptics) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    public void initNumKeyPad() {
        sCount = 0;
        sShuffled = false;
    }
}
