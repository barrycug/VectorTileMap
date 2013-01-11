/*
 * Copyright 2012 Hannes Janetzek
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

package org.oscim.renderer.overlays;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.TileManager;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.TextItem;
import org.oscim.renderer.layer.TextLayer;
import org.oscim.utils.FastMath;
import org.oscim.utils.GeometryUtils;
import org.oscim.utils.GlUtils;
import org.oscim.utils.PausableThread;
import org.oscim.view.MapView;

import android.opengl.Matrix;
import android.os.SystemClock;

public class TextOverlay extends RenderOverlay {

	private TileSet mTiles;
	private LabelThread mThread;

	private MapPosition mWorkPos;
	private TextLayer mWorkLayer;
	private TextLayer mNewLayer;

	/* package */boolean mRun;
	/* package */boolean mRerun;

	class LabelThread extends PausableThread {

		@Override
		protected void doWork() {
			SystemClock.sleep(250);
			if (!mRun)
				return;

			mRun = false;
			updateLabels();
			mMapView.redrawMap();
		}

		@Override
		protected String getThreadName() {
			return "Labeling";
		}

		@Override
		protected boolean hasWork() {
			return mRun || mRerun;
		}
	}

	public TextOverlay(MapView mapView) {
		super(mapView);

		mWorkPos = new MapPosition();
		mThread = new LabelThread();
		mThread.start();
	}

	void updateLabels() {
		mTiles = TileManager.getActiveTiles(mTiles);

		// Log.d("...", "relabel " + mRerun + " " + x + " " + y);
		if (mTiles.cnt == 0)
			return;

		mMapView.getMapViewPosition().getMapPosition(mWorkPos, null);

		TextLayer tl = mWorkLayer;

		if (tl == null)
			tl = new TextLayer();

		// mTiles might be from another zoomlevel than the current:
		// this scales MapPosition to the zoomlevel of mTiles...
		// TODO create a helper function in MapPosition
		int diff = mTiles.tiles[0].zoomLevel - mWorkPos.zoomLevel;

		// only relabel when tiles belong to the current zoomlevel or its parent
		if (diff > 1 || diff < -2) {
			synchronized (this) {
				mNewLayer = tl;
			}
			return;
		}

		float scale = mWorkPos.scale;
		double angle = Math.toRadians(mWorkPos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		TextItem ti2 = null;

		int maxx = Tile.TILE_SIZE << (mWorkPos.zoomLevel - 1);

		MapTile[] tiles = mTiles.tiles;

		// order tiles by x/y coordinate to make placement more consistent 
		// while map position changes
		//Arrays.sort(tiles, 0, mTiles.cnt, TileSet.coordComparator);

		// TODO more sophisticated placement :)
		for (int i = 0, n = mTiles.cnt; i < n; i++) {
			MapTile t = tiles[i];
			if (!t.isVisible)
				continue;

			float dx = (float) (t.pixelX - mWorkPos.x);
			float dy = (float) (t.pixelY - mWorkPos.y);

			// flip around date-line
			if (dx > maxx) {
				dx = dx - maxx * 2;
			} else if (dx < -maxx) {
				dx = dx + maxx * 2;
			}
			dx *= scale;
			dy *= scale;

			for (TextItem ti = t.labels; ti != null; ti = ti.next) {

				if (ti2 == null)
					ti2 = TextItem.get();

				ti2.move(ti, dx, dy, scale);

				boolean overlaps = false;

				if (ti.text.caption) {
					int tx = (int) (ti2.x);
					int ty = (int) (ti2.y);
					int tw = (int) (ti2.width / 2);
					int th = (int) (ti2.text.fontHeight / 2);

					for (TextItem lp = tl.labels; lp != null;) {
						int px = (int) (lp.x);
						int py = (int) (lp.y);
						int ph = (int) (lp.text.fontHeight / 2);
						int pw = (int) (lp.width / 2);

						if ((tx - tw) < (px + pw)
								&& (px - pw) < (tx + tw)
								&& (ty - th) < (py + ph)
								&& (py - ph) < (ty + th)) {

							overlaps = true;
							break;
						}
						lp = lp.next;
					}
				} else {

					if (cos * (ti.x2 - ti.x1) - sin * (ti.y2 - ti.y1) < 0) {
						// flip label upside-down
						ti2.x1 = (short) ((ti.x2 * scale + dx));
						ti2.y1 = (short) ((ti.y2 * scale + dy));
						ti2.x2 = (short) ((ti.x1 * scale + dx));
						ti2.y2 = (short) ((ti.y1 * scale + dy));
					} else {
						ti2.x1 = (short) ((ti.x1 * scale + dx));
						ti2.y1 = (short) ((ti.y1 * scale + dy));
						ti2.x2 = (short) ((ti.x2 * scale + dx));
						ti2.y2 = (short) ((ti.y2 * scale + dy));
					}

					//float normalLength = (float) Math.hypot(ti2.x2 - ti2.x1, ti2.y2 - ti2.y1);

					for (TextItem lp = tl.labels; lp != null;) {
						if (lp.text.caption) {
							lp = lp.next;
							continue;
						}

						if (GeometryUtils.lineIntersect(ti2.x1, ti2.y1, ti2.x2, ti2.y2,
								lp.x1, lp.y1, lp.x2, lp.y2)) {
							// just to make it more deterministic
							if (lp.width > ti2.width) {
								TextItem tmp = lp;
								lp = lp.next;

								tl.removeText(tmp);
								tmp.next = null;
								TextItem.release(tmp);
								continue;
							}
							overlaps = true;
							break;
						}

						if ((ti2.x1) < (lp.x2)
								&& (lp.x1) < (ti2.x2)
								&& (ti2.y1) < (lp.y2)
								&& (lp.y1) < (ti2.y2)) {

							// just to make it more deterministic
							if (lp.width > ti2.width) {
								TextItem tmp = lp;
								lp = lp.next;

								tl.removeText(tmp);
								tmp.next = null;
								TextItem.release(tmp);
								continue;
							}
							overlaps = true;
							break;
						}

						lp = lp.next;
					}
				}

				if (!overlaps) {
					tl.addText(ti2);
					ti2 = null;
				}
			}
		}

		if (ti2 != null)
			TextItem.release(ti2);

		// scale back to fixed zoom-level. could be done in setMatrix..
		for (TextItem lp = tl.labels; lp != null; lp = lp.next) {
			lp.x /= scale;
			lp.y /= scale;
		}

		// draw text to bitmaps and create vertices
		tl.setScale(scale);
		tl.prepare();

		// everything synchronized?
		synchronized (this) {
			mNewLayer = tl;
		}
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {
		// Log.d("...", "update " + tilesChanged + " " + positionChanged);
		if (mHolding)
			return;

		if (mNewLayer != null) {

			// keep text layer, not recrating its canvas each time...
			mWorkLayer = (TextLayer) layers.textureLayers;
			layers.clear();

			layers.textureLayers = mNewLayer;
			mNewLayer = null;

			// make the 'labeled' MapPosition current
			MapPosition tmp = mMapPosition;
			mMapPosition = mWorkPos;
			mWorkPos = tmp;

			// TODO should return true instead
			newData = true;
		}

		if (tilesChanged || positionChanged) {
			if (!mRun) {
				mRun = true;
				synchronized (mThread) {
					mThread.notify();
				}
			}
		}
	}

	@Override
	protected void setMatrix(MapPosition curPos, float[] matrix) {
		MapPosition oPos = mMapPosition;

		float div = FastMath.pow(oPos.zoomLevel - curPos.zoomLevel);
		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		float scale = curPos.scale / div;

		GlUtils.setMatrix(matrix, x * scale, y * scale,
				scale / GLRenderer.COORD_MULTIPLIER);

		Matrix.multiplyMM(matrix, 0, curPos.viewMatrix, 0, matrix, 0);
	}

	private boolean mHolding;

	public synchronized void hold(boolean enable) {
		//		mHolding = enable;
		//		if (!enable && !mRun) {
		//			mRun = true;
		//			synchronized (mThread) {
		//				mThread.notify();
		//			}
		//		} else {
		//			mRun = false;
		//		}
	}
}
