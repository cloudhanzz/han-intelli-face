package han.cloud.ai.model;

/**
 * This class wraps the face recognition result.
 * 
 * @author Jiayun Han
 *
 */
public class MatchInfo {

	private double distance;
	private int index = -1; // not found

	public MatchInfo(double distance, int index) {
		this.distance = distance;
		this.index = index;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public String toString() {
		return "MatchResult [matchDist=" + distance + ", index=" + index + "]";
	}

}
