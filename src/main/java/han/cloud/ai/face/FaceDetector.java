package han.cloud.ai.face;

import static java.util.stream.Collectors.toList;
import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.cvRectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_ROUGH_SEARCH;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_FIND_BIGGEST_OBJECT;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import han.cloud.ai.util.ImageTool;
import han.cloud.ai.util.IoTool;

/**
 * <p>
 * This class provides abilities to detect and mark the specified number of
 * biggest faces contained in an image.
 * 
 * <p>
 * It uses Intel's Open Source Computer Vision Library and the frontal face
 * classifier trained by Intel.
 * 
 * @author Jiayun Han
 *
 */
public final class FaceDetector {

	private static final Object LOCK = new Object();
	private static final Logger LOGGER = LoggerFactory.getLogger(FaceDetector.class);
	private static final String CASCADE_FILE_NAME = "haarcascade_frontalface_alt.xml";

	private static volatile FaceDetector instance;
	private CvHaarClassifierCascade cascade;

	private FaceDetector() {

		Loader.load(opencv_objdetect.class);

		try (InputStream sourceStream = IoTool.toInputStream(CASCADE_FILE_NAME)) {

			String cascadePath = IoTool.writeInputstream(sourceStream, CASCADE_FILE_NAME).getAbsolutePath();
			cascade = new CvHaarClassifierCascade(cvLoad(cascadePath));

			LOGGER.info("Created classifier with address = {}", cascade.address());
			instance = this;

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Returns the {@link java.awt.Rectangle}s of the faces contained in
	 * <i>image</i>
	 * 
	 * @param image
	 *            The image whose contained faces to be found
	 * @param max
	 *            The maximal number of faces to be found; -1 to find all faces
	 * @return The l{@link java.awt.Rectangle}s of the found faces or an empty list
	 *         if no face is found
	 */
	public List<Rectangle> findFaces(BufferedImage image, int max) {
		return findFacesHelper(image, max, 3, CV_HAAR_DO_CANNY_PRUNING);
	}

	/**
	 * Extracts and returns the specified maximal number of faces from the given
	 * <i>image</i>
	 * 
	 * @param image
	 *            The image whose contained faces to be extracted
	 * @param max
	 *            The maximal number of faces to be extracted; -1 to extract all
	 *            faces
	 * @return The specified maximal number of faces from the given <i>image</i> or
	 *         an empty list if no face is found
	 */
	public List<BufferedImage> extractFaces(BufferedImage image, int max) {
		return findFaces(image, max)//
				.stream()//
				.map(rect -> ImageTool.crop(image, rect))//
				.collect(Collectors.toList());
	}

	/**
	 * Returns the {@link java.awt.Rectangle} of the biggest face contained in
	 * <i>image</i> as an {@link java.util.Optional}
	 * 
	 * @param image
	 *            The image whose biggest face to be found
	 * @return The {@link java.awt.Rectangle} of the biggest face contained in
	 *         <i>image</i> as an {@link java.util.Optional}
	 */
	public Optional<Rectangle> findBiggestFace(BufferedImage image) {

		List<Rectangle> rects = findFacesHelper(image, 1, 1, CV_HAAR_DO_ROUGH_SEARCH | CV_HAAR_FIND_BIGGEST_OBJECT);
		return rects.isEmpty() ? Optional.empty() : Optional.of(rects.get(0));
	}

	public Optional<BufferedImage> extractBiggestFace(BufferedImage image) {
		return findBiggestFace(image).map(rect -> ImageTool.crop(image, rect));
	}

	private List<Rectangle> findFacesHelper(BufferedImage image, int maxFaces, int minNeighbors, int flags) {

		IplImage iplImage = ImageTool.toIplImage(image);
		IplImage gray = ImageTool.copyGray(iplImage);

		CvMemStorage storage = CvMemStorage.create();
		CvSeq faces = cvHaarDetectObjects(gray, cascade, storage, 1.1, minNeighbors, flags);
		cvClearMemStorage(storage);

		int foundFaces = faces.total();

		if (maxFaces == -1) {
			maxFaces = Integer.MAX_VALUE;
		}

		List<Rectangle> rectangles = new ArrayList<>();

		if (foundFaces > 0) {

			List<CvRect> cvRects = new ArrayList<>();

			for (int i = 0; i < foundFaces; i++) {
				cvRects.add(new CvRect(cvGetSeqElem(faces, i)));
			}

			rectangles = cvRects.stream() //
					// .sorted((a, b) -> Integer.compare(b.width() * b.height(), a.width() *
					// a.height()))//
					.limit(maxFaces) //
					.map(cv -> toRectangle(cv, 1)) //
					.collect(toList());
		}

		return rectangles;
	}

	private Rectangle toRectangle(CvRect rect, double scale) {

		Rectangle rectangle = new Rectangle( //
				(int) Math.round(rect.x() * scale), //
				(int) Math.round(rect.y() * scale), //
				(int) Math.round(rect.width() * scale), //
				(int) Math.round(rect.height() * scale));

		rect.close();

		return rectangle;
	}

	/**
	 * Returns the source image as {@link IplImage} with maximal <i>n</i> faces
	 * marked
	 * 
	 * @param source
	 *            The image containing the faces to be marked
	 * @param n
	 *            The biggest number of faces to be marked; -1 to mark all faces
	 * @return The source image as {@link IplImage} with maximal <i>n</i> faces
	 *         marked
	 */
	public IplImage markFaces(BufferedImage source, int n) {

		List<Rectangle> rectangles = findFaces(source, n);

		LOGGER.info("Found {} faces", rectangles.size());

		IplImage dest = ImageTool.toIplImage(source);		
		rectangles.forEach(rect -> markFace(dest, rect));

		return dest;
	}

	public static IplImage markFace(BufferedImage source, Rectangle rect) {

		IplImage dest = ImageTool.toIplImage(source);
		markFace(dest, rect);

		return dest;
	}
	
	public static IplImage markFace(IplImage source, Rectangle rect) {

		cvRectangle(source, //
				cvPoint(rect.x, rect.y), //
				cvPoint(rect.x + rect.width, rect.y + rect.height), //
				CvScalar.GREEN, 2, CV_AA, //
				0);

		return source;
	}

	/**
	 * Returns the only instance of this class
	 * <p>
	 * Given that instantiation of this class is expensive, this method ensures that
	 * it is instantiated only once, regardless thread.
	 * 
	 * @return The singleton instance of this class
	 */
	public static FaceDetector instance() {

		FaceDetector result = instance;
		if (result == null) {
			synchronized (LOCK) {
				result = instance;
				if (result == null) {
					result = new FaceDetector();
				}
			}
		}

		return result;
	}
}
