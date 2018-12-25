package han.cloud.ai.face;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import han.cloud.ai.face.ifs.FaceRecognizer;
import han.cloud.ai.model.MatchInfo;
import han.cloud.ai.util.ArrayTool;
import han.cloud.ai.util.FaceConstants;
import han.cloud.ai.util.ImageTool;
import han.cloud.ai.util.KeyValuePair;

/**
 * This is an implementation of Turk and Pentand's paper of "Eigenfaces for
 * Recognition" which can be found at
 * <i>http://www.face-rec.org/algorithms/PCA/jcn.pdf</i>
 * 
 * Another implementation was done by Dr Andrew Davison.
 * 
 * @author Jiayun Han
 *
 */
public class EigenFaceRecognizer implements FaceRecognizer {

	private static final class Eigen {

		private final double value;
		private final DoubleMatrix1D vector;

		private Eigen(double value, DoubleMatrix1D vector) {
			this.value = value;
			this.vector = vector;
		}
	}

	private static class Recognizer {

		private final int max;
		private final int subMax;

		private final double[] pixelMeans;
		private final double[] eigenValues;

		private final DoubleMatrix2D eigenspace;
		private final DoubleMatrix2D refWeights;

		/**
		 * 
		 * @param faces
		 *            Assuming they are gray and in standard size
		 */
		private Recognizer(List<BufferedImage> faces) {

			this.max = faces.size();
			this.subMax = max - 1;

			double[][] refData = faces //
					.stream() //
					.map(this::divideByMaxPixel) //
					.toArray(double[][]::new);

			this.pixelMeans = ArrayTool.findCrossMeans(refData);
			ArrayTool.minusCrossMeans(refData, pixelMeans);

			DenseDoubleMatrix2D refFaces = new DenseDoubleMatrix2D(refData);
			KeyValuePair<DoubleMatrix2D, double[]> kv = buildEigenspaceAndEigenValues(refFaces);

			this.eigenspace = kv.getKey();
			this.eigenValues = kv.getValue();

			this.refWeights = refFaces.zMult(eigenspace.viewDice(), null);
		}

		private KeyValuePair<MatchInfo, double[]> recognize(BufferedImage face) {

			double[] weights = projectFace(face).viewRow(0).toArray();

			int index = -1;
			double minSum = Double.MAX_VALUE;

			DoubleMatrix2D trainingWeights = refWeights.copy();

			for (int row = 0; row < trainingWeights.rows(); row++) {
				double sum = 0;
				for (int col = 0; col < trainingWeights.columns(); col++) {
					double value = trainingWeights.get(row, col);
					value -= weights[col];
					value *= value;
					sum += value;
				}

				if (Double.compare(sum, minSum) < 0) {
					minSum = sum;
					index = row;
				}
			}

			double distance = Math.sqrt(minSum);
			MatchInfo matchInfo = new MatchInfo(distance, index);

			return new KeyValuePair<MatchInfo, double[]>(matchInfo, weights);
		}

		private DoubleMatrix2D projectFace(BufferedImage face) {

			double[] pixels = divideByMaxPixel(face);
			ArrayTool.minusCrossMeans(pixels, pixelMeans);

			double[][] faceData = { pixels };
			DoubleMatrix2D faceMatrix = new DenseDoubleMatrix2D(faceData);

			return faceMatrix.zMult(eigenspace.viewDice(), null);
		}

		// Divide each pixel of the image by the image's max pixel value
		private double[] divideByMaxPixel(BufferedImage image) {
			double[] pixels = ImageTool.toPixels(image, FaceConstants.FACE_WIDTH, FaceConstants.FACE_HEIGHT);
			ArrayTool.divideByMax(pixels);
			return pixels;
		}

		/**
		 * 
		 * @param refFaces
		 *            Normalized training data pixels
		 * @return The{@code EigenSpace as key and EigenValues as value] built out of the training image pixels
		 */
		private KeyValuePair<DoubleMatrix2D, double[]> buildEigenspaceAndEigenValues(DoubleMatrix2D refFaces) {

			List<Eigen> eigens = buildAndSortEigens(refFaces);

			KeyValuePair<DoubleMatrix2D, double[]> kv = getEigenvectorsAndEigenvalues(eigens);

			DoubleMatrix2D eigenvectors = kv.getKey();

			DoubleMatrix2D eigenfaces = eigenvectors.viewDice().zMult(refFaces, null);

			for (int i = 0; i < subMax; i++) {
				double[] eigenface = eigenfaces.viewRow(i).toArray();
				ArrayTool.divideByNorm(eigenface);
				eigenfaces.viewRow(i).assign(eigenface);
			}

			DoubleMatrix2D eigenSpace = eigenfaces.viewPart(0, 0, subMax, FaceConstants.columns);
			double[] eigenValues = kv.getValue();

			return new KeyValuePair<DoubleMatrix2D, double[]>(eigenSpace, eigenValues);
		}

