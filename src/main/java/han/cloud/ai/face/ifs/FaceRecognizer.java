package han.cloud.ai.face.ifs;

import java.awt.image.BufferedImage;
import java.util.List;

import han.cloud.ai.model.MatchInfo;

public interface FaceRecognizer {
	
	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face);

}
