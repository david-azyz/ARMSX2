/*
   PathZz2 button look: dark "glass" button with a glowing coloured ring, a small
   PlayStation symbol and the action name. Green while attacking, blue while defending.
   GPLv3, part of this ARMSX2 fork.
*/

package kr.co.iefriends.pcsx2.input.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

public final class FcButtonPainter {

    public static final int SYMBOL_NONE = 0;
    public static final int SYMBOL_TRIANGLE = 1;
    public static final int SYMBOL_SQUARE = 2;
    public static final int SYMBOL_CROSS = 3;
    public static final int SYMBOL_CIRCLE = 4;
    public static final int SYMBOL_STICK = 5; // ring with a dot: "analog stick" mode
    public static final int SYMBOL_DPAD = 6;  // plus sign: "arrows" mode

    // Attack (with the ball) and defense (without the ball) colours.
    private static final int ACCENT_ATK = 0xFF3DF29A;
    private static final int ACCENT_DEF = 0xFF5CC2FF;
    private static final int FILL_IN_ATK = 0xE80F3B29;
    private static final int FILL_OUT_ATK = 0xF0071A12;
    private static final int FILL_IN_DEF = 0xE80F2F4D;
    private static final int FILL_OUT_DEF = 0xF006121F;

    private static volatile boolean sDefense = false;

    /** Switches every button between the attack (green) and defense (blue) look. Views must be invalidated. */
    public static void setDefense(boolean defense) { sDefense = defense; }

    public static boolean isDefense() { return sDefense; }

