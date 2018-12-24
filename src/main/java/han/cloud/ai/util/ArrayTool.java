package han.cloud.ai.util;

import java.util.Arrays;

/**
 * A convenience class for performing Array operations.
 * 
 * <p>
 * <b>Term</b>
 * <p>
 * {@code CrossMean} refers to the mean of the column of a 2D array.
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
	 *            The array to perform the division operation on
	 */
	public static void divideByMax(double[] array) {
		Arrays.stream(array).max().ifPresent(max -> {
			divideBy(array, max);
		});
	}

	/**
	 * Divide each element of {@code array} by the array's norm, which is the sum of
	 * the squares of all of its elements.
	 * 
	 * @param array
	 *            The array to perform the division operation on
	 */
	public static void divideByNorm(double[] array) {
		double norm = Arrays.stream(array).map(a -> a * a).sum();
		divideBy(array, norm);
	}

	private static void divideBy(double[] array, double denominator) {
		if (Double.compare(0, denominator) != 0) {
			for (int i = 0; i < array.length; i++) {
				array[i] /= denominator;
			}
		}
	}

	/**
	 * Returns the averages of the elements of all the arrays at the same indice
	 * 
	 * @param arrays
	 *            A non-empty 2D double array, requiring all arrays have the same
	 *            length
	 * @return The averages of the elements of all the arrays at the same indice
	 */
	public static double[] findCrossMeans(double[][] arrays) {

		int rows = arrays.length;
		int cols = arrays[0].length;

		double[] columnMeans = new double[cols];

		for (int col = 0; col < cols; col++) {
			double sum = 0;
			for (int row = 0; row < rows; row++) {
				sum += arrays[row][col];
			}
			double mean = sum / rows;
			columnMeans[col] = mean;
		}

		return columnMeans;
	}

	/**
	 * Subtracts from each element of {@code arrays} its corresponding cross mean
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

	/**
	 * Subtracts {@code means} from {@code array} element-wise
	 * 
	 * @param array
	 *            The array to subtract from
	 * @param means
	 *            The array to subtract with
	 */
	public static void minusCrossMeans(double[] array, double[] means) {
		for (int i = 0; i < array.length; i++) {
			array[i] -= means[i];
		}
	}
}
