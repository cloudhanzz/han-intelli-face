package han.cloud.ai.face.ifs;

import java.awt.image.BufferedImage;
import java.util.List;

import han.cloud.ai.model.MatchInfo;

/**
 * 
 * @author Jiayun Han
 *
 */
public interface FaceRecognizer {

	/**
	 * Returns the result of trying to recognize {@code face} among {@code faces}
	 * 
	 * @param faces
	 *            The faces among which to find the one that resembles {@code} most
	 * @param face
	 *            The face to match the one in {@code faces} that it resembles most
	 * @return The result of trying to recognize {@code face} among {@code faces}
	 * @see MatchInfo
	 */
	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face);

}
