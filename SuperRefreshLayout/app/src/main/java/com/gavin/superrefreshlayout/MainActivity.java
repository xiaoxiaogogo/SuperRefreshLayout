package com.gavin.superrefreshlayout;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.recycler_view_refresh).setOnClickListener(this);
        findViewById(R.id.list_view_refresh).setOnClickListener(this);
        findViewById(R.id.nested_scroll_view_refresh).setOnClickListener(this);
        findViewById(R.id.scroll_view_refresh).setOnClickListener(this);
        findViewById(R.id.normal_view_refresh).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.recycler_view_refresh:
                startActivity(new Intent(this, RecyclerViewRefreshActivity.class));
                break;
            case R.id.list_view_refresh:

                break;
            case R.id.nested_scroll_view_refresh:

                break;
            case R.id.scroll_view_refresh:

                break;
            case R.id.normal_view_refresh:

                break;
        }
    }
}
