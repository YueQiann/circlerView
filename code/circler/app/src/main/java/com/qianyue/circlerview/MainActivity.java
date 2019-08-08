package com.qianyue.circlerview;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Canvas;
import android.os.Bundle;
import android.widget.TextView;

import com.qianyue.circlerviewlib.OnScaleListener;
import com.qianyue.circlerviewlib.YQCircleView;

public class MainActivity extends AppCompatActivity implements OnScaleListener {
    private YQCircleView circleView;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        circleView.setOnScaleListener(this);
    }

    private void findView(){
        circleView = findViewById(R.id.main_circle);
        tv = findViewById(R.id.main_tv);
    }

    @Override
    public void onScaleChange(int scale) {
        tv.setText(scale+"");
    }

    @Override
    public void onScaleStart() {

    }

    @Override
    public void onScaleFinish() {

    }
}
