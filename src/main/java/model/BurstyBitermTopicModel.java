package model;

import util.DoubleArrayInit;
import util.Pretreatment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BBTM模型
 */
public class BurstyBitermTopicModel extends BitermForm {

    // 词对集合 B x 2
    int[][] biterms;

    // 词对集合对应的主题 B
    int[] z;

    // B 词对突发概率
    double[] eta;

    // 词对个数
    int B;

    // 主题个数
    int K;

    // 不同的词的个数
    int W;

    // K x W 主题k下词w出现的个数
    int[][] nkw;

    // K 主题k下词对的个数
    int[] nk;

    // K 主题K下所有词的个数
    int[] nksum;

    // K 词对集合-主题 超参数
    double[] alpha;

    // K x W 主题-词 超参数
    double[][] beta;

    // K 主题k下beta参数的和
    double[] betaksum;

    // alpha参数和
    double alphasum;

    public BurstyBitermTopicModel(String trainPath, String etaPath, int W, int K,
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
            alphasum += alpha[k];
            for (int w = 0; w < W; ++w) {
                betaksum[k] += beta[k][w];
            }
        }
    }

    @Override
    public void gibbs() {
        initial();

        for (int i = 0; i < ITERATIONS; ++i) {
            for (int b = 0; b < B; ++b) {
                int topic = sampleFullCondition(b);
                z[b] = topic;
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
        p[0] = (1 - eta[b]) * (nkw[0][biterms[b][0]] + beta[0][biterms[b][0]])
                * (nkw[0][biterms[b][1]] + beta[0][biterms[b][1]])
                / (nksum[0] + betaksum[0] + 1)
                / (nksum[0] + betaksum[0]);
        for (int k = 1; k < K; ++k) {
            p[k] = eta[b] * (nk[k] + alpha[k]) / (B - 1 + alphasum) * (nkw[k][biterms[b][0]] + beta[k][biterms[b][0]])
                    * (nkw[k][biterms[b][1]] + beta[k][biterms[b][1]])
                    / (nksum[k] + betaksum[k] + 1)
                    / (nksum[k] + betaksum[k]);
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
        nksum[topic]+=2;
        nk[topic]++;

        return topic;
    }

}
