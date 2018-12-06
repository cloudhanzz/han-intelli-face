package han.cloud.ai.face;

import static han.cloud.ai.util.FaceConstants.*;
import java.awt.image.BufferedImage;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import han.cloud.ai.face.ifs.FaceRecognizer;
import han.cloud.ai.model.MatchInfo;
import han.cloud.ai.util.ImageTool;
import jep.Jep;
import jep.JepException;
import jep.NDArray;

public class LandmarkFaceRegcognizer implements FaceRecognizer {

	private static final String FACE_KEY_1 = "face1";
	private static final String FACE_KEY_2 = "face2";
	private static final String F_STATEMENT = "%s = main.compare_faces_by_image(%s, %s)";

	private static final Logger LOGGER = LoggerFactory.getLogger(LandmarkFaceRegcognizer.class);
	private static Jep jep;

	static {

		try {

			String pyHome = System.getenv("PYTHON_HOME");
			String faceRecogDir = pyHome + "face_recognition";

			jep = new Jep();

			jep.eval("import sys");
			jep.set("face_reck", faceRecogDir);

			jep.eval("sys.path.append(face_reck)");
			jep.eval("import main");

			LOGGER.info("Created Jep {}", jep);

		} catch (JepException e) {
			LOGGER.error("Cannot create Jep", e);
		}
	}

	@Override
	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face) {

		double minDistance = Double.MAX_VALUE;
		int index = -1;

		for (int i = 0; i < faces.size(); i++) {

			BufferedImage face2 = faces.get(i);
			double distance = distance(face, face2);

			if (distance < minDistance) {
				minDistance = distance;
				index = i;
			}
		}

		return new MatchInfo(minDistance, index);
	}

	private double distance(BufferedImage face1, BufferedImage face2) {

		String distanceKey = "distance";
		double distanceValue = Double.MAX_VALUE;

		try {

			setImage(face1, FACE_KEY_1);
			setImage(face2, FACE_KEY_2);

			String statement = String.format(F_STATEMENT, distanceKey, FACE_KEY_1, FACE_KEY_2);

			jep.eval(statement);

			String x = jep.getValue(distanceKey, String.class);
			distanceValue = Double.parseDouble(x);

		} catch (JepException e) {
			LOGGER.debug("Cannot compare images", e);
		}

		return distanceValue;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setImage(BufferedImage face, String key) throws JepException {

		byte[] bytes = toBytes(face);
		NDArray value = new NDArray(bytes, true, FACE_HEIGHT, FACE_WIDTH, FACE_LAYERS);
		jep.set(key, value);
	}

	private byte[] toBytes(BufferedImage image) {

		double[] pixels = ImageTool.toPixels(image);
		byte[] bytes = new byte[pixels.length * FACE_LAYERS];

		int j = 0;

		for (int i = 0; i < pixels.length; i++) {

			for (int k = 0; k < FACE_LAYERS; k++) {

				double pixel = pixels[i];
				bytes[j] = ((Long) Math.round(pixel)).byteValue();
				j++;
			}

		}

		return bytes;
	}
}
