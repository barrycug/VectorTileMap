/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012, 2013 OpenScienceMap 
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
package org.oscim.generator;

import static org.oscim.generator.JobTile.STATE_LOADING;
import static org.oscim.generator.JobTile.STATE_NONE;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * A JobQueue keeps the list of pending jobs for a MapView and prioritizes them.
 */
public class JobQueue {
	private static final int INITIAL_CAPACITY = 64;

	private PriorityQueue<JobTile> mPriorityQueue;

	/**
	 */
	public JobQueue() {
		mPriorityQueue = new PriorityQueue<JobTile>(INITIAL_CAPACITY);
	}

	/**
	 * @param tiles
	 *            the job to be added to this queue.
	 */
	public synchronized void setJobs(ArrayList<JobTile> tiles) {

		for (int i = 0, n = tiles.size(); i < n; i++) {
			JobTile tile = tiles.get(i);
			tile.state = STATE_LOADING;
			mPriorityQueue.offer(tile);
		}
	}

	/**
	 * Removes all jobs from this queue.
	 */
	public synchronized void clear() {
		JobTile t;
		while ((t = mPriorityQueue.poll()) != null)
			t.state = STATE_NONE;

		mPriorityQueue.clear();
	}

	/**
	 * @return true if this queue contains no jobs, false otherwise.
	 */
	public synchronized boolean isEmpty() {
		return mPriorityQueue.isEmpty();
	}

	/**
	 * @return the most important job from this queue or null, if empty.
	 */
	public synchronized JobTile poll() {
		return mPriorityQueue.poll();
	}
}
