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
		this.index = (0 == Double.compare(distance, Double.MAX_VALUE)) ? -1 : index;
	}

	/**
	 * Returns the distance between two images
	 * <p>
	 * The shorter the distance, the more likely that the two faces are of the same
	 * person
	 * 
	 * @return The distance between two images
	 */
	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	/**
	 * Returns the index of the image of a series of images that resembles most the
	 * target image
	 * 
	 * @return The index of the image of a series of images that resembles most the
	 *         target; -1 indicates no closes image is found
	 */
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
