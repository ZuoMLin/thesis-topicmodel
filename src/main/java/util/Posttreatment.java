package util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具类: 后处理类(文件写入)
 */
public class Posttreatment {

    /**
     * matrix矩阵写入文件
     * @param path
     * @param matrix
     */
    public static void writeMatrix(String path, double[][] matrix) {
        try (
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path))
        ){
            for (int i = 0; i < matrix.length; ++i) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < matrix[i].length-1; ++j) {
                    sb.append(matrix[i][j] + " ");
                }
                sb.append(matrix[i][matrix[i].length-1] + "\n");
                osw.write(sb.toString());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * array数组写入文件
     * @param path
     * @param array
     */
    public static void writeArray(String path, double[] array) {
        try (
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path))
        ){
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length-1; ++i) {
                sb.append(array[i] + " ");
            }
            sb.append(array[array.length-1]);
            osw.write(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 读文件phi
     * @param phiPath phi文件路径
     * @return
     */
    public static double[][] readPhi(String phiPath) {
        try (
                BufferedReader docs_br = new BufferedReader(new InputStreamReader(new FileInputStream(phiPath)))
        ) {
            List<double[]> phiList = new ArrayList<>();
            String line;
            while ((line = docs_br.readLine()) != null) {
                String[] strs = line.split("\\s+");
                double[] phi_temp = new double[strs.length];
                for (int i = 0; i < phi_temp.length; ++i) {
                    phi_temp[i] = Double.valueOf(strs[i]);
                }
                phiList.add(phi_temp);
            }
            double[][] res = new double[phiList.size()][];
            phiList.toArray(res);
            return res;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
