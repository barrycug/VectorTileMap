/*
 * Copyright 2012 OpenScienceMap
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer;

import static android.opengl.GLES20.GL_SHORT;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.oscim.core.MapPosition;
import org.oscim.generator.TileGenerator;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */

public final class LineRenderer {
	private final static String TAG = "LineRenderer";

	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;
	//private static final int LINE_VERTICES_DATA_TEX_OFFSET = 4;

	// shader handles
	private static int[] lineProgram = new int[2];
	private static int[] hLineVertexPosition = new int[2];
	//private static int[] hLineTexturePosition = new int[2];
	private static int[] hLineColor = new int[2];
	private static int[] hLineMatrix = new int[2];
	private static int[] hLineScale = new int[2];
	private static int[] hLineWidth = new int[2];
	private static int[] hLineMode = new int[2];
	private static int mTexID;

	static boolean init() {
		lineProgram[0] = GlUtils.createProgram(lineVertexShader,
				lineFragmentShader);
		if (lineProgram[0] == 0) {
			Log.e(TAG, "Could not create line program.");
			return false;
		}

		lineProgram[1] = GlUtils.createProgram(lineVertexShader,
				lineSimpleFragmentShader);
		if (lineProgram[1] == 0) {
			Log.e(TAG, "Could not create simple line program.");
			return false;
		}

		for (int i = 0; i < 2; i++) {
			hLineMatrix[i] = glGetUniformLocation(lineProgram[i], "u_mvp");
			hLineScale[i] = glGetUniformLocation(lineProgram[i], "u_wscale");
			hLineWidth[i] = glGetUniformLocation(lineProgram[i], "u_width");
			hLineColor[i] = glGetUniformLocation(lineProgram[i], "u_color");
			hLineMode[i] = glGetUniformLocation(lineProgram[i], "u_mode");
			hLineVertexPosition[i] = glGetAttribLocation(lineProgram[i], "a_pos");
			//hLineTexturePosition[i] = glGetAttribLocation(lineProgram[i], "a_st");
		}

		byte[] pixel = new byte[128 * 128];

		for (int x = 0; x < 128; x++) {
			float xx = x * x;
			for (int y = 0; y < 128; y++) {
				float yy = y * y;
				int color = (int) (Math.sqrt(xx + yy) * 2);
				if (color > 255)
					color = 255;
				pixel[x + y * 128] = (byte) color;
				//pixel[(127 - x) + (127 - y) * 128] = (byte) color;
			}
		}

		int[] textureIds = new int[1];
		GLES20.glGenTextures(1, textureIds, 0);
		mTexID = textureIds[0];

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexID);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_MIRRORED_REPEAT); // Set U Wrapping
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_MIRRORED_REPEAT); // Set V Wrapping

		ByteBuffer buf = ByteBuffer.allocateDirect(128 * 128).order(ByteOrder.nativeOrder());
		buf.put(pixel);
		buf.position(0);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, 128, 128, 0, GLES20.GL_ALPHA,
				GLES20.GL_UNSIGNED_BYTE, buf);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		return true;
	}

	public static void beginLines() {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexID);
	}

	public static void endLines() {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
	}

	public static Layer draw(MapPosition pos, Layer layer, float[] matrix, float div,
			int mode, int bufferOffset) {

		int zoom = pos.zoomLevel;
		float scale = pos.scale;

		if (layer == null)
			return null;

		glUseProgram(lineProgram[mode]);

		int uLineScale = hLineScale[mode];
		int uLineMode = hLineMode[mode];
		int uLineColor = hLineColor[mode];
		int uLineWidth = hLineWidth[mode];

		GLState.enableVertexArrays(hLineVertexPosition[mode], -1); // hLineTexturePosition[mode]);

		glVertexAttribPointer(hLineVertexPosition[mode], 4, GL_SHORT,
				false, 0, bufferOffset + LINE_VERTICES_DATA_POS_OFFSET);

		//		glVertexAttribPointer(hLineTexturePosition[mode], 2, GL_SHORT,
		//				false, 8, bufferOffset + LINE_VERTICES_DATA_TEX_OFFSET);

		glUniformMatrix4fv(hLineMatrix[mode], 1, false, matrix, 0);

		// scale factor to map one pixel on tile to one pixel on screen:
		// only works with orthographic projection
		float s = scale / div;
		float pixel = 0;

		if (mode == 1)
			pixel = 1.5f / s;

		glUniform1f(uLineScale, pixel);
		int lineMode = 0;
		glUniform1i(uLineMode, lineMode);

		// line scale factor (for non fixed lines)
		float lineScale = (float) Math.sqrt(s);
		float blurScale = pixel;
		boolean blur = false;
		// dont increase scale when max is reached
		boolean strokeMaxZoom = zoom > TileGenerator.STROKE_MAX_ZOOM_LEVEL;

		Layer l = layer;
		for (; l != null && l.type == Layer.LINE; l = l.next) {
			LineLayer ll = (LineLayer) l;
			Line line = ll.line;
			float width;

			if (line.fade != -1 && line.fade > zoom)
				continue;

			float alpha = 1.0f;

			if (line.fade >= zoom)
				alpha = (scale > 1.2f ? scale : 1.2f) - alpha;

			GlUtils.setColor(uLineColor, line.color, alpha);

			if (mode == 0 && blur && line.blur == 0) {
				glUniform1f(uLineScale, 0);
				blur = false;
			}

			if (line.outline) {
				// draw outline for linelayers references by this outline
				for (LineLayer o = ll.outlines; o != null; o = o.outlines) {

					if (o.line.fixed || strokeMaxZoom) {
						width = (ll.width + o.width) / s;
					} else {
						width = ll.width / s + o.width / lineScale;
					}

					glUniform1f(uLineWidth, width);

					if (line.blur != 0) {
						//blurScale = (ll.width + o.width) / s - (line.blur / s);
						blurScale = 1 - (line.blur / s);
						glUniform1f(uLineScale, blurScale);
						blur = true;
					} else if (mode == 1) {
						glUniform1f(uLineScale, pixel / width);
					}

					if (o.roundCap) {
						if (lineMode != 1) {
							lineMode = 1;
							glUniform1i(uLineMode, lineMode);
						}
					} else if (lineMode != 0) {
						lineMode = 0;
						glUniform1i(uLineMode, lineMode);
					}

					glDrawArrays(GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			} else {

				if (line.fixed || strokeMaxZoom) {
					// invert scaling of extrusion vectors so that line width
					// stays the same.
					width = ll.width / s;
				} else {
					width = ll.width / lineScale;
				}

				glUniform1f(uLineWidth, width);

				if (line.blur != 0) {
					//blurScale = (ll.width / lineScale) * line.blur;
					blurScale = line.blur;
					glUniform1f(uLineScale, blurScale);
					blur = true;
				} else if (mode == 1) {
					glUniform1f(uLineScale, pixel / width);
				}

				if (ll.roundCap) {
					if (lineMode != 1) {
						lineMode = 1;
						glUniform1i(uLineMode, lineMode);
					}
				} else if (lineMode != 0) {
					lineMode = 0;
					glUniform1i(uLineMode, lineMode);
				}

				glDrawArrays(GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}

		return l;
	}

	private final static String lineVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform float u_width;"
			+ "attribute vec4 a_pos;"
			+ "uniform int u_mode;"
			+ "varying vec2 v_st;"
			+ "const float dscale = 8.0/2048.0;"
			+ "void main() {"
			// scale extrusion to u_width pixel
			// just ignore the two most insignificant bits of a_st :)
			+ "  vec2 dir = a_pos.zw;"
			+ "  gl_Position = u_mvp * vec4(a_pos.xy + (dscale * u_width * dir), 0.0, 1.0);"
			// last two bits of a_st hold the texture coordinates
			// ..maybe one could wrap texture so that `abs` is not required
			+ "  v_st = abs(mod(dir, 4.0)) - 1.0;"
			+ "}";

	private final static String lineSimpleFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ "uniform int u_mode;"
			+ "uniform float u_width;"
			+ "uniform float u_wscale;"
			+ "uniform vec4 u_color;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len;"
			+ "  if (u_mode == 0)"
			+ "    len = abs(v_st.s);"
			+ "  else"
			+ "    len = texture2D(tex, v_st).a;"
			// interpolate alpha between: 0.0 < 1.0 - len < u_wscale
			// where wscale is 'filter width' / 'line width' and 0 <= len <= sqrt(2) 
			+ "  gl_FragColor = u_color * smoothstep(0.0, u_wscale, 1.0 - len);"
			//+ "  gl_FragColor = u_color * min(1.0, (1.0 - len) / u_wscale);"
			+ "}";

	private final static String lineFragmentShader = ""
			+ "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ "uniform int u_mode;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_width;"
			+ "uniform float u_wscale;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len;"
			+ "  float fuzz;"
			+ "  if (u_mode == 0){"
			+ "    len = abs(v_st.s);"
			+ "    fuzz = fwidth(v_st.s);"
			+ "  } else {"
			+ "    len = texture2D(tex, v_st).a;"
			//+ "  len = length(v_st);"
			+ "    vec2 st_width = fwidth(v_st);"
			+ "    fuzz = max(st_width.s, st_width.t);"
			+ "  }"
			// smoothstep is too sharp, guess one could increase extrusion with z.. 
			// but this looks ok too:
			+ "  gl_FragColor = u_color * min(1.0, (1.0 - len) / (u_wscale + fuzz));"
			//+ "  gl_FragColor = u_color * smoothstep(0.0, fuzz + u_wscale, 1.0 - len);"
			+ "}";

	//	private final static String lineVertexShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform mat4 u_mvp;"
	//			+ "uniform float u_width;"
	//			+ "attribute vec4 a_pos;"
	//			+ "uniform int u_mode;"
	//			//+ "attribute vec2 a_st;"
	//			+ "varying vec2 v_st;"
	//			+ "const float dscale = 8.0/2048.0;"
	//			+ "void main() {"
	//			// scale extrusion to u_width pixel
	//			// just ignore the two most insignificant bits of a_st :)
	//			+ "  vec2 dir = a_pos.zw;"
	//			+ "  gl_Position = u_mvp * vec4(a_pos.xy + (dscale * u_width * dir), 0.0, 1.0);"
	//			// last two bits of a_st hold the texture coordinates
	//			+ "  v_st = u_width * (abs(mod(dir, 4.0)) - 1.0);"
	//			// use bit operations when available (gles 1.3)
	//			// + "  v_st = u_width * vec2(a_st.x & 3 - 1, a_st.y & 3 - 1);"
	//			+ "}";
	//
	//	private final static String lineSimpleFragmentShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform float u_wscale;"
	//			+ "uniform float u_width;"
	//			+ "uniform int u_mode;"
	//			+ "uniform vec4 u_color;"
	//			+ "varying vec2 v_st;"
	//			+ "void main() {"
	//			+ "  float len;"
	//			+ "  if (u_mode == 0)"
	//			+ "    len = abs(v_st.s);"
	//			+ "  else "
	//			+ "    len = length(v_st);"
	//			// fade to alpha. u_wscale is the width in pixel which should be
	//			// faded, u_width - len the position of this fragment on the
	//			// perpendicular to this line segment. this only works with no
	//			// perspective
	//			//+ "  gl_FragColor = min(1.0, (u_width - len) / u_wscale) * u_color;"
	//			+ "  gl_FragColor = u_color * smoothstep(0.0, u_wscale, (u_width - len));"
	//			+ "}";
	//
	//	private final static String lineFragmentShader = ""
	//			+ "#extension GL_OES_standard_derivatives : enable\n"
	//			+ "precision mediump float;"
	//			+ "uniform float u_wscale;"
	//			+ "uniform float u_width;"
	//			+ "uniform int u_mode;"
	//			+ "uniform vec4 u_color;"
	//			+ "varying vec2 v_st;"
	//			+ "void main() {"
	//			+ "  float len;"
	//			+ "  float fuzz;"
	//			+ "  if (u_mode == 0){"
	//			+ "    len = abs(v_st.s);"
	//			+ "    fuzz = u_wscale + fwidth(v_st.s);"
	//			+ "  } else {"
	//			+ "    len = length(v_st);"
	//			+ "    vec2 st_width = fwidth(v_st);"
	//			+ "    fuzz = u_wscale + max(st_width.s, st_width.t);"
	//			+ "  }"
	//			+ "  gl_FragColor = u_color * min(1.0, (u_width - len) / fuzz);"
	//			+ "}";
}
