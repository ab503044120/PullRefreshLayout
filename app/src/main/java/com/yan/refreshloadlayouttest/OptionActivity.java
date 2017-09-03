package com.yan.refreshloadlayouttest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.testactivity.CommonActivity1;
import com.yan.refreshloadlayouttest.testactivity.CommonActivity2;
import com.yan.refreshloadlayouttest.testactivity.CommonActivity3;
import com.yan.refreshloadlayouttest.testactivity.CommonActivity4;
import com.yan.refreshloadlayouttest.testactivity.NestedActivity;
import com.yan.refreshloadlayouttest.testactivity.NestedActivity2;
import com.yan.refreshloadlayouttest.testactivity.ScrollingActivity;
import com.yan.refreshloadlayouttest.testactivity.ScrollingActivity2;
import com.yan.refreshloadlayouttest.testactivity.TabActivity;

public class OptionActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);
        ViewGroup container = (ViewGroup) findViewById(R.id.ll_container);
        for (int i = 0; i < container.getChildCount(); i++) {
            final int finalI = i;
            container.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (finalI) {
                        case 0:
                            startActivity(new Intent(getBaseContext(), NestedActivity.class));
                            break;
                        case 1:
                            startActivity(new Intent(getBaseContext(), NestedActivity2.class));
                            break;
                        case 2:
                            startActivity(new Intent(getBaseContext(), CommonActivity1.class));
                            break;
                        case 3:
                            startActivity(new Intent(getBaseContext(), CommonActivity2.class));
                            break;
                        case 4:
                            startActivity(new Intent(getBaseContext(), CommonActivity3.class));
                            break;
                        case 5:
                            startActivity(new Intent(getBaseContext(), CommonActivity4.class));
                            break;
                        case 6:
                            startActivity(new Intent(getBaseContext(), ScrollingActivity.class));
                            break;
                        case 7:
                            startActivity(new Intent(getBaseContext(), ScrollingActivity2.class));
                            break;
                        case 8:
                            startActivity(new Intent(getBaseContext(), TabActivity.class));
                            break;
                    }
                }
            });
        }
    }

}
