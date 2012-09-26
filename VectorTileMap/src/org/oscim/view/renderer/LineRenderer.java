/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.view.renderer;

import java.nio.ShortBuffer;

import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;
import android.util.FloatMath;
import android.util.Log;

class LineRenderer {
	private final static String TAG = "LineRenderer";

	private static int NUM_VERTEX_SHORTS = 4;

	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_TEX_OFFSET = 4;

	// shader handles
	private static int[] lineProgram = new int[2];
	private static int[] hLineVertexPosition = new int[2];
	private static int[] hLineTexturePosition = new int[2];
	private static int[] hLineColor = new int[2];
	private static int[] hLineMatrix = new int[2];
	private static int[] hLineScale = new int[2];
	private static int[] hLineWidth = new int[2];

	static boolean init() {
		lineProgram[0] = GlUtils.createProgram(Shaders.lineVertexShader,
				Shaders.lineFragmentShader);
		if (lineProgram[0] == 0) {
			Log.e(TAG, "Could not create line program.");
			return false;
		}

		hLineMatrix[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_mvp");
		hLineScale[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_wscale");
		hLineWidth[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_width");
		hLineColor[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_color");

		hLineVertexPosition[0] = GLES20.glGetAttribLocation(lineProgram[0], "a_position");
		hLineTexturePosition[0] = GLES20.glGetAttribLocation(lineProgram[0], "a_st");

		lineProgram[1] = GlUtils.createProgram(Shaders.lineVertexShader,
				Shaders.lineSimpleFragmentShader);
		if (lineProgram[1] == 0) {
			Log.e(TAG, "Could not create simple line program.");
			return false;
		}

		hLineMatrix[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_mvp");
		hLineScale[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_wscale");
		hLineWidth[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_width");
		hLineColor[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_color");

		hLineVertexPosition[1] = GLES20.glGetAttribLocation(lineProgram[1], "a_position");
		hLineTexturePosition[1] = GLES20.glGetAttribLocation(lineProgram[1], "a_st");

		return true;
	}

	// static int mSimple = 1;

	static LineLayer drawLines(MapTile tile, LineLayer layer, int next, float[] matrix,
			float div, double zoom, float scale, int mode) {
		// int mode = mSimple;

		if (layer == null)
			return null;

		// TODO should use fast line program when view is not tilted
		GLES20.glUseProgram(lineProgram[mode]);

		GLES20.glEnableVertexAttribArray(hLineVertexPosition[mode]);
		GLES20.glEnableVertexAttribArray(hLineTexturePosition[mode]);

		GLES20.glVertexAttribPointer(hLineVertexPosition[mode], 2, GLES20.GL_SHORT,
				false, 8, tile.lineOffset + LINE_VERTICES_DATA_POS_OFFSET);

		GLES20.glVertexAttribPointer(hLineTexturePosition[mode], 2, GLES20.GL_SHORT,
				false, 8, tile.lineOffset + LINE_VERTICES_DATA_TEX_OFFSET);

		GLES20.glUniformMatrix4fv(hLineMatrix[mode], 1, false, matrix, 0);

		// scale factor to map one pixel on tile to one pixel on screen:
		// only works with orthographic projection
		float s = scale / div;
		float pixel = 2.0f / s;

		if (mode == 0)
			pixel = 0;

		GLES20.glUniform1f(hLineScale[mode], pixel);

		// line scale factor (for non fixed lines)
		float lineScale = FloatMath.sqrt(s);
		float blurScale = pixel;
		boolean blur = false;
		// dont increase scale when max is reached
		boolean strokeMaxZoom = zoom > TileGenerator.STROKE_MAX_ZOOM_LEVEL;
		float width = 1;

		LineLayer l = layer;
		for (; l != null && l.layer < next; l = l.next) {

			Line line = l.line;
			if (line.fade != -1 && line.fade > zoom)
				continue;

			float alpha = 1.0f;

			if (line.fade >= zoom)
				alpha = (scale > 1.2f ? scale : 1.2f) - alpha;

			GlUtils.setColor(hLineColor[mode], line.color, alpha);

			if (blur && line.blur == 0) {
				GLES20.glUniform1f(hLineScale[mode], pixel);
				blur = false;
			}

			if (l.isOutline) {
				for (LineLayer o = l.outlines; o != null; o = o.outlines) {

					if (o.line.fixed || strokeMaxZoom) {
						width = (l.width + o.width) / s;
					} else {
						width = l.width / s + o.width / lineScale;
					}

					GLES20.glUniform1f(hLineWidth[mode], width);

					if (line.blur != 0) {
						blurScale = (l.width + o.width) / s - (line.blur / s);
						GLES20.glUniform1f(hLineScale[mode], blurScale);
						blur = true;
					}

					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			} else {

				if (line.fixed || strokeMaxZoom) {
					// invert scaling of extrusion vectors so that line width
					// stays the same.
					width = l.width / s;
				} else {
					width = l.width / lineScale;
				}

				GLES20.glUniform1f(hLineWidth[mode], width);

				if (line.blur != 0) {
					blurScale = (l.width / lineScale) * line.blur;
					GLES20.glUniform1f(hLineScale[mode], blurScale);
					blur = true;
				}

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}

		}

		GLES20.glDisableVertexAttribArray(hLineVertexPosition[mode]);
		GLES20.glDisableVertexAttribArray(hLineTexturePosition[mode]);

		return l;
	}

	static int sizeOf(LineLayer layers) {
		int size = 0;
		for (LineLayer l = layers; l != null; l = l.next)
			size += l.verticesCnt;

		size *= NUM_VERTEX_SHORTS;
		return size;
	}

	static void compileLayerData(LineLayer layers, ShortBuffer sbuf) {
		int pos = 0;
		VertexPoolItem last = null, items = null;

		for (LineLayer l = layers; l != null; l = l.next) {
			if (l.isOutline)
				continue;

			for (VertexPoolItem item = l.pool; item != null; item = item.next) {

				if (item.next == null) {
					sbuf.put(item.vertices, 0, item.used);
				} else {
					// item.used = VertexPoolItem.SIZE;
					sbuf.put(item.vertices);
				}

				last = item;
			}

			l.offset = pos;
			pos += l.verticesCnt;

			if (last != null) {
				last.next = items;
				items = l.pool;
			}

			l.pool = null;
			l.curItem = null;
		}

		VertexPool.add(items);
	}

	// @SuppressLint("UseValueOf")
	// private static final Boolean lock = new Boolean(true);
	// private static final int POOL_LIMIT = 1500;
	//
	// static private LineLayer pool = null;
	// static private int count = 0;
	// static private int countAll = 0;
	//
	// static void finish() {
	// synchronized (lock) {
	// count = 0;
	// countAll = 0;
	// pool = null;
	// }
	// }
	//
	// static LineLayer get(int layer, Line line, float width, boolean outline)
	// {
	// synchronized (lock) {
	//
	// if (count == 0 && pool == null) {
	// countAll++;
	// return new LineLayer(layer, line, width, outline);
	// }
	// if (count > 0) {
	// count--;
	// } else {
	// int c = 0;
	// LineLayer tmp = pool;
	//
	// while (tmp != null) {
	// c++;
	// tmp = tmp.next;
	// }
	//
	// Log.d("LineLayersl", "eek wrong count: " + c + " left");
	// }
	//
	// LineLayer it = pool;
	// pool = pool.next;
	// it.next = null;
	// it.layer = layer;
	// it.line = line;
	// it.isOutline = outline;
	// it.width = width;
	// return it;
	// }
	// }
	//
	// static void add(LineLayer layers) {
	// if (layers == null)
	// return;
	//
	// synchronized (lock) {
	//
	// // limit pool items
	// if (countAll < POOL_LIMIT) {
	// LineLayer last = layers;
	//
	// while (true) {
	// count++;
	//
	// if (last.next == null)
	// break;
	//
	// last = last.next;
	// }
	//
	// last.next = pool;
	// pool = layers;
	//
	// } else {
	// int cleared = 0;
	// LineLayer prev, tmp = layers;
	// while (tmp != null) {
	// prev = tmp;
	// tmp = tmp.next;
	//
	// countAll--;
	// cleared++;
	//
	// prev.next = null;
	//
	// }
	// Log.d("LineLayers", "sum: " + countAll + " free: " + count + " freed "
	// + cleared);
	// }
	//
	// }
	// }
	//
	static void clear(LineLayer layer) {
		for (LineLayer l = layer; l != null; l = l.next) {
			if (l.pool != null) {
				VertexPool.add(l.pool);
				l.pool = null;
				l.curItem = null;
			}
		}
		// LineLayers.add(layer);
	}
}