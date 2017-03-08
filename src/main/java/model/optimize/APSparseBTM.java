package model.optimize;

import model.BitermForm;
import util.DoubleArrayInit;
import util.Pretreatment;

import java.util.*;
import java.util.concurrent.*;

/**
 * 采样算法优化: BTM模型并行化近似的Gibbs采样方法,同时考虑到nwk的稀疏性
 */
public class APSparseBTM extends BitermForm {

    // 词对个数
    private int B;

    // 不同的词的个数
    private int V;

    // 主题个数
    private int K;

    // M个线程
    private int M;

    // 词V被分成S块 S = M x 2
    private int S;

    // 一次迭代中Configure要循环处理的次数 R = S + 1 (biterms以此划分)
    private int R;

    private int step_V;

    // 词对集合
    private int[][] biterms;

    // R x M x d_block x 2
    // Configure(M线程)R次处理并行计算依赖的词对集合
    private int[][][][] block_biterms;

    // R x M x d_block
    private int[][][] z;

    // K 主题k下词对的个数
    int[] nk;

    // K 主题k下所有词的个数
    int[] nksum;

    // W个ArrayList,ArrayList中保存非0的nwk值, 后12位表示主题
//    List<Integer>[] nwk;
    SparseMatrix nwk;


    // K 词对集合-主题 超参数
    double[] alpha;

    // K x W 主题-词 超参数
    double[][] beta;

    // K 主题k下beta参数的和
    double[] betaksum;

    private final CyclicBarrier barrier;

    private String out_dir;

    HashMap<VSegIndex, int[]> vSegIndex2BlockIndexMap;

    /**
     * 条件: V词分S块
     * 词对(w0, w1)对应(Si, Sj)
     * 初始化函数保证 Si <= Sj
     */
    private class VSegIndex {
        int seg0;
        int seg1;

        public VSegIndex(int seg0, int seg1) {
            if (seg0 > seg1) {
                this.seg0 = seg1;
                this.seg1 = seg0;
            } else {
                this.seg0 = seg0;
                this.seg1 = seg1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VSegIndex vSegIndex = (VSegIndex) o;

            if (seg0 != vSegIndex.seg0) return false;
            return seg1 == vSegIndex.seg1;

        }

        @Override
        public int hashCode() {
            int result = seg0;
            result = 31 * result + seg1;
            return result;
        }
    }

    public APSparseBTM(String trainPath, int V, int K, int M,
                       double alphaVal, double betaVal, int iterations) {
        int[][] documents = Pretreatment.readDocs(trainPath);
        List<int[]> bitermsList = new ArrayList<>();
        Pretreatment.doc2Biterm(documents, bitermsList, 10);
        biterms = new int[bitermsList.size()][2];
        bitermsList.toArray(biterms);

        this.V = V;
        this.K = K;
        this.M = M;

        B = biterms.length;

        alpha = new double[K];
        beta = new double[K][V];

        Arrays.fill(alpha, alphaVal);
        DoubleArrayInit.initMatrix(beta, K, V, betaVal);

        barrier = new CyclicBarrier(M);

        this.ITERATIONS = iterations;
    }

    @Override
    protected void initial() {
        S = M * 2;
        R = S + 1;
        step_V = (int) Math.ceil(1.0 * V / S);
        block_biterms = new int[R][M][][];
        buildVSegIndex2BlockIndexMap();

        // 创建临时链表作变长插入
        LinkedList<int[]>[][] temp = new LinkedList[R][M];
        for (int r = 0; r < R; ++r) {
            temp[r] = new LinkedList[M];
            for (int m = 0; m < M; ++m) {
                temp[r][m] = new LinkedList<>();
            }
        }
        for (int b = 0; b < B; ++b) {
            int[] index = findIndexInBlocks(biterms[b]);
            temp[index[0]][index[1]].add(biterms[b]);
        }
        // 临时链表转成最终的 block_biterms
        for (int r = 0; r < R; ++r) {
            for (int m = 0; m < M; ++m) {
                block_biterms[r][m] = new int[temp[r][m].size()][];
                temp[r][m].toArray(block_biterms[r][m]);
            }
        }

        nwk = new SparseMatrix(V);

        nk = new int[K];
        z = new int[R][M][];
        nksum = new int[K];
        betaksum = new double[K];

        for (int r = 0; r < R; ++r) {
            for (int m = 0; m < M; ++m) {
                z[r][m] = new int[block_biterms[r][m].length];
                for (int b = 0; b < block_biterms[r][m].length; ++b) {
                    int topic = (int) (Math.random() * K);
                    z[r][m][b] = topic;
                    nk[topic]++;
                    nwk.increase(block_biterms[r][m][b][0], topic);
                    nwk.increase(block_biterms[r][m][b][1], topic);
                    nksum[topic] += 2;
                }
            }
        }

        // nwk排序-逆序
        nwk.sort();
    }

