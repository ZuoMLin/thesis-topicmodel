package util;

import java.util.Iterator;
import java.util.List;

/**
 * 工具类: List链表转数组类
 */
public class List2Array {

    /**
     * Double链表转换成double数组
     * @param list
     * @return
     */
    public static double[] convertDoubles(List<Double> list) {
        double[] res = new double[list.size()];
        Iterator<Double> iterator = list.iterator();
        for (int i = 0; i < res.length; ++i) {
            res[i] = iterator.next().doubleValue();
        }
        return res;
    }

    /**
     * Integer链表转换成int数组
     * @param list
     * @return
     */
    public static int[] convertIntegers(List<Integer> list) {
        int[] res = new int[list.size()];
        Iterator<Integer> iterator = list.iterator();
        for (int i = 0; i < res.length; ++i) {
            res[i] = iterator.next().intValue();
        }
        return res;
    }
}
