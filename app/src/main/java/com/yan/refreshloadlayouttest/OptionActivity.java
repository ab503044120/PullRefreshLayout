package com.yan.refreshloadlayouttest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

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
                            startActivity(new Intent(getBaseContext(), CommonActivity1.class));
                            break;
                        case 2:
                            startActivity(new Intent(getBaseContext(), CommonActivity2.class));
                            break;
                    }
                }
            });
        }
    }

}
