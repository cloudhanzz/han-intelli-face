package han.cloud.ai.model;

/**
 * This class wraps the face recognition result.
 * 
 * @author Jiayun Han
 *
 */
public class MatchInfo {

	//private String fileName;
	private double distance;
	private int index = -1; // not found

	public MatchInfo(double distance, int index) {
		this.distance = distance;
		this.index = index;
	}

//	public MatchInfo(String fileName, double distance) {
//		this.fileName = fileName;
//		this.distance = distance;
//	}
//
//	public String getFileName() {
//		return fileName;
//	}
//
//	public void setFileName(String fileName) {
//		this.fileName = fileName;
//	}

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

//	public String getPersonName() {
//		int slashPos = fileName.lastIndexOf('\\');
//		int extPos = fileName.lastIndexOf(".png");
//		String name = (slashPos == -1) ? fileName.substring(0, extPos) : fileName.substring(slashPos + 1, extPos);
//
//		name = name.replaceAll("[-_0-9]*$", ""); // remove trailing numbers, etc
//		return name;
//	}

	@Override
	public String toString() {
		//return "MatchResult [matchFnm=" + fileName + ", matchDist=" + distance + ", index=" + index + "]";
		return "MatchResult [matchDist=" + distance + ", index=" + index + "]";
	}

}
