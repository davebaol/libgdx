
package com.badlogic.gdx.ai.steer.paths;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.steer.behaviors.FollowPathBase.Path;
import com.badlogic.gdx.ai.steer.paths.LinePath.LinePathParam;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.utils.Array;

/**
 * A path for path following behaviors that is made up of a series of waypoints. Each waypoint is connected to the successor with a segment.
 *
 * @param <T> Type of vector, either 2D or 3D, implementing the {@link Vector} interface
 * 
 * @author davebaol
 */
public abstract class LinePath<T extends Vector<T>> implements Path<T, LinePathParam> {

	private T[] waypoints;
	private Array<Segment<T>> segments;
	private float pathLength;
	private T nearestPointOnCurrentSegment;
	private T nearestPointOnPath;

	public LinePath (T[] waypoints) {
		if (waypoints == null || waypoints.length == 0) throw new IllegalArgumentException();
		this.waypoints = waypoints;
		createPath(waypoints);
		nearestPointOnCurrentSegment = waypoints[0].cpy();
		nearestPointOnPath = waypoints[0].cpy();
	}

	/** Returns the square distance of the nearest point on line segment a-b, from point c. Also, the out vector is assigned to the
	 * nearest point.
	 * @param out
	 * @param a
	 * @param b
	 * @param c */
	public abstract float calculatePointSegmentSquareDistance (T out, T a, T b, T c);

	@Override
	public LinePathParam createParam () {
		return new LinePathParam();
	}

	// Sending the last parameter value to the path in order to calculate the current
	// parameter value. This is essential to avoid nasty problems when lines are close together.
	// We limit the getParam algorithm to only considering areas of the path close to the previous
	// parameter value. The character is unlikely to have moved far, after all.
	// This technique, assuming the new value is close to the old one, is called coherence, and it is a
	// feature of many geometric algorithms.
	@Override
	public float calculateDistance (T agentCurrPos, LinePathParam parameter) {
		// Find the nearest segment
		float smallestDistance2 = Float.POSITIVE_INFINITY;
		Segment<T> nearestSegment = null;
		for (int i = 0; i < segments.size; i++) {
			Segment<T> segment = segments.get(i);
			float distance2 = calculatePointSegmentSquareDistance(nearestPointOnCurrentSegment, segment.begin, segment.end, agentCurrPos);

			// first point
			if (distance2 < smallestDistance2) {
				nearestPointOnPath.set(nearestPointOnCurrentSegment);
				smallestDistance2 = distance2;
				nearestSegment = segment;
				parameter.segmentIndex = i;
//				System.out.println("segmentIndex = " + i);
			}
		}

		// Distance from path start
		float lengthOnPath = nearestSegment.cumulativeLength - nearestPointOnPath.dst(nearestSegment.end);

		parameter.setDistance(lengthOnPath);

		return lengthOnPath;
	}

	/** @param pathDistance the distance in meters on path.
	 * @return position in the world that corresponds to the distance on the path. */
	@Override
	public void calculateTargetPosition (T out, LinePathParam param, float targetDistance) {
		// Cycling path support
		if (targetDistance < 0) { // Backwards
			targetDistance += pathLength;
		}
		else if (targetDistance > pathLength) { // Forward
			targetDistance -= pathLength;
		}

		// Walk through lines to see on which line we are
		Segment<T> desiredSegment = null;
		for (int i = 0; i < segments.size; i++) {
			Segment<T> segment = segments.get(i);
			if (segment.cumulativeLength >= targetDistance) {
				desiredSegment = segment;
				break;
			}
		}

		// begin-------targetPos---end
		// targetPos to end == distance
		float distance = desiredSegment.cumulativeLength - targetDistance;

		out.set(desiredSegment.begin).sub(desiredSegment.end).scl(distance / desiredSegment.length).add(desiredSegment.end);
	}

	private void createPath (T[] waypoints) {
		segments = new Array<Segment<T>>(waypoints.length);
		pathLength = 0;
		T curr = waypoints[0];
		T prev = null;
		for (int i = 1; i <= waypoints.length; i++) {
			prev = curr;
			curr = i < waypoints.length ? waypoints[i] : waypoints[0];
			Segment<T> segment = new Segment<T>(prev, curr);
			pathLength += segment.length;
			segment.cumulativeLength = pathLength;
			segments.add(segment);
//			System.out.println("begin="+segment.begin +", end="+segment.begin+",len="+segment.length+",cumLen="+segment.cumulativeLength);
		}
	}

	public Array<Segment<T>> getSegments () {
		return segments;
	}

	public float getLength () {
		return pathLength;
	}

	public static class LinePathParam implements Param {
		int segmentIndex;
		float distance;

		@Override
		public float getDistance () {
			return distance;
		}

		@Override
		public void setDistance (float distance) {
			this.distance = distance;
		}
		
	}

	public static class Segment<T extends Vector<T>> {
		T begin;
		T end;
		float length;
		float cumulativeLength;

		Segment (T begin, T end) {
			this.begin = begin;
			this.end = end;
			this.length = begin.dst(end);
		}

		public T getBegin () {
			return begin;
		}

		public T getEnd () {
			return end;
		}
	}
}
