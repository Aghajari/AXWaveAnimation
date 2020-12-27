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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Amir Hossein Aghajari
 * @version 1.0.0
 */
public class AXWaveView extends View {

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float amplitude;
    float animateToAmplitude;
    float animateAmplitudeDiff;
    float amplitudeSpeed = 0.33f;

    private boolean stub;
    long lastStubUpdateAmplitude;

    AXWeavingState currentState;
    AXWeavingState previousState;

    float progressToState = 1f;
    boolean prepareToRemove;
    float progressToPrepareRemove = 0;
    private Shader prepareToRemoveShader;
    Matrix matrix = new Matrix();
    int removeColor = Color.RED;
    float removeAngle;
    int removeSize;
    boolean removeWave = true;

    float wavesEnter = 0f;
    Random random = new Random();

    boolean pressedState;
    float pressedProgress;
    float pinnedProgress;
    int maxAlpha = 76;
    float speedScale = 0.8f;
    boolean circleEnabled = true;
    float circleRadius = -1;

    float firstMinRadius = -1;
    float firstMaxRadius = -1;
    float secondMinRadius = -1;
    float secondMaxRadius = -1;

    int shaderColor1;
    int shaderColor2;

    List<AXWaveDrawable> waveDrawables = new ArrayList<>();
    Map<Integer,AXWeavingState> states = new HashMap<>();
    Interpolator interpolator = new OvershootInterpolator();

    public AXWaveView(Context context) {
        super(context);
        init(null,0,0);
    }

