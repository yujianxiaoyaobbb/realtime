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

public class Reader3 {
    public static void main(String[] args) throws IOException {
        //1.创建ES客户端
        JestClientFactory jestClientFactory = new JestClientFactory();
        HttpClientConfig httpClientConfig = new HttpClientConfig.Builder("http://hadoop111:9200").build();
        jestClientFactory.setHttpClientConfig(httpClientConfig);
        JestClient jestClient = jestClientFactory.getObject();

        //创建查询语句的对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.filter(new TermQueryBuilder("sex","male"));
        boolQueryBuilder.must(new MatchQueryBuilder("favor","球"));

        searchSourceBuilder.query(boolQueryBuilder);

        TermsAggregationBuilder count_by_class = new TermsAggregationBuilder("count_by_class", ValueType.LONG);
        count_by_class.field("class_id");
        count_by_class.size(2);
        searchSourceBuilder.aggregation(count_by_class);

        MaxAggregationBuilder max_age = new MaxAggregationBuilder("max_age");
        max_age.field("age");
        searchSourceBuilder.aggregation(max_age);
        Search search = new Search.Builder(searchSourceBuilder.toString()).build();
        //执行
        SearchResult result = jestClient.execute(search);

        //解析result
        System.out.println(result.getTotal());
        System.out.println(result.getMaxScore());

        List<SearchResult.Hit<Map, Void>> hits = result.getHits(Map.class);
        for (SearchResult.Hit<Map, Void> hit : hits) {
            Map source = hit.source;
            JSONObject jsonObject = new JSONObject();
            for (Object o : source.keySet()) {
                jsonObject.put((String)o,source.get(o));
            }
            jsonObject.put("index", hit.index);
            jsonObject.put("type",hit.type);
            jsonObject.put("id",hit.id);
            System.out.println(jsonObject);
        }

        MetricAggregation aggregations = result.getAggregations();
        MaxAggregation max_age1 = aggregations.getMaxAggregation("max_age");
        System.out.println(max_age1.getMax());

        TermsAggregation count_by_class1 = aggregations.getTermsAggregation("count_by_class");
        for (TermsAggregation.Entry bucket : count_by_class1.getBuckets()) {
            System.out.println(bucket.getKey() + "->" + bucket.getCount());
        }

        //关闭
        jestClient.shutdownClient();
    }
}
