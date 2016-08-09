package com.gavin.superrefreshlayout.refreshwidget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class SuperRefreshLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {
    // Maps to ProgressBar.Large style
    public static final int LARGE = MaterialProgressDrawable.LARGE;
    // Maps to ProgressBar default style
    public static final int DEFAULT = MaterialProgressDrawable.DEFAULT;

    private static final String LOG_TAG = SuperRefreshLayout.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .32f;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;



    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.enabled
    };

    private CircleImageView mCircleView;
    private int mCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffsetTop;

    private MaterialProgressDrawable mProgress;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mAlphaStartAnimation;

    private Animation mAlphaMaxAnimation;

    private Animation mScaleDownToStartAnimation;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mCircleWidth;

    private int mCircleHeight;

    // Whether the client has set a custom starting position;(用于判断,是否设置了header的初始位置,也就是是否调用了setProgressViewOffset(),调用了返回true)
    private boolean mUsingCustomStart;


    //add by me
    private int mTargetViewFrom = 0;
    /**
     * targetview的初始化的 offset(用于返回到原地的时候,作为重点,为什么含有 offset,因为target上面可能含有header,还有可能 layout含有padding)
     */
    private int mTargetViewOriginOffset= 0;

    //add by me (增加 footer view ,用于加载更多)
    private SwipeRefreshFooter mFooterView;
    private boolean mIsLoading = false;
    private OnLoadMoreListener mLoadMoreListener;
    private  int mCurrentBottomOffsetTop;
    private  int mOriginBottomOffsetTop;


    //add by me (增加HeaderView)
    private SwipeRefreshHeader mHeaderView;
    private int mCurrentHeaderOffsetTop;
    private int mOriginHeaderOffsetTop;


    private boolean mIsShowHeaderView = false;

    private static boolean mIsAddTailerToList = true;

    private boolean mIsRefreshEnable = true;
    private static boolean mIsLoadmoreEnable = true;

    private static final float MY_DRAG_RATE = .7f;

    /**
     * 因为 要将onLayout()在 执行下拉刷新和上拉加载更多的 滑动过程中禁止掉,防止有 view的操作会造成重新布局,造成控件位置的错乱(因为重新布局就会将view还原到初始位置)
     *   但是上面会出现问题:因为adapter.notifyDataChange()会重新布局,是为了 将新加入的item 加入到布局中(如果禁止掉, 带有尾巴的加载更多,加载完成 不会将item添加进来),
     *   这个时候就要放行 onLayout()
     */
    private boolean mIsNeedReLayout = false;

    private View mHeaderContainer;


    /**
     * 用来判定当前的view是否加载完毕了 (如果加载完成了,就不能执行上拉加载更多的操作)
     */
    private boolean mIsLoadCompleted = true;


    private void setLoadCompleted(boolean isLoadCompleted){
        this.mIsLoadCompleted = isLoadCompleted;

        //隐藏和显示  footview 的尾巴
        if(mIsLoadCompleted && mIsAddTailerToList){
            mFooterView.setVisibility(GONE);
        }else if(mIsAddTailerToList && !mIsLoadCompleted) {
            mFooterView.setVisibility(VISIBLE);
        }

    }

    /**
     * 获取当前数据是否加载完毕(也就是没有更多数据了)
     * @return
     */
    public boolean isLoadCompleted(){
        return mIsLoadCompleted;
    }

    public View getHeaderContainerView(){
        return mHeaderContainer;
    }


    public void setRefreshEnable(boolean isRefreshEnable){
        mIsRefreshEnable = isRefreshEnable;
    }

    public void setLoadmoreEnable(boolean isLoadmoreEnable){
        mIsLoadmoreEnable = isLoadmoreEnable;
    }

    /**
     * 设置refresh的类型(默认是SwipeRefreshLayout 默认的refresh方式)
     * @param isShowHeader 如果为true,就会使用添加头部的方式刷新
     */
    public void setRefreshTypeIsShowHeader(boolean isShowHeader){
        mIsShowHeaderView = isShowHeader;
    }

    /**
     * 设置laodmore方式,默认是 直接在list添加尾巴(值适用与RecyclerView),到达底部直接刷新
     * @param isAddTailer 如果设置为false ,那么就和 header 刷新那样的样式,来实现loadmore
     */
    public void setLoadmoreTypeIsAddTailer(boolean isAddTailer){
        mIsAddTailerToList = isAddTailer;
        if(mIsAddTailerToList){
            this.removeView(mFooterView);
        }else {
            if(mTarget instanceof  ListView){
                ((ListView)mTarget).removeFooterView(mFooterView);
            }
            if(mFooterView.getParent() == null){
                this.addView(mFooterView);
            }
        }
    }



    /**
     * 公开给外面自定义FooterView的方法
     * @param footerView
     */
    public void setFooterView(SwipeRefreshFooter footerView){
        if(!mIsAddTailerToList) {
            removeView(mFooterView);
            this.mFooterView = footerView;
            addView(mFooterView);
        }else {
            if(mFooterView.getParent() != null){
                throw new IllegalStateException("the footer view can't have a parent before add");
            }
            mFooterView = footerView;
        }
    }

    public SwipeRefreshFooter getFooterView(){
        return mFooterView;
    }

    public View getTargetView(){
        ensureTarget();
        return mTarget;
    }


    private boolean mIsRefreshBackground = false;

    private static final int MESSAGE_REFRESH =1;

    private int mRefreshCurrentHeight =0;


    /**
     * 含有动画的刷新
     */
    public void refreshNormalWithAnimation(){
        mRefreshCurrentHeight =0;
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                if(mIsShowHeaderView) {
                    float offset = mOriginHeaderOffsetTop + (0 - mOriginHeaderOffsetTop) * interpolatedTime - mHeaderView.getTop();
                    setTargetViewOffsetTopOrBottom((int) offset);
                    setHeaderOffsetTopAndBottom((int) offset);
                }else {
                    moveSpinner(mRefreshCurrentHeight += mTotalDragDistance * interpolatedTime * 1.4f);
                }
            }
        };
        animation.setDuration(200);
        animation.setStartOffset(50);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(mIsShowHeaderView){
                    mHeaderView.update(1, UpdateRefreshStatus.STATUS_HEADER_REFRESHING);
                    refreshInBackground();
                }else {
                    finishSpinner(mRefreshCurrentHeight);
                }
                mTarget.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mTarget.clearAnimation();
        mTarget.startAnimation(animation);
    }

    /**
     * 在后台刷新
     */
    public void refreshInBackground(){
        mIsRefreshBackground = true;
        setRefreshing(true, true /* notify */);
        mIsRefreshBackground = false;
    }


    public void addHeaderTopView(View view){
        if(view != null){
            if(view.getParent() != null){
                throw new IllegalStateException("the header top view can't have a parent before add");
            }
            mHeaderContainer = view;
            if(mHeaderContainer.getLayoutParams() == null){
                mHeaderContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
            if(mTarget instanceof ListView){
                if(mHeaderContainer.getLayoutParams() == null) {
                    mHeaderContainer.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                }else {
                    mHeaderContainer.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, mHeaderContainer.getLayoutParams().height));
                }
//                if(((ListView)mTarget).getAdapter() instanceof HeaderViewListAdapter){//在调用addHeaderView()之后调用了setAdapter(),内部会创建一个新的
//                                                                                      //HeaderViewListAdapter替换原来adapter
//                    ((ListView)mTarget).addHeaderView(mHeaderContainer);
//                }
                ((ListView)mTarget).addHeaderView(mHeaderContainer);
            }
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener){
        mLoadMoreListener = listener;
    }



    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mProgress.setAlpha(MAX_ALPHA);
                mProgress.start();//开始执行动画
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
                mCurrentTargetOffsetTop = mCircleView.getTop();//更新  mCurrentTargetOffsetTop
                mCurrentHeaderOffsetTop = mHeaderView.getTop();
                mHeaderView.update(1, UpdateRefreshStatus.STATUS_HEADER_REFRESHING);//修改状态,执行动画
            }else if(mIsLoading){
                //可以让 FooterView 执行加载动画
                mFooterView.update(1, UpdateRefreshStatus.STATUS_FOOTER_LOADING);

                if(mNotifyLoading){
                    if(mLoadMoreListener != null){
                        mLoadMoreListener.onLoading();
                    }
                }
                mCurrentBottomOffsetTop = mFooterView.getTop();
            }else {
                reset();
            }

        }
    };

    private void reset() {
        mCircleView.clearAnimation();
        mProgress.stop();
        mCircleView.setVisibility(View.GONE);
        setColorViewAlpha(MAX_ALPHA);
        // Return the circle to its start position
        if (mScale) {
            setAnimationProgress(0 /* animation complete and view is hidden */);
        } else {
            setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop,
                    true /* requires update */);
        }
        mCurrentTargetOffsetTop = mCircleView.getTop();


        //add by me
        //重置 Footer的位置
        mFooterView.clearAnimation();
        setFooterOffsetTopAndBottom(mOriginBottomOffsetTop - mCurrentBottomOffsetTop, true);
