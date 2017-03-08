package util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 工具类: 预处理类(文件读取)
 */
public class Pretreatment {

    /**
     * 读文件docs解析为index标识的文本集合
     * @param docsPath docs文件路径
     * @return
     */
    public static int[][] readDocs(String docsPath) {
        try (
                BufferedReader docs_br = new BufferedReader(new InputStreamReader(new FileInputStream(docsPath)))
        ) {
            List<int[]> docsList = new ArrayList<>();
            String line;
            while ((line = docs_br.readLine()) != null) {
                String[] strs = line.split("\\s+");
                int[] doc = new int[strs.length];
                for (int i = 0; i < doc.length; ++i) {
                    doc[i] = Integer.valueOf(strs[i]);
                }
                docsList.add(doc);
            }
            int[][] res = new int[docsList.size()][];
            docsList.toArray(res);
            return res;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读时间戳集合
     * @param docsTimePath
     * @return
     */
    public static double[] readTimes(String docsTimePath) {
        try (
                BufferedReader docs_br = new BufferedReader(new InputStreamReader(new FileInputStream(docsTimePath)))
        ) {
            List<Double> timeList = new ArrayList<>();
            String line;
            while ((line = docs_br.readLine()) != null) {
                timeList.add(Double.valueOf(line));
            }
            return List2Array.convertDoubles(timeList);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 文本集合docs矩阵提取词对
     * @param docs 文本集合
     * @param biterms 词对集合
     * @param windows 词对提取步长
     */
    public static void doc2Biterm(int[][] docs, List<int[]> biterms, int windows) {

        for (int m = 0; m < docs.length; ++m) {
            int len = docs[m].length;
            for (int i = 0; i < len-1; ++i) {
                for (int j = i+1; j < Math.min(i+windows, len); ++j) {
                    // TODO: 16/12/14 词对有序
                    biterms.add(new int[]{docs[m][i], docs[m][j]});
                }
            }
        }
    }

    /**
     * 带时间戳的文本集合docs矩阵提取词对
     * @param docs 文本集合
     * @param docts 文本时间戳集合
     * @param biterms 词对集合
     * @param bitermts 词对时间戳集合
     * @param windows 词对提取步长
     */
    public static void doct2Bitermt(int[][] docs, double[] docts,
                                    List<int[]> biterms, List<Double> bitermts, int windows) {
        for (int m = 0; m < docs.length; ++m) {
            double doct = docts[m];
            int len = docs[m].length;
            for (int i = 0; i < len-1; ++i) {
                for (int j=i+1; j < Math.min(i+windows, len); ++j) {
                    // TODO: 16/12/14 词对有序
                    biterms.add(new int[]{docs[m][i], docs[m][j]});
                    bitermts.add(doct);
                }
            }
        }
    }

    /**
     * int[][] docs 转化成 M x M x Seg_D x N_Seg_D int[][][][]
     * @param docs
     * @param M
     * @param D
     * @param W
     * @param step_D
     * @param step_V
     * @return
     */
    public static int[][][][] docs2MMdocs(int[][] docs, int M, int D, int W, int step_D, int step_V) {
        HashMap<MMDSeg, LinkedList<Integer>> map = new HashMap<>();
        int[][][][] res = new int[M][M][][];

        for (int i = 0; i < M; ++i) {
            int j = 0;
            for (; j < M-1; ++j) {
                res[j][i] = new int[step_D][];
            }
            res[j][i] = new int[D-(M-1)*step_D][];
        }

        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < M; ++j) {
                for (int k = 0; k < res[i][j].length; ++k) {
                    map.put(new MMDSeg(i, j, k), new LinkedList<>());
                }
            }
        }

        for (int i = 0; i < docs.length; ++i) {
            int p = i/step_D;
            for (int j = 0; j < docs[i].length; ++j) {
                int q = docs[i][j]/step_V;
                int r = i - (step_D * p);
                map.get(new MMDSeg(p, q, r)).add(docs[i][j]);
                // 具体单词就随意往上添加
            }
        }

        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < M; ++j) {
                for (int k = 0; k < res[i][j].length; ++k) {
                    res[i][j][k] = List2Array.convertIntegers(map.get(new MMDSeg(i, j, k)));
                }
            }
        }

        return res;
    }

    /**
     * MMDSeg 唯一标识块中的文件
     */
    private static final class MMDSeg {
        private int seg_index_docs;
        private int seg_index_v;
        private int d_index;

        public MMDSeg(int seg_index_docs, int seg_index_v, int d_index) {
            this.seg_index_docs = seg_index_docs;
            this.seg_index_v = seg_index_v;
            this.d_index = d_index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MMDSeg mmdSeg = (MMDSeg) o;

            if (seg_index_docs != mmdSeg.seg_index_docs) return false;
            if (seg_index_v != mmdSeg.seg_index_v) return false;
            return d_index == mmdSeg.d_index;

        }

        @Override
        public int hashCode() {
            int result = seg_index_docs;
            result = 31 * result + seg_index_v;
            result = 31 * result + d_index;
            return result;
        }
    }

}
