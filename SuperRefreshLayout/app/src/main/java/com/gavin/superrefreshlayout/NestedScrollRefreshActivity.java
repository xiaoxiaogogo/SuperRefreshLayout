package com.gavin.superrefreshlayout;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gavin.superrefreshlayout.refreshwidget.SuperRefreshLayout;

/**
 * Created by wangfei on 16/8/9.
 */
public class NestedScrollRefreshActivity extends AppCompatActivity {

    private SuperRefreshLayout mRefreshContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nested_scroll_refresh);

        mRefreshContainer = (SuperRefreshLayout) findViewById(R.id.container);


        mRefreshContainer.setOnRefreshListener(new SuperRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //切记:此方法会引发 界面的重新布局
                        //mAdapter.notifyDataSetChanged();//如果数据发生改变,但是没有调用此方法或其他通知数据变化的方法,就会向下滑动的到新的item的时候,会曝出"数组越界异常"
                        mRefreshContainer.setRefreshing(false);
                    }
                }, 2000);
            }
        });

        mRefreshContainer.setRefreshTypeIsShowHeader(true);
        mRefreshContainer.setLoadmoreTypeIsAddTailer(true);

        mRefreshContainer.setRefreshEnable(true);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (100 * getResources().getDisplayMetrics().density)));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        tv.setTextColor(Color.GREEN);
        tv.setBackgroundColor(0x993300ff);
        tv.setGravity(Gravity.CENTER);
        tv.setText("Top Contianer");
        mRefreshContainer.addHeaderTopView(tv);

        mRefreshContainer.refreshNormalWithAnimation();
    }



}
