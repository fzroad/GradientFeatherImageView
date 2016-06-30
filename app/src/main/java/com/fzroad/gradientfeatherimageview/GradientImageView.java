package com.fzroad.gradientfeatherimageview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by fzroad on 2016/6/29 13:25.
 * Email:496349136@qq.com
 * 原理:
 * 1.绘制原图
 * 2.绘制指定形状的渐变蒙版层
 *      //注：因为RadialGradient渐变只能在正圆范围内渐变完全（不规则图形边缘没有完全渐变），某些边界部分会出现明显的分割线，所以要进行缩放处理
 *      a.使用不规则图形的最小半径绘制正方形的RadialGradient渐变图层（此例指定渐变中心为左上角0,0）
 *      b.将正方形的渐变图层Bitmap进行缩放成不规则图形的外接长方形（使用“渐变中心”作为“缩放中心”）
 *      c.将缩放后的长方形作为蒙版层
 *
 * 3.合并原图和蒙版 采用DST_OUT
 */
public class GradientImageView extends ImageView {
    private Bitmap bmpImage = null;
    private Bitmap bmpTrans = null;
    private Canvas imageCanvas = null;
    private Canvas transCanvas = null;
    private PaintFlagsDrawFilter paintFlagsDrawFilter = null;

    private float curPointerX = -1;
    private float curPointerY = -1;
    Paint transPaint, addPaint, normalPaint;
    Path path;
    RadialGradient rg;
    Bitmap bmpTemp;
    Canvas tempCanvas;

    public GradientImageView(Context context) {
        super(context);
    }

    public GradientImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GradientImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bmpImage == null) {
            //初始化内容
            this.setLayerType(LAYER_TYPE_SOFTWARE, null);
            bmpImage = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            bmpTrans = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            imageCanvas = new Canvas(bmpImage);
            transCanvas = new Canvas(bmpTrans);
            paintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
            imageCanvas.setDrawFilter(paintFlagsDrawFilter);
            transCanvas.setDrawFilter(paintFlagsDrawFilter);

            transPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            transPaint.setFilterBitmap(true);
            transPaint.setColor(Color.RED);
            transPaint.setStyle(Paint.Style.FILL);

            addPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            addPaint.setFilterBitmap(true);
            addPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

            normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            normalPaint.setFilterBitmap(true);

            path = new Path();
        }

        imageCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//清空画布
        transCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        super.onDraw(imageCanvas);//将原生ImageView的内容绘制到bmpImage
        drawTrans(transCanvas, curPointerX, curPointerY);//绘制蒙版层

        //合并
        imageCanvas.drawBitmap(bmpTrans, 0, 0, addPaint);
        //绘制合并后的图像
        canvas.drawBitmap(bmpImage, 0, 0, normalPaint);
    }

    private void drawTrans(Canvas canvas, float x, float y) {
        if (x <= 0 || y <= 0) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        } else {
            //创建左上角正方形蒙版临时canvas
            int size = (int) Math.min(x, y);
            bmpTemp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            tempCanvas = new Canvas(bmpTemp);

            //先绘制到正方形临时canvas中
            rg = new RadialGradient(0, 0, size, Color.argb(255, 255, 0, 0), Color.argb(0, 255, 0, 0), Shader.TileMode.CLAMP);
            transPaint.setShader(rg);
            RectF rectF = new RectF(-x, -y, x, y);
            path.reset();
            path.moveTo(0, 0);
            path.addArc(rectF, 0, 90);
            path.lineTo(0, 0);
            tempCanvas.drawPath(path, transPaint);

            //缩放为长方形图像
            Matrix matrix = new Matrix();
            matrix.postScale(x / size, y / size, 0, 0);
            Bitmap bmp = Bitmap.createBitmap(bmpTemp, 0, 0, size, size, matrix, true);

            //绘制到蒙版canvas中
            canvas.drawBitmap(bmp, 0, 0, transPaint);
            //释放临时资源
            bmp.recycle();
            bmpTemp.recycle();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                curPointerX = event.getX();
                curPointerY = event.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                curPointerX = event.getX();
                curPointerY = event.getY();
                invalidate();
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                curPointerX = -1;
                curPointerY = -1;
                invalidate();
                break;
            }
        }

        return true;
    }
}
