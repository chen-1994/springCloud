package com.fishing.works.lucene;


import cn.neu.lucene.MyIKAnalyzer;
import com.fishing.annotation.Operation;
import com.fishing.common.constants.ResultCode;
import com.fishing.common.response.PageData;
import com.fishing.common.response.ResultDto;
import com.fishing.req.works.WorksQueryReq;
import com.fishing.resp.works.ArticleDTO;
import com.fishing.resp.works.ShortEssayDTO;
import com.fishing.resp.works.VideoDTO;
import com.fishing.utils.ReqUtil;
import com.fishing.works.common.service.WorksService;
import com.fishing.works.request.*;
import com.fishing.works.request.Video.VideoAddReq;
import com.fishing.works.request.single.VideoId;
import com.fishing.works.service.ArticleService;
import com.fishing.works.service.ShortEssayService;
import com.fishing.works.service.VideoService;
import com.fishing.works.tool.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.stream.Collectors;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author generator
 * @since 2022-07-25
 */
@RestController
@Api(tags = "视频模块")
@RequestMapping(value={"/articleNew","/videoNew","/shortEssayNew"})
@Slf4j
public class WorksLuceneController {

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

    @ResponseBody
    @RequestMapping("/createIndex")
    public String createIndex() throws IOException {
        worksLucenService.createIndex();
        return"success";
    }

    @ResponseBody
    @RequestMapping(value = "selectByKeyWord", method = {RequestMethod.POST})
    public ResultDto<PageData> selectByKeyWord(@RequestBody @Validated KeyWordReq req) throws Exception {
        String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
//        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/video");
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/"+dir));
        // 索引读取工具
        IndexReader reader = DirectoryReader.open(directory);
        // 索引搜索工具
        IndexSearcher searcher = new IndexSearcher(reader);
        // 创建查询解析器,两个参数：默认要查询的字段的名称，分词器
        MyIKAnalyzer analyzer = new MyIKAnalyzer();

        //2.多条件查询
        String[] fields = { "content","userName","lableNames"};
        String[] keys = { req.getName(),req.getName(),req.getName()};
        return ResultDto.success(pageData(fields,keys,analyzer,searcher,req.getCurrentPage(),req.getPageSize()));
    }

    @ResponseBody
    @RequestMapping(value = "selectByKeyWordOld", method = {RequestMethod.POST})
    public ResultDto<PageData> selectByKeyWordOld(@RequestBody @Validated KeyWordReq req) throws Exception {
        String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
//        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/video");
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/"+dir));
        // 索引读取工具
        IndexReader reader = DirectoryReader.open(directory);
        // 索引搜索工具
        IndexSearcher searcher = new IndexSearcher(reader);
        // 创建查询解析器,两个参数：默认要查询的字段的名称，分词器
        MyIKAnalyzer analyzer = new MyIKAnalyzer();

        //2.多条件查询
        String[] fields = { "content","userName","lableNames"};
        String[] keys = { req.getName(),req.getName(),req.getName()};
        /*Query query = null;

        TopDocs topDocs = null;
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (keys != null && keys.length > 0) {
            for (int i = 0; i < keys.length; i++) {
                QueryParser queryParser = new QueryParser(fields[i], analyzer);
                Query q = queryParser.parse(keys[i]);
//                builder.add(q, BooleanClause.Occur.MUST);//and操作
                builder.add(q, BooleanClause.Occur.SHOULD);//或者操作
            }
            //添加权限控制
            QueryParser queryParser = new QueryParser("open", analyzer);
            Query q = queryParser.parse("1");
            builder.add(q, BooleanClause.Occur.SHOULD);//或者操作
            Query q2 = queryParser.parse("2");
            builder.add(q2, BooleanClause.Occur.SHOULD);//或者操作

            // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
            query = builder.build();
//            topDocs = searcher.search(query,10);
            topDocs = pageSearch(query,searcher,req.getCurrentPage(),req.getPageSize());
        }*/

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

        TopDocs topDocs = pageSearch(query.build(),searcher,req.getCurrentPage(),req.getPageSize());


        // 获取总条数
        System.out.println("本次搜索共找到" + topDocs.totalHits + "条数据");
        // 显示查询结果
        List list = list(fields,keys,analyzer,searcher,req.getCurrentPage(),req.getPageSize());
        PageData pageData = new PageData(req.getCurrentPage(),req.getPageSize());
        pageData.setTotalRecord((int)topDocs.totalHits);
        pageData.setDatas(list);
        return ResultDto.success(pageData);
    }