//        mCurrentBottomOffsetTop = mFooterView.getTop();

        //重置 header 位置
        mHeaderView.clearAnimation();
        setHeaderOffsetTopAndBottom(mOriginHeaderOffsetTop - mCurrentHeaderOffsetTop );
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    private void setColorViewAlpha(int targetAlpha) {
        mCircleView.getBackground().setAlpha(targetAlpha);
        mProgress.setAlpha(targetAlpha);
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *            progress spinner should appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mCircleView.setVisibility(View.GONE);
        mOriginalOffsetTop = mCurrentTargetOffsetTop = start;
        mSpinnerFinalOffset = end;
        mUsingCustomStart = true;
        mCircleView.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        mCircleView.invalidate();
    }

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == MaterialProgressDrawable.LARGE) {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mCircleView.setImageDrawable(null);
        mProgress.updateSizes(size);
        mCircleView.setImageDrawable(mProgress);
    }


    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public SuperRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public SuperRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);


        //add by me (增加自定义 Footer)
        createFooterView();


        //add by me (增加自定义的Header)
        createHeaderView();

    }

    private void createHeaderView() {
        mHeaderView = new DefaultSwipeRefreshHeader(getContext());
        addView(mHeaderView);
    }

    private void createFooterView() {
        mFooterView = new DefaultSwipeRefreshFooter(getContext());
        if(!mIsAddTailerToList) {
            addView(mFooterView);
        }
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        mCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER/2);
        mProgress = new MaterialProgressDrawable(getContext(), this);
        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mCircleView.setImageDrawable(mProgress);
        mCircleView.setVisibility(View.GONE);
        addView(mCircleView);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset + mOriginalOffsetTop);
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop,
                    true /* requires update */);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }


    public void setLoading(boolean loading){
        if(loading && mIsLoading != loading){

        }else {
            setLoadingMore(loading, false);
        }
    }


    private void startScaleUpAnimation(Animation.AnimationListener listener) {
        mCircleView.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mProgress.setAlpha(MAX_ALPHA);
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     * @param progress
     */
    private void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
            setColorViewAlpha((int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(mCircleView, progress);
            ViewCompat.setScaleY(mCircleView, progress);
        }
    }

    //参数2:是否触发 本地设置的 OnRefreshListener的 onRefresh()方法
    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if(mIsRefreshBackground){
                if(mRefreshing && notify){
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
                return;
            }
            if (mRefreshing) {
                int from = 0;
                if(!mIsShowHeaderView){
                    from = mCurrentTargetOffsetTop;
                }else {
                    from = mCurrentHeaderOffsetTop;
                }
                animateOffsetToCorrectPosition(from, mRefreshListener);
            } else {
                mIsNeedReLayout = true;
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }


    private boolean mNotifyLoading = false;

    /**
     * add by me (触发 loading more)
     * @param loading
     * @param notifyLoading
     */
     private void setLoadingMore(boolean loading, boolean notifyLoading){
        if(mIsLoading != loading){
            mNotifyLoading = notifyLoading;
            ensureTarget();
            mIsLoading = loading;
            if(mIsAddTailerToList){
                if(mNotifyLoading && mLoadMoreListener != null){
                    //如果加在失败,返回false,更改 footer状态,做逻辑处理
                    mLoadMoreListener.onLoading();
                }
                mIsNeedReLayout = true;
                return;
            }
            if(loading){
                animateFooterOffsetToCorrectPostion(mCurrentBottomOffsetTop, mRefreshListener);
            }else {
                mIsNeedReLayout = true;
                startHideFooterAnimation(mRefreshListener);
            }
        }
    }

    /**
     * add by me
     * 执行隐藏Footer的Animation
     * @param listener
     */
    private void startHideFooterAnimation(Animation.AnimationListener listener) {
        Animation hideFooterAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                hideFooter(interpolatedTime);
            }
        };
        hideFooterAnimation.setDuration(SCALE_DOWN_DURATION);
        if(listener != null){
            mFooterView.setAnimationListener(listener);
        }
        mFooterView.clearAnimation();
        mFrom = mFooterView.getTop();
        mTargetViewFrom = mTarget.getTop();
        mFooterView.startAnimation(hideFooterAnimation);
    }

    private void hideFooter(float interpolatedTime) {
        int targetTop;
        targetTop = mFrom + (int) ((mOriginBottomOffsetTop - mFrom) * interpolatedTime);
        int offset = targetTop - mFooterView.getTop();
        setFooterOffsetTopAndBottom(offset,false);

        //target view
        int targetOffset = (mTargetViewFrom + (int)((mTargetViewOriginOffset - mTargetViewFrom) * interpolatedTime)) - mTarget.getTop();
        setTargetViewOffsetTopOrBottom(targetOffset);

    }


    //执行让mCircleView隐藏的动画
    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        mTargetViewFrom = mTarget.getTop();
        mFrom = mHeaderView.getTop();
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                if(!mIsShowHeaderView) {
                    setAnimationProgress(1 - interpolatedTime);//设置mCircleView逐渐隐藏
                }else {
                    int offset = (mFrom + (int)((mOriginHeaderOffsetTop - mFrom) * interpolatedTime)) - mHeaderView.getTop();
                    setHeaderOffsetTopAndBottom(offset);
                    //add by me (隐藏target)
                    int targetOffset = (mTargetViewFrom + (int)((mTargetViewOriginOffset - mTargetViewFrom) * interpolatedTime)) - mTarget.getTop();
                    setTargetViewOffsetTopOrBottom(targetOffset);

                }

            }
        };
        mScaleDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mHeaderView.clearAnimation();// 一个大坑:执行动画结束之后, 如果有重新布局的操作, 没有执行这句代码(将动画清空), 会造成界面的闪烁一下
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        if(!mIsShowHeaderView) {
            mCircleView.setAnimationListener(listener);
            mCircleView.clearAnimation();
            mCircleView.startAnimation(mScaleDownAnimation);
        }else {
            mHeaderView.setAnimationListener(listener);
            mHeaderView.clearAnimation();
            mHeaderView.startAnimation(mScaleDownAnimation);
        }
    }

    private void startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation(mProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
    }

    private void startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation(mProgress.getAlpha(), MAX_ALPHA);
    }

    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mScale && isAlphaUsedForScale()) {
            return null;
        }
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                mProgress
                        .setAlpha((int) (startingAlpha+ ((endingAlpha - startingAlpha)
                                * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mCircleView.setAnimationListener(null);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(alpha);
        return alpha;
    }

    /**
     * @deprecated Use {@link #setProgressBackgroundColorSchemeResource(int)}
     */
    @Deprecated
    public void setProgressBackgroundColor(int colorRes) {
        setProgressBackgroundColorSchemeResource(colorRes);
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setProgressBackgroundColorSchemeResource(@ColorRes int colorRes) {
        setProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    public void setProgressBackgroundColorSchemeColor(@ColorInt int color) {
        mCircleView.setBackgroundColor(color);
        mProgress.setBackgroundColor(color);
    }

//    /**
//     * @deprecated Use {@link #setColorSchemeResources(int...)}
//     */
//    @Deprecated
//    public void setColorScheme(@ColorInt int... colors) {
//        setColorSchemeResources(colors);
//    }

    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    public void setColorSchemeResources(@ColorRes int... colorResIds) {
        final Resources res = getResources();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++) {
            colorRes[i] = res.getColor(colorResIds[i]);
        }
        setColorSchemeColors(colorRes);
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors
     */
    @ColorInt
    public void setColorSchemeColors(int... colors) {
        ensureTarget();
        mProgress.setColorSchemeColors(colors);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    /**
     * 返回的是第一个不为mCircleView的 View对象(这里要是扩展,需要重写,这里需要获取  列表控件)
     */
    private void ensureTarget() {
        /*
            定义规则:如果有RecyclerView 或者 ListView ,直接设置mTarget
                    如果没有,那么获取这个Layout下面的第一个用户定义的View
         */
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if(child instanceof AbsListView || child instanceof RecyclerView){
                    mTarget = child;
                    break;
                }
            }
        }
        if(mTarget == null){
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mCircleView) && !child.equals(mFooterView) && !child.equals(mHeaderView))  {
                    mTarget = child;
                    break;
                }
            }
            if(mTarget != null){
                setLoadmoreEnable(false);// 因为不是list列表,所以不管外面设置没有设置,都不支持加载更多的操作
            }
        }

    }

    /**
     * Set the distance to trigger a sync in dips(下拉到刷新的高度)
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    boolean isFirstLoad = true;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
//        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();
        //布局的时候,已经将CircleView隐藏了
        mCircleView.layout((width / 2 - circleWidth / 2), mCurrentTargetOffsetTop,
                (width / 2 + circleWidth / 2), mCurrentTargetOffsetTop + circleHeight);


        if(isFirstLoad) {
            mHeaderView.layout(0, 0, width, mHeaderView.getMeasuredHeight());
            mHeaderView.offsetTopAndBottom(-mHeaderView.getMeasuredHeight());
            child.layout(childLeft,childTop ,childLeft + childWidth,
                    childTop + childHeight);
        }else {
            mHeaderView.layout(mHeaderView.getLeft(), mHeaderView.getTop(), mHeaderView.getRight(), mHeaderView.getBottom());
            child.layout(child.getLeft(),child.getTop() ,child.getRight(), child.getBottom());
        }

        if(!mIsAddTailerToList) {
            if(isFirstLoad) {
                mFooterView.layout(0, 0, width, mFooterView.getMeasuredHeight());
                mFooterView.offsetTopAndBottom(height);
            }else {
                mFooterView.layout(mFooterView.getLeft(),mFooterView.getTop(), mFooterView.getRight(), mFooterView.getBottom());
            }
        }

        if(mEmptyView != null){
            mEmptyView.layout(0,0,width,height);
        }
        isFirstLoad= false;

    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));

        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index;
                break;
            }
        }

        //add by me
        measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);

        measureChild(mFooterView, widthMeasureSpec, heightMeasureSpec);//这里移动要measureChild()一下,这样getMeasureHeight()才能获取到值


        //计算offset,只有初始化的时候计算
        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mCircleView.getMeasuredHeight();
            mCurrentBottomOffsetTop = mOriginBottomOffsetTop = getMeasuredHeight();
            mTargetViewOriginOffset = getPaddingTop();
            mCurrentHeaderOffsetTop = mOriginHeaderOffsetTop = -mHeaderView.getMeasuredHeight();

        }

        if(mEmptyView != null){
            mEmptyView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        }
    }

    /**
     * Get the diameter of the progress circle that is displayed as part of the
     * swipe to refresh layout. This is not valid until a measure pass has
     * completed.
     *
     * @return Diameter in pixels of the progress circle view.
     */
    public int getProgressCircleDiameter() {
        return mCircleView != null ?mCircleView.getMeasuredHeight() : 0;
    }

    /**
     * 用于下拉刷新的判断: 判断list child 是否可以下拉操作
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }


    /**
     * add by me (用来判断是否是否滑倒底部,用于上拉加载更多)
     * @return
     */
    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                View lastChild = absListView.getChildAt(absListView.getChildCount() - 1);
                if (lastChild != null) {
                    return (absListView.getLastVisiblePosition() == (absListView.getCount() - 1))
                            && lastChild.getBottom() > absListView.getPaddingBottom();
                }
                else {
                    return false;
                }
            } else {
                return mTarget.getHeight() - mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || mIsLoading   //update by me (去掉  canChildScrollUp()) 去掉mNestedScrollInProgress
                || mRefreshing || mNestedScrollInProgress  //(因为只要执行到子view的MOVE事件,就会执行内嵌滑动(在支持的情况下))mNestedScrollInProgress(true: 正在进行交互滑动)
                 ) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        //通过上面的判断,在move事件上,就不会再传递给  子view(target)
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCircleView.getTop(), true);//将mCircleView 重置到原来的位置

                //add  by me (重置  Footer View)
                setFooterOffsetTopAndBottom(mOriginBottomOffsetTop - mFooterView.getTop(),true);
                //reset header view
                setHeaderOffsetTopAndBottom(mOriginHeaderOffsetTop - mHeaderView.getTop());

                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);//获取的是第一个手指
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mLastY = mInitialDownY = initialDownY;
                if(!(mTarget instanceof NestedScrollingChild) && !(mTarget instanceof ListView) && !(mTarget instanceof GridView)
                        && !(mTarget instanceof ScrollView)){
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);//获取手指y轴的坐标
                if (y == -1) {//没有知道mActivePointerId对应的收手指,所以就返回 -1
                    return false;
                }

                final float yDiff = y - mInitialDownY;

                if (Math.abs(yDiff) > mTouchSlop && !mIsBeingDragged) {//将事件截断掉,以后的move事件都被截断掉
                    if(yDiff > 0 && !canChildScrollUp()) { // 下拉
                        mInitialMotionY = mInitialDownY + mTouchSlop;
                        mLastY = y;
                        mIsBeingDragged = true;
                        mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
                    }else if(yDiff < 0 && !canChildScrollDown()){
                        mLastY = mInitialMotionY = mInitialDownY - mTouchSlop;
                        mIsBeingDragged = true;
                    }else {
                        mIsBeingDragged = false;
                    }

                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP://一个非主要的手指抬起了(也就是第二个手指,)
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }



    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    // NestedScrollingParent

    /**
     * 填坑:此方法会在RecyclerView的 onInterceptTouchEvent()和 onTouchEvent()的 down方法中被调用,而此方法会回调onAcceptNestedScroll()(此方法会将mNestedScrollInProgress置为true)
     *      因此在onInterceptTouchEvent()提前做判断如果mNestedScrollInProgress为true,直接返回false,那么onInterceptTouchEvent()针对move做的处理就接受不到了
     * @param child
     * @param target
     * @param nestedScrollAxes
     * @return
     */
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        //这里: 之前官方有canChildScrollUp()的判断:也就是在target处于最顶部的时候,此方法返回false,因为不支持内嵌滑动,那么下拉刷新的操作是 在当前view的onTouchEvent()处理
            //我去掉的理由: 不管什么情况以内嵌滑动为主执行(原因:界面滑动更加流畅,不会有停顿不能滑动的现象(如果你对touch事件做的多的情况下,你会明白这个恶心的地方,因为touch事件只能给
        //     一个view处理,如果一个view处理不掉了,就废弃了,不能给其他view处理,  内嵌滑动就是为了解决这个bug出现的)),如果target不支持内嵌滑动,这个时候就会在onTouchEvent()中
                //进行处理了(容错处理)
        return isEnabled()  && !mReturningToStart && !mRefreshing && !mIsLoading  // 在refresh 和 loading的时候不能支持内嵌滑动,不然会在执行loading状态的时候,RefreshLayout会滑动
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    /**
     * 注意 : 此方法传递的参数 dx,dy都是和我们手指移动的差值的dx,dy正好是相反的(相当于取反)
     * @param target
     * @param dx
     * @param dy
     * @param consumed 如果它的 x,y位置不为0,就告诉子View,parent消耗了scroll;具体数值是消耗的距离
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll

        int targetTop = mTarget.getTop();

        if (dy > 0 && (targetTop > mTargetViewOriginOffset || mTotalUnconsumed > 0)) {//dy>0 :上拉(和手指移动计算出来的相反)
            if(mIsShowHeaderView) {
                //bug : 这里的业务: 下拉刷新,拉下来之后,再往回拉,如果 dy 没有完全消耗掉,这时是应该给上面那个判断条件进行消耗(headerContainer移动)
                int originDy = dy;
                if (dy > targetTop - mTargetViewOriginOffset) {
                    dy = consumed[1] = targetTop - mTargetViewOriginOffset;//只有之前移动出来的部分的距离被父view消耗掉了,回到原来的位置,不动了
                } else {
                    consumed[1] = dy;//全部被当前layout 消耗掉了
                }
                float offset = dy * MY_DRAG_RATE;
                if(dy * MY_DRAG_RATE <1){// 防止强转int的时候,变成0
                    offset =1;
                }
                moveSpinner(-offset);
            }else {
                if (dy > mTotalUnconsumed) {
                    consumed[1] = dy - (int) mTotalUnconsumed;//当前的距离全部使用不了, 只消耗自己使用的那部分,剩下的向下传递,先给子Nested Scroll View
                    mTotalUnconsumed = 0;
                } else {
                    mTotalUnconsumed -= dy;
                    consumed[1] = dy;//全部当前layout消耗掉了

                }
                moveSpinner(mTotalUnconsumed * DRAG_RATE);
            }

        }else if(dy < 0 && targetTop < 0 && !mIsAddTailerToList){// targetTop < 0 : 因为只有在list移动到顶部才会上拉加载更多,这里就用0 作为边界标准

            if (dy < targetTop) {
                dy= consumed[1] = targetTop ;
            } else {
                consumed[1] = dy;//全部被父view消耗了
            }
            moveFooter(-dy * MY_DRAG_RATE);

        }
        // If a client layout is using a custom start position for the circle
        // view, they mean to hide it again before scrolling the child view
        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
        // the circle so it isn't exposed if its blocking content is moved
        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
                && Math.abs(dy - consumed[1]) > 0) {
            mCircleView.setVisibility(View.GONE);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }

    }


    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {

        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {//下拉刷新交互滑动, 停止时,处理的操作
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }

        if(mTotalUnconsumed < 0){//加载更多交互滑动,停止时,处理的操作
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed =0;
        }
        mOriginTotalUnConsumed = 0;
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    private float mOriginTotalUnConsumed = mTotalUnconsumed;


    /**
     * 此方法传递的dx,dy也是和手指移动的dx,dy相反的;;这里只需要关心 dyUnconsumed:子View 和 onNestedPreScroll()没有消耗的dy
     * @param target
     * @param dxConsumed
     * @param dyConsumed
     * @param dxUnconsumed
     * @param dyUnconsumed
     */
    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {

        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);//mParentOffsetInWindow 返回的是,执行完NestedScroll当前View位置offset的距离(最终位置-初始位置)

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];

        if (dy < 0 ) {
            mTotalUnconsumed += Math.abs(dy);
            if(mIsShowHeaderView) {
                moveSpinner(-dy * MY_DRAG_RATE);
            }else {
                moveSpinner(mTotalUnconsumed * DRAG_RATE);
            }
        }else if(dy > 0 && !mIsAddTailerToList && !mIsLoadCompleted){//增加上拉加载更多交互滑动
            //增加判断,如果当前view没有铺满屏幕,不支持下拉加载更多
            if(mTarget !=null && mTarget instanceof RecyclerView){
                RecyclerView list = (RecyclerView) mTarget;
                int itemCount = list.getLayoutManager().getItemCount();
                int visibileCount = list.getLayoutManager().getChildCount();
                if(itemCount <= visibileCount){
                    return;
                }
            }
            mTotalUnconsumed -= dy;
            moveFooter(-dy * MY_DRAG_RATE);
        }
    }



    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    /**
     * 此方法在onTouchEvent()中的 UP事件调用, 如果此方法返回true, 那么 RecyclerView 和 NestedScrollView 的 fling操作就执行不到了,就不会有fling操作
     *   这里处理的业务需求: 增加fing操作的支持(在headerContainer 和 RecyclerView之间的)(只针对上拉向下滚动的操作)
     * @param target
     * @param velocityX
     * @param velocityY
     * @return
     */
    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    /**
     * 此方法 是在 RecyclerView 的fling的操作之前调用,但是它的调用不会影响到RecyclerView的 fling操作,返回值更是没有任何处理操作
     *    只要 onNestedPreFling()返回false,此方法就会执行到
     * @param target
     * @param velocityX
     * @param velocityY
     * @param consumed
     * @return
     */
    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    /**
     * 下拉刷新滑动操作
     * @param overscrollTop mIsShowHeaderView == false : 是header移动的总距离;; mIsShowHeaderView == true : 是每次移动的 距离
     */
    private void moveSpinner(float overscrollTop) {
        if(!mIsRefreshEnable){
            return;
        }
        if(!mIsShowHeaderView) {
            mProgress.showArrow(true);
            float originalDragPercent = overscrollTop / mTotalDragDistance;

            float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
            float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
            float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
            float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset - mOriginalOffsetTop
                    : mSpinnerFinalOffset;
            float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                    / slingshotDist);
            float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                    (tensionSlingshotPercent / 4), 2)) * 2f;
            float extraMove = (slingshotDist) * tensionPercent * 2;

            int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
            // where 1.0f is a full circle
            if (mCircleView.getVisibility() != View.VISIBLE) {
                mCircleView.setVisibility(View.VISIBLE);
            }
            if (!mScale) {
                ViewCompat.setScaleX(mCircleView, 1f);
                ViewCompat.setScaleY(mCircleView, 1f);
            }

            if (mScale) {
                setAnimationProgress(Math.min(1f, overscrollTop / mTotalDragDistance));
            }
            if (overscrollTop < mTotalDragDistance) {//下拉的距离还没有达到 刷新的高度
                if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                        && !isAnimationRunning(mAlphaStartAnimation)) {
                    // Animate the alpha
                    startProgressAlphaStartAnimation();
                }
            } else {//下拉距离到达或超过了  刷新的高度
                if (mProgress.getAlpha() < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
                    // Animate the alpha
                    startProgressAlphaMaxAnimation();
                }
            }
            float strokeStart = adjustedPercent * .8f;
            mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
            mProgress.setArrowScale(Math.min(1f, adjustedPercent));

            float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
            mProgress.setProgressRotation(rotation);

            setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true /* requires update */);

        }else {
            if((mTarget.getTop() + overscrollTop) <mTargetViewOriginOffset){
                overscrollTop = mTargetViewOriginOffset-mTarget.getTop();
            }
            if((mCurrentHeaderOffsetTop + overscrollTop) < 0){//还没有达到刷新的高度
                mHeaderView.update(1 - Math.abs(mCurrentHeaderOffsetTop + overscrollTop)/ mHeaderView.getMeasuredHeight(), UpdateRefreshStatus.STATUS_HEADER_NOT_REFRESH);
            }else {//达到或者超过了 刷新的高度
                overscrollTop = overscrollTop * MY_DRAG_RATE;
                mHeaderView.update(1 + Math.abs(mCurrentHeaderOffsetTop + overscrollTop) / mHeaderView.getMeasuredHeight(), UpdateRefreshStatus.STATUS_HEADER_CAN_REFRESH) ;
            }
            setTargetViewOffsetTopOrBottom((int) overscrollTop);
            setHeaderOffsetTopAndBottom((int) overscrollTop);
//            setHeaderContainerOffsetTopAndBottom((int) overscrollTop);
        }
    }

    private void moveFooter(float offset) {

        if(!mIsLoadmoreEnable){
            return;
        }

        if(Math.abs(mTarget.getTop()) < mFooterView.getMeasuredHeight()){//还没有达到刷新的高度
            //状态的变化  可以执行动画效果
            startLoadMoreStartAnimation();//
            mFooterView.update(Math.abs(mTarget.getTop()) * 1.0f / mFooterView.getMeasuredHeight() , UpdateRefreshStatus.STATUS_FOOTER_NOT_LOAD);
        }else {//达到刷新的高度
            //状态变化,可以执行动画下锅
            offset = (int) (offset * MY_DRAG_RATE);//可以自己设置
            mFooterView.update((1 + Math.abs(offset)*1.0f / mFooterView.getMeasuredHeight()), UpdateRefreshStatus.STATUS_FOOTER_CAN_LOAD);
            startLoadMoreOverAnimation();
        }
        if((mTarget.getTop() + offset) > mTargetViewOriginOffset){//越界处理
            offset = mTargetViewOriginOffset - mTarget.getTop();
        }
        setTargetViewOffsetTopOrBottom((int) offset);
        setFooterOffsetTopAndBottom((int) offset, true);
    }


    /**
     * 正在loading more的 动画(一般也不用实现,因为具体的动画,可以在footerview中去实现)
     */
    private void startLoadMoreOverAnimation() {

    }

    /**
     * 这个一般不实现
     */
    private void startLoadMoreStartAnimation() {

    }


    private void finishSpinner(float overscrollTop) {
        if(overscrollTop > 0) {//下拉刷新
            if(!mIsShowHeaderView) {
                if (overscrollTop > mTotalDragDistance) {//执行刷新操作
                    setRefreshing(true, true /* notify */);
                } else {//不执行刷新操作,返回原地
                    // cancel refresh
                    mRefreshing = false;
                    mProgress.setStartEndTrim(0f, 0f);
                    Animation.AnimationListener listener = null;
                    if (!mScale) {
                        listener = new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                if (!mScale) {
                                    startScaleDownAnimation(null);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                        };
                    }
                    animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
                    mProgress.showArrow(false);
                }
            }else {
                if(Math.abs(mCurrentHeaderOffsetTop - mOriginHeaderOffsetTop) >= mHeaderView.getMeasuredHeight()){
                    setRefreshing(true,true);
                }else {
                    mRefreshing = false;
                    animateOffsetToStartPosition(mCurrentHeaderOffsetTop, null);
                }
            }
        }else {//上拉加载更多
            if(mIsAddTailerToList){
                return;
            }
            if(Math.abs(mCurrentBottomOffsetTop - mOriginBottomOffsetTop) > mFooterView.getMeasuredHeight()){
                setLoadingMore(true,true);
            }else {
                mIsLoading = false;
                mNotifyLoading = false;
                animationFooterOffsetToStartPostion(mCurrentBottomOffsetTop, null);
            }
        }
    }

    private float mLastY;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex = -1;

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart ||mRefreshing || mIsLoading || canChildScrollUp() && canChildScrollDown() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);//第一个手指的id
                mIsBeingDragged = false;
                if(!(mTarget instanceof NestedScrollingChild) && !(mTarget instanceof ListView) && !(mTarget instanceof GridView)
                        && !(mTarget instanceof ScrollView)){
                    mIsBeingDragged = true;
                    mLastY = mInitialMotionY = MotionEventCompat.getY(ev,mActivePointerId);
                }
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;

                if (mIsBeingDragged) {
                    if ((overscrollTop > 0 || mTarget.getTop() > 0)&& !canChildScrollUp()) {//overscrollTop > 0 确保了是下拉刷新的操作(在这个范围内移动)
                        if(mIsShowHeaderView) {
                            moveSpinner((y - mLastY)*MY_DRAG_RATE);//自定义的方式

                        }else {
                            moveSpinner(overscrollTop);//系统的方式
                        }
                    }else if((overscrollTop < 0 || mTarget.getTop() < 0) && !canChildScrollDown()){//确保了是上拉加载更多的操作(在这个范围内移动)
                        //增加两条判断,第一条,总数目是否大于可视数目   第二条:是否List不带loadmore尾巴
                        if(mIsAddTailerToList || mIsLoadCompleted){
                            return false;
                        }
                        if(mTarget instanceof RecyclerView){
                            RecyclerView.LayoutManager layoutManager = ((RecyclerView) mTarget).getLayoutManager();
                            int totalItemCount = layoutManager.getItemCount();
                            int visibalItemCount = layoutManager.getChildCount();
                            if(totalItemCount <= visibalItemCount){
                                return false;
                            }
                        }else  if(mTarget instanceof AbsListView){
                            int totalItemCount = ((AbsListView)mTarget).getAdapter().getCount();
                            int visiableItemCount = ((AbsListView)mTarget).getChildCount();
                            if(totalItemCount <= visiableItemCount){
                                return false;
                            }
                        }

                        moveFooter((y - mLastY)* MY_DRAG_RATE);
                    } else {
                        return false;
                    }
                    mLastY = y;
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;//下拉的距离
                mIsBeingDragged = false;
                finishSpinner(overscrollTop);//停止下拉的时候,需要判断是否刷新
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }


    /**
     * 移动到 刷新的位置
     * @param from
     * @param listener
     */
    private void animateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if(!mIsShowHeaderView) {
            if (listener != null) {
                mCircleView.setAnimationListener(listener);
            }
            mCircleView.clearAnimation();
            mCircleView.startAnimation(mAnimateToCorrectPosition);
        }else {
            if(listener != null){
                mHeaderView.setAnimationListener(listener);
            }
            mHeaderView.clearAnimation();
            mHeaderView.startAnimation(mAnimateToCorrectPosition);
        }
    }

    /**
     * add by me (返回到loading状态的 Animation)
     * @param from
     * @param listener
     */
    private void animateFooterOffsetToCorrectPostion(int from, Animation.AnimationListener listener){
        mFrom = from;
        mAnimateBottomToCorrectPosition.reset();
        mAnimateBottomToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateBottomToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if(listener != null){
            //设置动画的监听事件
            mFooterView.setAnimationListener(listener);
        }
        mFooterView.clearAnimation();
        mFooterView.startAnimation(mAnimateBottomToCorrectPosition);
    }


    /**
     * 下拉刷新, 返回到开始位置的Animation
     * @param from
     * @param listener
     */
    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        //add by me
        mTargetViewFrom = mTarget.getTop();
        if(!mIsShowHeaderView) {
            if (mScale) {
                // Scale the item back down
                startScaleDownReturnToStartAnimation(from, listener);
            } else {
                mFrom = from;
                mAnimateToStartPosition.reset();
                mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
                mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
                if (listener != null) {
                    mCircleView.setAnimationListener(listener);
                }
                mCircleView.clearAnimation();
                mCircleView.startAnimation(mAnimateToStartPosition);
            }
        }else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if(listener != null){
                mHeaderView.setAnimationListener(listener);
            }
            mHeaderView.clearAnimation();
            mHeaderView.startAnimation(mAnimateToStartPosition);
        }
    }

    /**
     * add by me (footer 返回到原地)
     *
     * @param from
     * @param listener
     */
    private void animationFooterOffsetToStartPostion(int from ,Animation.AnimationListener listener){
        mFrom = from;
        mTargetViewFrom = mTarget.getTop();
        mAnimationFooterToStartPostion.reset();
        mAnimationFooterToStartPostion.setDuration(ANIMATE_TO_START_DURATION);
        mAnimationFooterToStartPostion.setInterpolator(mDecelerateInterpolator);
        if(listener != null){
            //为 footer view 设置监听事件
            mFooterView.setAnimationListener(listener);
        }
        mFooterView.clearAnimation();
        mFooterView.startAnimation(mAnimationFooterToStartPostion);

    }

    /**
     * add by me(footer 返回到原地的Animation)
     */
    private Animation mAnimationFooterToStartPostion = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStartByMe(interpolatedTime, false);
        }
    };


    /**
     * add by me (返回到原地)
     * @param interpolatedTime
     */
    private void moveToStartByMe(float interpolatedTime, boolean isHeader) {
        if(isHeader){

        }else {
            int targetTop =0;
            targetTop = mFrom + (int)((mOriginBottomOffsetTop - mFrom) * interpolatedTime);
            int offset = targetTop - mFooterView.getTop();
            setFooterOffsetTopAndBottom(offset, false);
        }
        int targetOffset = (mTargetViewFrom + (int)((mTargetViewOriginOffset - mTargetViewFrom) * interpolatedTime)) - mTarget.getTop();
        setTargetViewOffsetTopOrBottom(targetOffset);
    }


    //下拉时,up事件之后,如果到达刷新的高度, 这个动画是  移动到正在刷新的位置
    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int offset;
            if(!mIsShowHeaderView) {
                int targetTop = 0;
                int endTarget = 0;
                if (!mUsingCustomStart) {//如果没有设置初始的位置offset,那么需要减去  mOriginalOffsetTop(header的高度)
                    endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
                } else {
                    endTarget = (int) mSpinnerFinalOffset;
                }
                targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
                offset = targetTop - mCircleView.getTop();

                setTargetOffsetTopAndBottom(offset, false /* requires update */);
                mProgress.setArrowScale(1 - interpolatedTime);
            }else {
                int endTop = mOriginHeaderOffsetTop + mHeaderView.getMeasuredHeight();
                int targetTop = mFrom + (int)((endTop - mFrom) * interpolatedTime);
                offset = targetTop - mCurrentHeaderOffsetTop;
                setHeaderOffsetTopAndBottom(offset);
                //add by me
                setTargetViewOffsetTopOrBottom(offset);

            }

        }
    };

    //add by me
    //仿照上面,可以写出一个 上拉时,正在loading的时候,移动到正在loading的位置
    private final Animation mAnimateBottomToCorrectPosition = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop,endTag;
            endTag = mOriginBottomOffsetTop - mFooterView.getMeasuredHeight();
            targetTop = mFrom + (int)((endTag - mFrom) * interpolatedTime);
            int offset = targetTop - mFooterView.getTop();
            setTargetViewOffsetTopOrBottom(offset);
            setFooterOffsetTopAndBottom(offset, false);
            //如果  FooterView需要设置  执行的 比例的时候,下面可以设置

        }
    };



    private void moveToStart(float interpolatedTime) {
        if(!mIsShowHeaderView) {
            int targetTop = 0;
            targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
            int offset = targetTop - mCircleView.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
        }else {
            int offset = mFrom + (int)((mOriginHeaderOffsetTop - mFrom) * interpolatedTime) - mHeaderView.getTop();
            setHeaderOffsetTopAndBottom(offset);
            //add by me(将target 返回到原来的位置)
            int targetOffset = (mTargetViewFrom + (int)((mTargetViewOriginOffset - mTargetViewFrom) * interpolatedTime)) - mTarget.getTop();
            setTargetViewOffsetTopOrBottom(targetOffset);

        }

    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        if (isAlphaUsedForScale()) {
            mStartingScale = mProgress.getAlpha();
        } else {
            mStartingScale = ViewCompat.getScaleX(mCircleView);
        }
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale  * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownToStartAnimation);
    }

    /**
     * add by me
     * 移动mTarget指定的offset
     * @param offset
     */
    private void setTargetViewOffsetTopOrBottom(int offset){
        //add by me
        if(mTarget != null){
//            mTarget.bringToFront();
//            mTarget.offsetTopAndBottom(offset);

//            LayoutParams params = (LayoutParams) mTarget.getLayoutParams();
//            params.y = params.y + offset;
//            mTarget.setLayoutParams(params);
//            mTarget.requestLayout();
//            postInvalidate();
//            mTarget.offsetTopAndBottom(offset);
//            ViewCompat.offsetTopAndBottom(mTarget, offset);
            mTarget.offsetTopAndBottom(offset);
        }
    }

    /**
     * 移动 刷新状态header 指定的offset
     * @param offset
     * @param requiresUpdate
     */
    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mCircleView.bringToFront();
        mCircleView.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mCircleView.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }


    /**
     * add by me 移动footer指定offset
     * @param offset
     * @param requireUpdate
     */
    private void setFooterOffsetTopAndBottom(int offset, boolean requireUpdate) {

        ViewCompat.offsetTopAndBottom(mFooterView, offset);
        mCurrentBottomOffsetTop = mFooterView.getTop();
        invalidate();

    }


    private void setHeaderOffsetTopAndBottom(int offset){
        ViewCompat.offsetTopAndBottom(mHeaderView, offset);//推荐用这个,不会造成上面的 不会重绘 的问题
        mCurrentHeaderOffsetTop = mHeaderView.getTop();
        invalidate();//这个不是知道为什么,但是上面的 offsetTopAndBottom()调用了invalidate(),但是不知道为什么没有执行
    }


    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }

    //add by me (load more 回调函数(callback))
    public interface OnLoadMoreListener{
        void onLoading();
    }


    /***********************************Refresh Header*****************************************/
    public static abstract class SwipeRefreshHeader extends LinearLayout implements UpdateRefreshStatus{

        private Animation.AnimationListener mListener;

        protected int mState = -1;

        public SwipeRefreshHeader(Context context) {
            this(context,null);
        }

        public SwipeRefreshHeader(Context context, AttributeSet attrs) {
            this(context, attrs,0);
        }

        public SwipeRefreshHeader(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            setOrientation(VERTICAL);
            setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(getView());
        }

        public abstract View getView() ;

        /**
         * 自定义Header的高度
         * @return
         */
        public abstract int getHeaderHeight();

        public void setAnimationListener(Animation.AnimationListener listener){
            mListener = listener;
        }

        @Override
        protected void onAnimationStart() {
            super.onAnimationStart();
            if(mListener != null){
                mListener.onAnimationStart(getAnimation());
            }
        }

        @Override
        protected void onAnimationEnd() {
            super.onAnimationEnd();
            if(mListener != null){
                mListener.onAnimationEnd(getAnimation());
            }
        }
    }





    /***********************************Refresh Footer*****************************************/
    public static abstract class SwipeRefreshFooter extends LinearLayout implements UpdateRefreshStatus{

        private Animation.AnimationListener mListener;

        public SwipeRefreshFooter(Context context) {
            this(context,null);
        }

        public SwipeRefreshFooter(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public SwipeRefreshFooter(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            setOrientation(VERTICAL);
            setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(getView());
        }

        public abstract View getView() ;

        public void setAnimationListener(Animation.AnimationListener listener){
            mListener = listener;
        }

        @Override
        protected void onAnimationStart() {
            super.onAnimationStart();
            if(mListener != null){
                mListener.onAnimationStart(getAnimation());
            }
        }

        @Override
        protected void onAnimationEnd() {
            super.onAnimationEnd();
            if(mListener != null){
                mListener.onAnimationEnd(getAnimation());
            }
        }
    }


    public class DefaultSwipeRefreshHeader extends SwipeRefreshHeader{

        private TextView tv ;

        public DefaultSwipeRefreshHeader(Context context) {
            this(context, null);
        }

        public DefaultSwipeRefreshHeader(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public DefaultSwipeRefreshHeader(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public int getHeaderHeight() {
            return (int)(60 * getContext().getResources().getDisplayMetrics().density);
        }

        @Override
        public View getView() {
            RelativeLayout container = new RelativeLayout(getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(60 * getContext().getResources().getDisplayMetrics().density)));
            tv = new TextView(getContext());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            tv.setLayoutParams(params);
            tv.setTextSize(25);
            tv.setTextColor(Color.GRAY);
            tv.setText("下拉刷新");
            container.addView(tv);
            return container;
        }

        @Override
        public void update(float ratio, int type) {
            float scaleRatio = 0.7f + 0.3f * ratio;
            ViewCompat.setScaleX(tv, scaleRatio);
            ViewCompat.setScaleY(tv, scaleRatio);
            ViewCompat.setPivotX(tv, tv.getMeasuredWidth() / 2);
            ViewCompat.setPivotY(tv, tv.getMeasuredHeight() / 2);
            if(type == mState){
                return;
            }
            switch (type){
                case UpdateRefreshStatus.STATUS_HEADER_NOT_REFRESH:
                    tv.setText("下拉刷新");  //会  重新布局(这是个大坑)
                    tv.setTextColor(Color.GRAY);
                    break;
                case UpdateRefreshStatus.STATUS_HEADER_CAN_REFRESH:
                    tv.setText("松开刷新");
                    tv.setTextColor(Color.RED);
                    break;
                case UpdateRefreshStatus.STATUS_HEADER_REFRESHING:
                    tv.setText("正在刷新");
                    tv.setTextColor(Color.BLUE);
                    break;
            }
            mState = type;
        }
    }


    public static class DefaultSwipeRefreshFooter extends SwipeRefreshFooter{

        private TextView tv ;

        public DefaultSwipeRefreshFooter(Context context) {
            super(context);
        }

        public DefaultSwipeRefreshFooter(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public DefaultSwipeRefreshFooter(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public View getView() {
            RelativeLayout container = new RelativeLayout(getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(60 * getContext().getResources().getDisplayMetrics().density)));
            tv = new TextView(getContext());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            tv.setLayoutParams(params);
            tv.setTextSize(25);
            tv.setText("加载更多");
            tv.setTextColor(Color.GRAY);
            container.addView(tv);
            return container;
        }

        @Override
        public void update(float ratio, int type) {
            float scaleRatio = 0.7f + 0.3f * ratio;
            ViewCompat.setScaleX(tv, scaleRatio);
            ViewCompat.setScaleY(tv, scaleRatio);
            switch (type){
                case UpdateRefreshStatus.STATUS_FOOTER_NOT_LOAD:
                    tv.setText("加载更多");
                    tv.setTextColor(Color.GRAY);
                    break;
                case UpdateRefreshStatus.STATUS_FOOTER_CAN_LOAD:
                    tv.setText("松开加载");
                    tv.setTextColor(Color.RED);
                    break;
                case UpdateRefreshStatus.STATUS_FOOTER_LOADING:
                    tv.setText("正在加载");
                    tv.setTextColor(Color.BLUE);
                    break;
                case UpdateRefreshStatus.STATUS_FOOTER_LOAD_FAIL:
                    tv.setText("加载失败,点击重试");
                    tv.setTextColor(Color.GREEN);
                    //设置点击事件
                    setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            update(1, UpdateRefreshStatus.STATUS_FOOTER_LOADING);
                            //执行加载更多
                            
                        }
                    });
                    break;
                case UpdateRefreshStatus.STATUS_FOOTER_NOT_MORE:
                    tv.setText("没有更多数据");
                    tv.setTextColor(Color.YELLOW);
                    break;
            }
        }
    }


    public interface UpdateRefreshStatus{
        public static final  int STATUS_HEADER_NOT_REFRESH = 1;
        public static final int STATUS_HEADER_CAN_REFRESH = 2;
        public static final int STATUS_HEADER_REFRESHING = 3;
        public static final int STATUS_FOOTER_NOT_LOAD = 4;
        public static final int STATUS_FOOTER_CAN_LOAD = 5;
        public static final int STATUS_FOOTER_LOADING = 6;
        public static final int STATUS_FOOTER_NOT_MORE = 7;
        public static final int STATUS_FOOTER_LOAD_FAIL = 8;
        void update(float ratio, int type);
    }



    /**************************增加FooterAdapter********************************************/

    public static abstract class RecyclerRefreshAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        public static final int FOOTER_CONTENT_VIEW_TYPE = -0x1111;
        public static final  int HEADER_CONTAINER_VIEW_TYPE = -0x11111;
        private List<T> mDatas;
        private Context mContext;
        private SuperRefreshLayout mRefreshLayout;

        public RecyclerRefreshAdapter(Context context, @NonNull SuperRefreshLayout refreshLayout){
            mDatas = new ArrayList<>();
            if(refreshLayout == null){
                throw new NullPointerException("the dapter must bind it's refreshLayout, so the refreshLayout can't be null");
            }
            mContext = context;
            mRefreshLayout = refreshLayout;
        }

        /**
         * 更新数据源
         * @param datas
         * @param isComplete
         */
        public void updateDatas(@NonNull List<T> datas, boolean isComplete){
            if(datas == null){
                throw new NullPointerException("adapter's datas can't be null");
            }
            if(mDatas == null){
                mDatas = datas;
            }else {
                mDatas.clear();
                mDatas.addAll(datas);
            }
            mRefreshLayout.setLoadCompleted(isComplete);
            notifyDataSetChanged();
        }

        /**
         * 新增数据元
         * @param datas
         * @param isCompleted
         */
        public void appendDatas(List<T> datas, boolean isCompleted){
            if(datas == null){
                throw new NullPointerException("adapter's datas can't be null");
            }
            mDatas.addAll(datas);
            mRefreshLayout.setLoadCompleted(isCompleted);
            notifyDataSetChanged();
        }


        @Override
        public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if(viewType == FOOTER_CONTENT_VIEW_TYPE){
                //添加footer view
                SwipeRefreshFooter footer = mRefreshLayout.getFooterView();
                footer.update(1, UpdateRefreshStatus.STATUS_FOOTER_LOADING);
                return new FooterViewHolder(footer);
            }else if(viewType == HEADER_CONTAINER_VIEW_TYPE){
                return new HeaderContainerViewHolder(mRefreshLayout.getHeaderContainerView());
            }
            return getViewHolder(parent, viewType);
        }

        @Override
        public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(getItemViewType(position) == FOOTER_CONTENT_VIEW_TYPE){
                return;
            }

            if(mRefreshLayout.getHeaderContainerView() != null){
                position--;
            }
            if(position < 0){
                return;
            }
            bindViewToHolder(holder,position);
        }


        @Override
        public final int getItemCount() {
            int itemCount = getRealItemCount();
            if(itemCount > 0 && mIsAddTailerToList == true && mIsLoadmoreEnable && !mRefreshLayout.isLoadCompleted()){
                itemCount++;
            }
            if(mRefreshLayout.getHeaderContainerView() != null){
                itemCount++;
            }
            return itemCount;
        }

        /**
         * 返回 实际 数据的长度
         * @return
         */
        public int getRealItemCount(){
            return mDatas.size();
        }

        public List<T> getDatas(){
            return mDatas;
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0 && mRefreshLayout.getHeaderContainerView() != null){
                return HEADER_CONTAINER_VIEW_TYPE;
            }else if(position == getItemCount() -1 && (mIsAddTailerToList == true && mIsLoadmoreEnable && !mRefreshLayout.isLoadCompleted())){
                return FOOTER_CONTENT_VIEW_TYPE;
            }
            return getContentItemViewType(position);
        }

        /**
         * 处理GridLayoutManager的 HeaderContainer 和Footer 占据一行的问题
         * @param recyclerView
         */
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            if(recyclerView.getLayoutManager()instanceof GridLayoutManager){
                final GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    /**
                     * 设置当前position的item 占据单元格的个数(全部占据,就是占满当前这行)
                     * @param position
                     * @return
                     */
                    @Override
                    public int getSpanSize(int position) {
                        if(getItemViewType(position) == HEADER_CONTAINER_VIEW_TYPE || getItemViewType(position) == FOOTER_CONTENT_VIEW_TYPE){
                            return layoutManager.getSpanCount();
                        }
                        return 1;
                    }
                });
            }
        }


        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            LayoutParams layoutParams = holder.itemView.getLayoutParams();
            if(layoutParams != null && layoutParams instanceof StaggeredGridLayoutManager.LayoutParams &&
                    (getItemViewType(holder.getLayoutPosition()) == HEADER_CONTAINER_VIEW_TYPE || getItemViewType(holder.getLayoutPosition()) == FOOTER_CONTENT_VIEW_TYPE)){
                ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
            }
        }

        /**
         * 此方法中创建对应的item view 显示的view holder
         * @return
         * @param parent
         * @param viewType
         */
        public abstract RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType);

        /**
         * 将 bean中的数据绑定到 holder的view上
         * @param holder
         * @param position
         */
        protected abstract void bindViewToHolder(RecyclerView.ViewHolder holder, int position);

        /**
         * 自定义的 itemViewType
         * @param position
         * @return
         */
        protected abstract int getContentItemViewType(int position);
    }


    /**
     * 为List 增加 头部内容(用于扩展,不是当前List中的内容)
     */
    private static class HeaderContainerViewHolder extends RecyclerView.ViewHolder{

        public HeaderContainerViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 加载更多的FooterView
     */
    private static class FooterViewHolder extends RecyclerView.ViewHolder{

        private View mItemView;
        public FooterViewHolder(View itemView) {
            super(itemView);
            mItemView = itemView;
        }

        /**
         * 更新状态
         * @param type
         */
        public void update(int type){
            if(!(mItemView instanceof SwipeRefreshFooter)){
                throw new IllegalStateException("Footer view not extands SwipeRefreshFooter");
            }
            ((SwipeRefreshFooter) mItemView).update(1,type);
        }
    }


    /**
     * 由于上面在RecyclerView的基础上 增加了两个item, 所以造成使用ItemDecoration的时候,会多出来两个,进行修正
     */
    public static class RefreshItemDecoration extends RecyclerView.ItemDecoration{
        private SuperRefreshLayout mRefreshLayout;

        public RefreshItemDecoration(@NonNull SuperRefreshLayout refreshLayout){
            if(refreshLayout == null){
                throw new IllegalStateException("the SwipeRefreshLayout Can't be null while create this RefreshItemDecoration");
            }
            mRefreshLayout = refreshLayout;
        }

        @Override
        public final void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);
            int childCount = parent.getChildCount();
            for (int i=0;i < childCount; i++){
                View child = parent.getChildAt(i);
                if(child == mRefreshLayout.getHeaderContainerView()){
                    continue;
                }
                onDrawChild(child, c, parent, state);
            }
        }

        /**
         *
         * @param child 当前位置child view
         * @param c
         * @param parent
         * @param state
         */
        protected void onDrawChild(View child, Canvas c, RecyclerView parent, RecyclerView.State state) {

        }

        /**
         * 获取要绘制ItemDecoration的范围
         * @param outRect 范围由这个参数决定, 注意:它的left,right,top,bottom 并不是位置,而是每个位置所占 距离的大小
         * @param child
         * @param parent
         * @param state
         */
        @Override
        public final void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int position = params.getViewAdapterPosition();
            if(mRefreshLayout.getHeaderContainerView() != null && position == 0){
                outRect.set(0,0,0,0);
                return;
            }
            if(mIsAddTailerToList == true && mIsLoadmoreEnable && !mRefreshLayout.isLoadCompleted() && position == parent.getAdapter().getItemCount() -1){
                outRect.set(0,0,0,0);
                return;
            }
            if(mRefreshLayout.getHeaderContainerView() !=null){
                position--;
            }
            getItemOffsetsInRealPosition(position, outRect, child, parent, state);

        }

        protected void getItemOffsetsInRealPosition(int position, Rect outRect, View child, RecyclerView parent, RecyclerView.State state){
            super.getItemOffsets(outRect,child, parent, state);
        }
    }



    /*************************************针对ListView实现增加的Adapter(不推荐使用ListView)********************************************/
    public static class ListRefreshAdapter<T> extends BaseAdapter{

        private List<T> mDatas;
        private SuperRefreshLayout mRefreshLayout;

        public ListRefreshAdapter(@NonNull SuperRefreshLayout refreshLayout){
            if(refreshLayout == null){
                throw new NullPointerException("the dapter must bind it's refreshLayout, so the refreshLayout can't be null");
            }
            mRefreshLayout = refreshLayout;
            mDatas = new ArrayList<>();
        }

        public void updateDatas(@NonNull List<T> datas, boolean isCompleted){
            if(datas == null){
                throw new NullPointerException("the datas added to list can't be null");
            }
            if(mDatas == null){
                mDatas = datas;
            }else {
                mDatas.clear();
                mDatas.addAll(datas);
            }
            mRefreshLayout.setLoadCompleted(isCompleted);
            notifyDataSetChanged();
        }

        public void appendDatas(@NonNull List<T> datas, boolean isCompleted){
            if(datas == null){
                throw new NullPointerException("the datas added to list can't be null");
            }
            if(mDatas == null){
                mDatas = new ArrayList<>();
            }
            mDatas.addAll(datas);
            mRefreshLayout.setLoadCompleted(isCompleted);
            notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetChanged() {
            int totalItemCount = getCount();
            int visiableItemCount = ((AbsListView)mRefreshLayout.getTargetView()).getChildCount();
            if(totalItemCount <= visiableItemCount){
                ((ListView)mRefreshLayout.getTargetView()).removeFooterView(mRefreshLayout.getFooterView());
            }else if(mIsAddTailerToList && mIsLoadmoreEnable && !mRefreshLayout.isLoadCompleted()){
//                ((ListView)mRefreshLayout.getTargetView()).addFooterView(mRefreshLayout.getFooterView());
            }else {
                ((ListView)mRefreshLayout.getTargetView()).removeFooterView(mRefreshLayout.getFooterView());
            }
            super.notifyDataSetChanged();

        }

        public int getRealCount(){
            return mDatas.size();
        }

        public List<T> getDatas(){
            return mDatas;
        }

        @Override
        public final int getCount() {
            return getRealCount();
        }

        @Override
        public Object getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }



    /*****************************监听滑到尾部,执行加在更多的操作(借鉴 SuperRecyclerView 里面的判断代码,来实现)********************************/
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ensureTarget();
        if (mTarget instanceof RecyclerView) {
            ((RecyclerView) mTarget).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if(mIsAddTailerToList && mIsLoadmoreEnable && !mIsLoadCompleted)
                        processOnMore((RecyclerView) mTarget);// 这里的判断也是:只要最有一个item显示一点,就会触发 响应的操作

                }

            });
        }else if(mTarget instanceof ListView){
            if(mIsAddTailerToList && mIsLoadmoreEnable){
                mFooterView.update(1, UpdateRefreshStatus.STATUS_FOOTER_LOADING);
                View footerView =mFooterView;
                if(!(footerView.getLayoutParams() instanceof AbsListView.LayoutParams)){
                    footerView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,footerView.getLayoutParams().height));
                }
                ((ListView)mTarget).addFooterView(footerView);
            }

            if(mHeaderContainer != null){
                ((ListView)mTarget).addHeaderView(mHeaderContainer);
            }

            ((ListView) mTarget).setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if((firstVisibleItem + visibleItemCount ) == totalItemCount){//只要最后一个item显示出来一点,就会出现这里
                        //如果想要,完全显示出来才执行相应操作,需要做如下处理:
//                        View child = ((ListView) mTarget).getChildAt(((ListView) mTarget).getChildCount() - 1);
//                        if(child != null && mTarget.getHeight() >= child.getBottom()){}

                        if(mIsAddTailerToList && mIsLoadmoreEnable && !mIsLoadCompleted){
                            //执行加在更多的操作
                            setLoadingMore(true,true);
                        }

                    }
                }
            });
        }

    }

    /**
     * 这个可以设置,滑动到 剩余多少多,就开始load more
     */
    private static final int ITEM_LEFT_TO_LOAD_MORE = 1;

    private void processOnMore(RecyclerView target) {
        RecyclerView.LayoutManager layoutManager = target.getLayoutManager();
        int lastVisibleItemPosition = getLastVisibleItemPosition(layoutManager);
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();

        if ((totalItemCount > visibleItemCount) &&(((totalItemCount - lastVisibleItemPosition) <= ITEM_LEFT_TO_LOAD_MORE ||
                (totalItemCount - lastVisibleItemPosition) == 0 && totalItemCount > visibleItemCount))
                ) {
            setLoadingMore(true,true);
        }
    }

    private int getLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        int lastVisibleItemPosition = -1;
        if (layoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            lastVisibleItemPosition = caseStaggeredGrid(layoutManager);
        } else {
            throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
        }
        return lastVisibleItemPosition;

    }

    private int[] lastScrollPositions;

    private int caseStaggeredGrid(RecyclerView.LayoutManager layoutManager) {
        StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
        if (lastScrollPositions == null)
            lastScrollPositions = new int[staggeredGridLayoutManager.getSpanCount()];

        staggeredGridLayoutManager.findLastVisibleItemPositions(lastScrollPositions);
        return findMax(lastScrollPositions);
    }

    private int findMax(int[] lastPositions) {
        int max = Integer.MIN_VALUE;
        for (int value : lastPositions) {
            if (value > max)
                max = value;
        }
        return max;
    }


    /*******************增加EmptyView*****************/

    private View mEmptyView;

    /**
     * 为RecyclerView添加的方法,如果使用的ListView,直接调用ListView的 setEmptyView()即可
     * @param emptyView
     * @param adapter
     */
    public void setEmptyView(@NonNull final View emptyView, @NonNull final RecyclerView.Adapter adapter){
        if(emptyView == null){
            throw new IllegalStateException("if set empty view ,the empty view can't be nulll");
        }
        if(adapter == null){
            throw new IllegalStateException("if set empty view ,must set the adapter");
        }

        mEmptyView = emptyView;
        addView(mEmptyView);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                update();
            }


            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                update();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                super.onItemRangeChanged(positionStart, itemCount, payload);
                update();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                update();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                update();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                update();
            }

            private void update() {
                if(adapter.getItemCount() == 0){
                    mEmptyView.setVisibility(VISIBLE);
                }else {
                    mEmptyView.setVisibility(GONE);
                }
            }
        });
    }



}

