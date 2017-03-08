package model.online;

import model.BitermTopicModel;
import util.DoubleArrayInit;
import util.Posttreatment;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * 在线BTM模型
 */
public class OnlineBTM implements OnlineTopicModel {

    // docs目录
    String docsDir;

    String out_dir = "/Users/ZuoMLin/Desktop/thesis/experiments/OBTM/";

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

    // B x 2 词对集合
    int[][] biterms;

    // 词对个数
    int B;

    // 当前的不同的词的个数
    int W;

    // 主题数
    int K;

    // K BTM模型中的alpha不变
    double[] alpha;

    // K x W beta遗传
    double[][] beta;

    private int iterations;

    public OnlineBTM(String docsDir, String thetaDir, String phiDir,
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
        alpha = new double[K];
        Arrays.fill(alpha, 2);
    }

    @Override
    public void start() {
        initial();

        for (int i = 0; i < slices; ++i) {
            // 读取新的docs, 重置B, W (假设docs都已经被index好了,也就是说不会有新的w)
            // TODO: 16/12/15 读入的是context版本的docs,W会增大,遗传矩阵该做些变化

            // 重新计算beta
            genetic();

            BitermTopicModel btm = new BitermTopicModel(
                    String.format("%s%d.txt", docsDir, i), W, K, alpha, beta, iterations);
            btm.gibbs();

            // TODO: 16/12/15 每个slice作参数估计,估计结果写入文件
            double[] theta = btm.estimateTheta(K, btm.getB(), btm.getNk(), alpha);
            double[][] phi = btm.estimatePhi(K, W, btm.getNkw(), beta, btm.getNksum(), btm.getBetaksum());

            Posttreatment.writeArray(String.format("%sk%d_day%d_Theta.txt", thetaDir, K, i), theta);
            Posttreatment.writeMatrix(String.format("%sk%d_day%d_Phi.txt", phiDir, K, i), phi);

            if (hnkw.size() >= windows) {
                hnkw.removeFirst();
            }
            hnkw.add(btm.getNkw());
        }
    }

    @Override
    public void genetic() {
        geneticBeta();
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
