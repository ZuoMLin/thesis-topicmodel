package model.optimize;

import model.TermForm;
import util.DoubleArrayInit;
import util.Pretreatment;

import java.util.concurrent.*;

/**
 * 模拟腾讯的PeacockLDA(多线程形式, 非分布式形式)
 */
public class PeacockLDA extends TermForm {

    // W x K (W作M分: 保证 W>1000) 主题k下词w的个数
    private int[][] nwk;

    // D x Nd (D作M分) 文本集合
//    private int[][] docs;

    // M x M x Dseg x Ndseg
    private int[][][][] mmdndocs;

    // M x M 分块文本集合对应主题
    private int[][][][] z;

    // K 每个线程中是独立的,在线程计算完成后合并
    private int[] nksum;

    // D x K
    private int[][] ndk;

    // 文本个数
    private int D;

    // 划分块数
    private int M;

    private int step_V;

    private int step_D;

    private int K;

    private int W;

    // K x W
    private double[][] beta;

    // K 主题k下beta值的和
    private double[] betaksum;

    // M x K 文本主题的Dirichlet先验
    private double[][] alpha;

    private final CyclicBarrier barrier;

    public PeacockLDA(String trainPath, int W, int K, int M,
                      double alphaVal, double betaVal, int iterations) {
        int[][] docs = Pretreatment.readDocs(trainPath);
        this.W = W;
        D = docs.length;
        this.K = 50;

        double[][] alpha = new double[D][K];
        double[][] beta = new double[K][W];

        DoubleArrayInit.initMatrix(alpha, D, K, alphaVal);
        DoubleArrayInit.initMatrix(beta, K, W, betaVal);

        this.M = M;
        // M x M  =  2 x 2
        step_D = (int) Math.ceil(1.0*D/M);
        step_V = (int) Math.ceil(1.0*W/M);
        mmdndocs = Pretreatment.docs2MMdocs(docs, M, D, W, step_D, step_V);

        barrier = new CyclicBarrier(M);

        ITERATIONS = iterations;

    }

    @Override
    protected void initial() {
        nwk = new int[W][K];
        ndk = new int[D][K];
        nksum = new int[K];
        betaksum = new double[K];

        z = new int[M][M][][];
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < M; ++j) {
                int D_temp = mmdndocs[i][j].length;
                z[i][j] = new int[D_temp][];
                for (int d = 0; d < D_temp; ++d) {
                    int d_real = i * step_D + d;
                    int N_temp = mmdndocs[i][j][d].length;
                    z[i][j][d] = new int[N_temp];
                    for (int n = 0; n < N_temp; ++n) {
                        int topic = (int) (Math.random() * K);
                        z[i][j][d][n] = topic;
                        nwk[mmdndocs[i][j][d][n]][topic]++;
                        ndk[d_real][topic]++;
                        nksum[topic]++;
                    }
                }
            }
        }

        for (int k = 0; k < K; ++k) {
            for (int w = 0; w < W; ++w) {
                betaksum[k] += beta[k][w];
            }
        }
    }

    @Override
    public void gibbs() {
        initial();

        // 线程池方式实现,避免反复创建线程
        ExecutorService threadPools = Executors.newFixedThreadPool(M);

        long start = System.currentTimeMillis();

        // 假设只有1个Configure,即 M个SampleServer, M个DataServer
        for (int i = 0; i < ITERATIONS; ++i) {
            // 相应的 M x M 矩阵中有 M个diagonal, 故需要计算M次
            for (int m = 0; m < M; ++m) {
                // 每个对角线
                // 启动 M个线程
                // [SampleServer j]存储nwk[j * step_V 到 (j+1)*step_V-1][...]
                // diagonal m 中, [SampleServer j]去取对应的文本集合第[j-m(j>=m) or M+j-m(j<m)]块中的第j块的词[j*step_V,(j+1)*step_V-1]
                // 主题采样完成后,对 ndk 和 nwk 作更新
                // 等待线程执行完

                // 第j个SamplerServer

                Future<int[]>[] futures = new Future[M];
                for (int j = 0; j < M; ++j) {
                    int index_V = j;
                    int index_D = -1;
                    if (j>=m) {
                        index_D = j-m;
                    } else {
                        index_D = M+j-m;
                    }

                    futures[j] = threadPools.submit(new SegmentGibbsSampler(
                            index_D, mmdndocs[index_D][index_V], z[index_D][index_V]));

                }
                int[] nksum_temp = nksum.clone();
                for (int k = 0; k < K; ++k) {
                    nksum_temp[k] *= (1-M);
                }
                for (int j = 0; j < M; ++j) {
                    try {
                        int[] local_nksum = futures[j].get();
                        for (int k = 0; k < K; ++k) {
                            nksum_temp[k] += local_nksum[k];
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (int k = 0; k < K; ++k) {
                    nksum[k] = nksum_temp[k];
                }
            }
            if (i % 10 == 0) {
                System.out.printf("迭代%d次,耗时%d s\n", i, (System.currentTimeMillis() - start)/1000);
            }
        }
        threadPools.shutdown();
    }

    public class SegmentGibbsSampler implements Callable<int[]> {

        // 第index_D块文本集合
        private int index_D;

        private int[][] docs_seg;

        private int[][] z_seg;

        private int[] local_nksum;

        public SegmentGibbsSampler(int index_D,
                                  int[][] docs_seg, int[][] z_seg) {
            this.index_D = index_D;
            this.docs_seg = docs_seg;
            this.z_seg = z_seg;
        }

        @Override
        public int[] call() throws Exception {
            local_nksum = nksum.clone();

            // 找到 docs [index_D*step_D...(index_D+1)*step_D-1][] 下面所有在词区间 [index_V*step_V...(index_V+1)*step_V-1]中的词采样
            for (int d_seg = 0; d_seg < docs_seg.length; ++d_seg) {
                int d_real = index_D * step_D + d_seg;
                for (int w = 0; w < docs_seg[d_seg].length; ++w) {
                    int topic = sampleFullCondition(d_seg, w, d_real);
                    z_seg[d_seg][w] = topic;
                }
            }
            try {
                barrier.await();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return local_nksum;
        }


        /**
         * 对(m,w)词作全条件概率采样
         * @param m Segment中第m个文本
         * @param w Segment_m文本中第w个词
         * @param m_real docs中第m_real个文本
         * @return
         */
        private int sampleFullCondition(int m, int w, int m_real) {
            // 抹去(m,w)的主题
            int topic = z_seg[m][w];
            nwk[docs_seg[m][w]][topic]--;
            local_nksum[topic]--;
            ndk[m_real][topic]--;

            // 累计K个可能主题的概率,均匀生成一个随机数确定落在的主题k范围
            double[] p = new double[K];
            for (int k = 0; k < K; ++k) {
                p[k] = 1000.0*(nwk[docs_seg[m][w]][k] + beta[k][docs_seg[m][w]]) / (local_nksum[k] + betaksum[k])
                        * (ndk[m_real][k] + alpha[m_real][k]);
            }

            for (int k = 1; k < p.length; ++k) {
                p[k] += p[k - 1];
            }

            double u = Math.random() * p[K - 1];
            for (topic = 0; topic < p.length; ++topic) {
                if (u < p[topic])
                    break;
            }

            // 加上(n,w)的主题
            nwk[docs_seg[m][w]][topic]++;
            local_nksum[topic]++;
            ndk[m_real][topic]++;

            return topic;
        }

    }

    @Override
    protected int sampleFullCondition(int m, int w) {
        return 0;
    }


}
