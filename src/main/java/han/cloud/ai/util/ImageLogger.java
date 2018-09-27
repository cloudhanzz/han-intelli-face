package han.cloud.ai.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A convenience class for saving images for debugging purpose.
 * 
 * @author Jiayun Han
 *
 */
public class ImageLogger {

	private final String formatName;
	private final String pathFormat;

	public ImageLogger(String storageFolder, String formatName, String prefix) {
		super();
		this.formatName = formatName;
		this.pathFormat = storageFolder + File.separator + prefix + "-%d." + formatName;

		File dir = new File(storageFolder);

		if (!dir.exists()) {
			dir.mkdir();
		}
	}

	public void log(BufferedImage face) {
		log(face, null);
	}

	public void log(BufferedImage face, String suffix) {
		long ts = System.currentTimeMillis();
		String fileName = String.format(pathFormat, ts);
		if (suffix != null) {
			String a = "." + formatName;
			String b = "." + suffix + "." + formatName;
			fileName = fileName.replace(a, b);
		}
		File file = new File(fileName);
		try {
			ImageIO.write(face, formatName, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
