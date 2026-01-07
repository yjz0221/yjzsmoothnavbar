package com.github.mylibdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.github.yjz.widget.nav.SuperSmoothBottomBar;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class MainActivity extends AppCompatActivity {

    private SuperSmoothBottomBar bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomBar = findViewById(R.id.bottomBar1);

        bottomBar.setOnItemSelected(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer pos) {
                Log.e("MainActivity","选中："+pos);
                return null;
            }
        });

        bottomBar.setOnItemReselected(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer pos) {
                Log.e("MainActivity","重复选中："+pos);
                return null;
            }
        });
    }

}