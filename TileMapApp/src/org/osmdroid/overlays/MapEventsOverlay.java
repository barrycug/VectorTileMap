package org.osmdroid.overlays;

import org.oscim.core.GeoPoint;
import org.oscim.overlay.Overlay;
import org.oscim.view.MapView;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Empty overlay than can be used to detect events on the map, and to throw them
 * to a MapEventsReceiver.
 * 
 * @see MapEventsReceiver
 * @author M.Kergall
 */
public class MapEventsOverlay extends Overlay {

	private MapEventsReceiver mReceiver;
	private static final String TAG = MapEventsOverlay.class.getSimpleName();

	/**
	 * @param ctx
	 *            the context
	 * @param receiver
	 *            the object that will receive/handle the events. It must
	 *            implement MapEventsReceiver interface.
	 */
	public MapEventsOverlay(Context ctx, MapEventsReceiver receiver) {
		super(ctx);
		mReceiver = receiver;
	}

	//	@Override
	//	protected void draw(Canvas c, MapView osmv, boolean shadow) {
	//		// Nothing to draw
	//	}

	@Override
	public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
		//		Projection proj = mapView.getProjection();
		//		GeoPoint p = proj.fromPixels(e.getX(), e.getY());
		GeoPoint p = mapView.getMapViewPosition().fromScreenPixels(e.getX(), e.getY());

		return mReceiver.singleTapUpHelper(p);
	}

	@Override
	public boolean onLongPress(MotionEvent e, MapView mapView) {
		//		Projection proj = mapView.getProjection();
		//		GeoPoint p = proj.fromPixels(e.getX(), e.getY());
		if(e.getPointerCount() == 1){
			GeoPoint p = mapView.getMapViewPosition().fromScreenPixels(e.getX(), e.getY());

			// throw event to the receiver:
			return mReceiver.longPressHelper(p);
		}else if(e.getPointerCount() == 2){
			GeoPoint p1 = mapView.getMapViewPosition().fromScreenPixels(e.getX(0), e.getY(0));
			GeoPoint p2 = mapView.getMapViewPosition().fromScreenPixels(e.getX(1), e.getY(1));
			return mReceiver.longPressHelperFor2Finger(p1, p2);
		}else{
			return true;
		}
	}

}
