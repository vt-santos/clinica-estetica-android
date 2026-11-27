package br.com.clinicaestetica.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;

import br.com.clinicaestetica.R;

/** Campo de assinatura. Depois que a caneta é detectada, ignora o dedo e a palma da mão. */
public class SignatureView extends View {
    private static final float STROKE_DP = 2.2f;
    private static final float BASELINE = 0.75f;

    private final Path strokes = new Path();
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baseline = new Paint();

    private boolean stylusSeen;
    private boolean drawing;
    private boolean empty = true;
    private float lastX;
    private float lastY;

    public SignatureView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);

        ink.setColor(ContextCompat.getColor(context, R.color.ink));
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setStrokeWidth(Ui.dp(context, 1) * STROKE_DP);

        baseline.setColor(ContextCompat.getColor(context, R.color.sage));
        baseline.setStrokeWidth(Ui.dp(context, 1));
        describe();
    }

    /** O TalkBack informa se a área de assinatura já foi preenchida. */
    private void describe() {
        setContentDescription(empty ? "Área de assinatura, vazia. Assine com a caneta."
                : "Área de assinatura, assinada.");
    }

    public boolean isEmpty() {
        return empty;
    }

    public void clear() {
        strokes.reset();
        empty = true;
        describe();
        invalidate();
    }

    public byte[] toPng() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(strokes, ink);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        bitmap.recycle();
        return out.toByteArray();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float y = getHeight() * BASELINE;
        canvas.drawLine(0, y, getWidth(), y, baseline);
        canvas.drawPath(strokes, ink);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        stylusSeen |= isStylus(event);
        return super.onHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stylusSeen |= isStylus(event);
                if (stylusSeen && !isStylus(event)) {
                    return false;
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                drawing = true;
                lastX = event.getX();
                lastY = event.getY();
                strokes.moveTo(lastX, lastY);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!drawing) {
                    return false;
                }
                for (int i = 0; i < event.getHistorySize(); i++) {
                    addPoint(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                addPoint(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                if (!drawing) {
                    return false;
                }
                strokes.lineTo(event.getX(), event.getY() + 0.1f);
                empty = false;
                drawing = false;
                describe();
                break;
            case MotionEvent.ACTION_CANCEL:
                drawing = false;
                break;
            default:
                return drawing;
        }
        invalidate();
        return true;
    }

    private void addPoint(float x, float y) {
        strokes.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
        lastX = x;
        lastY = y;
    }

    private static boolean isStylus(MotionEvent event) {
        int tool = event.getToolType(0);
        return tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER;
    }
}
