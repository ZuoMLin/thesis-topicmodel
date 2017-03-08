package model;

/**
 * 词对模型形式的主题模型抽象(例如:BTM)
 */
public abstract class BitermForm extends TopicModel {

    /**
     * 词对b全条件概率采样
     */
    abstract protected int sampleFullCondition(int b);

    /**
     * 隐参数Theta估计, 计算方法: 最后一次采样结果作为估计值
     */
    public double[] estimateTheta(int K, int B, int[] nk, double[] alpha) {
        double[] theta = new double[K];

        double denominator = B;
        for (int k = 0; k < K; ++k) {
            denominator += alpha[k];
        }

        for (int k = 0; k < K; ++k) {
            theta[k] = (nk[k] + alpha[k]) / denominator;
        }
        return theta;
    }

}
