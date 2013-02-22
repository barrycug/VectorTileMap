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
package org.oscim.theme.renderinstruction;

import java.util.Locale;
import java.util.regex.Pattern;

import org.oscim.core.Tag;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.utils.GlUtils;
import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint.Cap;

/**
 * Represents a polyline on the map.
 */
public final class Line extends RenderInstruction {
	private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	/**
	 * @param line
	 *            ...
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @param isOutline
	 *            ...
	 * @return a new Line with the given rendering attributes.
	 */
	public static Line create(Line line, String elementName, Attributes attributes,
			int level, boolean isOutline) {
		String src = null;
		int stroke = Color.BLACK;
		float strokeWidth = 0;
		int stipple = 0;
		Cap strokeLinecap = Cap.ROUND;
		int fade = -1;
		boolean fixed = false;
		String style = null;
		float blur = 0;
		float min = 0;

		if (line != null) {
			fixed = line.fixed;
			fade = line.fade;
			strokeLinecap = line.cap;
			blur = line.blur;
			min = line.min;
		}
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("name".equals(name))
				style = value;
			else if ("src".equals(name)) {
				src = value;
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("stipple".equals(name)) {
				stipple = Integer.parseInt(value);
			} else if ("cap".equals(name)) {
				strokeLinecap = Cap.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("fade".equals(name)) {
				fade = Integer.parseInt(value);
			} else if ("min".equals(name)) {
				min = Float.parseFloat(value);
			} else if ("fixed".equals(name)) {
				fixed = Boolean.parseBoolean(value);
			} else if ("blur".equals(name)) {
				blur = Float.parseFloat(value);
			} else if ("from".equals(name)) {
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		if (stipple != 0)
			strokeLinecap = Cap.BUTT;

		if (line != null) {

			strokeWidth = line.width + strokeWidth;
			if (strokeWidth <= 0)
				strokeWidth = 1;

			return new Line(line, style, src, stroke, strokeWidth, stipple,
					strokeLinecap, level, fixed, fade, blur, isOutline, min);
		}

		if (!isOutline)
			validate(strokeWidth);

		return new Line(style, src, stroke, strokeWidth, stipple, strokeLinecap,
				level, fixed, fade, blur, isOutline, min);
	}

	public Line(int stroke, float width, Cap cap) {
		this.level = 0;
		this.blur = 0;
		this.cap = cap;
		this.outline = false;
		this.style = "";
		this.width = width;
		this.fixed = true;
		this.fade = -1;
		this.stipple = 2;
		this.min = 0;
		color = GlUtils.colorToFloatP(stroke);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("width must not be negative: "
					+ strokeWidth);
		}
	}

	static float[] parseFloatArray(String dashString) {
		String[] dashEntries = SPLIT_PATTERN.split(dashString);
		float[] dashIntervals = new float[dashEntries.length];
		for (int i = 0; i < dashEntries.length; ++i) {
			dashIntervals[i] = Float.parseFloat(dashEntries[i]);
		}
		return dashIntervals;
	}

	private final int level;

	public final float width;

	// public final boolean round;

	public final float color[];

	public final boolean outline;

	public final boolean fixed;

	public final int fade;

	public final String style;

	public final Cap cap;

	public final float blur;

	public final int stipple;

	public final float min;

	/**
	 * @param style
	 *            ...
	 * @param src
	 *            ...
	 * @param stroke
	 *            ...
	 * @param strokeWidth
	 *            ...
	 * @param stipple
	 *            ...
	 * @param strokeLinecap
	 *            ...
	 * @param level
	 *            ...
	 * @param fixed
	 *            ...
	 * @param fade
	 *            ...
	 * @param blur
	 *            ...
	 * @param isOutline
	 *            ...
	 * @param min ...
	 */
	private Line(String style, String src, int stroke, float strokeWidth,
			int stipple, Cap strokeLinecap, int level, boolean fixed,
			int fade, float blur, boolean isOutline, float min) {
		super();

		this.style = style;

		// paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//
		// if (src != null) {
		// Shader shader = BitmapUtils.createBitmapShader(src);
		// paint.setShader(shader);
		// }
		//
		// paint.setStyle(Style.STROKE);
		// paint.setColor(stroke);
		// if (strokeDasharray != null) {
		// paint.setPathEffect(new DashPathEffect(strokeDasharray, 0));
		// }
		// paint.setStrokeCap(strokeLinecap);

		// round = (strokeLinecap == Cap.ROUND);

		this.cap = strokeLinecap;

		color = GlUtils.colorToFloatP(stroke);

		this.width = strokeWidth;
		this.level = level;
		this.outline = isOutline;
		this.fixed = fixed;
		this.blur = blur;
		this.fade = fade;
		this.stipple = stipple;
		this.min = min;

		if (stipple != 0){
			System.out.println("a");
		}
	}


	private Line(Line line, String style, String src, int stroke, float strokeWidth,
			int stipple, Cap strokeLinecap, int level, boolean fixed,
			int fade, float blur, boolean isOutline, float min) {
		super();

		this.style = style;

		// round = (strokeLinecap == Cap.ROUND);

		color = line.color;

		this.width = strokeWidth;
		this.level = level;
		this.outline = isOutline;
		this.fixed = fixed;
		this.fade = fade;
		this.cap = strokeLinecap;
		this.blur = blur;
		this.stipple = stipple;
		this.min = min;
	}

	@Override
	public void renderWay(IRenderCallback renderCallback, Tag[] tags) {
		// renderCallback.renderWay(mPaint, mLevel, mColor, mStrokeWidth,
		// mRound, mOutline);
		renderCallback.renderWay(this, level);
	}

	// @Override
	// public void scaleStrokeWidth(float scaleFactor) {
	// paint.setStrokeWidth(strokeWidth * scaleFactor);
	// }

	public int getLevel() {
		return this.level;
	}
}
