package com.cjw.waterwaveview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.cjw.waterwaveview.waterwave.WaterView;

public class MainActivity extends AppCompatActivity {

    private TextView tv_curr_drink, tv_total_drink;
    private ImageView iv_decrease, iv_increase;
    private WaterView waterview;

    private float drinkToday = 0.0f, drinkGoal = 2.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_drink);
        initView();
    }

    private void initView() {
        tv_curr_drink = findViewById(R.id.tv_curr_drink);
        tv_total_drink = findViewById(R.id.tv_total_drink);
        tv_curr_drink = findViewById(R.id.tv_curr_drink);

        iv_decrease = findViewById(R.id.iv_decrease);
        iv_increase = findViewById(R.id.iv_increase);

        waterview = findViewById(R.id.waterview);


        tv_curr_drink.setText("" + drinkToday);
        tv_total_drink.setText("" + drinkGoal);

        waterview.setWaterWaveListener(currentAmount -> {
            drinkToday = currentAmount;
            tv_curr_drink.setText("" + drinkToday);
        });

        waterview.update(drinkToday, drinkGoal);

        iv_decrease.setOnClickListener(view -> waterview.reduce());

        iv_increase.setOnClickListener(view -> waterview.increase());
    }
}