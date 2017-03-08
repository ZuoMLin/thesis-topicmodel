package model;

import org.apache.commons.math3.special.Gamma;
import util.DoubleArrayInit;
import util.List2Array;
import util.Pretreatment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BTOT模型
 */
public class BitermTopicsOverTime extends BitermForm implements TimestampForm {

    private int[][] biterms;

    private int[] z;

    private double[] t;

    private int B;

    private int K;

    private int W;

    private int[][] nkw;

    private int[] nk;

    private int[] nksum;

    private double[] alpha;

    private double[][] beta;

    // K x 2 主题强度分布参数
    private double[][] psi;

    private double[] betaksum;

    // K Beta值
    private double[] betapsik;

    // K 主题k下词对时间t的和
    private double[] tksum;

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

    public double[][] getPsi() {
        return psi;
    }

    public double[] getBetaksum() {
        return betaksum;
    }

    public double[] getBetapsik() {
        return betapsik;
    }

    public double[] getTksum() {
        return tksum;
    }

    public BitermTopicsOverTime(String docsPath, String timesPath, int W, int K,
                                double alphaVal, double betaVal, double psiVal, int iterations) {
        int[][] docs = Pretreatment.readDocs(docsPath);
        double[] times = Pretreatment.readTimes(timesPath);

        List<int[]> bitermList = new ArrayList<>();
        List<Double> timeList = new ArrayList<>();
        Pretreatment.doct2Bitermt(docs, times, bitermList, timeList, 10);
        biterms = new int[bitermList.size()][2];
        bitermList.toArray(biterms);
        t = List2Array.convertDoubles(timeList);

        this.W = W;
        this.K = K;

        B = biterms.length;

        alpha = new double[K];
        beta = new double[K][W];
        psi = new double[K][2];

        Arrays.fill(alpha, alphaVal);
        DoubleArrayInit.initMatrix(beta, K, W, betaVal);
        DoubleArrayInit.initMatrix(psi, K, 2, psiVal);

        ITERATIONS = iterations;
    }

    public BitermTopicsOverTime(String docsPath, String timesPath, int W, int K,
                                double[] alpha, double[][] beta, double[][] psi, int iterations) {
        int[][] docs = Pretreatment.readDocs(docsPath);
        double[] times = Pretreatment.readTimes(timesPath);

        List<int[]> bitermList = new ArrayList<>();
        List<Double> timeList = new ArrayList<>();
        Pretreatment.doct2Bitermt(docs, times, bitermList, timeList, 10);
        biterms = new int[bitermList.size()][2];
        bitermList.toArray(biterms);
        t = List2Array.convertDoubles(timeList);

        this.W = W;
        this.K = K;

        B = biterms.length;

        this.alpha = alpha;
        this.beta = beta;
        this.psi = psi;

        ITERATIONS = iterations;
    }

    @Override
    protected void initial() {
        z = new int[B];
        nkw = new int[K][W];
        nk = new int[K];
        nksum = new int[K];
        betaksum = new double[K];
        betapsik = new double[K];
        tksum = new double[K];

        for (int b = 0; b < B; ++b) {
            int topic = (int) (Math.random() * K);
            z[b] = topic;
            nk[topic]++;
            nkw[topic][biterms[b][0]]++;
            nkw[topic][biterms[b][1]]++;
            nksum[topic] += 2;
            tksum[topic] += t[b];
        }
        for (int k = 0; k < K; ++k) {
            for (int w = 0; w < W; ++w) {
                betaksum[k] += beta[k][w];
            }
            betapsik[k] = Gamma.gamma(psi[k][0]) * Gamma.gamma(psi[k][1]) / Gamma.gamma(psi[k][0]+psi[k][1]);
        }
    }

    @Override
    public void gibbs() {
        initial();

        long start = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; ++i) {
            for (int b = 0; b < B; ++b) {
                int topic = sampleFullCondition(b);
                z[b] = topic;
            }
            for (int k = 0; k < K; ++k) {
                estimatePsi(k);
            }

            if ((i+1) % 2 == 0) {
//                Posttreatment.writeMatrix(String.format("%sphi_k%d_iter%d.txt", out_dir, K, i+1), Estimate.estimatePhi(K, W, nkw, beta, nksum, betaksum));
                System.out.printf("迭代%d次,耗时%fs\n", i+1, 1.0*(System.currentTimeMillis() - start)/1000);
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
            p[k] = (nk[k] + alpha[k]) * (nkw[k][biterms[b][0]] + beta[k][biterms[b][0]])
                    * (nkw[k][biterms[b][1]] + beta[k][biterms[b][1]])
                    / (nksum[k] + betaksum[k] + 1)
                    / (nksum[k] + betaksum[k])
                    * Math.pow(t[b], psi[k][0]-1)
                    * Math.pow(1-t[b], psi[k][1]-1)
                    / betapsik[k];
        }

//        System.out.printf("hello%f, %f, %f, %f\n", nksum[0] + betaksum[0], nksum[1] + betaksum[1], Math.pow(t[b], psi[0][0]-1) * Math.pow(1-t[b], psi[0][1]-1),
//                Math.pow(t[b], psi[1][0]-1) * Math.pow(1-t[b], psi[1][1]-1));

        for (int k = 1; k < K; ++k) {
            p[k] += p[k-1];
        }

        double u = Math.random() * p[K-1];
        for (topic = 0; topic < p.length; topic++) {
            if (u < p[topic])
                break;
        }

        // 补上b的主题
//        System.out.printf("%f, (%f %f), topic: %d\n" , u, p[0], p[1], topic);
//        System.out.printf("Beta: %f (%f %f)\n", betapsik[1], psi[1][0], psi[1][1]);
        nkw[topic][biterms[b][0]]++;
        nkw[topic][biterms[b][1]]++;
        nksum[topic]+=2;
        nk[topic]++;

        return topic;
    }

    @Override
    public void estimatePsi(int k) {
        // 若: 词对集中没有主题k的词对,还原为均匀分布
        if (nk[k] == 0) {
            psi[k][0] = 1;
            psi[k][1] = 1;
            betapsik[k] = 1;
            return;
        }
        double avg = tksum[k] / nk[k];
        if (avg == 0)
            avg = 0.0001;
        else if (avg == 1)
            avg = 1.0 - 0.0001;
        double variance = .0;
        for (int b = 0; b < B; ++b) {
            if (z[b] == k) {
                variance += Math.pow(t[b] - avg, 2);
            }
        }
        variance /= (nk[k]-1);
        double psik0 = avg * (avg * (1-avg) / variance -1);
        double psik1 = (1 - avg) * (avg * (1-avg) / variance -1);

        psi[k][0] = psik0;
        psi[k][1] = psik1;

        betapsik[k] = Gamma.gamma(psik0) * Gamma.gamma(psik1) / Gamma.gamma(psik0+psik1);
    }

}
