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

import android.graphics.Shader;

public abstract class AXLineWeavingState extends AXWeavingState {

    public AXLineWeavingState(int state) {
        super(state);
    }

    protected void updateTargets() {
        targetX = 1.1f + 0.2f * (random.nextInt(100) / 100f);
        targetY = 4f * random.nextInt(100) / 100f;
    }

    public void setSize (float left,float top,float right,float bottom){
        this.width = (int) (bottom - top);
        this.height = (int) (right - left);
    }

    public static AXLineWeavingState create(final int state, final Shader weavingShader, final float weavingScale){
        return new AXLineWeavingState(state) {

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

    public static AXLineWeavingState create(final int state,final Shader weavingShader){
        return new AXLineWeavingState(state) {

            @Override
            public Shader createShader() {
                return weavingShader;
            }
        };
    }

}
