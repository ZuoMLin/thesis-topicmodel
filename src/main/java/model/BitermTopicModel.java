package model;

import util.DoubleArrayInit;
import util.Pretreatment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BTM模型
 */
public class BitermTopicModel extends BitermForm {

    // 词对集合 B x 2
    private int[][] biterms;

    // 词对集合对应的主题 B
    private int[] z;

    // 词对个数
    private int B;

    // 主题个数
    private int K;

    // 不同的词的个数
    private int W;

    // K x W 主题k下词w出现的个数
    private int[][] nkw;

    // K 主题k下词对的个数
    private int[] nk;

    // K 主题K下所有词的个数
    private int[] nksum;

    // K 词对集合-主题 超参数
    private double[] alpha;

    // K x W 主题-词 超参数
    private double[][] beta;

    // K 主题k下beta参数的和
    private double[] betaksum;

    public int getB() {
        return B;
    }

    public int[][] getNkw() {
        return nkw;
    }

    public int[] getNk() {
        return nk;
    }

    public int[] getNksum() {
        return nksum;
    }

    public double[] getBetaksum() {
        return betaksum;
    }

    public BitermTopicModel(String trainPath, int W, int K,
                            double alphaVal, double betaVal, int iterations) {
        int[][] documents = Pretreatment.readDocs(trainPath);
        List<int[]> bitermsList = new ArrayList<>();
        Pretreatment.doc2Biterm(documents, bitermsList, 10);
        biterms = new int[bitermsList.size()][2];
        bitermsList.toArray(biterms);

        this.W = W;
        this.K = K;

        B = biterms.length;

        alpha = new double[K];
        beta = new double[K][W];

        Arrays.fill(alpha, alphaVal);
        DoubleArrayInit.initMatrix(beta, K, W, betaVal);

        this.ITERATIONS = iterations;
    }

    public BitermTopicModel(String trainPath, int W, int K,
                            double[] alpha, double[][] beta, int iterations) {
        int[][] documents = Pretreatment.readDocs(trainPath);
        List<int[]> bitermsList = new ArrayList<>();
        Pretreatment.doc2Biterm(documents, bitermsList, 10);
        biterms = new int[bitermsList.size()][2];
        bitermsList.toArray(biterms);

        this.W = W;
        this.K = K;

        B = biterms.length;

        this.alpha = alpha;
        this.beta = beta;

        this.ITERATIONS = iterations;
    }

    @Override
    public void gibbs() {
        initial();

        long start = System.currentTimeMillis();

//        Posttreatment.writeMatrix(String.format("%sphi_k%d_iter%d.txt", out_dir, K, 0), Estimate.estimatePhi(K, W, nkw, beta, nksum, betaksum));

        for (int i = 0; i < ITERATIONS; ++i) {
            for (int b = 0; b < B; ++b) {
                int topic = sampleFullCondition(b);
                z[b] = topic;
            }
//            Posttreatment.writeMatrix(String.format("%sphi_k%d_iter%d.txt", out_dir, K, i+1), Estimate.estimatePhi(K, W, nkw, beta, nksum, betaksum));

            if ((i+1) % 20 == 0) {
//                Posttreatment.writeMatrix(String.format("%sphi_k%d_iter%d.txt", out_dir, K, i+1), Estimate.estimatePhi(K, W, nkw, beta, nksum, betaksum));
                System.out.printf("迭代%d次,耗时%fs\n", i+1, 1.0*(System.currentTimeMillis() - start)/1000);
            }
        }
    }

    @Override
    protected void initial() {
        nkw = new int[K][W];
        nk = new int[K];
        z = new int[B];
        nksum = new int[K];
        betaksum = new double[K];

        for (int b = 0; b < B; ++b) {
            int topic = (int) (Math.random() * K);
            z[b] = topic;
            nk[topic]++;
            nkw[topic][biterms[b][0]]++;
            nkw[topic][biterms[b][1]]++;
            nksum[topic] += 2;
        }

        for (int k = 0; k < K; ++k) {
            for (int w = 0; w < W; ++w) {
                betaksum[k] += beta[k][w];
            }
        }
    }

    @Override
    protected int sampleFullCondition(int b) {
        // 抹去词对b的主题
        int topic = z[b];
        nk[topic]--;
        nkw[topic][biterms[b][0]]--;
        nkw[topic][biterms[b][1]]--;
        nksum[topic] -= 2;

        // 累计主题K个可能的取值
        double[] p = new double[K];
        for (int k = 0; k < K; ++k) {
            p[k] = (nk[k] + alpha[k]) * ((nkw[k][biterms[b][0]] + beta[k][biterms[b][0]]) / (nksum[k] + betaksum[k]))
                    * ((nkw[k][biterms[b][1]] + beta[k][biterms[b][1]]) / (nksum[k] + betaksum[k] + 1));

        }

        for (int k = 1; k < K; ++k) {
            p[k] += p[k-1];
        }

        double u = Math.random() * p[K-1];
        for (topic = 0; topic < p.length; topic++) {
            if (u < p[topic])
                break;
        }

        // 补上b的主题
        nkw[topic][biterms[b][0]]++;
        nkw[topic][biterms[b][1]]++;
        nksum[topic] += 2;
        nk[topic]++;

        return topic;
    }

}
