package com.fishing.works.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbMakerConfigException;
import org.lionsoul.ip2region.DbSearcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URL;

@Slf4j
public class IpUtils {

    private static DbConfig config;
    private static DbSearcher searcher;
    private static String dbPath;


    static {
//        try {
////            dbPath = ResourceUtils.getURL("classpath:").getPath()+"city/ip2region.db";
////            URL url = this.getClass().getResource("/city/ip2region.db");
////            dbPath = url.getFile();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            config = new DbConfig();
//        } catch (DbMakerConfigException e) {
//            e.printStackTrace();
//        }
//        try {
//            searcher = new DbSearcher(config, dbPath);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    public static void init(String path){
        /*URL url = this.getClass().getResource("/city/ip2region.db");
        dbPath = url.getFile();*/
        dbPath = path;
        log.info("ip path:{}",path);
        try {
            config = new DbConfig();
        } catch (DbMakerConfigException e) {
            e.printStackTrace();
        }
        try {
            searcher = new DbSearcher(config, dbPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String getCityInfo(String ip) {

        if (StringUtil.isEmpty(dbPath)) {
            log.error("Error: Invalid ip2region.db file");
            return null;
        }
        if(config == null || searcher == null){
            log.error("Error: DbSearcher or DbConfig is null");
            return null;
        }

        //查询算法
        //B-tree, B树搜索（更快）
        int algorithm = DbSearcher.BTREE_ALGORITHM;

        //Binary,使用二分搜索
        //DbSearcher.BINARY_ALGORITHM

        //Memory,加载内存（最快）
        //DbSearcher.MEMORY_ALGORITYM
        try {
            // 使用静态代码块，减少文件读取操作
//            DbConfig config = new DbConfig();
//            DbSearcher searcher = new DbSearcher(config, dbPath);

            //define the method
            Method method = null;
            switch (algorithm) {
                case DbSearcher.BTREE_ALGORITHM:
                    method = searcher.getClass().getMethod("btreeSearch", String.class);
                    break;
                case DbSearcher.BINARY_ALGORITHM:
                    method = searcher.getClass().getMethod("binarySearch", String.class);
                    break;
                case DbSearcher.MEMORY_ALGORITYM:
                    method = searcher.getClass().getMethod("memorySearch", String.class);
                    break;
                default:
            }

            DataBlock dataBlock = null;
            if (org.lionsoul.ip2region.Util.isIpAddress(ip) == false) {
                System.out.println("Error: Invalid ip address");
            }

            dataBlock = (DataBlock) method.invoke(searcher, ip);
            String ipInfo = dataBlock.getRegion();
//            if (!StringUtils.isEmpty(ipInfo)) {
            if (!StringUtil.isNullOrEmpty(ipInfo)) {
                ipInfo = ipInfo.replace("|0", "");
                ipInfo = ipInfo.replace("0|", "");
            }
            return ipInfo;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取IP属地
     * @param ip
     * @return
     */
    public static String getIpPossession(String ip) {
        System.out.println("ip:"+ip);
        log.info("ip:{} ",ip);
        String cityInfo = getCityInfo(ip);
        if (!StringUtils.isEmpty(cityInfo)) {
            cityInfo = cityInfo.replace("|", " ");
            String[] cityList = cityInfo.split(" ");
            if (cityList.length > 0) {
                // 国内的显示到具体的省
                if ("中国".equals(cityList[0])) {
                    if (cityList.length > 1) {
                        String name = cityList[1];
                        name.replace("省","");
                        if(name.charAt(name.length()-1) == '省') {
                            name = name.substring(0, name.length()-1);
                        }
                        return name;
                    }
                }
                // 国外显示到国家
                return cityList[0];
            }
        }
        return "未知";
    }

    public static void main(String[]args){
        dbPath = "E:\\work\\mswj-fishing\\fishing-parent\\api-parent\\api-works\\src\\main\\resources\\city\\ip2region.db";
        try {
            config = new DbConfig();
        } catch (DbMakerConfigException e) {
            e.printStackTrace();
        }
        try {
            searcher = new DbSearcher(config, dbPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String ip = "113.110.253.216";
        System.out.println(getCityInfo(ip));
        System.out.println(getIpPossession(ip));

        String ip1 = "218.195.219.255";
        System.out.println(getCityInfo(ip1));
        System.out.println(getIpPossession(ip1));
        String ip2 = "180.149.130.16";
        System.out.println(getCityInfo(ip2));
        System.out.println(getIpPossession(ip2));
        String ip3 = "122.9.255.255";
        System.out.println(getCityInfo(ip3));
        System.out.println(getIpPossession(ip3));

    }
}
