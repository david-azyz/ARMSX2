package kr.co.iefriends.pcsx2.input.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

public class PSButtonView extends View {
    
    private Paint basePaint;
    private Paint pressedPaint;
    private Paint strokePaint;
    private Paint symbolPaint;
    
    private RectF bounds;
    private boolean isPressed = false;
    private String symbol = "";
    private int symbolColor = 0xFFFFFFFF; 
    private boolean isRectangular = false;
    private Drawable iconDrawable;
    
    public interface OnPSButtonListener {
        void onButtonPressed(boolean pressed);
    }
    
    private OnPSButtonListener listener;

    // Futebol Mobile look (null = original look)
    private FcButtonPainter fcPainter;
    private int fcSymbol;
    private String fcLabel;
    private String fcLabelDefense;

    /** Name shown while defending (null = same as the attack name). */
    public void setFcDefenseLabel(String label) {
        fcLabelDefense = label;
        invalidate();
    }

    // PathZz2 compact controls: what the button shows while defending, and swipe hints.
    private int fcSymbolDefense = -1;
    private String fcSymbolTextDefense;
    private String fcHint;
    private String fcHintDefense;

    public void setFcDefenseSymbol(int symbolType, String symbolText) {
        fcSymbolDefense = symbolType;
        fcSymbolTextDefense = symbolText;
        invalidate();
    }

    public void setFcHints(String attackHint, String defenseHint) {
        fcHint = attackHint;
        fcHintDefense = defenseHint;
        invalidate();
    }

    /**
     * Tap/hold = primary action, quick drag of the finger = second action (like eFootball).
     * When set, it replaces the normal press listener.
     */
    public interface FcGesture {
        void onPrimary(boolean pressed);
        void onSwipe(boolean pressed);
        boolean swipeEnabled();
    }

    private FcGesture fcGesture;
    private int fcGestureState = 0; // 0 idle, 1 undecided, 2 primary, 3 swipe
    private float fcDownX, fcDownY;
    private final Runnable fcHoldDecide = () -> {
        if (fcGestureState == 1 && fcGesture != null) {
            fcGestureState = 2;
            fcGesture.onPrimary(true);
        }
    };

    public void setFcGesture(FcGesture gesture) {
        if (gesture == fcGesture) return;
        removeCallbacks(fcHoldDecide);
        fcGestureState = 0;
        fcGesture = gesture;
    }

