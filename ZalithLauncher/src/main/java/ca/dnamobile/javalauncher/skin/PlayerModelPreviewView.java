package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * A simple 2D player-skin preview widget.
 * Renders a flat representation of the player skin (head + torso outline)
 * using the skin bitmap if available, or a placeholder silhouette.
 */
public class PlayerModelPreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private android.graphics.Bitmap skinBitmap;

    public PlayerModelPreviewView(Context context) {
        super(context);
    }

    public PlayerModelPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerModelPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** Set the skin bitmap to display. Pass null to show the placeholder silhouette. */
    public void setSkin(@Nullable android.graphics.Bitmap skin) {
        this.skinBitmap = skin;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        if (skinBitmap != null) {
            // Draw head (top-left 8x8 in skin = face)
            int headSize = w / 2;
            int headLeft = (w - headSize) / 2;
            Rect src = new Rect(8, 8, 16, 16);
            Rect dst = new Rect(headLeft, 4, headLeft + headSize, 4 + headSize);
            canvas.drawBitmap(skinBitmap, src, dst, paint);

            // Draw torso (20-28, 20-32 in skin)
            int torsoTop = 4 + headSize + 2;
            int torsoW = headSize;
            int torsoH = (int) (headSize * 1.5f);
            Rect srcT = new Rect(20, 20, 28, 32);
            Rect dstT = new Rect((w - torsoW) / 2, torsoTop,
                    (w - torsoW) / 2 + torsoW, torsoTop + torsoH);
            canvas.drawBitmap(skinBitmap, srcT, dstT, paint);
        } else {
            // Placeholder silhouette
            paint.setColor(Color.parseColor("#AAAAAA"));
            int headSize = w / 3;
            int headLeft = (w - headSize) / 2;
            canvas.drawRect(headLeft, 8, headLeft + headSize, 8 + headSize, paint);
            int bodyTop = 8 + headSize + 4;
            canvas.drawRect(w / 4f, bodyTop, w * 3 / 4f, bodyTop + headSize * 2, paint);
        }
    }
}
