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

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.animation.Interpolator;

import java.util.Random;

public abstract class AXWeavingState {

    public float targetX = -1f;
    public float targetY = -1f;
    public float startX;
    public float startY;
    public float duration;
    public float time;
    public float interpolation;
    public float x;
    public float y;

    private final int state;

    public Shader shader;
    public final Matrix matrix = new Matrix();
    protected Random random = new Random();

    public int width = 0;
    public int height = 0;
    public float fixedScale = 1.5f;
    public float scale;
    public int durationBound = 1000;
    public boolean supportWaves = true;

    public float speedMin = 0.5f;
    public float speedMax = 0.01f;
    public Interpolator interpolator;

    public AXWeavingState(int state){
        this.state = state;
        init();
    }

    protected void init(){
        interpolator = new CubicBezierInterpolator(0, 0, .58, 1);
    }

    /**
     * create Paint shader
     */
    public abstract Shader createShader();

    /**
     * update shader
     */
    public void update(long dt, float amplitude) {
        if (shader==null) shader = createShader();
        if (shader == null) return;

        if (duration == 0 || time >= duration) {
            duration = random.nextInt(durationBound) + 500;
            time = 0;
            if (targetX == -1f) {
                updateTargets();
            }
            startX = targetX;
            startY = targetY;
            updateTargets();
        }

        time += dt * (0.5f + speedMin) + dt * (speedMax * 2) * amplitude;
        if (time > duration) {
            time = duration;
        }

        if (interpolator!=null) {
            interpolation = interpolator.getInterpolation(time / duration);
        } else {
            interpolation = 1f;
        }

        updateScale();
        updateTranslate();
    }

    /**
     * calculate targets
     */
    protected void updateTargets() {
        targetX = 0.8f + 0.2f * (random.nextInt(100) / 100f);
        targetY = random.nextInt(100) / 100f;
    }

    /**
     * calculate scale
     */
    protected void updateScale(){
        scale = width / 400.0f * this.fixedScale;
    }

    /**
     * calculate x,y using targets and start points
     */
    protected void updateTranslate(){
        x = width * (startX + (targetX - startX) * interpolation) - 200;
        y = height * (startY + (targetY - startY) * interpolation) - 200;
    }

    /**
     * apply changes and updated values
     */
    protected void loadMatrix(){
        matrix.reset();
        matrix.postTranslate(x, y);
        matrix.postScale(scale, scale, x + 200, y + 200);

        if (shader!=null) shader.setLocalMatrix(matrix);
    }

    /**
     * apply shader into the target paint
     */
    public void setToPaint(Paint paint) {
        paint.setShader(shader);
    }

    /**
     * @return true if current weaving state supports showing waves
     */
    public boolean supportWaves() {
        return supportWaves;
    }

    /**
     * @return state id
     */
    public int getState() {
        return state;
    }

    public static AXWeavingState create(final int state,final Shader weavingShader,final float weavingScale){
        return new AXWeavingState(state) {

            @Override
            protected void init() {
                super.init();
                fixedScale = weavingScale;
            }

            @Override
            public Shader createShader() {
                return weavingShader;
            }
        };
    }

    public static AXWeavingState create(final int state,final Shader weavingShader){
        return new AXWeavingState(state) {

            @Override
            public Shader createShader() {
                return weavingShader;
            }
        };
    }
}
