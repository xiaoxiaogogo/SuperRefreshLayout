package com.gavin.superrefreshlayout;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.gavin.superrefreshlayout.refreshwidget.SuperRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangfei on 16/8/9.
 */
public class ListViewRefreshActivity extends AppCompatActivity {

    private SuperRefreshLayout mRefreshContainer;
    private ListView mRefreshList;
    private SuperRefreshLayout.ListRefreshAdapter mAdapter;
    private int index;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_view_refresh);

        mRefreshContainer = (SuperRefreshLayout) findViewById(R.id.refresh_container);
        mRefreshList = (ListView) findViewById(R.id.refresh_list);
        mAdapter = new MyAdapter(mRefreshContainer);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (100 * getResources().getDisplayMetrics().density)));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        tv.setTextColor(Color.GREEN);
        tv.setBackgroundColor(0x993300ff);
        tv.setGravity(Gravity.CENTER);
        tv.setText("Top Contianer");
        mRefreshContainer.addHeaderTopView(tv);//可以直接调用ListView的 addHeaderView()(内部就是这样实现的)
        TextView tv2 = new TextView(this);
        tv2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (100 * getResources().getDisplayMetrics().density)));
        tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        tv2.setTextColor(Color.YELLOW);
        tv2.setBackgroundColor(0x993388ff);
        tv2.setGravity(Gravity.CENTER);
        tv2.setText("Top Contianer2");
        mRefreshContainer.addHeaderTopView(tv2);


        mRefreshList.setAdapter(mAdapter);


        mRefreshContainer.setColorSchemeColors(Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE);//设置加载图标的颜色变化
        mRefreshContainer.setSize(SuperRefreshLayout.LARGE);//设置刷新图标的大小
        mRefreshContainer.setProgressBackgroundColorSchemeColor(Color.GRAY);
        mRefreshContainer.setDistanceToTriggerSync(100);//达到刷新的高度
        mRefreshContainer.setProgressViewEndTarget(false, 150);//设置正在刷新的高度(如果没有设置setProgressViewOffset(),那么位置这个方法设置的值,还需要减去初始顶部offset


        mRefreshContainer.setOnRefreshListener(new SuperRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.updateDatas(loadMoreData(), false);
                        //切记:此方法会引发 界面的重新布局
                        //mAdapter.notifyDataSetChanged();//如果数据发生改变,但是没有调用此方法或其他通知数据变化的方法,就会向下滑动的到新的item的时候,会曝出"数组越界异常"
                        mRefreshContainer.setRefreshing(false);
                        index =0;
                    }
                }, 2000);
            }
        });


        mRefreshContainer.setOnLoadMoreListener(new SuperRefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoading() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(index < 3){
                            mAdapter.appendDatas(loadMoreData(), false);
                        }else {
                            mAdapter.appendDatas(loadMoreData(), true);
                        }
                        index++;
                        mRefreshContainer.setLoading(false);
                    }
                }, 2000);

            }
        });

        mRefreshContainer.setRefreshTypeIsShowHeader(false);
        mRefreshContainer.setLoadmoreTypeIsAddTailer(false);

        mRefreshContainer.setRefreshEnable(true);
        mRefreshContainer.setLoadmoreEnable(true);

        mRefreshContainer.refreshNormalWithAnimation();


    }


    private List<String> loadMoreData() {
        List<String> datas = new ArrayList<>();
        for (int i =0; i< 20;i++){
            datas.add("测试数据" + i);
        }
        return datas;
    }

    class MyAdapter extends SuperRefreshLayout.ListRefreshAdapter<String>{


        public MyAdapter(@NonNull SuperRefreshLayout refreshLayout) {
            super(refreshLayout);
        }


        @Override
        public Object getItem(int position) {
            return getDatas().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(ListViewRefreshActivity.this);
            tv.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,200));
            tv.setTextSize(30);
            tv.setBackgroundColor(Color.YELLOW);
            tv.setTextColor(Color.RED);
            tv.setText(getDatas().get(position));
            return tv;
        }
    }
}