    private boolean fcGestureTouch(MotionEvent event) {
        final FcGesture g = fcGesture;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float dx = event.getX() - getWidth() / 2f;
                float dy = event.getY() - getHeight() / 2f;
                float radius = Math.min(getWidth(), getHeight()) / 2f;
                if (dx * dx + dy * dy > radius * radius) return false;
                isPressed = true;
                invalidate();
                fcDownX = event.getX();
                fcDownY = event.getY();
                if (g.swipeEnabled()) {
                    fcGestureState = 1;
                    postDelayed(fcHoldDecide, 110);
                } else {
                    fcGestureState = 2;
                    g.onPrimary(true);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                if (fcGestureState == 1) {
                    float mx = event.getX() - fcDownX;
                    float my = event.getY() - fcDownY;
                    float limit = Math.min(getWidth(), getHeight()) * 0.22f;
                    if (mx * mx + my * my > limit * limit) {
                        removeCallbacks(fcHoldDecide);
                        fcGestureState = 3;
                        g.onSwipe(true);
                    }
                }
                return fcGestureState != 0;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                removeCallbacks(fcHoldDecide);
                int state = fcGestureState;
                fcGestureState = 0;
                if (state == 0) return false;
                isPressed = false;
                invalidate();
                if (state == 1) {
                    // Quick tap: press now and let go a moment later so the game sees it.
                    g.onPrimary(true);
                    postDelayed(() -> g.onPrimary(false), 70);
                } else if (state == 2) {
                    g.onPrimary(false);
                } else {
                    postDelayed(() -> g.onSwipe(false), 70);
                }
                return true;
            }
        }
        return fcGestureState != 0;
    }

    public void setFcSymbol(int symbolType) {
        fcSymbol = symbolType;
        invalidate();
    }

    public void setFcStyle(int symbolType, String label) {
        fcPainter = new FcButtonPainter();
        fcSymbol = symbolType;
        fcLabel = label;
        invalidate();
    }
    
    public PSButtonView(Context context) {
        super(context);
        init();
    }
    
    public PSButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PSButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setColor(0x60FFFFFF);
        basePaint.setStyle(Paint.Style.FILL);
        
        pressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pressedPaint.setColor(0xFFFFFFFF);
        pressedPaint.setStyle(Paint.Style.FILL);
        
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(0x80000000);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2.0f);
        
        symbolPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        symbolPaint.setTextAlign(Paint.Align.CENTER);
        symbolPaint.setStyle(Paint.Style.FILL);
        
        bounds = new RectF();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        float padding = Math.min(w, h) * 0.1f;
        bounds.set(padding, padding, w - padding, h - padding);
        
        symbolPaint.setTextSize(Math.min(w, h) * 0.4f);
        updateIconBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (fcPainter != null) {
            boolean def = FcButtonPainter.isDefense();
            int sym = def && fcSymbolDefense >= 0 ? fcSymbolDefense : fcSymbol;
            String symText = def ? fcSymbolTextDefense : null;
            String label = def && fcLabelDefense != null ? fcLabelDefense : fcLabel;
            String hint = def ? fcHintDefense : fcHint;
            fcPainter.draw(canvas, getWidth(), getHeight(), isPressed, sym, symText, label, hint);
            return;
        }

        float centerX = bounds.centerX();
        float centerY = bounds.centerY();

        if (iconDrawable != null) {
            iconDrawable.setBounds(
                    Math.round(bounds.left),
                    Math.round(bounds.top),
                    Math.round(bounds.right),
                    Math.round(bounds.bottom)
            );
            iconDrawable.setState(isPressed ? new int[]{android.R.attr.state_pressed} : new int[]{});
            iconDrawable.setAlpha(isPressed ? 200 : 255);
            iconDrawable.draw(canvas);
            return;
        }

        Paint fillPaint = isPressed ? pressedPaint : basePaint;
        
        if (isRectangular) {
            float cornerRadius = Math.min(bounds.width(), bounds.height()) * 0.1f;
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, fillPaint);
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, strokePaint);
        } else {
            float radius = Math.min(bounds.width(), bounds.height()) / 2f;
            canvas.drawCircle(centerX, centerY, radius, fillPaint);
            canvas.drawCircle(centerX, centerY, radius, strokePaint);
        }
        
        if (!symbol.isEmpty()) {
            symbolPaint.setColor(symbolColor);
            
            if ("▢".equals(symbol)) {
                drawSquareSymbol(canvas, centerX, centerY, Math.min(bounds.width(), bounds.height()) / 2f * 0.4f);
            } else if ("△".equals(symbol)) {
                drawTriangleSymbol(canvas, centerX, centerY, Math.min(bounds.width(), bounds.height()) / 2f * 0.4f);
            } else if ("⨉".equals(symbol)) {
                drawXSymbol(canvas, centerX, centerY, Math.min(bounds.width(), bounds.height()) / 2f * 0.4f);
            } else if ("○".equals(symbol)) {
                drawCircleSymbol(canvas, centerX, centerY, Math.min(bounds.width(), bounds.height()) / 2f * 0.4f);
            } else if ("▷".equals(symbol)) {
                drawRightTriangleSymbol(canvas, centerX, centerY, Math.min(bounds.width(), bounds.height()) / 2f * 0.4f);
            } else {
                float textY = centerY + symbolPaint.getTextSize() / 3;
                canvas.drawText(symbol, centerX, textY, symbolPaint);
            }
        }
    }
    
    private void drawSquareSymbol(Canvas canvas, float centerX, float centerY, float size) {
        Paint squarePaint = new Paint(symbolPaint);
        squarePaint.setStyle(Paint.Style.STROKE);
        squarePaint.setStrokeWidth(size * 0.15f);
        
        RectF square = new RectF(
            centerX - size, centerY - size,
            centerX + size, centerY + size
        );
        canvas.drawRect(square, squarePaint);
    }
    
    private void drawTriangleSymbol(Canvas canvas, float centerX, float centerY, float size) {
        Paint trianglePaint = new Paint(symbolPaint);
        trianglePaint.setStyle(Paint.Style.STROKE);
        trianglePaint.setStrokeWidth(size * 0.15f);
        trianglePaint.setStrokeJoin(Paint.Join.ROUND);
        
        Path triangle = new Path();
        triangle.moveTo(centerX, centerY - size);
        triangle.lineTo(centerX - size * 0.866f, centerY + size * 0.5f);
        triangle.lineTo(centerX + size * 0.866f, centerY + size * 0.5f);
        triangle.close();
        
        canvas.drawPath(triangle, trianglePaint);
    }
    
    private void drawXSymbol(Canvas canvas, float centerX, float centerY, float size) {
        Paint xPaint = new Paint(symbolPaint);
        xPaint.setStyle(Paint.Style.STROKE);
        xPaint.setStrokeWidth(size * 0.15f);
        xPaint.setStrokeCap(Paint.Cap.ROUND);
        
        canvas.drawLine(centerX - size, centerY - size, centerX + size, centerY + size, xPaint);
        canvas.drawLine(centerX + size, centerY - size, centerX - size, centerY + size, xPaint);
    }
    
    private void drawCircleSymbol(Canvas canvas, float centerX, float centerY, float size) {
        Paint circlePaint = new Paint(symbolPaint);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(size * 0.15f);
        
        canvas.drawCircle(centerX, centerY, size, circlePaint);
    }
    
    private void drawRightTriangleSymbol(Canvas canvas, float centerX, float centerY, float size) {
        Paint trianglePaint = new Paint(symbolPaint);
        trianglePaint.setStyle(Paint.Style.FILL);
        
        Path triangle = new Path();
        triangle.moveTo(centerX - size * 0.4f, centerY - size * 0.6f);
        triangle.lineTo(centerX + size * 0.6f, centerY);
        triangle.lineTo(centerX - size * 0.4f, centerY + size * 0.6f);
        triangle.close();
        
        canvas.drawPath(triangle, trianglePaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (fcGesture != null) {
            return fcGestureTouch(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                boolean hitTest;
                if (isRectangular) {
                    hitTest = bounds.contains(event.getX(), event.getY());
                } else {
                    float dx = event.getX() - bounds.centerX();
                    float dy = event.getY() - bounds.centerY();
                    float radius = Math.min(bounds.width(), bounds.height()) / 2f;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    hitTest = distance <= radius;
                }
                
                if (hitTest) {
                    isPressed = true;
                    if (listener != null) {
                        listener.onButtonPressed(true);
                    }
                    invalidate();
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isPressed) {
                    isPressed = false;
                    if (listener != null) {
                        listener.onButtonPressed(false);
                    }
                    invalidate();
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    public void setSymbol(String symbol) {
        this.iconDrawable = null;
        this.symbol = symbol;
        invalidate();
    }

    public void setSymbolColor(int color) {
        this.symbolColor = color;
        invalidate();
    }

    public void setIconResource(@DrawableRes int resId) {
        if (resId == 0) {
            iconDrawable = null;
        } else {
            iconDrawable = AppCompatResources.getDrawable(getContext(), resId);
            if (iconDrawable != null) {
                iconDrawable = iconDrawable.mutate();
                updateIconBounds();
            }
        }
        invalidate();
    }

    public void setIconDrawable(@Nullable Drawable drawable) {
        if (drawable == null) {
            iconDrawable = null;
        } else {
            iconDrawable = drawable.mutate();
            updateIconBounds();
        }
        invalidate();
    }

    public void setOnPSButtonListener(OnPSButtonListener listener) {
        this.listener = listener;
    }

    public void setRectangular(boolean rectangular) {
        this.isRectangular = rectangular;
        invalidate();
    }

    private void updateIconBounds() {
        if (iconDrawable == null) {
            return;
        }
        iconDrawable.setBounds(
                Math.round(bounds.left),
                Math.round(bounds.top),
                Math.round(bounds.right),
                Math.round(bounds.bottom)
        );
    }
}
