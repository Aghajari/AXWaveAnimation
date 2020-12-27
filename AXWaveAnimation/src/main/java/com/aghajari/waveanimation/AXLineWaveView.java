/*
 * Copyright (C) 2020 - Amir Hossein Aghajari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.aghajari.waveanimation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AXLineWaveView extends View {

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float amplitude;
    float animateToAmplitude;
    float animateAmplitudeDiff;
    float amplitudeSpeed = 0.33f;

    private boolean stub;
    long lastStubUpdateAmplitude;

    AXLineWeavingState currentState;
    AXLineWeavingState previousState;

    float progressToState = 1f;
    Random random = new Random();

    int maxAlpha = 76;

    int shaderColor1;
    int shaderColor2;

    List<AXLineWaveDrawable> waveDrawables = new ArrayList<>();
    Map<Integer,AXLineWeavingState> states = new HashMap<>();

    AXLineWaveDrawable mainWave;
    boolean mainWaveEnabled = true;
    float mainWaveHeight = -1;

    public AXLineWaveView(Context context) {
        super(context);
        init(null,0,0);
    }

    public AXLineWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs,0,0);
    }

    public AXLineWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs,defStyleAttr,0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AXLineWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr,defStyleRes);
        init(attrs,defStyleAttr,defStyleRes);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs!=null) {
            final TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.AXLineWaveView, defStyleAttr, defStyleRes);
            shaderColor1 = a.getColor(R.styleable.AXLineWaveView_shader_color_1, getThemeColor(1));
            shaderColor2 = a.getColor(R.styleable.AXLineWaveView_shader_color_2, getThemeColor(2));
            amplitudeSpeed = a.getFloat(R.styleable.AXLineWaveView_amplitude_speed, amplitudeSpeed);
            maxAlpha = a.getInteger(R.styleable.AXLineWaveView_max_alpha, maxAlpha);
            mainWaveEnabled = a.getBoolean(R.styleable.AXLineWaveView_main_wave, mainWaveEnabled);
            mainWaveHeight = a.getDimensionPixelSize(R.styleable.AXLineWaveView_main_wave_height, -1);
            setAmplitude(a.getFloat(R.styleable.AXLineWaveView_amplitude, -1f));

            a.recycle();
        }else {
            shaderColor1 = getThemeColor(1);
            shaderColor2 = getThemeColor(2);
            setAmplitude(-1f);
            mainWaveHeight = -1;
        }

        addWaveDrawable(new AXLineWaveDrawable(7,0.7f){

            @Override
            public void draw(float left, float top, float right, float bottom, Canvas canvas, Paint paint, float amplitude) {
                deltaTop = dp(6) * amplitude;
                super.draw(left, top, right, bottom, canvas, paint, amplitude);
            }

            @Override
            protected void updateLine(float amplitude,float maxHeight) {
                super.updateLine(amplitude,maxHeight);
                setMinRadius(0);
                setMaxRadius(Math.max(maxHeight  * amplitude,dp(3)));
            }

        });

        addWaveDrawable(new AXLineWaveDrawable(8,0.7f){

            @Override
            protected void updateLine(float amplitude,float maxHeight) {
                super.updateLine(amplitude,maxHeight);
                setMinRadius(0);
                setMaxRadius(Math.max(maxHeight  * amplitude,dp(3)));
            }

            @Override
            public void draw(float left, float top, float right, float bottom, Canvas canvas, Paint paint, float amplitude) {
                deltaTop = dp(6) * amplitude;
                super.draw(left, top, right, bottom, canvas, paint, amplitude);
            }
        });

        mainWave = new AXLineWaveDrawable(5,0.3f){

                @Override
                protected void updateLine(float amplitude,float maxHeight) {
                    super.updateLine(amplitude,maxHeight);
                    setMinRadius(0);
                    setMaxRadius(dp(2) + dp(2) * amplitude);
                }
        };
    }

    protected AXLineWeavingState createDefaultState(){
        return AXLineWeavingState.create(-1,AXWaveView.createLinearShader(getWidth(),shaderColor1,shaderColor2,shaderColor1));
    }

    private int getThemeColor(int index) {
        try {
            int colorAttr = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                switch (index) {
                    case 1:
                        colorAttr = android.R.attr.colorPrimary;
                        break;
                    case 2:
                        colorAttr = android.R.attr.colorPrimaryDark;
                        break;
                }
            } else {
                String colorKey = "";
                switch (index) {
                    case 1:
                        colorKey = "colorPrimary";
                        break;
                    case 2:
                        colorKey = "colorPrimaryDark";
                        break;
                }
                colorAttr = getContext().getResources().getIdentifier(colorKey, "attr", getContext().getPackageName());
            }
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(colorAttr, outValue, true);
            return outValue.data;
        }catch (Exception e){
            return Color.BLUE;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (currentState == null && previousState == null && isEnabled()) {
            setState(createDefaultState());
        }

        if (mainWaveHeight == -1) mainWaveHeight = h * 0.75f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        draw(0,0,getWidth(),getHeight(),canvas,1f);
    }

    protected void draw(float left, float top, float right, float bottom, Canvas canvas, float progress) {

        if (stub) loadStubWaves();

        if (animateToAmplitude != amplitude) {
            amplitude += animateAmplitudeDiff * 16;
            if (animateAmplitudeDiff > 0) {
                if (amplitude > animateToAmplitude) {
                    amplitude = animateToAmplitude;
                }
            } else {
                if (amplitude < animateToAmplitude) {
                    amplitude = animateToAmplitude;
                }
            }
        }

        if (previousState != null) {
            progressToState += 16 / 250f;
            if (progressToState > 1f) {
                progressToState = 1f;
                previousState = null;
            }
        }

        for (int i = 0; i < 2; i++) {
            float alpha;
            if (i == 0 && previousState == null) {
                continue;
            }

            if (i == 0) {
                alpha = 1f - progressToState;
                previousState.setSize(left,top,right,bottom);
                previousState.update(16, amplitude);
                previousState.loadMatrix();
                previousState.setToPaint(paint);
            } else {
                if (currentState == null) {
                    return;
                }
                alpha = previousState != null ? progressToState : 1f;
                currentState.setSize(left,top,right,bottom);
                currentState.update(16, amplitude);
                currentState.loadMatrix();
                currentState.setToPaint(paint);
            }

            paint.setAlpha((int) (maxAlpha * alpha));

            float wavesHeight = bottom - top;
            if (mainWaveEnabled) wavesHeight -= mainWaveHeight;

            for (int index=0;index<waveDrawables.size(); index++){
                AXLineWaveDrawable waveDrawable = waveDrawables.get(index);
                waveDrawable.updateLine(amplitude,wavesHeight - dp(6));
                waveDrawable.update(amplitude,waveDrawable.lineSpeedScale);

                waveDrawable.draw(left, top, right, wavesHeight, canvas, paint,amplitude);
            }

            if (mainWaveEnabled) {
                mainWave.updateLine(amplitude,wavesHeight);
                mainWave.update(amplitude,mainWave.lineSpeedScale);

                if (i == 1) {
                    paint.setAlpha((int) (255 * alpha));
                } else {
                    paint.setAlpha(255);
                }

                mainWave.draw(left,bottom - wavesHeight,right,bottom,canvas,paint,amplitude);
            }
        }

        invalidate();
    }

    protected void loadStubWaves(){
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStubUpdateAmplitude > 1000) {
            lastStubUpdateAmplitude = currentTime;
            animateToAmplitude = 0.5f + 0.5f * Math.abs(random.nextInt() % 100) / 100f;
            animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100 + 1500.0f * getAmplitudeSpeed());
        }
    }

    public void addWaveDrawable(@NonNull AXLineWaveDrawable blobDrawable){
        if (waveDrawables.contains(blobDrawable)) return;
        waveDrawables.add(blobDrawable);
        blobDrawable.generateBlob();
        invalidate();
    }

    public void removeWaveDrawable(AXLineWaveDrawable blobDrawable){
        waveDrawables.remove(blobDrawable);
        invalidate();
    }

    public void removeWaveDrawable(int index){
        waveDrawables.remove(index);
        invalidate();
    }

    public @NonNull List<AXLineWaveDrawable> getAllWaveDrawables() {
        return waveDrawables;
    }

    public void addState(int key,@NonNull AXLineWeavingState state){
        states.put(key,state);
    }

    public void removeState(int key){
        states.remove(key);
    }

    public Map<Integer,AXLineWeavingState> getAllWeavingStates(){
        return states;
    }

    public AXLineWeavingState getState(int key){
        return states.get(key);
    }

    public void setState(int key){
        setState(getState(key));
    }

    public final static float MAX_AMPLITUDE = 8_500f;

    public void setAmplitude(float value) {
        if (value < 0) {
            stub = true;
            return;
        }
        stub = false;
        animateToAmplitude = (float) (Math.min(MAX_AMPLITUDE, value) / MAX_AMPLITUDE);
        animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100 + 500.0f * getAmplitudeSpeed());
    }

    public float getAmplitude() {
        return amplitude;
    }

    public AXLineWeavingState getCurrentState() {
        return currentState;
    }

    public AXLineWeavingState getPreviousState() {
        return previousState;
    }

    public void setState(AXLineWeavingState state) {
        if (currentState != null && currentState == state) {
            return;
        }

        previousState = currentState;
        currentState = state;
        if (previousState != null) {
            progressToState = 0;
        } else {
            progressToState = 1;
        }
        invalidate();
    }

    public int getMaxAlpha() {
        return maxAlpha;
    }

    public void setMaxAlpha(int maxAlpha) {
        this.maxAlpha = maxAlpha;
    }

    public float getAmplitudeSpeed() {
        return amplitudeSpeed;
    }

    public void setAmplitudeSpeed(float amplitudeSpeed) {
        this.amplitudeSpeed = amplitudeSpeed;
    }

    public boolean isMainWaveEnabled() {
        return mainWaveEnabled;
    }

    public void setMainWaveEnabled(boolean mainWaveEnabled) {
        this.mainWaveEnabled = mainWaveEnabled;
    }

    public void setMainWave(@NonNull AXLineWaveDrawable mainWave) {
        this.mainWave = mainWave;
    }

    public AXLineWaveDrawable getMainWave() {
        return mainWave;
    }

    public float getMainWaveHeight() {
        return mainWaveHeight;
    }

    public void setMainWaveHeight(float mainWaveHeight) {
        this.mainWaveHeight = mainWaveHeight;
    }

    private int dp(int value){
        return (int) (getContext().getResources().getDisplayMetrics().density * value);
    }

}
