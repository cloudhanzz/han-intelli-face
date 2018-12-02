package han.cloud.ai.util;

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.javacv.OpenCVFrameConverter;

/**
 * A convenience class for handling image processing
 * 
 * @author Jiayun Han
 *
 */
public class ImageTool {

	private static final OpenCVFrameConverter.ToIplImage CONVERTER = new OpenCVFrameConverter.ToIplImage();

	public static Frame toJavaCvFrame(IplImage intelImage) {
		return CONVERTER.convert(intelImage);
	}

	public static IplImage toIplImage(Frame frame) {
		return CONVERTER.convert(frame);
	}

	public static IplImage toIntelImage(BufferedImage bufferedImage) {
		return Java2DFrameUtils.toIplImage(bufferedImage);
	}

	public static BufferedImage toBufferedImage(IplImage intelImage) {
		return Java2DFrameUtils.toBufferedImage(intelImage);
	}

	public static IplImage copyGray(IplImage intelImage) {
		IplImage gray = cvCreateImage(intelImage.cvSize(), IPL_DEPTH_8U, 1);
		cvCvtColor(intelImage, gray, CV_BGR2GRAY);
		return gray;
	}

	public static void showImage(IplImage intelImage, CanvasFrame canvas, int action) {

		canvas.setDefaultCloseOperation(action);
		Frame frame = toJavaCvFrame(intelImage);
		canvas.showImage(frame);

		try {
			canvas.waitKey(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void showImage(IplImage intelImage, String title, int action) {
		showImage(intelImage, new CanvasFrame(title), action);
	}

	/**
	 * Returns a double array containing all sample pixels of {@code image}.
	 * 
	 * @param image
	 *            The image whose sample pixels to be returned
	 * @param width
	 *            The width of the image
	 * @param height
	 *            The height of the image
	 * @return A double array containing all sample pixels of {@code image}
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the coordinates are not in bounds, or if
	 *             {@code width * height} is too small to hold the output.
	 */
	public static double[] toPixels(BufferedImage image, int width, int height) {
		int size = width * height;
		return image.getData().getPixels(0, 0, width, height, new double[size]);
	}

	public static BufferedImage createImageFromPixels(double[] pixels, int width) {

		BufferedImage image = null;
		int brightness = 255;

		try {

			double maxVal = Arrays.stream(pixels).max().getAsDouble();
			double minVal = Arrays.stream(pixels).min().getAsDouble();
			double range = maxVal - minVal;

			int height = pixels.length / width;
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

			WritableRaster raster = image.getData().createCompatibleWritableRaster();

			double normalized;
			for (int i = 0; i < pixels.length; i++) {
				normalized = (pixels[i] - minVal) / range;
				pixels[i] = normalized * brightness;
			}

			raster.setPixels(0, 0, width, height, pixels);
			image.setData(raster);

		} catch (Exception e) {
			System.out.println(e);
		}
		return image;
	}

	public static BufferedImage extract(BufferedImage image, Rectangle rectangel) {
		return image.getSubimage(rectangel.x, rectangel.y, rectangel.width, rectangel.height);
	}

	public static BufferedImage grayAndResizeToFace(BufferedImage image) {
		return grayAndResizeTo(image, FaceConstants.width, FaceConstants.height);
	}

	/*
	 * resize to at least a standard size, then convert to gray scale
	 */
	public static BufferedImage grayAndResizeTo(BufferedImage image, int desiredWidth, int desiredHeight) {

		int width = image.getWidth();
		int height = image.getHeight();

		double widthScale = desiredWidth / (double) width;
		double heightScale = desiredHeight / (double) height;
		double scale = Math.max(widthScale, heightScale);

		int newWidth = (int) Math.round(width * scale);
		int newHeight = (int) Math.round(height * scale);

		BufferedImage grayAndScaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);

		Graphics2D g2 = grayAndScaled.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, width, height, null);
		g2.dispose();

		return clipToFace(grayAndScaled, desiredWidth, desiredHeight);
	}

	/*
	 * Shrink image to FACE_WIDTH*FACE_HEIGHT size, assuming image is face size or
	 * bigger
	 */
	private static BufferedImage clipToFace(BufferedImage image, int desiredWidth, int desiredHeight) {

		int xOffset = (image.getWidth() - desiredWidth) / 2;
		int yOffset = (image.getHeight() - desiredHeight) / 2;

		BufferedImage face = null;

		try {
			face = image.getSubimage(xOffset, yOffset, desiredWidth, desiredHeight);
		} catch (RasterFormatException e) {
		}

		return face;
	}
}