		private List<Eigen> buildAndSortEigens(DoubleMatrix2D refFaces) {

			DoubleMatrix2D covarMatrix = refFaces.zMult(refFaces.viewDice(), null);
			EigenvalueDecomposition decom = new EigenvalueDecomposition(covarMatrix);

			DoubleMatrix2D eigenvectors = decom.getV();
			DoubleMatrix1D eigenvalues = decom.getRealEigenvalues();

			List<Eigen> eigens = new ArrayList<>();

			for (int column = 0; column < eigenvectors.columns(); column++) {
				double eigenvalue = eigenvalues.get(column);
				DoubleMatrix1D eigenvector = eigenvectors.viewColumn(column).copy();
				Eigen eigen = new Eigen(eigenvalue, eigenvector);
				eigens.add(eigen);
			}

			eigens.sort((a, b) -> Double.compare(b.value, a.value)); // descending
			return eigens;
		}

		private KeyValuePair<DoubleMatrix2D, double[]> getEigenvectorsAndEigenvalues(List<Eigen> eigens) {

			DoubleMatrix2D vectors = new DenseDoubleMatrix2D(max, max);
			double[] values = new double[subMax];

			for (int i = 0; i < max; i++) {
				Eigen eigen = eigens.get(i);
				vectors.viewColumn(i).assign(eigen.vector);

				if (i < subMax) {
					values[i] = eigen.value;
				}
			}

			return new KeyValuePair<DoubleMatrix2D, double[]>(vectors, values);
		}

		private void multiEigenValues(DoubleMatrix2D weights) {
			transform(weights, eigenValues, (a, b) -> a * b);
		}

		private void addCrossMeans(DoubleMatrix2D reconstructedFaces) {
			transform(reconstructedFaces, pixelMeans, (a, b) -> a + b);
		}

		private void transform(DoubleMatrix2D matrix, double[] horiValues, BiFunction<Double, Double, Double> f) {
			for (int row = 0; row < matrix.rows(); row++) {
				for (int column = 0; column < matrix.columns(); column++) {
					double v1 = matrix.get(row, column);
					double v2 = horiValues[column];

					double value = f.apply(v1, v2);
					matrix.setQuick(row, column, value);
				}
			}
		}

		private void saveMatrixAsImages(DoubleMatrix2D faces, String prefix) throws IOException {

			String filenameFormat = "reconstructed_%d.png";
			for (int row = 0; row < faces.rows(); row++) {
				String filename = String.format(filenameFormat, row);
				if(prefix != null) {
					filename = prefix + "_" + filename;
				}
				double[] pixels = faces.viewRow(row).toArray();

				BufferedImage image = ImageTool.createImageFromPixels(pixels, FaceConstants.FACE_WIDTH);
				ImageIO.write(image, "png", new File(filename));
			}

		}
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(EigenFaceRecognizer.class);

	private final boolean doReconstrution;

	/**
	 * Constructs an instance of this class
	 * 
	 * @param doReconstrution
	 *            True to require perform reconstruction of the faces from
	 *            EigenFaces; false no such operation.
	 */
	public EigenFaceRecognizer(boolean doReconstrution) {
		this.doReconstrution = doReconstrution;
	}

	@Override
	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face) {
		
		Recognizer recognizer = new Recognizer(faces);
		KeyValuePair<MatchInfo, double[]> kv = recognizer.recognize(face);
		
		MatchInfo matchInfo = kv.getKey();
		double[] newFaceWeights = kv.getValue();

		if (doReconstrution) {
			try {
				reconstruct(recognizer);
				reconstruct(recognizer, newFaceWeights);
			} catch (IOException e) {
				LOGGER.error("Error occurred during face reconstruction.", e);
			}
		}
		return matchInfo;
	}

	private void reconstruct(Recognizer recognizer) throws IOException {
		reconstructHelper(recognizer, recognizer.refWeights.copy(), "training");
	}
	
	private void reconstruct(Recognizer recognizer, double[] newFaceWeights) throws IOException {
		DoubleMatrix2D weights = new DenseDoubleMatrix2D(new double[][] {newFaceWeights});
		reconstructHelper(recognizer, weights, "testing");
	}
	
	private void reconstructHelper(Recognizer recognizer, DoubleMatrix2D weights, String filenamePrefix) throws IOException {

		recognizer.multiEigenValues(weights);
		DoubleMatrix2D faces = weights.zMult(recognizer.eigenspace, null);

		recognizer.addCrossMeans(faces);
		recognizer.saveMatrixAsImages(faces, filenamePrefix);
	}
}
