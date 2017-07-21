package io.github.jhipster.sample.web.rest;

import io.github.jhipster.sample.web.rest.model.SparkEstimate;
import io.github.jhipster.sample.web.rest.support.Classification;
import org.apache.spark.ml.Model;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.jhipster.sample.web.rest.model.SparkClassification;
import io.github.jhipster.sample.web.rest.model.SparkCluster;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.json.JSONObject;
import io.github.jhipster.sample.web.rest.util.HDFSFileUtil;
import io.github.jhipster.sample.web.rest.util.SparkUtil;

import java.io.File;
import java.util.*;

/**
 * Created by WJ on 2017/4/25.
 */
@RestController
@RequestMapping("/api")
public class JavaSparkAPI {
    private static String ProjectPathPrefix = "src/main/webappfiles/Project/";
    private static String HDFSPathPrefix = "/user/hadoop/data_platform/data/";

    static HDFSFileUtil hdfsFileUtil = new HDFSFileUtil();
    static SparkUtil sparkUtil = new SparkUtil();
    static SparkCluster sparkCluster = new SparkCluster();
    static FileController fileController = new FileController();
    static SparkClassification sparkClassification = new SparkClassification();





    @GetMapping("/getModel")
    @ResponseBody
    public List<String> getModel() throws Exception{
        return hdfsFileUtil.list("/user/hadoop/data_platform/model/");
    }

    /**
     * 获取数据列名
     * @param DataName HDFS上数据文件名
     * @return String []columns 数据列名
     * @throws Exception
     */
    @GetMapping("/getDataColumns")
    @ResponseBody
    public String [] getDataColumns(@RequestParam(value = "DataName") String DataName) throws Exception{
        String hdfsDir = "/user/hadoop/data_platform/data/" + DataName;
        String [] columns = sparkUtil.getColumns(hdfsDir, "HDFS", "json");
        return columns;
    }

    @GetMapping("/getParameter")
    @ResponseBody
    public  List<String> getParam(@RequestParam(value = "Algorithm") String Algorithm){
        String []arrParam = sparkClassification.getParam(Algorithm).split("\n");
        List<String> resList = new ArrayList<String> ();
        for (int i = 0; i < arrParam.length; i++) {
            String result = "";
            String []arrDefault = arrParam[i].split(":");
            if (arrDefault.length == 3){
                result = arrDefault[0] + ":" +arrDefault[2].substring(1,arrDefault[2].length()-1);
                resList.add(result);
            }

        }
        return resList;
    }

    /**
     * train
     * @param DataName
//     * @param featureCols
     * @param ModelName
     * @param Parameters
     * @param Algorithm
     * @return model saved or not
     * @throws Exception
     */
    @GetMapping("/SparkTrain")
    @ResponseBody
    public boolean SparkTrain(@RequestParam(value = "DataName") String DataName,
//                              @RequestParam(value = "Columns") String [] featureCols,
                              @RequestParam(value = "ModelName") String ModelName,
                              @RequestParam(value = "Parameters") String Parameters,
                              @RequestParam(value = "Algorithm") String Algorithm) throws Exception{
        Date date = new Date();
        String[] featureCols = getDataColumns(DataName);
//        String[] featureCols = {"wigth", "age", "heigth", "interets"};
        String hdfsDir = "/user/hadoop/data_platform/data/" + DataName;
        Dataset<Row> dataset = sparkUtil.readData(hdfsDir, "HDFS", "json",featureCols, "label");
        System.out.println(dataset.count());
        for(String column : dataset.columns())
            System.out.println(column);
//        {'maxIter':10, 'regParam' : 0.5, 'elasticNetParam' : 0.8, 'standardization' : true}
        JSONObject jsonObject = new JSONObject(Parameters);

        String newModelName = ModelName + String.valueOf(date.getTime());
        String modelPath = hdfsFileUtil.HDFSPath("/user/hadoop/data_platform/model/" + newModelName);
        switch (Algorithm){
            case "lr":
                sparkClassification.lr(jsonObject, dataset, modelPath);
                break;
            default:
                break;
        }
        return getModel().contains(newModelName);
    }

    @GetMapping("/SparkPredict")
    @ResponseBody
    public  List<String> SparkPredict(@RequestParam(value = "DataName") String DataName,
//                             @RequestParam(value = "Columns") String [] featureCols,
                             @RequestParam(value = "ModelName") String ModelName,
                             @RequestParam(value = "Algorithm") String Algorithm) throws Exception{

        String[] featureCols = getDataColumns(DataName);
//        String[] featureCols = {"wigth", "age", "heigth", "interets"};
        String hdfsDir = "/user/hadoop/data_platform/data/" + DataName;
        System.out.println(hdfsDir);
        List<String> result = new ArrayList<String>();
        Dataset<Row> dataset = sparkUtil.readData(hdfsDir, "HDFS", "json",featureCols, "label");
        System.out.println("read over");
        String modelPath = hdfsFileUtil.HDFSPath("/user/hadoop/data_platform/model/" + ModelName);
        System.out.println(modelPath);
        SparkEstimate sparkEstimate = new SparkEstimate();
        switch (Algorithm){
            case "lr":
                Model model = sparkEstimate.loadModel(modelPath, Classification.LR);
                Dataset<Row> rows = sparkEstimate.predict(dataset,model);
                for (Row r: rows.collectAsList()) {
                    result.add("(" + r.get(0) + ", " + r.get(1) + ") -> prob=" + r.get(2) + ", prediction=" + r.get(3));
                }
                break;
            default:
                break;
        }
    return result;
    }




    public static void testCluster() throws Exception{
        SparkCluster sparkCluster = new SparkCluster();
        String[] featureCols = {"wigth", "age", "heigth", "interets"};
        Dataset<Row> dataset = sparkUtil.readData("/user/hadoop/data_platform/data.json", "HDFS", "json",
            featureCols, "label");
        System.out.println(dataset.count());
        for(String column : dataset.columns())
            System.out.println(column);
        JSONObject jsonObject = new JSONObject("{'K':10, 'seed' : 10, 'initSteps' : 30, 'tol' : 0.5}");
        Vector[] vectors = sparkCluster.kmeans(jsonObject, dataset);
        for(Vector vector : vectors) {
            System.out.println(vector);
        }
    }
}
