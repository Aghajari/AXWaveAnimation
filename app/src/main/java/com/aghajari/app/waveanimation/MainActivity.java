package com.aghajari.app.waveanimation;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.aghajari.waveanimation.AXLineWaveView;
import com.aghajari.waveanimation.AXLineWeavingState;
import com.aghajari.waveanimation.AXWaveView;
import com.aghajari.waveanimation.AXWeavingState;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout layout = findViewById(R.id.layout);

        final AXWaveView waveView = findViewById(R.id.wave);
        final AXLineWaveView lineWaveView = findViewById(R.id.line_wave);

        waveView.addState(1, AXWeavingState.create(1,
                new RadialGradient(200,200,200, new int[]{0xff2BCEFF,0xff0976E3}
                ,null, Shader.TileMode.CLAMP)));

        waveView.addState(2, new AXWeavingState(2){

            @Override
            protected void updateTargets() {
                targetX = 0.2f + 0.1f * random.nextInt(100) / 100f;
                targetY = 0.7f + 0.1f * random.nextInt(100) / 100f;
            }

            @Override
            public Shader createShader() {
                return new RadialGradient(200,200,200, new int[]{0xff12B522,0xff00D6C1}
                        ,null, Shader.TileMode.CLAMP);
            }
        });

        lineWaveView.addState(1, AXLineWeavingState.create(1,AXWaveView.createLinearShader(1000,0xff0976E3,0xff0976E3,0xff0976E3)));
        lineWaveView.addState(2, AXLineWeavingState.create(1,AXWaveView.createLinearShader(1000,0xff00D6C1,0xff00D6C1,0xff00D6C1)));
        lineWaveView.setState(1);
        setNavigationBarColor(0xff0976E3);

        waveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (waveView.getCurrentState().getState() == 1) {
                    waveView.setState(2);
                    lineWaveView.setState(2);
                    setNavigationBarColor(0xff00D6C1);
                } else {
                    waveView.setState(1);
                    lineWaveView.setState(1);
                    setNavigationBarColor(0xff0976E3);
                }
            }
        });


        SeekBar seekBar = findViewById(R.id.seek_bar);
        seekBar.setMax((int) (AXWaveView.MAX_AMPLITUDE));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                waveView.setAmplitude(i);
                lineWaveView.setAmplitude(Math.max(i,AXLineWaveView.MAX_AMPLITUDE/50));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setNavigationBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(color);
        }
    }
}