    public static int accent() { return sDefense ? ACCENT_DEF : ACCENT_ATK; }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint symbol = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public FcButtonPainter() {
        fill.setStyle(Paint.Style.FILL);
        shade.setStyle(Paint.Style.FILL);
        ring.setStyle(Paint.Style.STROKE);
        symbol.setStyle(Paint.Style.STROKE);
        symbol.setStrokeJoin(Paint.Join.ROUND);
        symbol.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setLetterSpacing(0.04f);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24);
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24);
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Soft coloured halo just outside a circle of radius r. */
    private void drawHalo(Canvas c, float cx, float cy, float r, float outer, int alpha) {
        int acc = accent();
        float total = r + outer;
        shade.setShader(new RadialGradient(cx, cy, total,
                new int[]{withAlpha(acc, 0), withAlpha(acc, alpha), withAlpha(acc, 0)},
                new float[]{Math.max(0f, (r - outer * 0.5f) / total), r / total, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, total, shade);
        shade.setShader(null);
    }

    /** Dark glass disc with a light reflection on top. */
    private void drawGlass(Canvas c, float cx, float cy, float r, boolean pressed) {
        int in = sDefense ? FILL_IN_DEF : FILL_IN_ATK;
        int out = sDefense ? FILL_OUT_DEF : FILL_OUT_ATK;
        if (pressed) {
            in = mix(in, accent(), 0.35f);
            out = mix(out, accent(), 0.18f);
        }
        fill.setShader(new RadialGradient(cx, cy + r * 0.2f, r * 1.15f, in, out, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(new RadialGradient(cx, cy - r * 0.62f, r * 0.8f, 0x38FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(null);
    }

    private void fitText(String s, float size, float maxWidth) {
        text.setTextSize(size);
        float w = text.measureText(s);
        if (w > maxWidth && w > 0) text.setTextSize(size * maxWidth / w);
    }

    /**
     * @param symbolType one of SYMBOL_*; ignored when {@code symbolText} is set
     * @param symbolText text drawn instead of a symbol (e.g. "L1"), may be null
     * @param label action name, may be null
     */
    public void draw(Canvas c, int w, int h, boolean pressed, int symbolType, String symbolText, String label) {
        draw(c, w, h, pressed, symbolType, symbolText, label, null);
    }

    /** Same as above; {@code hint} is a small coloured line under the name (e.g. "deslize: CRUZAR") plus swipe marks. */
    public void draw(Canvas c, int w, int h, boolean pressed, int symbolType, String symbolText, String label, String hint) {
        float d = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float halo = d * 0.11f;
        float r = d / 2f - halo;
        int acc = accent();

        drawHalo(c, cx, cy, r, halo, pressed ? 200 : 120);
        drawGlass(c, cx, cy, r, pressed);

        ring.setColor(acc);
        ring.setAlpha(pressed ? 255 : 235);
        ring.setStrokeWidth(Math.max(2f, d * 0.024f));
        c.drawCircle(cx, cy, r, ring);
        ring.setAlpha(60);
        ring.setStrokeWidth(Math.max(1f, d * 0.01f));
        c.drawCircle(cx, cy, r * 0.88f, ring);

        boolean hasLabel = label != null && !label.isEmpty();
        boolean hasHint = hint != null && !hint.isEmpty();
        text.setShadowLayer(d * 0.02f, 0f, d * 0.008f, 0xAA000000);
        float shift = hasHint ? d * 0.05f : 0f;
        if (hasHint) {
            // Little chevrons around the ring: "you can drag your finger here".
            symbol.setColor(acc);
            symbol.setStrokeWidth(Math.max(1.5f, d * 0.016f));
            float o = r + halo * 0.55f;
            float a = d * 0.03f;
            drawChevron(c, cx, cy - o, a, 0);
            drawChevron(c, cx + o, cy, a, 1);
            drawChevron(c, cx, cy + o, a, 2);
            drawChevron(c, cx - o, cy, a, 3);
            text.setColor(acc);
            fitText(hint, d * 0.075f, r * 1.5f);
            c.drawText(hint, cx, cy + d * 0.23f, text);
        }
        cy -= shift;

        if (symbolText != null && !symbolText.isEmpty()) {
            if (hasLabel) {
                text.setColor(acc);
                fitText(symbolText, d * 0.15f, r * 1.2f);
                c.drawText(symbolText, cx, cy - d * 0.06f, text);
                text.setColor(0xFFFFFFFF);
                fitText(label, d * 0.135f, r * 1.6f);
                c.drawText(label, cx, cy + d * 0.13f, text);
            } else {
                text.setColor(0xFFFFFFFF);
                fitText(symbolText, d * 0.22f, r * 1.5f);
                c.drawText(symbolText, cx, cy + text.getTextSize() * 0.36f, text);
            }
            return;
        }

        if (hasLabel && symbolType == SYMBOL_NONE) {
            // No PlayStation symbol (L3, R3, SELECT, START): just the name, big and centred.
            text.setColor(0xFFFFFFFF);
            fitText(label, d * 0.24f, r * 1.5f);
            c.drawText(label, cx, cy + text.getTextSize() * 0.36f, text);
        } else if (hasLabel) {
            // Small symbol in a little circle above the action name.
            float chipR = d * 0.085f;
            float chipY = cy - d * 0.12f;
            fill.setColor(0x40000000);
            c.drawCircle(cx, chipY, chipR, fill);
            ring.setColor(acc);
            ring.setAlpha(230);
            ring.setStrokeWidth(Math.max(1.5f, d * 0.012f));
            c.drawCircle(cx, chipY, chipR, ring);
            symbol.setColor(acc);
            symbol.setStrokeWidth(Math.max(1.5f, d * 0.014f));
            drawSymbol(c, symbolType, cx, chipY, chipR * 0.48f);

            text.setColor(0xFFFFFFFF);
            fitText(label, d * 0.14f, r * 1.62f);
            c.drawText(label, cx, cy + d * 0.14f, text);
        } else {
            symbol.setColor(0xFFFFFFFF);
            symbol.setStrokeWidth(Math.max(2.5f, d * 0.045f));
            drawSymbol(c, symbolType, cx, cy, d * 0.15f);
        }
    }

    private void drawSymbol(Canvas c, int symbolType, float cx, float cy, float s) {
        switch (symbolType) {
            case SYMBOL_TRIANGLE:
                path.reset();
                path.moveTo(cx, cy - s);
                path.lineTo(cx + s * 1.1f, cy + s * 0.8f);
                path.lineTo(cx - s * 1.1f, cy + s * 0.8f);
                path.close();
                c.drawPath(path, symbol);
                break;
            case SYMBOL_SQUARE:
                c.drawRect(cx - s * 0.85f, cy - s * 0.85f, cx + s * 0.85f, cy + s * 0.85f, symbol);
                break;
            case SYMBOL_CROSS:
                c.drawLine(cx - s * 0.9f, cy - s * 0.9f, cx + s * 0.9f, cy + s * 0.9f, symbol);
                c.drawLine(cx + s * 0.9f, cy - s * 0.9f, cx - s * 0.9f, cy + s * 0.9f, symbol);
                break;
            case SYMBOL_CIRCLE:
                c.drawCircle(cx, cy, s, symbol);
                break;
            case SYMBOL_STICK: {
                c.drawCircle(cx, cy, s * 1.1f, symbol);
                fill.setColor(symbol.getColor());
                c.drawCircle(cx, cy, s * 0.4f, fill);
                break;
            }
            case SYMBOL_DPAD: {
                fill.setColor(symbol.getColor());
                c.drawRoundRect(cx - s * 0.35f, cy - s * 1.15f, cx + s * 0.35f, cy + s * 1.15f, s * 0.15f, s * 0.15f, fill);
                c.drawRoundRect(cx - s * 1.15f, cy - s * 0.35f, cx + s * 1.15f, cy + s * 0.35f, s * 0.15f, s * 0.15f, fill);
                break;
            }
            default:
                break;
        }
    }

    /** Joystick base: dark glass disc, four chevrons and a glowing ring around the centre. */
    public void drawStickBase(Canvas c, float cx, float cy, float r, boolean active) {
        int acc = accent();
        fill.setShader(new RadialGradient(cx, cy, r, 0xB01A2633, 0xD80A0F16, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(null);
        ring.setColor(0x40FFFFFF);
        ring.setStrokeWidth(Math.max(1.5f, r * 0.02f));
        c.drawCircle(cx, cy, r, ring);

        float inner = r * 0.52f;
        drawHalo(c, cx, cy, inner, r * 0.16f, active ? 210 : 140);
        ring.setColor(acc);
        ring.setAlpha(active ? 255 : 220);
        ring.setStrokeWidth(Math.max(2f, r * 0.03f));
        c.drawCircle(cx, cy, inner, ring);

        symbol.setColor(0xD9FFFFFF);
        symbol.setStrokeWidth(Math.max(2f, r * 0.035f));
        float o = r * 0.8f;
        float a = r * 0.08f;
        drawChevron(c, cx, cy - o, a, 0);
        drawChevron(c, cx + o, cy, a, 1);
        drawChevron(c, cx, cy + o, a, 2);
        drawChevron(c, cx - o, cy, a, 3);
    }

    // dir: 0 up, 1 right, 2 down, 3 left
    private void drawChevron(Canvas c, float x, float y, float a, int dir) {
        path.reset();
        switch (dir) {
            case 0: path.moveTo(x - a, y + a * 0.5f); path.lineTo(x, y - a * 0.5f); path.lineTo(x + a, y + a * 0.5f); break;
            case 2: path.moveTo(x - a, y - a * 0.5f); path.lineTo(x, y + a * 0.5f); path.lineTo(x + a, y - a * 0.5f); break;
            case 3: path.moveTo(x + a * 0.5f, y - a); path.lineTo(x - a * 0.5f, y); path.lineTo(x + a * 0.5f, y + a); break;
            default: path.moveTo(x - a * 0.5f, y - a); path.lineTo(x + a * 0.5f, y); path.lineTo(x - a * 0.5f, y + a); break;
        }
        c.drawPath(path, symbol);
    }

    /** Joystick knob: polished metal ball. */
    public void drawStickKnob(Canvas c, float x, float y, float r) {
        fill.setColor(0x66000000);
        c.drawCircle(x, y + r * 0.12f, r * 1.03f, fill);
        fill.setShader(new RadialGradient(x - r * 0.25f, y - r * 0.35f, r * 1.4f,
                new int[]{0xFFFFFFFF, 0xFFC9CED4, 0xFF7D858E, 0xFF5B626A},
                new float[]{0f, 0.4f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, fill);
        fill.setShader(null);
        fill.setColor(0x33283038);
        c.drawCircle(x, y, r * 0.42f, fill);
        ring.setColor(0x80FFFFFF);
        ring.setStrokeWidth(Math.max(1f, r * 0.04f));
        c.drawCircle(x, y, r * 0.42f, ring);
    }

    /**
     * One arrow key of the d-pad (no circle behind it): dark glass key, glowing
     * coloured outline and a white arrow. dir: 0 up, 1 right, 2 down, 3 left.
     */
    public void drawDpadKey(Canvas c, android.graphics.RectF r, boolean pressed, int dir) {
        int acc = accent();
        float m = Math.min(r.width(), r.height());
        float rad = m * 0.22f;
        ring.setColor(acc);
        ring.setStrokeWidth(m * 0.18f);
        ring.setAlpha(pressed ? 70 : 35);
        c.drawRoundRect(r, rad, rad, ring);
        ring.setStrokeWidth(m * 0.09f);
        ring.setAlpha(pressed ? 130 : 75);
        c.drawRoundRect(r, rad, rad, ring);
        int in = sDefense ? FILL_IN_DEF : FILL_IN_ATK;
        fill.setColor(pressed ? mix(in, acc, 0.35f) : in);
        c.drawRoundRect(r, rad, rad, fill);
        ring.setStrokeWidth(Math.max(2f, m * 0.04f));
        ring.setAlpha(pressed ? 255 : 225);
        c.drawRoundRect(r, rad, rad, ring);
        fill.setColor(0xFFFFFFFF);
        drawArrow(c, fill, r.centerX(), r.centerY(), m * 0.2f, dir);
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
