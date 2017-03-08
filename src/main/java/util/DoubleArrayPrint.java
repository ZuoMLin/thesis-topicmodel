package util;

/**
 * 工具类: double[][]形式二维数组打印类
 */
public class DoubleArrayPrint {

    /**
     * 打印矩阵形式double数组
     * @param matrix
     * @param R
     * @param C
     */
    public static void printMatrix(double[][] matrix, int R, int C) {
        for (int r = 0; r < R; ++r) {
            for (int c = 0; c < C; ++c) {
                System.out.print(DoubleDecorate.bracket(matrix[r][c]) + " ");
            }
            System.out.println();
        }
    }
}
