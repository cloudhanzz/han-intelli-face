package han.cloud.ai.face;

import java.awt.image.BufferedImage;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import han.cloud.ai.face.LandmarkFaceRegcognizer;
import han.cloud.ai.model.MatchInfo;
import jep.Jep;
import jep.JepException;

public class LandmarkFaceRecognizerProvider {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LandmarkFaceRecognizerProvider.class);
	
	private ThreadLocal<LandmarkFaceRegcognizer> threadLoal;
	
	private LandmarkFaceRegcognizer recognizer() {
		
		if(threadLoal == null) {
			
			try {

				String pyHome = System.getenv("PYTHON_HOME");
				String faceRecogDir = pyHome + "face_recognition";

				Jep j = new Jep();

				j.eval("import sys");
				j.set("face_reck", faceRecogDir);

				j.eval("sys.path.append(face_reck)");
				j.eval("import main");

				LOGGER.info("Created Jep {}", j);
				
				threadLoal = new ThreadLocal<>();
				threadLoal.set(new LandmarkFaceRegcognizer(j));

			} catch (JepException e) {
				LOGGER.error("Cannot create Jep", e);
			}
		}
		
		return threadLoal.get();
	}

	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face) {
		LandmarkFaceRegcognizer rec = recognizer();
		return rec.recognize(faces, face);
	}

}
