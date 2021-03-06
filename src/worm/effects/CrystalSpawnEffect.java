/*
 * Copyright (c) 2003-onwards Shaven Puppy Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'Shaven Puppy' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package worm.effects;

import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.widgets.Beam;
import net.puppygames.applet.widgets.Ring;

import org.lwjgl.util.Color;

import worm.CrystalResource;
import worm.Layers;
import worm.Res;
import worm.SFX;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.GLRenderable;

import static org.lwjgl.opengl.GL11.*;

/**
 * Special effect highlighting where a saucer has appeared
 */
public class CrystalSpawnEffect extends Effect {

	public static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/** Duration */
	private static final int RES_IN_DURATION = 240;
	private static final int WAIT_DURATION = 300;
	private static final int RES_OUT_DURATION = 60;

	private static final float ALPHA = 0.5f;
	private static final float OUTER_ALPHA_MULT = 0.75f;

	/** Size of the outer circle */
	private static final float OUTER_START_SIZE = 2048.0f;

	private static final float OUTER_END_SIZE = 32.0f;

	private static final float INNER_SIZE = 128.0f;

	/** Speed the inner circle rotates at */
	private static final float INNER_ROTATION = 0.25f;

	/** Speed the outer circle rotates at */
	private static final float OUTER_ROTATION = -0.25f;

	/** Length of crosshair lines */
	private static final float LINE_LENGTH = 8.0f;

	/** line widths */
	private static final float LINE_WIDTH =  2.0f;

	/** Length of crosshair lines on inner circle */
	private static final float INNER_LINE_RADIUS = INNER_SIZE - LINE_LENGTH;

	/** Tick */
	private int tick;

	/** Outer circle angle */
	private float outerAngle;

	/** Inner circle angle */
	private float innerAngle;

	/** The saucer we are targeting on */
	private CrystalResource crystal;

	/** Phase */
	private int phase;
	private static final int PHASE_RES_IN = 0;
	private static final int PHASE_WAIT = 1;
	private static final int PHASE_RES_OUT = 2;

	private final Ring outerRing = new Ring();
	private final Ring innerRing = new Ring();
	private final Beam beam = new Beam();

	private transient float x, y, alpha, radius;


	/**
	 * C'tor
	 * @param crystal
	 */
	public CrystalSpawnEffect(CrystalResource crystal) {
		this.crystal = crystal;
		innerRing.setColor(new Color(0, 255, 173));
		innerRing.setThickness(LINE_WIDTH);
		innerRing.setDash(16.0f);
		outerRing.setColor(new Color(0, 255, 173));
		outerRing.setThickness(LINE_WIDTH);
	}

	@Override
	protected void render() {

		float lineRadius = radius + LINE_LENGTH;

		// Draw outer circle
		glRender(new GLRenderable() {
			@Override
			public void render() {
//				glEnable(GL_STENCIL_TEST);
//				glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
//				glStencilFunc(GL_NOTEQUAL, 1, 1);
				glEnable(GL_BLEND);
				glEnable(GL_TEXTURE_2D);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE);
				glPushMatrix();
				glTranslatef(getOffset().getX() + x, getOffset().getY() + y, 0.0f);
				Res.getSolidTexture().render();
			}
		});

		outerRing.setRadius(radius);
		outerRing.setAlpha((int) (255.0f * alpha));
		outerRing.render(this);

		// Draw crosshair lines at the edge of the circle
		glRender(new GLRenderable() {
			@Override
			public void render() {
				glRotatef(outerAngle, 0.0f, 0.0f, 1.0f);
				Res.getBeamTexture().render();
			}
		});
		beam.setWidth(LINE_WIDTH + 1.0f);
		for (int i = 0; i < 32; i += 8) {
			beam.setLocation((float) Math.cos(i * Math.PI / 16.0) * (radius + 0.5f), (float) Math.sin(i * Math.PI / 16.0) * (radius + 0.5f), (float) Math.cos(i * Math.PI / 16.0) * lineRadius, (float) Math.sin(i * Math.PI / 16.0) * lineRadius);
			beam.render(this);
		}

		glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
				glPushMatrix();
				glTranslatef(getOffset().getX() + x, getOffset().getY() + y, 0.0f);
				glRotatef(innerAngle, 0.0f, 0.0f, 1.0f);
				Res.getDashTexture().render();
			}
		});

		innerRing.setRadius(INNER_SIZE);
		innerRing.setAlpha((int) (255.0f * alpha * OUTER_ALPHA_MULT));
		innerRing.render(this);

		glRender(new GLRenderable() {
			@Override
			public void render() {
				Res.getBeamTexture().render();
			}
		});

		for (int i = 0; i < 32; i += 4) {
			beam.setLocation((float) Math.cos(i * Math.PI / 16.0) * (INNER_SIZE - LINE_WIDTH - 0.5f), (float) Math.sin(i * Math.PI / 16.0) * (INNER_SIZE - LINE_WIDTH - 0.5f), (float) Math.cos(i * Math.PI / 16.0) * INNER_LINE_RADIUS, (float) Math.sin(i * Math.PI / 16.0) * INNER_LINE_RADIUS);
			beam.render(this);
		}

		glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
//				glDisable(GL_STENCIL_TEST);
			}
		});
	}

	@Override
	public int getDefaultLayer() {
		return Layers.BUILDING_INFO;
	}

	@Override
	protected void doTick() {
		innerAngle += INNER_ROTATION;
		outerAngle += OUTER_ROTATION;
		x = crystal.getMapX() + crystal.getCollisionX();
		y = crystal.getMapY() + crystal.getCollisionY();
		switch (phase) {
			case PHASE_RES_IN:
				tick ++;
				radius = LinearInterpolator.instance.interpolate(OUTER_START_SIZE, OUTER_END_SIZE, tick / (float) RES_IN_DURATION);
				alpha = LinearInterpolator.instance.interpolate(0.0f, ALPHA, tick / (float) RES_IN_DURATION);
				if (tick == RES_IN_DURATION) {
					SFX.crystalSpawned(x, y);
				}
				if (crystal.isReady() && tick > RES_IN_DURATION) {
					phase = PHASE_WAIT;
					tick = 0;
				}
				break;
			case PHASE_WAIT:
				tick ++;
				radius = OUTER_END_SIZE;
				alpha = ALPHA;
				if (tick == WAIT_DURATION) {
					phase = PHASE_RES_OUT;
					tick = 0;
				}
				break;
			case PHASE_RES_OUT:
				tick ++;
				radius = LinearInterpolator.instance.interpolate(OUTER_END_SIZE, OUTER_START_SIZE, tick / (float) RES_OUT_DURATION);
				alpha = LinearInterpolator.instance.interpolate(ALPHA, 0.0f, tick / (float) RES_OUT_DURATION);
				break;
			default:
				assert false;
				radius = 0.0f;
				alpha = 0.0f;
		}
	}

	@Override
	public boolean isEffectActive() {
		return !(phase == PHASE_RES_OUT && tick == RES_OUT_DURATION);
	}


}