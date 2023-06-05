package com.ljx.example.pre;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;


/**
 * User: ljx
 * Date: 2023/6/5
 * Time: 19:18
 */
public class CustomCheckBoxPreference extends CheckBoxPreference {

    public CustomCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomCheckBoxPreference(Context context) {
        super(context);
    }
}
