/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.utils;

import org.oscim.renderer.GLRenderer;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Utility functions
 */
public class GlUtils {
	private static String TAG = "GlUtils";

	/**
	 * @param bitmap
	 *            ...
	 * @return gl identifier
	 */
	public static int loadTextures(Bitmap bitmap) {

		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);

		int textureID = textures[0];
		// Log.i(TAG, "new texture " + textureID + " " + textureCnt++);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_LINEAR);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR);

		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);

		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		return textureID;
	}

	/**
	 * @param shaderType
	 *            shader type
	 * @param source
	 *            shader code
	 * @return gl identifier
	 */
	public static int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

	/**
	 * @param vertexSource
	 *            ...
	 * @param fragmentSource
	 *            ...
	 * @return gl identifier
	 */
	public static int createProgram(String vertexSource, String fragmentSource) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}

		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			checkGlError("glCreateProgram");
			GLES20.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			checkGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}

	/**
	 * @param op
	 *            ...
	 */
	public static void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			// throw new RuntimeException(op + ": glError " + error);
		}
	}

	public static boolean checkGlOutOfMemory(String op) {
		int error;
		boolean oom = false;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			// throw new RuntimeException(op + ": glError " + error);
			if (error == 1285)
				oom = true;
		}
		return oom;
	}

	// this is save as it can only be called from glThread
	private static float[] tmpColor = new float[4];

	public static void setBlendColors(int handle, float[] c1, float[] c2, float alpha) {
		tmpColor[0] = c1[0] * (1 - alpha) + c2[0] * alpha;
		tmpColor[1] = c1[1] * (1 - alpha) + c2[1] * alpha;
		tmpColor[2] = c1[2] * (1 - alpha) + c2[2] * alpha;
		tmpColor[3] = c1[3] * (1 - alpha) + c2[3] * alpha;

		GLES20.glUniform4fv(handle, 1, tmpColor, 0);
	}

	public static void setColor(int handle, float[] c, float alpha) {
		if (alpha >= 1)
			GLES20.glUniform4fv(handle, 1, c, 0);
		else {
			tmpColor[0] = c[0] * alpha;
			tmpColor[1] = c[1] * alpha;
			tmpColor[2] = c[2] * alpha;
			tmpColor[3] = c[3] * alpha;

			GLES20.glUniform4fv(handle, 1, tmpColor, 0);
		}
	}

	public static float[] colorToFloat(int color) {
		float[] c = new float[4];
		c[3] = (color >> 24 & 0xff) / 255.0f;
		c[0] = (color >> 16 & 0xff) / 255.0f;
		c[1] = (color >> 8 & 0xff) / 255.0f;
		c[2] = (color >> 0 & 0xff) / 255.0f;
		return c;
	}

	// premultiply alpha
	public static float[] colorToFloatP(int color) {
		float[] c = new float[4];
		c[3] = (color >> 24 & 0xff) / 255.0f;
		c[0] = (color >> 16 & 0xff) / 255.0f * c[3];
		c[1] = (color >> 8 & 0xff) / 255.0f * c[3];
		c[2] = (color >> 0 & 0xff) / 255.0f * c[3];
		return c;
	}

	private final static float[] mIdentity = {
			1, 0, 0, 0,
			0, 1, 0, 0,
			0, 0, 1, 0,
			0, 0, 0, 1 };

	public static void setTileMatrix(float[] matrix, float tx, float ty, float s) {
		System.arraycopy(mIdentity, 0, matrix, 0, 16);
		// scale tile relative to map scale
		matrix[0] = matrix[5] = s / GLRenderer.COORD_MULTIPLIER;
		// translate relative to map center
		matrix[12] = tx * s;
		matrix[13] = ty * s;
	}

	public static void setTranslation(float[] matrix, float x, float y, float z) {
		System.arraycopy(mIdentity, 0, matrix, 0, 16);
		matrix[12] = x;
		matrix[13] = y;
		matrix[14] = z;
	}

	public static void setMatrix(float[] matrix, float tx, float ty, float scale) {
		System.arraycopy(mIdentity, 0, matrix, 0, 16);
		matrix[12] = tx;
		matrix[13] = ty;
		matrix[0] = scale;
		matrix[5] = scale;
		//matrix[10] = scale;
	}

	public static void setIdentity(float[] matrix) {
		System.arraycopy(mIdentity, 0, matrix, 0, 16);
	}

	public static void setScaleM(float[] matrix, float sx, float sy, float sz) {
		System.arraycopy(mIdentity, 0, matrix, 0, 16);
		matrix[0] = sx;
		matrix[5] = sy;
		matrix[10] = sz;
	}
}