    public AXWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs,0,0);
    }

    public AXWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs,defStyleAttr,0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AXWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr,defStyleRes);
        init(attrs,defStyleAttr,defStyleRes);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs!=null) {
            final TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.AXWaveView, defStyleAttr, defStyleRes);
            shaderColor1 = a.getColor(R.styleable.AXWaveView_shader_color_1, getThemeColor(1));
            shaderColor2 = a.getColor(R.styleable.AXWaveView_shader_color_2, getThemeColor(2));
            amplitudeSpeed = a.getFloat(R.styleable.AXWaveView_amplitude_speed, amplitudeSpeed);
            speedScale = a.getFloat(R.styleable.AXWaveView_speed_scale, speedScale);
            pinnedProgress = a.getFloat(R.styleable.AXWaveView_pinned_progress, pinnedProgress);
            maxAlpha = a.getInteger(R.styleable.AXWaveView_max_alpha, maxAlpha);
            circleEnabled = a.getBoolean(R.styleable.AXWaveView_circle, true);
            circleRadius = a.getDimensionPixelSize(R.styleable.AXWaveView_circle_radius, (int) circleRadius);
            removeWave = a.getBoolean(R.styleable.AXWaveView_remove_wave, removeWave);
            removeColor = a.getColor(R.styleable.AXWaveView_remove_color, removeColor);
            removeSize = a.getDimensionPixelSize(R.styleable.AXWaveView_remove_size, dp(250));
            removeAngle = a.getFloat(R.styleable.AXWaveView_remove_angle, removeAngle);
            firstMinRadius = a.getDimensionPixelSize(R.styleable.AXWaveView_first_min_radius, (int) firstMinRadius);
            firstMaxRadius = a.getDimensionPixelSize(R.styleable.AXWaveView_first_max_radius, (int) firstMaxRadius);
            secondMinRadius = a.getDimensionPixelSize(R.styleable.AXWaveView_second_min_radius, (int) secondMinRadius);
            secondMaxRadius = a.getDimensionPixelSize(R.styleable.AXWaveView_second_max_radius, (int) secondMaxRadius);

            setAmplitude(a.getFloat(R.styleable.AXWaveView_amplitude, -1f));

            prepareToRemoveShader = new LinearGradient(0, 0, dp(100 + 250), 0,
                    new int[] {a.getColor(R.styleable.AXWaveView_shader_remove_color_1, getThemeColor(1)),
                            a.getColor(R.styleable.AXWaveView_shader_remove_color_2, getThemeColor(2)),
                            Color.TRANSPARENT}, new float[] {0, 0.4f, 1f}, Shader.TileMode.CLAMP);
            a.recycle();
        }else {
            shaderColor1 = getThemeColor(1);
            shaderColor2 = getThemeColor(2);
            removeSize = dp(250);
            prepareToRemoveShader = createDefaultRemoveShader();
            setAmplitude(-1f);
        }

        addWaveDrawable(new AXWaveDrawable(8){

            @Override
            public void update() {
                if (autoMax) setMaxRadius(AXWaveView.this.findSecondWaveMaxRadius(),true);
                if (autoMin) setMinRadius(findSecondWaveMinRadius(),true);
            }
        });

        addWaveDrawable(new AXWaveDrawable(9){
            @Override
            public void update() {
                if (autoMax) setMaxRadius(findSecondWaveMaxRadius() - dp(2),true);
                if (autoMin) setMinRadius(findSecondWaveMinRadius() + dp(1),true);
            }
        });
    }

    protected Shader createDefaultRemoveShader(){
        return createLinearShader(dp(350),Color.GRAY,Color.GRAY,Color.TRANSPARENT);
    }

    protected AXWeavingState createDefaultState(){
        return AXWeavingState.create(-1,createRadialShader(200,shaderColor1,shaderColor2));
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int xPadding = getPaddingLeft() + getPaddingRight();
        int yPadding = getPaddingTop() + getPaddingBottom();
        int width = getMeasuredWidth() - xPadding;
        int height = getMeasuredHeight() - yPadding;
        int size = Math.min(width, height);
        setMeasuredDimension(size + xPadding, size + yPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (AXWaveDrawable waveDrawable : waveDrawables){
            if (waveDrawable.autoMax) {
                waveDrawable.setMaxRadius(waveDrawable.firstDraw ? findFirstWaveMaxRadius() : findSecondWaveMaxRadius(),true);
            }
            if (waveDrawable.autoMin) {
                waveDrawable.setMinRadius(waveDrawable.firstDraw ? findFirstWaveMinRadius() : findSecondWaveMinRadius(),true);
            }

            if (waveDrawable.autoMax || waveDrawable.autoMin) waveDrawable.generateBlob();
        }

        if (currentState == null && previousState == null && isEnabled()) {
            setState(createDefaultState());
        }
    }

    /**
     * calculate default shader size
     */
    public int findShaderSize (){
        return getWidth() + dp(20);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getAlpha() == 0) return;

        float cx = getMeasuredWidth() >> 1;
        float cy = getMeasuredHeight() >> 1;

        // DESIGNER
        if (isInEditMode()){
            currentState.update(16, MAX_AMPLITUDE);
            currentState.loadMatrix();
            currentState.setToPaint(paint);

            for (int index = 0; index < waveDrawables.size(); index++) {
                AXWaveDrawable waveDrawable = waveDrawables.get(index);
                waveDrawable.update();

                float scale = findWaveScale(index);
                scale = Math.min(scale, 1.3f);
                canvas.save();
                canvas.scale(scale, scale, cx, cy);
                paint.setAlpha(maxAlpha*2);
                canvas.drawCircle(cx,cy,waveDrawable.maxRadius,paint);
                paint.setAlpha(maxAlpha);
                canvas.drawCircle(cx,cy,waveDrawable.minRadius,paint);
                canvas.restore();
            }

            if (isCircleEnabled()) {
                paint.setAlpha(255);

                canvas.save();
                canvas.scale(1f, 1f, cx, cy);
                canvas.drawCircle(cx, cy, circleRadius==-1 ? findCircleRadius() : circleRadius, paint);
                canvas.restore();
            }
            return;
        }

        if (pressedState && pressedProgress != 1f) {
            pressedProgress += 16f / 150f;
            if (pressedProgress > 1f) {
                pressedProgress = 1f;
            }
        } else if (!pressedState && pressedProgress != 0) {
            pressedProgress -= 16f / 150f;
            if (pressedProgress < 0f) {
                pressedProgress = 0f;
            }
        }

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

        if (prepareToRemove && progressToPrepareRemove != 1f) {
            progressToPrepareRemove += 16f / 350f;
            if (progressToPrepareRemove > 1f) {
                progressToPrepareRemove = 1f;
            }
        } else if (!prepareToRemove && progressToPrepareRemove != 0) {
            progressToPrepareRemove -= 16f / 350f;
            if (progressToPrepareRemove < 0f) {
                progressToPrepareRemove = 0f;
            }
        }
        boolean showWaves = currentState.supportWaves();

        if (showWaves && wavesEnter != 1f) {
            wavesEnter += 16f / 350f;
            if (wavesEnter > 1f) {
                wavesEnter = 1f;
            }
        } else if (!showWaves && wavesEnter != 0f) {
            wavesEnter -= 16f / 350f;
            if (wavesEnter < 0f) {
                wavesEnter = 0f;
            }
        }

        float wavesEnter = 0.65f + 0.35f * interpolator.getInterpolation(this.wavesEnter);

        for (AXWaveDrawable blobDrawable : waveDrawables){
            blobDrawable.update(amplitude, stub ? 0.1f : getSpeedScale());
        }

        for (int i = 0; i < 3; i++) {
            float alpha = 1f;
            if (i == 0 && previousState == null) {
                continue;
            }

            if (i == 0) {
                if (progressToPrepareRemove == 1f) {
                    continue;
                }
                alpha = 1f - progressToState;
                previousState.update(16, amplitude);
                previousState.loadMatrix();
                previousState.setToPaint(paint);
            } else if (i == 1) {
                if (currentState == null) {
                    return;
                }
                if (progressToPrepareRemove == 1f) {
                    continue;
                }
                alpha = previousState != null ? progressToState : 1f;
                currentState.update(16, amplitude);
                currentState.loadMatrix();
                currentState.setToPaint(paint);
            } else {
                if (prepareToRemoveShader!=null) {
                    if (progressToPrepareRemove == 0) {
                        continue;
                    }
                    alpha = 1f;
                    paint.setColor(removeColor);
                    matrix.reset();
                    matrix.postTranslate(-removeSize * (1f - progressToPrepareRemove), 0);
                    matrix.postRotate(removeAngle, cx, cy);
                    prepareToRemoveShader.setLocalMatrix(matrix);
                    paint.setShader(prepareToRemoveShader);
                }
            }

            if (this.wavesEnter != 0) {
                if (removeWave){
                    paint.setAlpha((int) (maxAlpha * alpha * (1f - progressToPrepareRemove)));
                }else{
                    if (i != 2) {
                        paint.setAlpha((int) (maxAlpha * alpha * (1f - progressToPrepareRemove)));
                    } else {
                        paint.setAlpha((int) (maxAlpha * alpha * progressToPrepareRemove));
                    }
                }

                for (int index = 0; index < waveDrawables.size(); index++) {
                    AXWaveDrawable waveDrawable = waveDrawables.get(index);
                    waveDrawable.update();

                    float scale = findWaveScale(index);
                    scale = Math.min(scale, 1.3f) * wavesEnter;
                    canvas.save();
                    canvas.scale(scale, scale, cx, cy);
                    waveDrawable.draw(cx, cy, canvas, paint);
                    canvas.restore();
                }
            }

            if (isCircleEnabled()) {
                if (i == 2) {
                    paint.setAlpha((int) (255 * progressToPrepareRemove));
                } else if (i == 1) {
                    paint.setAlpha((int) (255 * alpha));
                } else {
                    paint.setAlpha(255);
                }
                canvas.save();
                canvas.scale(1f + 0.1f * pressedProgress, 1f + 0.1f * pressedProgress, cx, cy);
                canvas.drawCircle(cx, cy, circleRadius==-1 ? findCircleRadius() : circleRadius, paint);
                canvas.restore();
            }
        }

        if (this.wavesEnter > 0)
            invalidate();
    }

    protected float findCircleRadius(){
        float rad = 0;
        for (AXWaveDrawable waveDrawable : waveDrawables){
            if (rad == 0) {
                rad = waveDrawable.minRadius;
            }else{
                rad = Math.min(rad,waveDrawable.maxRadius);
            }
        }
        return Math.max(rad,dp(32));
    }

    /**
     * load the default animation
     */
    protected void loadStubWaves(){
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStubUpdateAmplitude > 1000) {
            lastStubUpdateAmplitude = currentTime;
            animateToAmplitude = 0.5f + 0.5f * Math.abs(random.nextInt() % 100) / 100f;
            animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100 + 1500.0f * getAmplitudeSpeed());
        }
    }

    /**
     * calculate wave drawable scale for the giving index
     */
    protected float findWaveScale(int index) {
        return (1f + (findWaveScaleRange() - (index* findWaveScaleDef())) * amplitude + 0.1f * pressedProgress) * (1f - pinnedProgress);
    }

    protected float findWaveScaleDef(){
        return 0.04f;
    }

    protected float findWaveScaleRange(){
        return 0.3f;
    }

    /**
     * calculate starting wave max radius
     */
    protected float findFirstWaveMaxRadius(){
        if (firstMaxRadius != -1) return firstMaxRadius;
        return findSecondWaveMaxRadius() - dp(10);
    }

    /**
     * calculate starting wave min radius
     */
    protected float findFirstWaveMinRadius(){
        if (firstMinRadius != -1) return firstMinRadius;
       return findSecondWaveMinRadius() - dp(10);
    }

    /**
     * calculate final wave max radius
     */
    protected float findSecondWaveMaxRadius(){
        if (secondMaxRadius != -1) return secondMaxRadius;
        return getWidth() / 2f / 1.25f;
    }

    /**
     * calculate final wave min radius
     */
    protected float findSecondWaveMinRadius(){
        if (secondMinRadius != -1) return secondMinRadius;
        return getWidth() / 2f / 1.5f;
    }

    public void setPressedState(boolean pressedState) {
        this.pressedState = pressedState;
    }

    public void setPinnedProgress(float pinnedProgress) {
        this.pinnedProgress = pinnedProgress;
    }

    /**
     * add new wave drawable
     */
    public void addWaveDrawable(@NonNull AXWaveDrawable waveDrawable){
        if (waveDrawables.contains(waveDrawable)) return;
        waveDrawables.add(waveDrawable);

        if (waveDrawable.getMaxRadius() < 0)
            waveDrawable.setMaxRadius(findFirstWaveMaxRadius(),true);
        if (waveDrawable.getMinRadius() < 0)
            waveDrawable.setMinRadius(findFirstWaveMinRadius(),true);
        waveDrawable.generateBlob();

        invalidate();
    }

    /**
     * remove wave drawable
     */
    public void removeWaveDrawable(AXWaveDrawable blobDrawable){
        waveDrawables.remove(blobDrawable);
        invalidate();
    }

    /**
     * remove wave drawable
     */
    public void removeWaveDrawable(int index){
        waveDrawables.remove(index);
        invalidate();
    }

    /**
     * @return all wave drawables
     */
    public @NonNull List<AXWaveDrawable> getAllWaveDrawables() {
        return waveDrawables;
    }

    /**
     * add and save an state
     */
    public void addState(int key,@NonNull AXWeavingState state){
        states.put(key,state);
    }

    /**
     * remove an saved state
     */
    public void removeState(int key){
        states.remove(key);
    }

    /**
     * @return all saved states
     */
    public Map<Integer,AXWeavingState> getAllWeavingStates(){
        return states;
    }

    public AXWeavingState getState(int key){
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

    public AXWeavingState getCurrentState() {
        return currentState;
    }

    public AXWeavingState getPreviousState() {
        return previousState;
    }

    public void setState(AXWeavingState state) {
        if (currentState != null && currentState == state) {
            return;
        }

        if (state.width == 0 || state.height == 0) {
            if (state.width == 0) state.width = currentState!=null ? currentState.width : findShaderSize();
            if (state.height == 0) state.height = currentState!=null ? currentState.height : findShaderSize();
        }

        previousState = currentState;
        currentState = state;
        if (previousState != null) {
            progressToState = 0;
        } else {
            progressToState = 1;
            wavesEnter = state.supportWaves() ? 1f : 0f;
        }
        invalidate();
    }

    public void prepareToRemove(boolean prepare) {
        if (this.prepareToRemove != prepare) {
            invalidate();
        }
        this.prepareToRemove = prepare;
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

    public float getSpeedScale() {
        return speedScale;
    }

    public void setSpeedScale(float speedScale) {
        this.speedScale = speedScale;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    public boolean isCircleEnabled() {
        return circleEnabled;
    }

    public void setCircleEnabled(boolean circleEnabled) {
        this.circleEnabled = circleEnabled;
    }

    public float getCircleRadius() {
        return circleRadius;
    }

    public void setCircleRadius(float circleRadius) {
        this.circleRadius = circleRadius;
    }

    public int getPrepareToRemoveColor() {
        return removeColor;
    }

    public void setPrepareToRemoveColor(int removeColor) {
        this.removeColor = removeColor;
    }

    public int getPrepareToRemoveSize() {
        return removeSize;
    }

    public void setPrepareToRemoveSize(int removeSize) {
        this.removeSize = removeSize;
    }

    public float getPrepareToRemoveAngle() {
        return removeAngle;
    }

    public void setPrepareToRemoveAngle(double angle) {
        removeAngle = (float) angle;
    }

    public Shader getPrepareToRemoveShader() {
        return prepareToRemoveShader;
    }

    public void setPrepareToRemoveShader(Shader prepareToRemoveShader) {
        this.prepareToRemoveShader = prepareToRemoveShader;
    }

    public void setPrepareToRemoveWaveEnabled(boolean removeWave) {
        this.removeWave = removeWave;
    }

    public boolean isPreparingToRemove() {
        return prepareToRemove;
    }

    public static Shader createLinearShader(float size,int color1,int color2,int color3){
        return new LinearGradient(0, 0, size, 0,
                new int[] {color1,color2, color3},
                new float[] {0, 0.4f, 1f}, Shader.TileMode.CLAMP);
    }

    public static Shader createRadialShader(float size,int color1,int color2){
        return new RadialGradient(size,size,size, new int[]{color1,color2},null, Shader.TileMode.CLAMP);
    }

    private int dp(int value){
        return (int) (getContext().getResources().getDisplayMetrics().density * value);
    }

}