    @ApiOperation("视频查询：根据地区")
    @RequestMapping(value = "selectByRegion", method = {RequestMethod.POST})
    public ResultDto<PageData> selectByRegion(@RequestBody @Validated RegionReq req) throws Exception {
        {
            String dir = request.getRequestURI().split("/")[1].replaceAll("New","");
            Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexDir+"/"+dir));
            // 索引读取工具
            IndexReader reader = DirectoryReader.open(directory);
            // 索引搜索工具
            IndexSearcher searcher = new IndexSearcher(reader);
            // 创建查询解析器,两个参数：默认要查询的字段的名称，分词器
            MyIKAnalyzer analyzer = new MyIKAnalyzer();

            //2.多条件查询
            String[] fields = new String[0];
            String[] keys = new String[0];
            if (!StringUtil.isNullOrEmpty(req.getRegion())){
                fields = new String[]{"region"};
                keys = new String[]{req.getRegion()};
            } else {
                fields = new String[]{"lng","lat"};
                keys = new String[]{req.getLng(),req.getLat()};
            }
            Query query = null;
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            TopDocs topDocs = null;
            if (keys != null && keys.length > 0) {
                for (int i = 0; i < keys.length; i++) {
                    QueryParser queryParser = new QueryParser(fields[i], analyzer);
                    Query q = queryParser.parse(keys[i]);
                builder.add(q, BooleanClause.Occur.MUST);//and操作
//                    builder.add(q, BooleanClause.Occur.SHOULD);//或者操作
                }
                // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
                query = builder.build();
//            topDocs = searcher.search(query,10);
                topDocs = pageSearch(query,searcher,req.getCurrentPage(),req.getPageSize());
            }
            // 获取总条数
            System.out.println("本次搜索共找到" + topDocs.totalHits + "条数据");
            // 显示查询结果
            List list = list(fields,keys,analyzer,searcher,req.getCurrentPage(),req.getPageSize());
            PageData pageData = new PageData(req.getCurrentPage(),req.getPageSize());
            pageData.setTotalRecord((int)topDocs.totalHits);
            pageData.setDatas(list);
            return ResultDto.success(pageData);
        }
    }

    @ApiOperation("视频查询：关注")
    @RequestMapping(value = {"selectByFollow"}, method = RequestMethod.POST)
    public ResultDto<PageData> selectByFollow(@RequestBody @Validated PageParam param) {
        return ResultDto.success(((WorksService)getService()).selectByFollow(param));
    }

    @ApiOperation("视频查询：推荐算法")
    @Operation(name = "name")
    @RequestMapping(value = {"getPureVideo","selectByHot"}, method = RequestMethod.POST)
    public ResultDto<PageData> getPureVideo(@RequestBody @Validated KeyWordHotReq req) {
        return ResultDto.success(((WorksService)getService()).selectByHot(req));
    }

    @ApiOperation("视频查询：我的")
    @RequestMapping(value = "getMyVideo", method = RequestMethod.POST)
    public ResultDto<PageData> getMyVideo(@RequestBody @Validated UserWorksReq req) {
        PageParam pageParam = new PageParam();
        pageParam.setCurrentPage(req.getCurrentPage());
        pageParam.setPageSize(req.getPageSize());
        if (req.getUserId()==null || ReqUtil.getUserId().intValue() == req.getUserId()){
            return ResultDto.success(((WorksService)getService()).getMyWorks(pageParam));
        } else {
            return ResultDto.success(((WorksService)getService()).selectByUserId(Long.valueOf(req.getUserId()),pageParam));
        }
    }

    @ApiOperation("视频：发布")
    @RequestMapping(value = "add", method = {RequestMethod.POST})
    public ResultDto addModel(@RequestBody @Validated VideoAddReq videoAddReq){
//        videoService.addModel(videoAddReq);
        ((WorksService)getService()).addModel(videoAddReq);
        return ResultDto.success();
    }

    @ApiOperation("视频：删除（修改状态）")
    @RequestMapping(value = "del", method = {RequestMethod.POST})
    public ResultDto updateModel(@RequestBody @Validated VideoId videoId){
        if (videoId==null) {
            ResultDto.failure(ResultCode.VIDEOID_IS_NOT_ALLOW_EMPTY);
        }
        ((WorksService)getService()).delModel(videoId.getVideoId());
        return ResultDto.success();
    }

    @ApiOperation("视频查询：根据作品id查询")
    @RequestMapping(value = "selectById", method = RequestMethod.POST)
    public ResultDto<VideoDTO> selectById(@RequestBody @Validated VideoId videoId) {
        return ResultDto.success(((WorksService)getService()).selectByIdDto(videoId.getVideoId()));
    }




    //================================查询方法==========================================
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



    //2. 多条件查询。这里实现的是and操作
    //注：要查询的字段必须是index的
    //即doc.add(new Field("pid", rs.getString("pid"), Field.Store.YES,Field.Index.TOKENIZED));
    public TopDocs queryByMultiKeys(IndexSearcher indexSearcher, String[] fields,
                                                     String[] keys,Query query2) {
        try {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            if (keys != null && keys.length > 0) {
                for (int i = 0; i < keys.length; i++) {
                    QueryParser queryParser = new QueryParser(fields[i], new MyIKAnalyzer());
                    Query query = queryParser.parse(keys[i]);
                    builder.add(query, BooleanClause.Occur.MUST);//and操作
                }
                // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
                TopDocs topDocs = indexSearcher.search(builder.build(),10);
                query2 = builder.build();
                return topDocs;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    private List showSearchResults(IndexSearcher searcher, ScoreDoc[] hits, Query query, MyIKAnalyzer analyzer) throws Exception {
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
            Map<String,String > map = ((BooleanQuery)query).clauses().stream().collect(Collectors.toMap(obj -> ((TermQuery)obj.getQuery()).getTerm().field(), obj -> obj.getQuery().toString()));
            for (IndexableField f : fields) {
                java.lang.reflect.Field field = cla.getDeclaredField(f.name());
                field.setAccessible(true);
                field.set(dto,valueOf(field,d.get(f.name())));
//                if("content".equals(f.name())){
                if (map.get(f.name())!=null){
                    TokenStream tokenStream = analyzer.tokenStream(f.name(), new StringReader(d.get(f.name())));
                    String fieldContent = highlighter.getBestFragment(tokenStream, d.get(f.name()));
                    field.set(dto,valueOf(field,fieldContent));
                    System.out.print("\t"+fieldContent);
                }
                else{
                    System.out.print("\t"+d.get(f.name()));
                }
            }
            System.out.println("<br>");
            list.add(dto);
        }
        return list;
    }


    private List<VideoDTO> showSearchResultsOld(IndexSearcher searcher, ScoreDoc[] hits, Query query, MyIKAnalyzer analyzer) throws Exception {
        List<VideoDTO> list = new ArrayList<>();
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
            VideoDTO dto = new VideoDTO();
            Class cla = dto.getClass();
            Map<String,String > map = ((BooleanQuery)query).clauses().stream().collect(Collectors.toMap(obj -> ((TermQuery)obj.getQuery()).getTerm().field(), obj -> obj.getQuery().toString()));
            for (IndexableField f : fields) {
                java.lang.reflect.Field field = cla.getDeclaredField(f.name());
                field.setAccessible(true);
                field.set(dto,valueOf(field,d.get(f.name())));
//                if("content".equals(f.name())){
                if (map.get(f.name())!=null){
                    TokenStream tokenStream = analyzer.tokenStream(f.name(), new StringReader(d.get(f.name())));
                    String fieldContent = highlighter.getBestFragment(tokenStream, d.get(f.name()));
                    field.set(dto,valueOf(field,fieldContent));
                    System.out.print("\t"+fieldContent);
                }
                else{
                    System.out.print("\t"+d.get(f.name()));
                }
            }
            System.out.println("<br>");
            list.add(dto);
        }
        return list;
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

    //我这是更新指定id的数据
    @RequestMapping("/updateIndex")
    @ResponseBody
    public String update(String videoId) throws IOException{
        // 创建目录对象
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath("E:\\indexDir"));
        // 创建配置对象
        IndexWriterConfig conf = new IndexWriterConfig(new MyIKAnalyzer());
        // 创建索引写出工具
        IndexWriter writer = new IndexWriter(directory, conf);
        // 创建新的文档数据
        Document doc = new Document();
        doc.add(new StringField("id",videoId, Field.Store.YES));
        VideoDTO dto = (VideoDTO)videoService.getWorks(Long.valueOf(videoId));
        dto.setUserName("刘马按时啊");
//        myUserMapper.updateByPrimaryKeySelective(dto);
        VideoDTO user = (VideoDTO)videoService.getWorks(Long.valueOf(videoId));
        doc.add(new TextField("content", dto.getUserName()+" "+dto.getContent()+" "+dto.getLableNames(), Field.Store.YES));
        writer.updateDocument(new Term("id",videoId), doc);
        // 提交
        writer.commit();
        // 关闭
        writer.close();
        return "success";
    }

    @RequestMapping("/deleteIndex")
    @ResponseBody
    public String deleteIndex() throws IOException{
        // 创建目录对象
        Directory directory = FSDirectory.open(FileSystems.getDefault().getPath("E:\\indexDir"));
        // 创建配置对象
        IndexWriterConfig conf = new IndexWriterConfig(new MyIKAnalyzer());
        // 创建索引写出工具
        IndexWriter writer = new IndexWriter(directory, conf);
        // 根据词条进行删除
        writer.deleteDocuments(new Term("id", "000e17a25e704ee59a91697cd4075baf"));
        // 提交
        writer.commit();
        // 关闭
        writer.close();
        return "success";
    }


}

