package han.cloud.ai.face;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javax.imageio.ImageIO;

import han.cloud.ai.face.ifs.FaceRecognizer;
import han.cloud.ai.model.MatchInfo;
import han.cloud.ai.util.ArrayTool;
import han.cloud.ai.util.FaceConstants;
import han.cloud.ai.util.ImageTool;
import han.cloud.ai.util.KeyValuePair;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

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
public class EigenFaceRecognizerComplete implements FaceRecognizer {

	private static final class Eigen {

		private final double value;
		private final DoubleMatrix1D vector;

		private Eigen(double value, DoubleMatrix1D vector) {
			this.value = value;
			this.vector = vector;
		}
	}

	private final int max;
	private final int subMax;

	private final double[] pixelMeans;
	private final double[] eigenvalues;

	private final DoubleMatrix2D eigenspace;
	private final DoubleMatrix2D refWeights;

	/**
	 * 
	 * @param faces
	 *            Assuming they are gray and in standard size
	 */
	public EigenFaceRecognizerComplete(List<BufferedImage> faces) {

		this.max = faces.size();
		this.subMax = max - 1;

		double[][] refData = faces //
				.stream() //
				.map(this::divideByMaxPixel) //
				.toArray(double[][]::new);

		this.pixelMeans = ArrayTool.findCrossMeans(refData);
		ArrayTool.minusCrossMeans(refData, pixelMeans);

		DenseDoubleMatrix2D refFaces = new DenseDoubleMatrix2D(refData);
		KeyValuePair<DoubleMatrix2D, double[]> kv = buildEigenspaceAndEigenvalues(refFaces);

		this.eigenspace = kv.getKey();
		this.eigenvalues = kv.getValue();

		this.refWeights = refFaces.zMult(eigenspace.viewDice(), null);
	}

	public MatchInfo recognize(BufferedImage face) {

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
		MatchInfo result = new MatchInfo(distance, index);

		return result;
	}

	public void reconstruct() throws IOException {

		DoubleMatrix2D weights = this.refWeights.copy();

		processWeights(weights);
		DoubleMatrix2D faces = weights.zMult(eigenspace, null);

		addPixelMeans(faces);
		saveMatrixAsImages(faces);
	}

	private void processWeights(DoubleMatrix2D faceWeights) {
		transform(faceWeights, eigenvalues, (a, b) -> a * b);
	}

	private void addPixelMeans(DoubleMatrix2D reconstructedFaces) {
		transform(reconstructedFaces, pixelMeans, (a, b) -> a + b);
	}

	private void transform(DoubleMatrix2D maxtrix, double[] horiValues, BiFunction<Double, Double, Double> f) {
		for (int row = 0; row < maxtrix.rows(); row++) {
			for (int col = 0; col < maxtrix.columns(); col++) {
				double v1 = maxtrix.get(row, col);
				double v2 = horiValues[col];

				double value = f.apply(v1, v2);
				maxtrix.setQuick(row, col, value);
			}
		}
	}

	private DoubleMatrix2D projectFace(BufferedImage face) {

		double[] pixels = divideByMaxPixel(face);
		ArrayTool.minusCrossMeans(pixels, pixelMeans);

		double[][] faceData = { pixels };
		DoubleMatrix2D faceMatrix = new DenseDoubleMatrix2D(faceData);

		return faceMatrix.zMult(eigenspace.viewDice(), null);
	}

	private double[] divideByMaxPixel(BufferedImage image) {
		double[] pixels = ImageTool.toPixels(image, FaceConstants.width, FaceConstants.height);
		ArrayTool.divideByMax(pixels);
		return pixels;
	}

	private KeyValuePair<DoubleMatrix2D, double[]> buildEigenspaceAndEigenvalues(DoubleMatrix2D refFaces) {

		List<Eigen> eigens = buildAndSortEigens(refFaces);

		KeyValuePair<DoubleMatrix2D, double[]> kv = getEigenvectorsAndEigenvalues(eigens);

		DoubleMatrix2D eigenvectors = kv.getKey();
		double[] eigenvalues = kv.getValue();

		DoubleMatrix2D space = buildEigenspace(eigenvectors, refFaces);
		return new KeyValuePair<DoubleMatrix2D, double[]>(space, eigenvalues);
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

	private DoubleMatrix2D buildEigenspace(DoubleMatrix2D eigenvectors, DoubleMatrix2D refFaces) {

		DoubleMatrix2D eigenfaces = eigenvectors.viewDice().zMult(refFaces, null);

		for (int i = 0; i < subMax; i++) {
			double[] eigenface = eigenfaces.viewRow(i).toArray();
			ArrayTool.divideByNorm(eigenface);
			eigenfaces.viewRow(i).assign(eigenface);
		}

		return eigenfaces.viewPart(0, 0, subMax, FaceConstants.columns);
	}

	private static void saveMatrixAsImages(DoubleMatrix2D imageMatrix) throws IOException {

		String filenameFormat = "reconstructed_%d.png";

		for (int row = 0; row < imageMatrix.rows(); row++) {

			String filename = String.format(filenameFormat, row);
			double[] pixels = imageMatrix.viewRow(row).toArray();

			BufferedImage image = ImageTool.createImageFromPixels(pixels, FaceConstants.width);
			ImageIO.write(image, "png", new File(filename));
		}
	}

	@Override
	public MatchInfo recognize(List<BufferedImage> faces, BufferedImage face) {
		
		return null;
	}
}
