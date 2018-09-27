package han.cloud.ai.util;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import cern.colt.matrix.DoubleMatrix2D;

/**
 * A convenience class for performing IO operations.
 * @author Jiayun Han
 *
 */
public final class IoTool {

	private IoTool() {
	}

	public static InputStream toInputStream(String path) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	public static File writeInputstream(InputStream sourceStream, String destPath) throws IOException {

		File targetFile = new File(destPath);

		Files.copy(sourceStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

		return targetFile;
	}

	public static void saveImage(BufferedImage image, String extension, String folder, String filename) {
		String savePath = folder + File.separator + filename + "." + extension;
		try {
			ImageIO.write(image, extension, new File(savePath));
		} catch (IOException e) {
			System.err.println("Could not save image to " + savePath);
		}
	}

	public static void saveMatrix(DoubleMatrix2D matrix, String filename) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new PrintWriter(filename))) {
			writer.write(matrix.toString());
		}
	}
	

	public static BufferedImage readImageFromClassPath(Path path) {
		return readImageFromClassPath(path.toString());
	}
	
	public static BufferedImage readImageFromClassPath(String imagePath) {
		
        try (InputStream input = IoTool.toInputStream(imagePath)) {
			
			if(input == null) {
				System.err.println(imagePath + " does not exist");
				System.exit(1);
			}		
			return ImageIO.read(input);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
        
        return null;
	}
}
