package burst;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举词突发概率
 */
public class Bursts {

    static class Slice {
        long rel_count;
        long doc_count;

        public Slice() {
            this.rel_count = 0;
            this.doc_count = 0;
        }

        public Slice(long rel_count, long doc_count) {
            this.rel_count = rel_count;
            this.doc_count = doc_count;
        }
    }

    /**
     * 最优突发状态序列 infinite-state automation
     * @param slices 切片序列
     * @param s 状态概率增长系数
     * @param gamma 状态转移cost函数系数
     */
    public static double burstStateSequence(List<Slice> slices, double s, double gamma) {
        if (s <= 1)
            throw new AssertionError("s must greater than 1!");
        long R = 0, D = 0;
        for (Slice slice : slices) {
            R += slice.rel_count;
            D += slice.doc_count;
        }
        double p_0 = 1.0 * R / D;
        int k = (int)Math.floor(Math.log(1 / p_0) / Math.log(s)) + 1;

        return burstStateSequence(slices, s, gamma, k);
    }

    /**
     * 最优突发状态序列 k-state automation
     * @param slices 切片序列
     * @param s 状态概率增长系数
     * @param gamma 状态转移cost函数系数
     * @param k 状态个数
     */
    public static double burstStateSequence(List<Slice> slices, double s, double gamma, int k) {
        long R = 0, D = 0;
        for (Slice slice : slices) {
            R += slice.rel_count;
            D += slice.doc_count;
        }

        double p_0 = 1.0 * R / D;

        // 相当于保存上一回合的cost
        double[] cost = new double[k];
        for (int i = 1; i < cost.length; ++i)
            cost[i] = Double.MAX_VALUE;

        // 状态序列
        int[][] statesSequence = new int[slices.size()+1][k];

        for (int i = 0; i < slices.size(); ++i) {
            // 当前计算得到的cost
            double[] cur_cost = new double[k];
            for (int j = 0; j < k; ++j) {
                // 每个前置状态t转移到j的cost取最小,即为当前最优状态
                double min = Double.MAX_VALUE;
                for (int t = 0; t < k; ++t) {
                    double temp = cost[t]+tau(gamma, t, j);
                    if (temp < min) {
                        min = temp;
                        statesSequence[i][j] = t;
                    }
                }
                cur_cost[j] = min;
            }
            for (int v = 0; v < k; ++v) {
                cost[v] = cur_cost[v] + sigma(p_0, s, v, slices.get(i));
            }
        }

        double min = Double.MAX_VALUE;
        int min_index = 0;
        for (int i = 0; i < cost.length; ++i) {
            if (min > cost[i]) {
                min = cost[i];
                min_index = i;
            }
        }
        return min_index == 0 ? 0 : normCDF(3.0*min_index/k);
    }

    /**
     * 标准正态分布累积分布函数
     */
    public static double normCDF(double a)
    {
        double p = 0.2316419;
        double b1 = 0.31938153;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;

        double x = Math.abs(a);
        double t = 1/(1+p*x);

        double val = 1 - (1/(Math.sqrt(2*Math.PI))  * Math.exp(-1*Math.pow(a, 2)/2)) * (b1*t + b2 * Math.pow(t,2) + b3*Math.pow(t,3) + b4 * Math.pow(t,4) + b5 * Math.pow(t,5) );

        if ( a < 0 )
        {
            val = 1- val;
        }

        return val;
    }


    /**
     * 状态转移cost
     * @param gamma 状态转移cost函数系数
     * @param i 第i状态
     * @param j 第j状态
     * @return cost
     */
    private static double tau(double gamma, int i, int j) {
        if (i >= j)
            return 0;
        else
            return (j - i) * gamma;
    }

    /**
     * 第i个状态下关联文本Slice生成cost
     * @param p_0 状态p_0概率
     * @param s 状态概率增长系数
     * @param i 第i个状态
     * @param slice
     * @return
     */
    private static double sigma(double p_0, double s, int i, Slice slice) {
        double p = p_0 * Math.pow(s, i);
        long r = slice.rel_count;
        long d = slice.doc_count;
        long r_temp = r;
        if (r > d/2)
            r = d-r;
        double res = 0;
        if (r != 0) {
            for (long j = 0; j < r; ++j) {
                res += Math.log(d - j);
                res -= Math.log(j + 1);
            }
        }
        res += (r_temp * Math.log(p));
        res += ((d-r_temp) * Math.log(1.0-p));
        return -res;
    }

    /**
     * 处理day天的数据, e.g. day=2, 输出 1 day=3, 输出 1, 2
     * rc format: wi wj  day:rel day:rel
     */
    public static void procDay(String bdrc_path, String bddc_path, String eta_path, int day) {
        try (
                BufferedReader dc_br = new BufferedReader(new InputStreamReader(new FileInputStream(bddc_path)));
                BufferedReader rc_br = new BufferedReader(new InputStreamReader(new FileInputStream(bdrc_path)));
        ) {
            // dc_list: daily doc_count
            long[] dc_list = new long[day+1];
            for (int i = 0; i <= day; ++i) {
                dc_list[i] = Integer.valueOf(dc_br.readLine());
            }
            String rc_line;
            Map b_eta = new HashMap<String, Double>();
            int total = 6887602;
            long start = System.currentTimeMillis();
            int index = 0;
            while ( (rc_line = rc_br.readLine()) != null) {
                String[] strs = rc_line.split("\t");
                String[] rc_strs = strs[1].split("\\s+");
                boolean hasBiterm = false;
                for (String str : rc_strs) {
                    if (Integer.valueOf(str.split(":")[0]) == day) {
                        hasBiterm = true;
                        break;
                    }
                }
                if (hasBiterm) {
                    List<Slice> slices = new ArrayList<>(day+1);
                    for (int k = 0; k <= day; ++k) {
                        slices.add(new Slice());
                        slices.get(k).doc_count = dc_list[k];
                    }
                    for (String str : rc_strs) {
                        String[] d_r = str.split(":");
                        int cur_day = Integer.valueOf(d_r[0]);
                        if (cur_day <= day) {
                            slices.get(cur_day).rel_count = Long.valueOf(d_r[1]);
                        }
                    }
                    double eta = burstStateSequence(slices, 1.1, 0.0001);
                    b_eta.put(strs[0], eta);
                }
                index ++;
                if (index%10000 == 0) {
                    long cur_time = System.currentTimeMillis();
                    System.out.printf("Cal:%d Remain:%d, Maybe %fs has to run.\n", index, total-index, 1.0*(cur_time-start)/index*(total-index)/1000.0);
                }
            }
            write_etas(b_eta, eta_path);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
    }

    public static void write_etas(Map<String, Double> etas, String path) {
        try (
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path))
        ){
            for (Map.Entry<String, Double> item: etas.entrySet()) {
                osw.write(String.format("%s\t%f\n", item.getKey(), item.getValue()));
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
    }

    static class DayTask implements Runnable {
        String bdrc_path;
        String bddc_path;
        String eta_path;
        int day;

        DayTask(String bdrc_path, String bddc_path, String eta_path, int day) {
            this.bddc_path = bddc_path;
            this.bdrc_path = bdrc_path;
            this.eta_path = eta_path;
            this.day = day;
        }

        @Override
        public void run() {
            procDay(bdrc_path, bddc_path, eta_path, day);
        }
    }
}
