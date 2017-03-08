package model;

/**
 * 主题模型抽象类
 */
public abstract class TopicModel {

    protected int ITERATIONS = 100;

    abstract public void gibbs();

    abstract protected void initial();

    /**
     * 隐参数Phi估计, 计算方法: 最后一次采样结果作为估计值
     */
    public double[][] estimatePhi(int K, int W,
                                  int[][] nkw, double[][] beta, int[] nksum, double[] betaksum) {
        double[][] phi = new double[K][W];
        for (int k = 0; k < K; ++k) {
            for (int w = 0; w < W; ++w) {
                phi[k][w] = (nkw[k][w] + beta[k][w]) / (nksum[k] + betaksum[k]);
            }
        }
        return phi;
    }
}
