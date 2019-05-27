import com.cnws.base.model.Sample;
import com.cnws.base.model.Sentence;
import com.cnws.base.segmentor.BaseSegmenter;
import com.cnws.base.segmentor.EnhanceSegmenter;
import com.cnws.classifier.DoublePropagation;
import com.cnws.classifier.POSClassifier;
import com.cnws.classifier.WordClassifier;
import com.cnws.util.FileUtil;
import com.cnws.util.LogUtil;
import com.cnws.base.segmentor.Segmenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Main {

    public static void main(String[] args) {
        String path = Main.class.getResource("/").getPath() + "../../../";
        LogUtil.info("project path : " + path);

        String configPath = path + "./data/config/param.txt";
        Map<String, String> config = FileUtil.readConfig(configPath);
        for (String key : config.keySet()){
            LogUtil.info("config param : " + key + " = " + config.get(key));
        }

        // load data
        String trainPath = path + config.get("train_path");
        String devPath = path + config.get("dev_path");
        String testPath = path + config.get("test_path");
        String infPath = path + config.get("inf_path");

        Sample trainSample = FileUtil.loadSample(trainPath);
        Sample devSample = FileUtil.loadSample(devPath);
        Sample testSample = FileUtil.loadSample(testPath);
        Sample infSample = FileUtil.loadRawSample(infPath);

        // choose segment :
        Segmenter model;
        String configSegment = config.get("segment");
        String dataType = config.get("data_type");

        String modelPath = path + config.get("model_path");
        String npPath = path + config.get("np_path");
        // wc param
        String featurePath = path + config.get("feature_path");
        String scriptPath = path + config.get("script_path");
        String pyEnv = config.get("py_env");
        //dp
        String originFile = path + config.get("dp_origin");
        String subName = config.get("sub_name");

        // whether the mode is dp
        String mode = config.get("mode");
        if("wc".equals(config.get("mode"))){
            wordClassifier(trainSample, featurePath, pyEnv, scriptPath);
            return;
        }
        if("pc".equals(config.get("mode"))){
            posClassifier(trainSample, featurePath, pyEnv, scriptPath);
            return;
        }

        if("dp".equals(config.get("mode"))){
            doublePropagation(trainSample, testSample, featurePath, npPath, pyEnv, scriptPath, originFile);
            return;
        }

        if("base".equals(configSegment)){
            model = new BaseSegmenter(dataType, modelPath);
        }else if("enhance".equals(configSegment)){
            model = new EnhanceSegmenter(dataType, modelPath, npPath, mode, subName);
        }else{
            throw new Error("the segmentor can not be support!");
        }

        // choose mode
        if ("train".equals(mode)){
            train(model, trainSample, devSample, testSample);
        } else if ("test".equals(mode)){
            test(model, trainSample, testSample);
        }else if ("inf".equals(mode)){
            inference(model, trainSample, infSample);
        }else{
            throw new Error("the mode can not be support!");
        }
    }

    private static void wordClassifier(Sample trainSample, String fPath, String pyEnv, String sPath) {
        WordClassifier classifier = new WordClassifier();
        classifier.initData(trainSample);
        classifier.trainWordClassifier(fPath, pyEnv, sPath);
    }

    private static void posClassifier(Sample trainSample, String fPath, String pyEnv, String sPath) {
        POSClassifier classifier = new POSClassifier();
        classifier.initData(trainSample);
        classifier.trainPOSClassifier(fPath, pyEnv, sPath);
    }

    private static void train(Segmenter segmentor, Sample trainSample, Sample devSample, Sample testSample){
        segmentor.statKnowledge(trainSample);
        segmentor.train(trainSample, devSample, testSample);
    }

    private static void test(Segmenter segmentor, Sample trainSample, Sample testSample){
        segmentor.statKnowledge(trainSample);
        segmentor.test(testSample, true);
    }

    private static void inference(Segmenter segmentor, Sample trainSample, Sample infSample){
        segmentor.statKnowledge(trainSample);
        segmentor.inference(infSample, true);
    }

    private static void doublePropagation(Sample trainSample, Sample testSample, String featurePath, String npPath,
                                          String pyEnv, String scriptPath, String originFile) {
        Sample novelSamples = FileUtil.loadSample(originFile);
        String[] arr = originFile.split("/");
        String sub = arr[arr.length - 1].split("_")[0];
        LogUtil.info(String.format("origin file size %d novel type : %s", novelSamples.size(), sub));

        DoublePropagation dp = new DoublePropagation();
        dp.train(trainSample, novelSamples, testSample, sub, featurePath, npPath, pyEnv, scriptPath);
    }
}