    @Override
    public void gibbs() {
        initial();

        ExecutorService threadPools = Executors.newFixedThreadPool(M);

        long start = System.currentTimeMillis();
        long time_min = Long.MAX_VALUE;

        Runtime runtime = Runtime.getRuntime();

        for (int i = 0; i < ITERATIONS; ++i) {
            for (int r = 0; r < R; ++r) {
                Future<int[][]>[] futures = new Future[M];
                for (int m = 0; m < M; ++m) {
                    // 启动m个线程并行采样
                    futures[m] = threadPools.submit(new BlockGibbsSampler(block_biterms[r][m], z[r][m]));
                }
                int[] nksum_temp = nksum.clone();
                int[] nk_temp = nk.clone();
                for (int k = 0; k < K; ++k) {
                    nk_temp[k] *= (1-M);
                    nksum_temp[k] *= (1-M);
                }
                for (int j = 0; j < M; ++j) {
                    try {
                        int[][] locals = futures[j].get();
                        for (int k = 0; k < K; ++k) {
                            nk_temp[k] += locals[0][k];
                            nksum_temp[k] += locals[1][k];
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (int k = 0; k < K; ++k) {
                    nk[k] = nk_temp[k];
                    nksum[k] = nksum_temp[k];
                }
            }

            if ((i+1) % 5 == 0) {
                Long time_cur = System.currentTimeMillis() - start;
                if (time_cur < time_min) {
                    time_min = time_cur;
                }
                start = System.currentTimeMillis();
            }

        }

        System.gc();
        System.out.printf("主题数%d, 最小内存占用量%f\n", K, (runtime.totalMemory() - runtime.freeMemory())/1024.0/1024.0);
        System.out.printf("主题数%d, 平均一次迭代耗时%fs\n", K, 1.0*(time_min)/1000.0/5.0);

        threadPools.shutdown();

        // 稀疏性依据实验
//        int[] res = new int[K];
//        for (int w = 0; w < V; ++w) {
//            for (Map.Entry entry : nwk.getInnerMap(w).entrySet()) {
//                int k = (int) entry.getKey();
//                res[k]++;
//            }
//        }
//        System.out.println(Arrays.toString(res));
    }

    @Override
    protected int sampleFullCondition(int b) {
        return 0;
    }

    /**
     * 建立词对到分块矩阵的映射
     * map<VSegIndex, int[]>
     */
    private void buildVSegIndex2BlockIndexMap() {
        vSegIndex2BlockIndexMap = new HashMap<>();
        for (int i = 0; i < M; ++i) {
            vSegIndex2BlockIndexMap.put(new VSegIndex(i, i), new int[]{0, i});
            vSegIndex2BlockIndexMap.put(new VSegIndex(M + i, M + i), new int[]{1, i});
        }
        int row = 2;
        for (int i = 0; i < M; ++i) {
            int range = (int) Math.pow(2, i);
            for (int j = 0; j < range; ++j) {
                for (int m = 0; m < M; ++m) {
                    int seg0 = 2*range*(m/range)+m%range;
                    int seg1 = seg0 + range + j;
                    if (seg1 >= (m/range)*2*range+2*range)
                        seg1 -= range;
                    vSegIndex2BlockIndexMap.put(new VSegIndex(seg0, seg1), new int[]{row, m});
//                    System.out.print("[" + seg0 + ", " + seg1 + "]");
                }
                row++;
            }
        }
    }

    /**
     * 查找词对biterm(w0, w1)对应Blocks中的index
     * @param biterm int[]{w0, w1}
     * @return int[]{r, m}
     */
    private int[] findIndexInBlocks(int[] biterm) {
        int seg0 = biterm[0]/step_V;
        int seg1 = biterm[1]/step_V;
        return vSegIndex2BlockIndexMap.get(new VSegIndex(seg0, seg1));
    }

    private class BlockGibbsSampler implements Callable<int[][]> {
        // 局部的nk, 采样完成后需要合并计算
        private int[] local_nk;

        // 局部的nksum, 采样完成后需要合并计算
        private int[] local_nksum;

        // 局部的biterms, 只是从名字上作区别, 多个采样器并不会冲突
        private int[][] local_biterms;

        // 局部的z, 只是从名字上作区别, 多个采样器并不会冲突
        private int[] local_z;

        public BlockGibbsSampler(int[][] local_biterms, int[] local_z) {
            this.local_biterms = local_biterms;
            this.local_z = local_z;
        }

        @Override
        public int[][] call() throws Exception {
            local_nk = nk.clone();
            local_nksum = nksum.clone();

            for (int b = 0; b < local_biterms.length; ++b) {
                int topic = sparseSampleFullCondition(b);
                local_z[b] = topic;
            }

            int[][] res = new int[2][];
            res[0] = local_nk;
            res[1] = local_nksum;

            try {
                barrier.await();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return res;
        }


        //--------------Sparse采样--------------
        private int sparseSampleFullCondition(int b) {
            int w0 = local_biterms[b][0];
            int w1 = local_biterms[b][1];
            int topic = local_z[b];
            local_nk[topic]--;
            local_nksum[topic] -= 2;

            final double beta_square = beta[0][0] * beta[0][0];
            double[] I_val_K = new double[K];
            double[] D_val_K = new double[K];
            double[] G_val_K = new double[K];
            for (int k = 0; k < K; ++k) {
                double temp = local_nksum[k] + betaksum[k];
                I_val_K[k] = temp * (temp + 1);

                D_val_K[k] = local_nk[k] + alpha[k];
                G_val_K[k] = (local_nk[k] + alpha[0]) * beta_square / I_val_K[k];
            }

            double[] C_val_K = new double[K];
            Arrays.fill(C_val_K, beta[0][0]);
            double[] F_val_K = new double[K];

            for (ListIterator<Integer> iter = nwk.getInnerCollection(w1).listIterator(); iter.hasNext(); ) {
                int val = iter.next();
                int k = val & SparseMatrix.TOPIC_MASK;
                int count = val >>> SparseMatrix.TOPIC_BITS;
                if (k == topic) {
                    if (count-- == 1) {
                        iter.remove();
                        continue;
                    } else {
                        iter.set(val-SparseMatrix.ONE_COUNT);
                    }
                }
                C_val_K[k] += count;
                F_val_K[k] = count * D_val_K[k] * beta[k][w0] / I_val_K[k];
            }
            // List版本0.1
//            for (Integer val : nwk.getInnerCollection(w1)) {
//                int k = val & SparseMatrix.TOPIC_MASK;
//                int count = val >>> SparseMatrix.TOPIC_BITS;
//                if (k == topic) {
//                    count--;
//
//                }
//                C_val_K[k] += count;
//                F_val_K[k] = count * D_val_K[k] * beta[k][w0] / I_val_K[k];
//            }
            double[] A_val_K = new double[K];

            for (ListIterator<Integer> iter = nwk.getInnerCollection(w0).listIterator(); iter.hasNext(); ) {
                int val = iter.next();
                int k = val & SparseMatrix.TOPIC_MASK;
                int count = val >>> SparseMatrix.TOPIC_BITS;
                if (k == topic) {
                    if (count-- == 1) {
                        iter.remove();
                        continue;
                    } else {
                        iter.set(val-SparseMatrix.ONE_COUNT);
                    }
                }
                A_val_K[k] = count * C_val_K[k] * D_val_K[k] / I_val_K[k];
            }
            // List版本0.1
//            for (Integer val : nwk.getInnerCollection(w0)) {
//                int k = val & SparseMatrix.TOPIC_MASK;
//                int count = val >>> SparseMatrix.TOPIC_BITS;
//                A_val_K[k] = count * C_val_K[k] * D_val_K[k] / I_val_K[k];
//            }
            for (int k = 1; k < K; ++k) {
                G_val_K[k] += G_val_K[k-1];
                F_val_K[k] += F_val_K[k-1];
                A_val_K[k] += A_val_K[k-1];
            }

            double u = Math.random() * (A_val_K[K-1] + F_val_K[K-1] + G_val_K[K-1]);
            double[] decision;
            if (u < A_val_K[K-1]) {
                decision = A_val_K;
            } else if (u < A_val_K[K-1] + F_val_K[K-1]) {
                decision = F_val_K;
            } else {
                decision = G_val_K;
            }
            double q = Math.random() * decision[K-1];
            for (topic = 0; topic < decision.length; topic++) {
                if (q < decision[topic])
                    break;
            }
            nwk.increase(w0, topic);
            nwk.increase(w1, topic);
            local_nksum[topic]+=2;
            local_nk[topic]++;

            return topic;
        }

    }

}
