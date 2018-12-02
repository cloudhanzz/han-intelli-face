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
	 * Divide each element of {@code array} by the array's max element value
	 * 
	 * @param array
	 *            Assuming its max value is not zero.
	 */
	public static void divideByMax(double[] array) {
		Arrays.stream(array).max().ifPresent(max -> {
			for (int i = 0; i < array.length; i++) {
				array[i] /= max;
			}
		});
	}

	/**
	 * Divide each element of {@code array} by the array's norm, which is the sum of
	 * the squares of all of its elements.
	 * 
	 * @param array
	 *            Assuming it is not empty and its norm value is not zero.
	 */
	public static void divideByNorm(double[] array) {
		double norm = Arrays.stream(array).map(a -> a * a).sum();
		for (int i = 0; i < array.length; i++) {
			array[i] /= norm;
		}
	}

	/**
	 * Returns the averages of the elements of all the arrays of the same indice
	 * 
	 * @param arrays
	 *            A non-empty 2D double array, requiring all arrays have the same
	 *            length
	 * @return The averages of the elements of all the arrays of the same indice
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

	/**
	 * Subtracts the each element of {@code arrays} by its corresponding cross mean
	 * 
	 * @param arrays
	 *            A non-empty 2D double array, requiring all arrays have the same
	 *            length
	 * @param crossMeans
	 *            Each elements represents the column mean of {@code arrays}
	 */
	public static void minusCrossMeans(double[][] arrays, double[] crossMeans) {

		int rows = arrays.length;
		int cols = arrays[0].length;

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				arrays[row][col] -= crossMeans[col];
			}
		}
	}

	public static void minusCrossMeans(double[] array, double[] means) {
		for (int i = 0; i < array.length; i++) {
			array[i] -= means[i];
		}
	}
}
