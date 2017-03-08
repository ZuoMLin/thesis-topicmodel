package factory;

import model.*;
import model.optimize.APSparseBTM;
import model.optimize.PeacockLDA;

import java.util.HashMap;
import java.util.Map;

/**
 * 主题模型工厂类(静态工厂模式)
 */
public class TopicModelFactory {

    public static TopicModel createModel(String modelType, Map<String, Object> paras) {
        if (modelType == null)
            return null;
        switch (modelType) {
            case "LDA":
                return new LatentDirichletDistribution((String) paras.get("trainPath"),
                        (int) paras.get("W"), (int) paras.get("K"),
                        (double) paras.get("alpha"), (double) paras.get("beta"), (int) paras.get("iterations"));
            case "BTM":
                return new BitermTopicModel((String) paras.get("trainPath"), (int) paras.get("W"), (int) paras.get("K"),
                        (double) paras.get("alpha"), (double) paras.get("beta"), (int) paras.get("iterations"));
            case "BTOT":
                return new BitermTopicsOverTime((String) paras.get("docsPath"), (String) paras.get("timesPath"),
                        (int) paras.get("W"), (int) paras.get("K"),
                        (double) paras.get("alpha"), (double) paras.get("beta"), (double) paras.get("psi"),
                        (int) paras.get("iterations"));
            case "PeacockLDA":
                return new PeacockLDA((String) paras.get("trainPath"),
                        (int) paras.get("W"), (int) paras.get("K"), (int) paras.get("M"),
                        (double) paras.get("alpha"), (double) paras.get("beta"), (int) paras.get("iterations"));
            case "APSparseBTM":
                return new APSparseBTM((String) paras.get("trainPath"),
                        (int) paras.get("W"), (int) paras.get("K"), (int) paras.get("M"),
                        (double) paras.get("alpha"), (double) paras.get("beta"), (int) paras.get("iterations"));
            case "BBTM":
                return new BurstyBitermTopicModel((String) paras.get("trainPath"), (String) paras.get("etaPath"),
                        (int) paras.get("W"), (int) paras.get("K"),
                        (double) paras.get("alpha"), (double) paras.get("beta"), (int) paras.get("iterations"));
        }
        return null;

    }

    public static void main(String[] args) {
        Map<String, Object> paras = new HashMap<>();

        // LDA or BTM experiment
        paras.put("trainPath", "/Users/ZuoMLin/Desktop/thesis/experiments/doc_wids/data0.8");
        paras.put("W", 28634);
        paras.put("K", 50);
        paras.put("alpha", 0.25);
        paras.put("beta", 0.01);
        paras.put("iterations", 100);

        // BTOT experiment
        paras.put("docsPath", "/Users/ZuoMLin/Desktop/thesis/实验结果/doc_wids/0.txt");
        paras.put("timesPath", "/Users/ZuoMLin/Desktop/thesis/实验结果/doc_time/0.txt");
        paras.put("W", 74477);
        paras.put("K", 50);
        paras.put("alpha", 0.25);
        paras.put("beta", 0.01);
        paras.put("psi", 1.0);
        paras.put("iterations", 100);

        // PeacockLDA or APSparseBTM experiment
        paras.put("trainPath", "/Users/ZuoMLin/Desktop/thesis/experiments/doc_wids/data0.8");
        paras.put("W", 28634);
        paras.put("K", 50);
        paras.put("M", 4);
        paras.put("alpha", 0.25);
        paras.put("beta", 0.01);
        paras.put("iterations", 100);

        // BBTM experiment
        paras.put("trainPath", "/Users/ZuoMLin/Desktop/thesis/experiments/doc_wids/data0.8");
        paras.put("etaPath", "/Users/ZuoMLin/Desktop/thesis/experiments/doc_wids/data0.8");
        paras.put("W", 28634);
        paras.put("K", 50);
        paras.put("alpha", 0.25);
        paras.put("beta", 0.01);
        paras.put("iterations", 100);

        TopicModel model = TopicModelFactory.createModel("BTOT", paras);
        model.gibbs();
    }
}
