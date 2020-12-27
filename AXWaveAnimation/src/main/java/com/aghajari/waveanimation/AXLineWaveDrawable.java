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
import android.graphics.Paint;

public class AXLineWaveDrawable extends AXWaveDrawable {

    protected float lineSpeedScale;
    protected float deltaLeft;
    protected float deltaTop;
    protected float deltaRight;
    protected float deltaBottom;

    public AXLineWaveDrawable(int n,float lineSpeedScale) {
        super(n);
        this.lineSpeedScale = lineSpeedScale;
    }

    @Override
    void init(int n) {
        N = n;
        radius = new float[n + 1];

        radiusNext = new float[n + 1];
        progress = new float[n + 1];
        speed = new float[n + 1];

        for (int i = 0; i <= N; i++) {
            generateBlob(radius,null, i);
            generateBlob(radiusNext,null, i);
            progress[i] = 0;
        }
    }

    @Override
    protected void generateBlob(float[] radius, float[] angle, int i) {
        float radDif = maxRadius - minRadius;
        radius[i] = minRadius + Math.abs(((random.nextInt() % 100f) / 100f)) * radDif;
        speed[i] = (float) (0.017 + 0.003 * (Math.abs(random.nextInt() % 100f) / 100f));
    }

    @Override
    public void update(float amplitude, float speedScale) {
        final float s = speedScale == -1 ? lineSpeedScale : speedScale;
        for (int i = 0; i <= N; i++) {
            progress[i] += (speed[i] * minSpeed) + amplitude * speed[i] * maxSpeed * s;
            if (progress[i] >= 1f) {
                progress[i] = 0;
                radius[i] = radiusNext[i];
                generateBlob(radiusNext,null,i);
            }
        }
    }

    @Override
    public void draw(float cX, float cY, Canvas canvas, Paint paint) {}

    public void draw(float left, float top, float right, float bottom, Canvas canvas, Paint paint, float amplitude) {
        realDraw(left+deltaLeft,bottom - top - deltaTop,right+deltaRight,bottom - deltaBottom,canvas,paint,0,1f);
    }

    private void realDraw(float left, float top, float right, float bottom, Canvas canvas, Paint paint, float  pinnedTop, float progressToPinned) {
        path.reset();

        path.moveTo(right, bottom);
        path.lineTo(left, bottom);

        for (int i = 0; i <= N; i++) {
            if (i == 0) {
                float progress = this.progress[i];
                float r1 = radius[i] * (1f - progress) + radiusNext[i] * progress;
                float y = (top - r1) * progressToPinned + pinnedTop * (1f - progressToPinned);
                path.lineTo(left, y);
            } else {
                float progress = this.progress[i - 1];
                float r1 = radius[i - 1] * (1f - progress) + radiusNext[i - 1] * progress;
                float progressNext = this.progress[i];
                float r2 = radius[i] * (1f - progressNext) + radiusNext[i] * progressNext;
                float x1 = (right - left) / N * (i - 1);
                float x2 = (right - left) / N * i;
                float cx = x1 + (x2 - x1) / 2;

                float y1 = (top - r1) * progressToPinned + pinnedTop * (1f - progressToPinned);
                float y2 = (top - r2) * progressToPinned + pinnedTop * (1f - progressToPinned);
                path.cubicTo(
                        cx, y1,
                        cx, y2,
                        x2, y2
                );
                if (i == N) {
                    path.lineTo(right, bottom);
                }
            }
        }

        canvas.drawPath(path, paint);
    }

    public void generateBlob() {
        for (int i = 0; i < N; i++) {
            generateBlob(radius, null, i);
            generateBlob(radiusNext,null, i);
            progress[i] = 0;
        }
    }

    protected void updateLine (float amplitude,float maxHeight){}
}
