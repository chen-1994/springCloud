package com.fishing.works.lucene;

import cn.neu.lucene.MyIKAnalyzer;
import com.fishing.common.constants.KConstants;
import com.fishing.common.response.PageData;
import com.fishing.req.works.WorksQueryReq;
import com.fishing.resp.works.ArticleDTO;
import com.fishing.resp.works.ShortEssayDTO;
import com.fishing.resp.works.VideoDTO;
import com.fishing.works.service.ArticleService;
import com.fishing.works.service.ShortEssayService;
import com.fishing.works.service.VideoService;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class WorksLucenService {

    @Value("${indexDir}")
    private String indexDir;
    @Autowired
    private VideoService videoService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private ShortEssayService shortEssayService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private WorksLucenService worksLucenService;

    private Object getService(){
        String uri = request.getRequestURI().split("/")[1].replaceAll("New","");
        if ("video".equals(uri)){
            return videoService;
        }
        if ("article".equals(uri)){
            return articleService;
        }
        if ("shortEssay".equals(uri)){
            return shortEssayService;
        }
        return videoService;
    }


    public String createIndex(List list) throws IOException {
        String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
        LuceneUtil.createIndex(list,indexDir+"/"+dir);
        return"success";
    }

    public String createIndex() throws IOException {
        WorksQueryReq req = new WorksQueryReq();
        req.setPageSize(100000);
        PageData videoData = videoService.worksList(req);
        List<VideoDTO> videoList = videoData.getDatas();

        PageData articleData = articleService.worksList(req);
        List<ArticleDTO> articleList = articleData.getDatas();

        PageData pageData = shortEssayService.worksList(req);
        List<ShortEssayDTO> seList = pageData.getDatas();

        LuceneUtil.createIndex(videoList,indexDir+"/video");
        LuceneUtil.createIndex(articleList,indexDir+"/article");
        LuceneUtil.createIndex(seList,indexDir+"/shortEssay");
        return"success";
    }

    //我这是更新指定id的数据
    public String update(String worksId) throws IOException{
        // 创建目录对象
        String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/"+dir));
        // 创建配置对象
        IndexWriterConfig conf = new IndexWriterConfig(new MyIKAnalyzer());
        // 创建索引写出工具
        IndexWriter writer = new IndexWriter(directory, conf);
        // 创建新的文档数据
        Document doc = LuceneUtil.addDoc(videoService.getWorks(Long.valueOf(worksId)));
        writer.updateDocument(new Term("id",worksId), doc);
        // 提交
        writer.commit();
        // 关闭
        writer.close();
        return "success";
    }

    public String deleteIndex(String worksId) throws IOException{
        // 创建目录对象
        String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/"+dir));
        // 创建配置对象
        IndexWriterConfig conf = new IndexWriterConfig(new MyIKAnalyzer());
        // 创建索引写出工具
        IndexWriter writer = new IndexWriter(directory, conf);
        // 根据词条进行删除
        writer.deleteDocuments(new Term("id", worksId));
        // 提交
        writer.commit();
        // 关闭
        writer.close();
        return "success";
    }



    public List purview(Map map){
        try {
            String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
//        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/video");
            Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/"+dir));
            // 索引读取工具
            IndexReader reader = DirectoryReader.open(directory);
            // 索引搜索工具
            IndexSearcher searcher = new IndexSearcher(reader);
            // 创建查询解析器,两个参数：默认要查询的字段的名称，分词器
            MyIKAnalyzer analyzer = new MyIKAnalyzer();

            BooleanQuery.Builder query = new BooleanQuery.Builder();

            Integer currentPage = Integer.valueOf(map.get("currentPage").toString());
            Integer pageSize = Integer.valueOf(map.get("pageSize").toString());
            if (map.get("name")!=null){
                String name = map.get("name").toString();
                String[] fields = { "content","userName","lableNames"};
                String[] keys = { name,name,name};
                BooleanQuery.Builder query1 = new BooleanQuery.Builder();
                for (int i = 0; i < keys.length; i++) {
                    TermQuery termQuery = new TermQuery(new Term(fields[i], keys[i]));
                    query1.add(termQuery, BooleanClause.Occur.SHOULD);
                }
                query.add(query1.build(), BooleanClause.Occur.MUST);
            }
            map.remove("name");
            for(Object key:map.keySet()){
                TermQuery termQuery = new TermQuery(new Term(key.toString(), map.get(key).toString()));
                query.add(termQuery, BooleanClause.Occur.MUST);
            }
            TermQuery termQuery = new TermQuery(new Term("status", "0"));
            query.add(termQuery, BooleanClause.Occur.MUST);
            termQuery = new TermQuery(new Term("examineStatus", "2"));
            query.add(termQuery, BooleanClause.Occur.MUST);
            termQuery = new TermQuery(new Term("releaseStatus", "0"));
            query.add(termQuery, BooleanClause.Occur.MUST);


            query.add(power(map.get("userId").toString()).build(), BooleanClause.Occur.MUST);

            TopDocs topDocs = pageSearch(query.build(),searcher,currentPage,pageSize);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            // 获取总条数
            System.out.println("本次搜索共找到" + topDocs.totalHits + "条数据");
            // 显示查询结果
            List list = searchResults(searcher, scoreDocs,query.build(),analyzer);
            return list;
        } catch (Exception e){

        }
        return null;
    }

    public PageData pageData(String[] fields,String[] keys,MyIKAnalyzer analyzer,IndexSearcher searcher,int currentPage,int pageSize) throws Exception {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        BooleanQuery.Builder query1 = new BooleanQuery.Builder();
        for (int i = 0; i < keys.length; i++) {
            TermQuery termQuery = new TermQuery(new Term(fields[i], keys[i]));
            query1.add(termQuery, BooleanClause.Occur.SHOULD);
        }

        TermQuery termQuery = new TermQuery(new Term("status", "0"));
        query.add(termQuery, BooleanClause.Occur.MUST);
        termQuery = new TermQuery(new Term("examineStatus", "2"));
        query.add(termQuery, BooleanClause.Occur.MUST);
        termQuery = new TermQuery(new Term("releaseStatus", "0"));
        query.add(termQuery, BooleanClause.Occur.MUST);

        query.add(query1.build(), BooleanClause.Occur.MUST);
        query.add(worksLucenService.power("100207").build(), BooleanClause.Occur.MUST);

        TopDocs topDocs = pageSearch(query.build(),searcher,currentPage,pageSize);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        // 获取总条数
        System.out.println("本次搜索共找到" + topDocs.totalHits + "条数据");
        // 显示查询结果
        List list = searchResults(searcher, scoreDocs,query.build(),analyzer);
        PageData pageData = new PageData(currentPage,pageSize);
        pageData.setTotalRecord((int)topDocs.totalHits);
        pageData.setDatas(list);
        return pageData;
    }

    public List list(String[] fields,String[] keys,MyIKAnalyzer analyzer,IndexSearcher searcher,int currentPage,int pageSize) throws Exception {
        Query query = null;
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        TopDocs topDocs = null;
        if (keys != null && keys.length > 0) {
            for (int i = 0; i < keys.length; i++) {
                QueryParser queryParser = new QueryParser(fields[i], analyzer);
                Query q = queryParser.parse(keys[i]);
//                builder.add(q, BooleanClause.Occur.MUST);//and操作
                builder.add(q, BooleanClause.Occur.SHOULD);//and操作
            }
            // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
            query = builder.build();
//            topDocs = searcher.search(query,10);
            topDocs = pageSearch(query,searcher,currentPage,pageSize);

            /*int start = (currentPage - 1) * pageSize;
            if(0==start){
                topDocs = searcher.search(query, currentPage*pageSize);
            } else {
                // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
                topDocs = searcher.search(query, start);
                //获取到上一页最后一条
                ScoreDoc preScore;
                if (topDocs.scoreDocs.length<start){
                    preScore = topDocs.scoreDocs[topDocs.scoreDocs.length-1];
                } else {
                    preScore = topDocs.scoreDocs[start-1];
                }
                //查询最后一条后的数据的一页数据
                topDocs = searcher.searchAfter(preScore, query, pageSize);
            }*/
        }

        // 获取得分文档对象（ScoreDoc）数组.SocreDoc中包含：文档的编号、文档的得分
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        // 获取总条数
        System.out.println("本次搜索共找到" + topDocs.totalHits + "条数据");
        // 显示查询结果
        List list = searchResults(searcher, scoreDocs,query,analyzer);
        return list;
    }

    private List searchResults(IndexSearcher searcher, ScoreDoc[] hits, Query query, MyIKAnalyzer analyzer) throws Exception {
        List list = new ArrayList<>();
        System.out.println("找到 " + hits.length + " 个命中.");

        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<span style='color:red'>", "</span>");
        Highlighter highlighter = new Highlighter(simpleHTMLFormatter, new QueryScorer(query));

        System.out.println("找到 " + hits.length + " 个命中.");
        System.out.println("序号\t匹配度得分\t结果");

        for (int i = 0; i < hits.length; ++i) {
            ScoreDoc scoreDoc= hits[i];
            int docId = scoreDoc.doc;
            Document d = searcher.doc(docId);
            List<IndexableField> fields= d.getFields();
            System.out.print((i + 1) );
            System.out.print("\t" + scoreDoc.score);
            Object dto = obj();
            Class cla = dto.getClass();
//            Map<String,String > map = ((BooleanQuery)query).clauses().stream().collect(Collectors.toMap(obj -> ((TermQuery)obj.getQuery()).getTerm().field(), obj -> obj.getQuery().toString()));
            for (IndexableField f : fields) {
                java.lang.reflect.Field field = cla.getDeclaredField(f.name());
                field.setAccessible(true);
                field.set(dto,valueOf(field,d.get(f.name())));
//                if("content".equals(f.name())){
                /*if (map.get(f.name())!=null){
                    TokenStream tokenStream = analyzer.tokenStream(f.name(), new StringReader(d.get(f.name())));
                    String fieldContent = highlighter.getBestFragment(tokenStream, d.get(f.name()));
                    field.set(dto,valueOf(field,fieldContent));
                    System.out.print("\t"+fieldContent);
                }
                else{
                    System.out.print("\t"+d.get(f.name()));
                }*/
                System.out.print("\t"+d.get(f.name()));
            }

            System.out.println("<br>");
            list.add(dto);
        }
        return list;
    }

    private TopDocs pageSearch(Query query, IndexSearcher searcher, int pageNow, int pageSize)
            throws IOException {

        int start = (pageNow - 1) * pageSize;
        if(0==start){
            TopDocs topDocs = searcher.search(query, pageNow*pageSize);
            return topDocs;
        }
        // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
        TopDocs topDocs = searcher.search(query, start);
        //获取到上一页最后一条
        ScoreDoc preScore;
        if (topDocs.scoreDocs.length<start){
            preScore = topDocs.scoreDocs[topDocs.scoreDocs.length-1];
        } else {
            preScore = topDocs.scoreDocs[start-1];
        }
        //查询最后一条后的数据的一页数据
        topDocs = searcher.searchAfter(preScore, query, pageSize);
        return topDocs;

    }

    public BooleanQuery.Builder openPower(String open, List<String> userIds){
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        //open=2\3
        TermQuery termQuery = new TermQuery(new Term("open", open));
        query.add(termQuery, BooleanClause.Occur.MUST);
        BooleanQuery.Builder query2 = new BooleanQuery.Builder();
        for (String id : userIds){
            TermQuery termQuery2 = new TermQuery(new Term("userId", id));
            query2.add(termQuery2, BooleanClause.Occur.SHOULD);
        }
        query.add(query2.build(), BooleanClause.Occur.MUST);
        return query;
    }

    public BooleanQuery.Builder openPower2(String open, List<Integer> userIds){
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        //open=2\3
        TermQuery termQuery = new TermQuery(new Term("open", open));
        query.add(termQuery, BooleanClause.Occur.MUST);
        BooleanQuery.Builder query2 = new BooleanQuery.Builder();
        for (Integer id : userIds){
            TermQuery termQuery2 = new TermQuery(new Term("userId", id.toString()));
            query2.add(termQuery2, BooleanClause.Occur.SHOULD);
        }
        query.add(query2.build(), BooleanClause.Occur.MUST);
        return query;
    }

    public BooleanQuery.Builder openPower(String open,String myUserId){
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        //open=5\6
        TermQuery termQuery = new TermQuery(new Term("open", open));
        query.add(termQuery, BooleanClause.Occur.MUST);
        TermQuery termQuery2 = new TermQuery(new Term("userIds", myUserId));
        if ("5".equals(open)){
            query.add(termQuery2, BooleanClause.Occur.MUST);
        } else {
            query.add(termQuery2, BooleanClause.Occur.MUST_NOT);
        }
        return query;
    }

    public BooleanQuery.Builder power(String myUserId){
//        List<String> friends = Arrays.asList("100265","100203","100308","100208","100206","100289","100220");
//        List<String> follows = Arrays.asList("100265","100308","100208","100206","100289","100220");
//        String myUserId = "10027";

//        Map map = videoService.userRelation(Integer.valueOf(myUserId));
//        map.put("toUserIds", Arrays.asList(-1));
//        map.put("friendIds", Arrays.asList(-1));
        List<Integer> friends = videoService.friendIds(Integer.valueOf(myUserId), KConstants.FriendStatus.ACTIVE_ATTENTION);
        List<Integer> follows = videoService.friendIds(Integer.valueOf(myUserId),KConstants.FriendStatus.FRIENDS);

        BooleanQuery.Builder query = new BooleanQuery.Builder();

        //open=1
        TermQuery termQuery1 = new TermQuery(new Term("open", "1"));
        query.add(termQuery1, BooleanClause.Occur.SHOULD);

        //open=2
        query.add(openPower2("2",friends).build(), BooleanClause.Occur.SHOULD);

        query.add(openPower2("3",follows).build(), BooleanClause.Occur.SHOULD);

        query.add(openPower("5",myUserId).build(), BooleanClause.Occur.SHOULD);

        query.add(openPower("6",myUserId).build(), BooleanClause.Occur.SHOULD);
        return query;
    }

    private Object obj(){
        String uri = request.getRequestURI().split("/")[1].replaceAll("New","");
        if ("video".equals(uri)){
            return new VideoDTO();
        }
        if ("article".equals(uri)){
            return new ArticleDTO();
        }
        if ("shortEssay".equals(uri)){
            return new ShortEssayDTO();
        }
        return null;
    }

    private Object valueOf(java.lang.reflect.Field field,String obj){
        if ("null".equals(obj)){
            return null;
        }
        // 如果类型是String
        if (field.getGenericType().toString().equals(
                "class java.lang.String")) { // 如果type是类类型，则前面包含"class "，后面跟类名
            return obj;
        }
        // 如果类型是Integer
        if (field.getGenericType().toString().equals(
                "class java.lang.Integer")) {
            return Integer.valueOf(obj);
        }
        // 如果类型是Double
        if (field.getGenericType().toString().equals(
                "class java.lang.Double")) {
            return Double.valueOf(obj);
        }
        // 如果类型是Long
        if (field.getGenericType().toString().equals(
                "class java.lang.Long")) {
            return Long.valueOf(obj);
        }
        // 如果类型是Boolean 是封装类
        if (field.getGenericType().toString().equals(
                "class java.lang.Boolean")) {
            return Boolean.valueOf(obj);
        }
        // 如果类型是boolean 基本数据类型不一样 这里有点说名如果定义名是 isXXX的 那就全都是isXXX的
        // 反射找不到getter的具体名
        if (field.getGenericType().toString().equals("boolean")) {
            return Boolean.valueOf(obj);
        }
        if (field.getGenericType().toString().equals("int")) {
            return Integer.valueOf(obj);
        }
        return obj;
    }
}
