package model.online;

import model.LatentDirichletDistribution;
import util.DoubleArrayInit;
import util.Posttreatment;

import java.util.LinkedList;

/**
 * 在线LDA模型
 */
public class OnlineLDA implements OnlineTopicModel {
    // docs目录
    String docsDir;

    // theta目录
    String thetaDir;

    // phi目录
    String phiDir;

    // 切片数
    int slices;

    // 遗传窗口
    int windows;

    // windows 遗传权重系数
    double[] wp;

    // windows x K x W history of nkw
    LinkedList<int[][]> hnkw;

    // 当前切片的文本集合
    int[][] curDocs;

    // 当前的不同的词的个数
    int W;

    // 主题数
    int K;

    // 当前文本集合的文本数
    int M;

    // M x K LDA模型中的alpha不遗传,只通过初始值设定
    double[][] alpha;

    // K x W beta遗传
    double[][] beta;

    private int iterations;

    public OnlineLDA(String docsDir, String thetaDir, String phiDir,
                     double[] wp, int slices, int windows, int W, int K, int iterations) {
        this.docsDir = docsDir;
        this.thetaDir = thetaDir;
        this.phiDir = phiDir;
        this.wp = wp;
        this.slices = slices;
        this.windows = windows;
        this.W = W;
        this.K = K;
        this.iterations = iterations;
    }

    @Override
    public void initial() {
        hnkw = new LinkedList<>();
    }

    @Override
    public void start() {
        initial();

        for (int i = 0; i < slices; ++i) {
            // 读取新的docs, 重置M, W (假设docs都已经被index好了,也就是说不会有新的w)
            // TODO: 16/12/15 读入的是context版本的docs,W会增大,遗传矩阵该做些变化

            // 重新计算alpha, beta
            genetic();

            LatentDirichletDistribution lda = new LatentDirichletDistribution(
                    String.format("%s%d.txt", docsDir, i), W, K, alpha, beta, iterations);
            lda.gibbs();

            // TODO: 16/12/15 每个slice作参数估计,估计结果写入文件
            double[][] theta = lda.estimateTheta(lda.getM(), K, lda.getNdk(), alpha);
            double[][] phi = lda.estimatePhi(K, W, lda.getNkw(), beta, lda.getNksum(), lda.getBetaksum());

            Posttreatment.writeMatrix(String.format("%sk%d_day%d_Theta.txt", thetaDir, K, i), theta);
            Posttreatment.writeMatrix(String.format("%sk%d_day%d_Phi.txt", phiDir, K, i), phi);

            if (hnkw.size() >= windows) {
                hnkw.removeFirst();
            }
            hnkw.add(lda.getNkw());
        }
    }

    @Override
    public void genetic() {
        geneticAlpha();
        geneticBeta();
    }

    /**
     * alpha超参数遗传(OLDA模型并不需要,只是文本数M变化,alpha的维度跟着变化)
     */
    private void geneticAlpha() {
        alpha = new double[M][K];
        DoubleArrayInit.initMatrix(alpha, M, K, 2);
    }

    private void geneticBeta() {
        // 遗传矩阵空,以初始值设置beta
        int history = hnkw.size();
        if (history == 0) {
            beta = new double[K][W];
            DoubleArrayInit.initMatrix(beta, K, W, .5);
            return;
        }
        for (int k = 0; k < K; ++k) {
            for (int w = 0; w < W; ++w) {
                double betaTemp = .0;
                for (int i = 0; i < history; ++i) {
                    double weight = wp[windows-history+i];
                    int[][] temp = hnkw.get(i);
                    betaTemp += (weight * temp[k][w]);
                }
                beta[k][w] = betaTemp;
            }
        }
    }
}
