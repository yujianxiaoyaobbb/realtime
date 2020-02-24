package reader;

import com.alibaba.fastjson.JSONObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MaxAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Reader {
    public static void main(String[] args) throws IOException {
        //1.创建ES客户端
        JestClientFactory jestClientFactory = new JestClientFactory();
        HttpClientConfig httpClientConfig = new HttpClientConfig.Builder("http://hadoop111:9200").build();
        jestClientFactory.setHttpClientConfig(httpClientConfig);
        JestClient jestClient = jestClientFactory.getObject();

        //创建查询语句的对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //Bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.filter(new TermQueryBuilder("sex", "male"));
        boolQueryBuilder.must(new MatchQueryBuilder("favor","球"));
        searchSourceBuilder.query(boolQueryBuilder);

        //aggs
        TermsAggregationBuilder count_by_class = new TermsAggregationBuilder("count_by_class", ValueType.LONG);
        count_by_class.field("class_id");
        count_by_class.size(2);

        MaxAggregationBuilder max_age = new MaxAggregationBuilder("max_age");
        max_age.field("age");

        searchSourceBuilder.aggregation(count_by_class);
        searchSourceBuilder.aggregation(max_age);

        //分页
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(2);

        //创建查询对象
        Search search = new Search.Builder(searchSourceBuilder.toString()).build();

        //执行查询
        SearchResult result = jestClient.execute(search);

        //解析结果
        System.out.println("成功获取" + result.getTotal() + "条");
        System.out.println("最高分:" + result.getMaxScore());

        //获取hits标签
        List<SearchResult.Hit<Map, Void>> hits = result.getHits(Map.class);
        for (SearchResult.Hit<Map, Void> hit : hits) {
            JSONObject jsonObject = new JSONObject();
            Map source = hit.source;
            for (Object o : source.keySet()) {
                jsonObject.put((String)o,source.get(o));
            }
            jsonObject.put("index",hit.index);
            jsonObject.put("type",hit.type);
            jsonObject.put("id",hit.id);
            System.out.println(jsonObject);
        }

        //解析聚合
        MetricAggregation aggregations = result.getAggregations();

        //获取年龄组
        MaxAggregation max_age1 = aggregations.getMaxAggregation("max_age");
        System.out.println("最大年龄为:" + max_age1.getMax());

        //获取班级分组
        TermsAggregation count_by_class1 = aggregations.getTermsAggregation("count_by_class");
        for (TermsAggregation.Entry bucket : count_by_class1.getBuckets()) {
            System.out.println(bucket.getKey() + "->" + bucket.getCount());
        }

        //关闭
        jestClient.shutdownClient();
    }
}
