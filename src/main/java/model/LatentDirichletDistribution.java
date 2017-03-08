package model;

import util.DoubleArrayInit;
import util.Pretreatment;

/**
 * LDA模型
 */
public class LatentDirichletDistribution extends TermForm {

    // 文本集合
    private int[][] documents;

    // 文本集合中的词对应的主题集合
    private int[][] z;

    // 文本数
    private int M;

    // 主题个数
    private int K;

    // 不同的词的个数
    private int W;

    // K x W 主题k下词w出现个数
    private int[][] nkw;

    // M x K 文本d下主题k词的个数
    private int[][] ndk;

    // M x K 文本主题的Dirichlet先验
    private double[][] alpha;

    // K x W 主题词的Dirichlet先验
    private double[][] beta;

    // K 主题k下所有的词的个数
    private int[] nksum;

    // K 主题k下beta值的和
    private double[] betaksum;

    public int getM() {
        return M;
    }

    public int[][] getNkw() {
        return nkw;
    }

    public int[][] getNdk() {
        return ndk;
    }

    public int[] getNksum() {
        return nksum;
    }

    public double[] getBetaksum() {
        return betaksum;
    }

    public LatentDirichletDistribution(String trainPath, int W, int K,
                                       double alphaVal, double betaVal, int iterations) {
        documents = Pretreatment.readDocs(trainPath);
        M = documents.length;
        this.W = W;
        this.K = K;
        alpha = new double[M][K];
        beta = new double[K][W];
        DoubleArrayInit.initMatrix(alpha, M, K, alphaVal);
        DoubleArrayInit.initMatrix(beta, K, W, betaVal);
        ITERATIONS = iterations;
    }

    public LatentDirichletDistribution(String trainPath, int W, int K,
                                       double[][] alpha, double[][] beta, int iterations) {
        documents = Pretreatment.readDocs(trainPath);
        M = documents.length;
        this.W = W;
        this.K = K;
        this.alpha = alpha;
        this.beta = beta;
        ITERATIONS = iterations;
    }

    /**
     * 1. 初始化计数器 2. 随机采样词w的主题k
     */
    @Override
    protected void initial() {
        // 初始化计数器
        nkw = new int[K][W];
        ndk = new int[M][K];
        nksum = new int[K];
        betaksum = new double[K];

        z = new int[M][];
        for (int m = 0; m < M; ++m) {
            int N = documents[m].length;
            z[m] = new int[N];
            for (int n = 0; n < N; ++n) {
                // 随机采样词w的主题k
                int topic = (int) (Math.random() * K);
                z[m][n] = topic;
                nkw[topic][documents[m][n]]++;
                ndk[m][topic]++;
                nksum[topic]++;
            }
        }

        // 初始化计数统计器
        for (int k = 0; k < K; ++k) {
            for (int w = 0; w < W; ++w) {
                betaksum[k] += beta[k][w];
            }
        }
    }

    @Override
    protected int sampleFullCondition(int m, int w) {
        // 抹去(m,w)的主题
        int topic = z[m][w];
        nkw[topic][documents[m][w]]--;
        nksum[topic]--;
        ndk[m][topic]--;

        // 累计K个可能主题的概率,均匀生成一个随机数确定落在的主题k范围
        double[] p = new double[K];
        for (int k = 0; k < K; ++k) {
            p[k] = 1000.0*(nkw[k][documents[m][w]] + beta[k][documents[m][w]]) / (nksum[k] + betaksum[k])
                    * (ndk[m][k] + alpha[m][k]);
        }

        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }

        double u = Math.random() * p[K - 1];
        for (topic = 0; topic < p.length; topic++) {
            if (u < p[topic])
                break;
        }

        // 加上(n,w)的主题
        nkw[topic][documents[m][w]]++;
        nksum[topic]++;
        ndk[m][topic]++;

        return topic;
    }

    @Override
    public void gibbs() {
        initial();
        long start = System.currentTimeMillis();

        // 开始采样
        for (int i = 0; i < ITERATIONS; ++i) {
            for (int m = 0; m < z.length; ++m) {
                for (int w = 0; w < z[m].length; ++w) {
                    int topic = sampleFullCondition(m, w);
                    z[m][w] = topic;
                }
            }
            if ((i+1) % 20 == 0) {
//                Posttreatment.writeMatrix(String.format("/Users/ZuoMLin/Desktop/thesis/experiments/LDA/phi_k%d_iter%d.txt", K, i+1), Estimate.estimatePhi(K, W, nkw, beta, nksum, betaksum));
                System.out.printf("迭代%d次,耗时%fs\n", i+1, 1.0*(System.currentTimeMillis() - start)/1000);
            }
        }
    }
}
