package han.cloud.ai.face;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import han.cloud.ai.face.ifs.FaceRecognizer;
import han.cloud.ai.model.MatchInfo;
import jep.Jep;
import jep.JepException;

public class LandmarkFaceRegcognizer implements FaceRecognizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(LandmarkFaceRegcognizer.class);
	private static Jep jep;
	
	static {
		try {
			String pyHome = System.getenv("PYTHON_HOME");
			String faceRecogDir = pyHome + "face_recognition";
			
			jep = new Jep();
			
			jep.eval("import os");
			jep.eval("import sys");
			
			jep.set("face_reck", faceRecogDir);
			
			jep.eval("sys.path.append(face_reck)");
			jep.eval("import main");
			
			LOGGER.info("Created Jep {}", jep);

		} catch (JepException e) {
			LOGGER.error("Cannot create Jep", e);
		}
	}

	public LandmarkFaceRegcognizer() {
	}

	@Override
	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face) {

		String img1 = saveImage(face);

		double minDistance = Double.MAX_VALUE;
		int index = -1;

		for (int i = 0; i < faces.size(); i++) {

			BufferedImage face2 = faces.get(i);
			String img2 = saveImage(face2);

			double distance = distance(img1, img2);
			if (distance < minDistance) {
				minDistance = distance;
				index = i;
			}
			
			new File(img2).delete();
		}
		
		new File(img1).delete();

		return new MatchInfo(minDistance, index);
	}

	private String saveImage(BufferedImage image) {
		String extension = "png";
		String savePath = System.nanoTime() + "." + extension;
		try {
			ImageIO.write(image, extension, new File(savePath));
		} catch (IOException e) {
			System.err.println("Could not save image to " + savePath);
		}
		return savePath;
	}

	private double distance(String img1, String img2) {

		String str;
		try {
			jep.set("img1", img1);
			jep.set("img2", img2);

			jep.eval("distance = main.compare_faces(img1, img2)");

			str = jep.getValue("distance", String.class);
			return Double.parseDouble(str);
		} catch (JepException e) {
		}

		return Double.MAX_VALUE;
	}
}
