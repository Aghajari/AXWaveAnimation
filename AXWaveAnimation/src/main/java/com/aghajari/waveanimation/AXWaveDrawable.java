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

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

public class AXWaveDrawable {

    protected float maxSpeed = 8.2f;
    protected float minSpeed = 0.8f;

    protected float minRadius = -1f;
    protected float maxRadius = -1f;
    protected boolean autoMin = true;
    protected boolean autoMax = true;
    protected boolean firstDraw = true;

    protected Path path = new Path();

    protected float[] radius;
    protected float[] angle;
    protected float[] radiusNext;
    protected float[] angleNext;
    protected float[] progress;
    protected float[] speed;

    private float[] pointStart = new float[4];
    private float[] pointEnd = new float[4];

    final Random random = new Random();

    protected int N;
    protected float L;

    protected final Matrix m = new Matrix();

    public AXWaveDrawable(int n) {
        init(n);
    }

    void init(int n) {
        N = n;
        L = (float) ((4.0 / 3.0) * Math.tan(Math.PI / (2 * N)));
        radius = new float[n];
        angle = new float[n];

        radiusNext = new float[n];
        angleNext = new float[n];
        progress = new float[n];
        speed = new float[n];

        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }

    protected void generateBlob(float[] radius, float[] angle, int i) {
        float angleDif = 360f / N * 0.05f;
        float radDif = maxRadius - minRadius;
        radius[i] = minRadius + Math.abs(((random.nextInt() % 100f) / 100f)) * radDif;
        angle[i] = 360f / N * i + ((random.nextInt() % 100f) / 100f) * angleDif;
        speed[i] = (float) (0.017 + 0.003 * (Math.abs(random.nextInt() % 100f) / 100f));
    }

    public void update(float amplitude, float speedScale) {
        for (int i = 0; i < N; i++) {
            progress[i] += (speed[i] * minSpeed) + amplitude * speed[i] * maxSpeed * speedScale;
            if (progress[i] >= 1f) {
                progress[i] = 0;
                radius[i] = radiusNext[i];
                angle[i] = angleNext[i];
                generateBlob(radiusNext, angleNext, i);
            }
        }
    }

    public void draw(float cX, float cY, Canvas canvas, Paint paint) {
        path.reset();
        firstDraw = false;

        for (int i = 0; i < N; i++) {
            float progress = this.progress[i];
            int nextIndex = i + 1 < N ? i + 1 : 0;
            float progressNext = this.progress[nextIndex];
            float r1 = radius[i] * (1f - progress) + radiusNext[i] * progress;
            float r2 = radius[nextIndex] * (1f - progressNext) + radiusNext[nextIndex] * progressNext;
            float angle1 = angle[i] * (1f - progress) + angleNext[i] * progress;
            float angle2 = angle[nextIndex] * (1f - progressNext) + angleNext[nextIndex] * progressNext;

            float l = L * (Math.min(r1, r2) + (Math.max(r1, r2) - Math.min(r1, r2)) / 2f);
            m.reset();
            m.setRotate(angle1, cX, cY);

            pointStart[0] = cX;
            pointStart[1] = cY - r1;
            pointStart[2] = cX + l;
            pointStart[3] = cY - r1;

            m.mapPoints(pointStart);

            pointEnd[0] = cX;
            pointEnd[1] = cY - r2;
            pointEnd[2] = cX - l;
            pointEnd[3] = cY - r2;

            m.reset();
            m.setRotate(angle2, cX, cY);

            m.mapPoints(pointEnd);

            if (i == 0) {
                path.moveTo(pointStart[0], pointStart[1]);
            }

            path.cubicTo(
                    pointStart[2], pointStart[3],
                    pointEnd[2], pointEnd[3],
                    pointEnd[0], pointEnd[1]
            );
        }

        canvas.save();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void generateBlob() {
        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }

    protected void update() {};

    public float getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
        autoMax = maxRadius<0;
    }

    void setMaxRadius(float maxRadius,boolean auto) {
        this.maxRadius = maxRadius;
        autoMax = auto;
    }

    public float getMinRadius() {
        return minRadius;
    }

    public void setMinRadius(float minRadius) {
        this.minRadius = minRadius;
        autoMin = minRadius<0;
    }

    void setMinRadius(float minRadius,boolean auto) {
        this.minRadius = minRadius;
        autoMin = auto;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public float getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(float minSpeed) {
        this.minSpeed = minSpeed;
    }

    public int getBlobCount() {
        return N;
    }

    public void getBlobCount(int count) {
        init(count);
    }
}
