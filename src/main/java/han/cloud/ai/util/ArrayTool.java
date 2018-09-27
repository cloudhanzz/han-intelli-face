package han.cloud.ai.util;

import java.util.Arrays;

/**
 * A convenience class for performing Array operations.
 * 
 * <p>
 * <b>Term</b>
 * <p>
 * CrossMean refers to the mean of the column of a 2D array.
 * 
 * @author Jiayun Han
 *
 */
public final class ArrayTool {

	private ArrayTool() {
	}

	/**
	 * 
	 * @param array
	 *            Assume it is not empty and its max value is not zero.
	 */
	public static void divideByMax(double[] array) {
		Arrays.stream(array).max().ifPresent(max -> {
			for (int i = 0; i < array.length; i++) {
				array[i] = array[i] / max;
			}
		});
	}

	/**
	 * 
	 * @param array
	 */
	public static void divideByNorm(double[] array) {
		double norm = Arrays.stream(array).map(a -> a * a).sum();
		for (int i = 0; i < array.length; i++) {
			array[i] /= norm;
		}
	}

	/**
	 * 
	 * @param arrays
	 *            Assume it is not empty.
	 */
	public static double[] findCrossMeans(double[][] arrays) {

		int rows = arrays.length;
		int cols = arrays[0].length;

		double[] means = new double[cols];

		for (int col = 0; col < cols; col++) {
			double sum = 0;
			for (int row = 0; row < rows; row++) {
				sum += arrays[row][col];
			}
			double mean = sum / rows;
			means[col] = mean;
		}

		return means;
	}

	public static void minusCrossMeans(double[][] arrays, double[] means) {

		int rows = arrays.length;
		int cols = arrays[0].length;

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				arrays[row][col] = arrays[row][col] - means[col];
			}
		}
	}

	public static void minusCrossMeans(double[] array, double[] means) {
		for (int i = 0; i < array.length; i++) {
			array[i] -= means[i];
		}
	}
}
