package factory;

import model.online.OnlineBTM;
import model.online.OnlineBTOT;
import model.online.OnlineLDA;
import model.online.OnlineTopicModel;

import java.util.HashMap;
import java.util.Map;

/**
 * 在线主题模型工厂类(静态工厂模式)
 */
public class OnlineTopicModelFactory {

    public static OnlineTopicModel createModel(String modelType, Map<String, Object> paras) {
        if (modelType == null)
            return null;
        switch (modelType) {
            case "OLDA":
                return new OnlineLDA((String) paras.get("docsDir"), (String) paras.get("thetaDir"), (String) paras.get("phiDir"),
                        (double[]) paras.get("wp"), (int) paras.get("slices"), (int) paras.get("windows"),
                        (int) paras.get("W"), (int) paras.get("K"), (int) paras.get("iterations"));
            case "OBTM":
                return new OnlineBTM((String) paras.get("docsDir"), (String) paras.get("thetaDir"), (String) paras.get("phiDir"),
                        (double[]) paras.get("wp"), (int) paras.get("slices"), (int) paras.get("windows"),
                        (int) paras.get("W"), (int) paras.get("K"), (int) paras.get("iterations"));
            case "OBTOT":
                return new OnlineBTOT((String) paras.get("docsDir"), (String) paras.get("timesDir"), (String) paras.get("thetaDir"),
                        (String) paras.get("phiDir"), (String) paras.get("psiDir"),
                        (double) paras.get("wp"), (int) paras.get("slices"), (int) paras.get("windows"),
                        (int) paras.get("W"), (int) paras.get("K"), (int) paras.get("iterations"));
        }
        return null;
    }

    public static void main(String[] args) {
        Map<String, Object> paras = new HashMap<>();

        // OnlineLDA or OnlineBTM experiment
        paras.put("docsDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/doc_wids/");
        paras.put("thetaDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/");
        paras.put("phiDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/");
        paras.put("wp", new double[]{0.01, 0.03, 0.36, 0.6});
        paras.put("slices", 8);
        paras.put("windows", 4);
        paras.put("W", 74477);
        paras.put("K", 50);
        paras.put("iterations", 100);


        // OnlineBTOT experiment
        paras.put("docsDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/doc_wids/");
        paras.put("timesDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/doc_wids/");
        paras.put("thetaDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/");
        paras.put("phiDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/");
        paras.put("psiDir", "/Users/ZuoMLin/Desktop/thesis/code/Online-BTOT/output/doc_wids/");
        paras.put("wp", 0.6);
        paras.put("slices", 8);
        paras.put("windows", 4);
        paras.put("W", 74477);
        paras.put("K", 50);
        paras.put("iterations", 100);


        OnlineTopicModel model = OnlineTopicModelFactory.createModel("OLDA", paras);
        model.start();
    }
}
