package com.example.demo;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateConfig;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

/**
 * Mybatis生成器
 * @author yinchd
 * @date 2019 /3/26
 */
public class CodeGenerator {

    /**
     * 当前项目的路径
     */
    static final String PROJECT_PATH = System.getProperty("user.dir");
    /**
     * java文件路径
     */
    static final String CODE_HOME = "/demo/src/main/java";

    /**
     * 代码生成器入口.
     */
    public static void main(String[] args) {
        // 待生成的表名
        String tableName = "t_log_content";
        // 生成的时候要去掉的表前缀，如果不需要去除什么前缀，则这里为空就行
        String trimTablePrefix = "t_";
        // 生成文件的父包路径
        String codeGeneratePath = "com.tencent.qidian";
        System.out.println("开始生成如下表：" + tableName + " 到 " + codeGeneratePath + " 目录中...");
        generate(tableName, trimTablePrefix, codeGeneratePath, true, true, true);
        System.out.println("生成成功...");
    }

    /**
     * 代码生成入口
     * @param tableName 表名
     * @param trimTablePrefix 要去除的表前缀 eg：表名：t_user, 如果不去除`t_`前缀的话，则生成的类名为TUser，如果去掉，则生成的类名为User
     * @param basePkg 生成文件的父包路径
     * @param controller 是否生成controller
     * @param service 是否生成service
     * @param mapper 是否生成mapper
     */
    private static void generate(String tableName, String trimTablePrefix, String basePkg,
            boolean controller, boolean service, boolean mapper) {
        AutoGenerator generator = new AutoGenerator();
        // 全局配置
        generator.setGlobalConfig(getGlobalConfig());
        // 数据源
        generator.setDataSource(getDataSourceConfig());
        // 生成策略
        generator.setStrategy(getStrategyConfig(tableName, trimTablePrefix));
        // 生成模板
        generator.setTemplate(getTemplateConfig(controller, service, mapper));
        // 生成目标信息
        generator.setPackageInfo(getPackageConfig(basePkg));
        // 执行生成
        generator.execute();
    }

    private static PackageConfig getPackageConfig(String basePkg) {
        // 包配置
        PackageConfig packageConfig = new PackageConfig();
        // 父包路径
        packageConfig.setParent(basePkg);
        packageConfig.setController("controller");
        packageConfig.setService("service");
        packageConfig.setServiceImpl("service.impl");
        packageConfig.setMapper("mapper");
        packageConfig.setEntity("model");
        packageConfig.setXml("mapper");
        return packageConfig;
    }

    private static GlobalConfig getGlobalConfig() {
        // 全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        // 生成文件输出根目录
        globalConfig.setOutputDir(PROJECT_PATH + CODE_HOME);
        /*globalConfig.setOutputDir("E:\\BaiduNetdiskDownload");*/
        // 生成完成后不弹出文件框
        globalConfig.setOpen(false);
        // 文件覆盖
        globalConfig.setFileOverride(true);
        // 不需要ActiveRecord特性的请改为false
        globalConfig.setActiveRecord(false);
        // XML 二级缓存
        globalConfig.setEnableCache(false);
        // XML ResultMap
        globalConfig.setBaseResultMap(true);
        // XML columList
        globalConfig.setBaseColumnList(false);
        // 作者
        globalConfig.setAuthor("generator");
        globalConfig.setSwagger2(true);

        // 自定义文件命名，注意 %s 会自动填充表实体属性！
        globalConfig.setControllerName("%sController");
        globalConfig.setServiceName("%sService");
        globalConfig.setServiceImplName("%sServiceImpl");
        globalConfig.setMapperName("%sMapper");
        globalConfig.setXmlName("%sMapper");
        globalConfig.setEntityName("%sModel");
        return globalConfig;
    }

    private static TemplateConfig getTemplateConfig(boolean controller, boolean service, boolean mapper) {
        // 解决Mapper.java上没有@Mapper注解的问题
        TemplateConfig templateConfig = new TemplateConfig();
        if (!controller) {
            templateConfig.setController("");
        }
        if (!service) {
            templateConfig.setService("").setServiceImpl("");
        }
        templateConfig.setMapper("/generator/mapper.java.vm");
        if (!mapper) {
            templateConfig.setMapper("").setXml("");
        }
        return templateConfig;
    }

    private static StrategyConfig getStrategyConfig(String tableName, String trimTablePrefix) {
        // 策略配置
        StrategyConfig strategy = new StrategyConfig();

        // 表名生成策略
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);

        // 需要生成的表
        strategy.setInclude(tableName);
        strategy.setSuperServiceClass("com.baomidou.mybatisplus.extension.service.IService");
        strategy.setSuperServiceImplClass("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl");
        strategy.setSuperMapperClass("com.baomidou.mybatisplus.core.mapper.BaseMapper");
        strategy.setEntityLombokModel(true);
        strategy.setRestControllerStyle(true);

        strategy.setControllerMappingHyphenStyle(true);
        strategy.setEntityColumnConstant(true);
        strategy.setEntityBuilderModel(true);
        strategy.setEntityTableFieldAnnotationEnable(true);

        // 去除表前缀
        if (trimTablePrefix != null && !"".equals(trimTablePrefix)) {
            strategy.setTablePrefix(trimTablePrefix);
        }
        // 去除字段前缀
        // strategy.setFieldPrefix("");
        return strategy;
    }

    private static DataSourceConfig getDataSourceConfig() {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        // 设置数据库类型
        dataSourceConfig.setDbType(DbType.MYSQL);
        dataSourceConfig.setDriverName("com.mysql.cj.jdbc.Driver");
        // 指定数据库 master库
        dataSourceConfig.setUrl("jdbc:mysql://172.16.8.50:33106/qidian_qss?useUnicode=true&zeroDateTimeBehavior=convertToNull&autoReconnect=true&characterEncoding=utf-8&useSSL=false");
        // 用户名
        dataSourceConfig.setUsername("root");
        // 密码
        dataSourceConfig.setPassword("admin@123");
        return dataSourceConfig;
    }

}