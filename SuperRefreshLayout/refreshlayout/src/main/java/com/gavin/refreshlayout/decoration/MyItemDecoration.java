package com.gavin.refreshlayout.decoration;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gavin.refreshlayout.refreshwidget.SuperRefreshLayout;


/**
 * Created by wangfei on 16/6/3.
 */
public class MyItemDecoration extends SuperRefreshLayout.RefreshItemDecoration {

    private Drawable mDivider;

    private Paint mPaint;

    private Paint mMaskPaint;

    public MyItemDecoration(SuperRefreshLayout refreshLayout, Context context){
        super(refreshLayout);
        TypedArray ta = context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});
        mDivider = ta.getDrawable(0);
        ta.recycle();
        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GRAY);
        mMaskPaint = new Paint();
        mMaskPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setColor(Color.GREEN);
        mMaskPaint.setAlpha(100);

    }



    @Override
    protected void onDrawChild(View  child, Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() -parent.getPaddingRight();
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        int top = child.getBottom() + params.bottomMargin;
        int bottom = top + 50;
        c.drawRect(left,top,right,bottom, mPaint);
    }

    /**
     * 过度绘制方法(可以绘制 超过 outRect区域的绘制方法),此方法是运行在  childview的  onDraw()方法之后的,所以会覆盖在childview的上面(他绘制的内容会显示在RecyclerView的最上层的)
     * @param c
     * @param parent
     * @param state
     */
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        //比如实现:在RecyclerView的最顶部添加一个萌版
//        c.drawRect(parent.getPaddingLeft(), parent.getPaddingTop(), parent.getWidth() - parent.getPaddingRight(),
//                150,mMaskPaint);



    }

    @Override
    public void getItemOffsetsInRealPosition(int position, Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsetsInRealPosition(position, outRect, child, parent, state);
        outRect.set(0,0,0, 50);
    }

}
