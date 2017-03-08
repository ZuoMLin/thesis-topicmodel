package model.online;

import model.BitermTopicsOverTime;
import org.apache.commons.math3.special.Beta;
import util.DoubleArrayInit;
import util.List2Array;
import util.Posttreatment;
import util.Pretreatment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 在线BTOT模型
 */
public class OnlineBTOT implements OnlineTopicModel {

    // docs目录
    String docsDir;

    // 时间戳目录
    String timesDir;

    // theta目录
    String thetaDir;

    // phi目录
    String phiDir;

    // psi目录
    String psiDir;

    // 切片数
    int slices;

    // 遗传窗口
    int windows;

    // 遗传权重系数
    double wp;

    // windows x K x W history of nkw
    LinkedList<int[][]> hnkw;

    // windows x K x 2 history of psi
    LinkedList<double[][]> hpsi;

    // B x 2 词对集合
    int[][] biterms;

    // B 时间戳集合
    double[] t;

    // 词对个数
    int B;

    // 当前的不同的词的个数
    int W;

    // 主题数
    int K;

    // K OBTOT模型中的alpha不变
    double[] alpha;

    // K x W beta遗传
    double[][] beta;

    // K x 2 主题强度分布参数
    double[][] psi;

    private int iterations;

    public OnlineBTOT(String docsDir, String timesDir, String thetaDir, String phiDir, String psiDir,
                      double wp, int slices, int windows, int W, int K, int iterations) {
        this.docsDir = docsDir;
        this.timesDir = timesDir;
        this.thetaDir = thetaDir;
        this.phiDir = phiDir;
        this.psiDir = psiDir;
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
        psi = new double[K][2];
    }

    @Override
    public void start() {
        initial();

        for (int i = 0; i < slices; ++i) {
            // 读取新的docs, 重置B, W (假设docs都已经被index好了,也就是说不会有新的w)
            // TODO: 16/12/15 读入的是context版本的docs,W会增大,遗传矩阵该做些变化

            // 重新计算beta, 重置psi(psi在采样过程中变化),alpha不变
            genetic();
            DoubleArrayInit.initMatrix(psi, K, 2, 1);

            BitermTopicsOverTime btot = new BitermTopicsOverTime(
                    String.format("%s%d.txt", docsDir, i), String.format("%s%d.txt", timesDir, i),
                    W, K, alpha, beta, psi, iterations);
            btot.gibbs();


            // TODO: 16/12/15 每个slice作参数估计,估计结果写入文件
            double[] theta = btot.estimateTheta(K, btot.getB(), btot.getNk(), alpha);
            double[][] phi = btot.estimatePhi(K, W, btot.getNkw(), beta, btot.getNksum(), btot.getBetaksum());
            psi = btot.getPsi();

            Posttreatment.writeArray(String.format("%sk%d_day%d_Theta.txt", thetaDir, K, i), theta);
            Posttreatment.writeMatrix(String.format("%sk%d_day%d_Phi.txt", phiDir, K, i), phi);

            // TODO: 16/12/15 校验写入文件psi的值是否发生变化
            Posttreatment.writeMatrix(String.format("%sk%d_day%d_Psi.txt", psiDir, K, i), psi);

            if (hnkw.size() >= windows) {
                hnkw.removeFirst();
                hpsi.removeFirst();
            }
            hnkw.add(btot.getNkw());
            hpsi.add(psi);
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
                for (int i = 1; i <= history; ++i) {
                    double[][] psi_temp = hpsi.get(hpsi.size()-i);
                    int[][] nkw_temp = hnkw.get(hnkw.size()-i);
                    for (double t = 0.001; t < 1; t+=0.001) {
                        double weight = Math.pow(wp, i-t);
                        betaTemp += (weight * nkw_temp[k][w] *
                                Beta.regularizedBeta(t, psi_temp[k][0], psi_temp[k][1]));
                    }
                }
                beta[k][w] = betaTemp;
            }
        }
    }
}
