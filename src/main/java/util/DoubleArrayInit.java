package util;

import java.util.Arrays;

/**
 * 工具类: double[][]形式二维数组初始化类
 */
public class DoubleArrayInit {

    public static void initMatrix(double[][] matrix, int R, int C, double val) {
        for (int r = 0; r < R; ++r) {
            Arrays.fill(matrix[r], val);
        }
    }
}
