package com.trillai.trillroot.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Widgets {
    // True AMOLED black with high-luminance neon accents.
    static final int BG = 0xFF000000;
    static final int SURFACE = 0xFF070B0E;
    static final int RAISED = 0xFF0D151A;
    static final int STROKE = 0xFF184A55;
    static final int ACCENT = 0xFF70FF00;
    static final int TEXT = 0xFFFFFFFF;
    static final int MUTED = 0xFFB7C8CD;
    static final int WARNING = 0xFFFFE600;
    static final int CYAN = 0xFF00F5FF;
    static final int MAGENTA = 0xFFFF2BD6;
    static final int VIOLET = 0xFF9D72FF;
    static final int DANGER = 0xFFFF496C;

    private Widgets() { }

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    static LinearLayout row(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    static TextView text(Context c, String value, float size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans", Typeface.NORMAL));
        v.setLineSpacing(dp(c, 3), 1.0f);
        v.setIncludeFontPadding(true);
        return v;
    }

    static TextView heading(Context c, String value, float size) {
        TextView v = text(c, value, size, TEXT, true);
        SpannableString neon = new SpannableString(value);
        int[] colors = {CYAN, MAGENTA, ACCENT, VIOLET};
        int word = 0;
        boolean inWord = false;
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            boolean letter = i < value.length() && !Character.isWhitespace(value.charAt(i));
            if (letter && !inWord) { start = i; inWord = true; }
            if (!letter && inWord) {
                neon.setSpan(new ForegroundColorSpan(colors[word++ % colors.length]), start, i,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                inWord = false;
            }
        }
        v.setText(neon);
        if (Build.VERSION.SDK_INT >= 28) v.setAccessibilityHeading(true);
        return v;
    }

    static TextView eyebrow(Context c, String value) {
        TextView v = text(c, value, 11, CYAN, true);
        v.setLetterSpacing(0.12f);
        return v;
    }

    static void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(parent.getContext(), top);
        parent.addView(child, lp);
    }

    static LinearLayout card(Context c) {
        LinearLayout v = column(c);
        v.setBackground(rounded(c, SURFACE, 20, STROKE));
        int p = dp(c, 20);
        v.setPadding(p, p, p, p);
        return v;
    }

    static GradientDrawable rounded(Context c, int fill, float radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radius));
        if (stroke != 0) d.setStroke(dp(c, 1), stroke);
        return d;
    }

    static TextView pill(Context c, String value, int color) {
        TextView v = text(c, value, 11, color, true);
        v.setBackground(rounded(c, SURFACE, 12, STROKE));
        int px = dp(c, 10), py = dp(c, 6);
        v.setPadding(px, py, px, py);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    static Button button(Context c, String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(primary ? BG : TEXT);
        b.setMinHeight(dp(c, 52));
        b.setMinimumHeight(dp(c, 52));
        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        b.setStateListAnimator(null);
        b.setElevation(0);
        GradientDrawable fill = rounded(c, primary ? ACCENT : RAISED, 14, primary ? 0 : STROKE);
        GradientDrawable mask = rounded(c, Color.WHITE, 14, 0);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x338BDDD8), fill, mask));
        b.setOnClickListener(listener);
        return b;
    }

    static void detail(LinearLayout parent, String label, String value) {
        Context c = parent.getContext();
        LinearLayout row = row(c);
        row.setGravity(Gravity.TOP);
        TextView left = text(c, label, 13, MUTED, false);
        TextView right = text(c, value, 13, TEXT, false);
        right.setGravity(Gravity.END);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.85f);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f);
        b.leftMargin = dp(c, 14);
        row.addView(left, a);
        row.addView(right, b);
        int py = dp(c, 10);
        row.setPadding(0, py, 0, py);
        add(parent, row, 0);
    }

    static void bullet(LinearLayout parent, String value, String marker, int color) {
        Context c = parent.getContext();
        LinearLayout row = row(c);
        row.setGravity(Gravity.TOP);
        TextView number = text(c, marker, 13, color, true);
        TextView body = text(c, value, 14, TEXT, false);
        row.addView(number, new LinearLayout.LayoutParams(dp(c, 32), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setPadding(0, dp(c, 8), 0, dp(c, 8));
        add(parent, row, 0);
    }

    static void notice(LinearLayout parent, String title, String body, int color) {
        Context c = parent.getContext();
        LinearLayout box = column(c);
        box.setBackground(rounded(c, SURFACE, 16, STROKE));
        int p = dp(c, 16);
        box.setPadding(p, p, p, p);
        add(box, text(c, title, 14, color, true), 0);
        add(box, text(c, body, 13, MUTED, false), 7);
        add(parent, box, 14);
    }

    static ImageView icon(Context c, int resource, int tint) {
        ImageView image = new ImageView(c);
        image.setImageResource(resource);
        image.setImageTintList(ColorStateList.valueOf(tint));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return image;
    }

    static final class MaxWidthLayout extends LinearLayout {
        private final int maximum;
        MaxWidthLayout(Context c) {
            super(c);
            setOrientation(VERTICAL);
            maximum = dp(c, 760);
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            if (width > maximum) widthSpec = MeasureSpec.makeMeasureSpec(maximum, MeasureSpec.EXACTLY);
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
