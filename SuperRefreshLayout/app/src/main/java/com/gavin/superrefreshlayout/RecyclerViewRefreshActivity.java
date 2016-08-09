package com.gavin.superrefreshlayout;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gavin.superrefreshlayout.refreshwidget.SuperRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangfei on 16/8/9.
 */
public class RecyclerViewRefreshActivity extends AppCompatActivity {

    private SuperRefreshLayout mRefreshLayout;
    private RecyclerView mRefreshList;
    private SuperRefreshLayout.RecyclerRefreshAdapter mAdapter;
    private int index;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_refresh);

        mRefreshLayout = (SuperRefreshLayout) findViewById(R.id.container);
        mRefreshList = (RecyclerView) findViewById(R.id.refresh_list);

        /**
         * 完美支持所有的layoutManager(但是支持方向值有 Vertival)
         */
//        mRefreshList.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mRefreshList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MyAdapter(this, mRefreshLayout);
        TextView emptyView = new TextView(this);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setText("数据为空");
        emptyView.setTextSize(40);
        emptyView.setGravity(Gravity.CENTER);

        mRefreshLayout.setEmptyView(emptyView,mAdapter);
        mRefreshList.setAdapter(mAdapter);

        mRefreshLayout.setProgressViewEndTarget(false, 100);
        mRefreshLayout.setDistanceToTriggerSync(100);
        mRefreshLayout.setColorSchemeColors(Color.YELLOW, Color.RED, Color.BLUE, Color.GREEN);
        mRefreshLayout.setSize(SuperRefreshLayout.LARGE);//设置刷新图标的大小
        mRefreshLayout.setProgressBackgroundColorSchemeColor(Color.GRAY);
        mRefreshLayout.setOnRefreshListener(new SuperRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.updateDatas(loadMoreData(), false);//参数2: true:没有更多数据(不再支持加在更多的操作);;false : 数据没有加载完全(支持加载更多的操作)
                        mRefreshLayout.setRefreshing(false);
                        index =0;
                    }
                }, 2000);
            }
        });


        mRefreshLayout.setOnLoadMoreListener(new SuperRefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoading() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(index < 3){
                            mAdapter.appendDatas(loadMoreData(), false);//参数2: true:没有更多数据(不再支持加在更多的操作);;false : 数据没有加载完全(支持加载更多的操作)
                        }else {
                            mAdapter.appendDatas(loadMoreData(), true);
                        }
                        index++;
                        mRefreshLayout.setLoading(false);
                    }
                }, 2000);

            }
        });

        /**
         * 两种刷新方法: 第一种:SwipeRefreshLayout的方式
         *             第二种:市场上常见的下拉刷新的方式
         */
        mRefreshLayout.setRefreshTypeIsShowHeader(true);
        /**
         * 两种加载更多操作:第一种:上拉加载更多
         *                第二种:添加footerview, 滑倒这个位置自动加载更多
         *
         */
        mRefreshLayout.setLoadmoreTypeIsAddTailer(true);

        /**
         * 是否支持下拉刷新操作
         */
        mRefreshLayout.setRefreshEnable(true);
        /**
         * 是否支持 加载更多的操作
         */
        mRefreshLayout.setLoadmoreEnable(true);


        /**
         * 完美支持:在RecyclerView list列表上面添加一个 top 布局view
         */
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (100 * getResources().getDisplayMetrics().density)));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        tv.setTextColor(Color.GREEN);
        tv.setBackgroundColor(0x993300ff);
        tv.setGravity(Gravity.CENTER);
        tv.setText("Top Contianer");
        mRefreshLayout.addHeaderTopView(tv);

        /**
         * 默认带有动画效果的刷新方式
         * 以及后台刷新方式的支持
         */
        mRefreshLayout.refreshNormalWithAnimation();

    }

    private List<String> loadMoreData() {
        List<String> list = new ArrayList<>();
        for (int i =0 ; i < 30;i++){
            list.add("测试demo"+ i);
        }
        return list;
    }


    class MyAdapter extends SuperRefreshLayout.RecyclerRefreshAdapter<String> {

        public MyAdapter(Context context, SuperRefreshLayout refreshLayout) {
            super(context, refreshLayout);
        }

        @Override
        public RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(RecyclerViewRefreshActivity.this);
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));
            tv.setTextSize(30);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.RED);
            tv.setBackgroundColor(0x4400ff00);
            return new HomeHolder(tv);
        }

        @Override
        protected void bindViewToHolder(RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(getDatas().get(position));

        }

        @Override
        protected int getContentItemViewType(int position) {
            return 0;
        }
    }

    class HomeHolder extends RecyclerView.ViewHolder{

        public HomeHolder(View itemView) {
            super(itemView);
        }
    }


}
