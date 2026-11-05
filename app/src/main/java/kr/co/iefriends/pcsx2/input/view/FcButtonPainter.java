/*
   Futebol Mobile button look: dark round button with a blue glowing ring,
   a white PlayStation symbol (or "L1"/"R1") and the action name inside.
   GPLv3, part of this ARMSX2 fork.
*/

package kr.co.iefriends.pcsx2.input.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

public final class FcButtonPainter {

    public static final int SYMBOL_NONE = 0;
    public static final int SYMBOL_TRIANGLE = 1;
    public static final int SYMBOL_SQUARE = 2;
    public static final int SYMBOL_CROSS = 3;
    public static final int SYMBOL_CIRCLE = 4;
    public static final int SYMBOL_STICK = 5; // ring with a dot: "analog stick" mode
    public static final int SYMBOL_DPAD = 6;  // plus sign: "arrows" mode

    private static final int GLOW = 0xFF4FA8FF;
    private static final int FILL = 0xE00D1826;
    private static final int FILL_PRESSED = 0xF0214A78;
    private static final int INNER_RING = 0x5596C8FF;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint symbol = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public FcButtonPainter() {
        fill.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.STROKE);
        glow.setColor(GLOW);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(GLOW);
        inner.setStyle(Paint.Style.STROKE);
        inner.setColor(INNER_RING);
        symbol.setStyle(Paint.Style.STROKE);
        symbol.setColor(0xFFFFFFFF);
        symbol.setStrokeJoin(Paint.Join.ROUND);
        symbol.setStrokeCap(Paint.Cap.ROUND);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setShadowLayer(3f, 0f, 1f, 0xCC000000);
    }

    /**
     * @param symbolType one of SYMBOL_*; ignored when {@code symbolText} is set
     * @param symbolText big text drawn instead of a symbol (e.g. "L1"), may be null
     * @param label small action name under the symbol, may be null
     */
    public void draw(Canvas c, int w, int h, boolean pressed, int symbolType, String symbolText, String label) {
        float d = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float glowW = d * 0.07f;
        float r = d / 2f - glowW;

        // Soft glow: two wide, faint rings outside the main ring.
        glow.setStrokeWidth(glowW * 1.6f);
        glow.setAlpha(pressed ? 90 : 45);
        c.drawCircle(cx, cy, r, glow);
        glow.setStrokeWidth(glowW * 0.8f);
        glow.setAlpha(pressed ? 150 : 90);
        c.drawCircle(cx, cy, r, glow);

        fill.setColor(pressed ? FILL_PRESSED : FILL);
        c.drawCircle(cx, cy, r, fill);

        ring.setStrokeWidth(Math.max(2f, d * 0.025f));
        ring.setAlpha(pressed ? 255 : 220);
        c.drawCircle(cx, cy, r, ring);

        inner.setStrokeWidth(Math.max(1f, d * 0.012f));
        c.drawCircle(cx, cy, r * 0.86f, inner);

        boolean hasLabel = label != null && !label.isEmpty();
        float symCy = hasLabel ? cy - d * 0.08f : cy;
        float s = d * 0.15f; // half-size of the symbol

        if (symbolText != null && !symbolText.isEmpty()) {
            text.setTextSize(d * 0.24f);
            c.drawText(symbolText, cx, symCy + text.getTextSize() * 0.36f, text);
        } else {
            symbol.setStrokeWidth(Math.max(2.5f, d * 0.045f));
            switch (symbolType) {
                case SYMBOL_TRIANGLE:
                    path.reset();
                    path.moveTo(cx, symCy - s);
                    path.lineTo(cx + s * 1.1f, symCy + s * 0.8f);
                    path.lineTo(cx - s * 1.1f, symCy + s * 0.8f);
                    path.close();
                    c.drawPath(path, symbol);
                    break;
                case SYMBOL_SQUARE:
                    c.drawRect(cx - s * 0.9f, symCy - s * 0.9f, cx + s * 0.9f, symCy + s * 0.9f, symbol);
                    break;
                case SYMBOL_CROSS:
                    c.drawLine(cx - s, symCy - s, cx + s, symCy + s, symbol);
                    c.drawLine(cx + s, symCy - s, cx - s, symCy + s, symbol);
                    break;
                case SYMBOL_CIRCLE:
                    c.drawCircle(cx, symCy, s, symbol);
                    break;
                case SYMBOL_STICK:
                    c.drawCircle(cx, symCy, s * 1.1f, symbol);
                    fill.setColor(0xFFFFFFFF);
                    c.drawCircle(cx, symCy, s * 0.4f, fill);
                    break;
                case SYMBOL_DPAD:
                    fill.setColor(0xFFFFFFFF);
                    c.drawRoundRect(cx - s * 0.35f, symCy - s * 1.15f, cx + s * 0.35f, symCy + s * 1.15f, s * 0.15f, s * 0.15f, fill);
                    c.drawRoundRect(cx - s * 1.15f, symCy - s * 0.35f, cx + s * 1.15f, symCy + s * 0.35f, s * 0.15f, s * 0.15f, fill);
                    break;
                default:
                    break;
            }
        }

        if (hasLabel) {
            text.setTextSize(d * 0.12f);
            c.drawText(label, cx, cy + d * 0.25f, text);
        }
    }

    /** Joystick base: dark disc, glowing ring, four small arrows. */
    public void drawStickBase(Canvas c, float cx, float cy, float r, boolean active) {
        float glowW = r * 0.08f;
        glow.setStrokeWidth(glowW * 1.6f);
        glow.setAlpha(active ? 90 : 45);
        c.drawCircle(cx, cy, r, glow);
        glow.setStrokeWidth(glowW * 0.8f);
        glow.setAlpha(active ? 150 : 90);
        c.drawCircle(cx, cy, r, glow);

        fill.setColor(0xB00D1826);
        c.drawCircle(cx, cy, r, fill);
        ring.setStrokeWidth(Math.max(2f, r * 0.03f));
        ring.setAlpha(230);
        c.drawCircle(cx, cy, r, ring);
        inner.setStrokeWidth(Math.max(1f, r * 0.015f));
        c.drawCircle(cx, cy, r * 0.9f, inner);

        Paint arrow = fill;
        arrow.setColor(0xE6FFFFFF);
        float a = r * 0.08f;
        float o = r * 0.78f;
        drawArrow(c, arrow, cx, cy - o, a, 0);
        drawArrow(c, arrow, cx, cy + o, a, 2);
        drawArrow(c, arrow, cx - o, cy, a, 3);
        drawArrow(c, arrow, cx + o, cy, a, 1);
    }

    /**
     * One arrow key of the d-pad (no circle behind it): dark rounded key, blue glowing
     * outline and a white arrow. dir: 0 up, 1 right, 2 down, 3 left.
     */
    public void drawDpadKey(Canvas c, android.graphics.RectF r, boolean pressed, int dir) {
        float m = Math.min(r.width(), r.height());
        float rad = m * 0.22f;
        float glowW = m * 0.12f;
        glow.setStrokeWidth(glowW * 1.6f);
        glow.setAlpha(pressed ? 90 : 45);
        c.drawRoundRect(r, rad, rad, glow);
        glow.setStrokeWidth(glowW * 0.8f);
        glow.setAlpha(pressed ? 150 : 90);
        c.drawRoundRect(r, rad, rad, glow);
        fill.setColor(pressed ? FILL_PRESSED : FILL);
        c.drawRoundRect(r, rad, rad, fill);
        ring.setStrokeWidth(Math.max(2f, m * 0.04f));
        ring.setAlpha(pressed ? 255 : 220);
        c.drawRoundRect(r, rad, rad, ring);
        fill.setColor(0xFFFFFFFF);
        drawArrow(c, fill, r.centerX(), r.centerY(), m * 0.2f, dir);
    }

    /** Joystick knob: light grey ball. */
    public void drawStickKnob(Canvas c, float x, float y, float r) {
        fill.setColor(0xFFD5DBE1);
        c.drawCircle(x, y, r, fill);
        inner.setStrokeWidth(Math.max(1f, r * 0.05f));
        c.drawCircle(x, y, r, inner);
    }

    // dir: 0 up, 1 right, 2 down, 3 left
    private void drawArrow(Canvas c, Paint p, float x, float y, float a, int dir) {
        path.reset();
        switch (dir) {
            case 0: path.moveTo(x, y - a); path.lineTo(x + a, y + a * 0.6f); path.lineTo(x - a, y + a * 0.6f); break;
            case 2: path.moveTo(x, y + a); path.lineTo(x + a, y - a * 0.6f); path.lineTo(x - a, y - a * 0.6f); break;
            case 3: path.moveTo(x - a, y); path.lineTo(x + a * 0.6f, y - a); path.lineTo(x + a * 0.6f, y + a); break;
            default: path.moveTo(x + a, y); path.lineTo(x - a * 0.6f, y - a); path.lineTo(x - a * 0.6f, y + a); break;
        }
        path.close();
        c.drawPath(path, p);
    }
}
