package com.fishing.works.lucene;

import cn.neu.lucene.MyIKAnalyzer;
import com.fishing.filter.FilterSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LuceneUtil {

    public static void createIndex(List list, String path) throws IOException {
        // 创建文档的集合
        Collection<Document> docs = new ArrayList<>();
        list.forEach(dto->{
            docs.add(addDoc(dto));
        });
        // 索引目录类,指定索引在硬盘中的位置，我的设置为D盘的indexDir文件夹
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(path));
        // 引入IK分词器
        Analyzer analyzer = new MyIKAnalyzer();
        // 索引写出工具的配置对象，这个地方就是最上面报错的问题解决方案
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        // 设置打开方式：OpenMode.APPEND 会在索引库的基础上追加新索引。OpenMode.CREATE会先清空原来数据，再提交新的索引
        conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        // 创建索引的写出工具类。参数：索引的目录和配置信息
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        // 把文档集合交给IndexWriter
        indexWriter.addDocuments(docs);
        // 提交
        indexWriter.commit();
        // 关闭
        indexWriter.close();
    }

    private static List<String> list = Arrays.asList("userName","lableNames","content","address","userIds");
    public static Document addDoc(Object dto){
        Class cla = dto.getClass();
        java.lang.reflect.Field[] fields = cla.getDeclaredFields();
        Document doc = new Document();
        try {
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                if (list.contains(field.getName())){
                    doc.add(new TextField(field.getName(), String.valueOf(field.get(dto)), Field.Store.YES));
                } else {
                    doc.add(new StringField(field.getName(), String.valueOf(field.get(dto)), Field.Store.YES));
                }
            }
        } catch (Exception e){
            System.out.println("Document初始化失败");
        }
        return doc;
    }

}
