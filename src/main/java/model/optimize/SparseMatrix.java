package model.optimize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * nwk稀疏性数据结构
 */
public class SparseMatrix {

    public static final int TOPIC_BITS = 12;
    public static final int TOPIC_MASK = (1 << TOPIC_BITS) - 1;
    public static final int ONE_COUNT = 1 << TOPIC_BITS;

    private List<Integer>[] _inner_collection;

    public List<Integer> getInnerCollection(int w) {
        return _inner_collection[w];
    }

    public SparseMatrix(int V) {
        _inner_collection = new ArrayList[V];
        for (int i = 0; i < _inner_collection.length; ++i) {
            _inner_collection[i] = new ArrayList<>();
        }
    }

    // 32位Integer, 后 m 位表示主题, 前 32-m 位表示nwk[w][k]
    public void increase(int w, int topic) {
        ListIterator<Integer> iter;
        for (iter = _inner_collection[w].listIterator(); iter.hasNext(); ) {
            int val = iter.next();
            if ((val & topic) == topic) {
                iter.set(val+ONE_COUNT);
                return;
            }
        }
        iter.add(ONE_COUNT+topic);
    }

    public void decrease(int w, int topic) {
        for (ListIterator<Integer> iter = _inner_collection[w].listIterator(); iter.hasNext(); ) {
            int val = iter.next();
            if ((val & TOPIC_MASK) == topic) {
                if (val>>>TOPIC_BITS == 1) {
                    iter.remove();
                } else {
                    iter.set(val-ONE_COUNT);
                }
                return;
            }
        }
    }

    public void sort() {
        for (List<Integer> list : _inner_collection) {
            list.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o2 - o1;
                }
            });
        }
    }

}
