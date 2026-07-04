package ca.dnamobile.javalauncher.input;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * An overlay view that renders a software cursor for virtual-mouse mode
 * (used when touch controls are in pointer/cursor mode).
 */
public class GameCursorOverlay extends View {
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cursorX = 0f;
    private float cursorY = 0f;
    private boolean visible = false;

    public GameCursorOverlay(Context context) {
        super(context);
        init();
    }

    public GameCursorOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cursorPaint.setColor(0xFFFFFFFF);
        cursorPaint.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
    }

    public void setCursorPosition(float x, float y) {
        cursorX = x;
        cursorY = y;
        if (visible) invalidate();
    }

    public void setCursorVisible(boolean visible) {
        this.visible = visible;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!visible) return;
        canvas.drawCircle(cursorX, cursorY, 8f, cursorPaint);
    }
}
