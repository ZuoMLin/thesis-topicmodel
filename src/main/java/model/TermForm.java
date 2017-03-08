package model;

/**
 * 词袋模型形式的主题模型抽象(例如:LDA)
 */
public abstract class TermForm extends TopicModel {

    /**
     * 词(m,w)全条件概率采样
     */
    abstract protected int sampleFullCondition(int m, int w);

    /**
     * 隐参数Theta估计, 计算方法: 最后一次采样结果作为估计值
     * @param M 文本数
     */
    public double[][] estimateTheta(int M, int K, int[][] ndk, double[][] alpha) {
        double[][] theta = new double[M][K];
        for (int m = 0; m < M; ++m) {
            double denominator = .0;
            for (int k = 0; k < K; ++k) {
                denominator += (ndk[m][k] + alpha[m][k]);
            }
            for (int k = 0; k < K; ++k) {
                theta[m][k] = (ndk[m][k] + alpha[m][k]) / denominator;
            }
        }
        return theta;
    }

}
