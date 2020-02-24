package bulk;

import bean.Student;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;

import java.io.IOException;

public class BulkWriter {
    public static void main(String[] args) throws IOException {
        //创建连接
        JestClientFactory jestClientFactory = new JestClientFactory();
        HttpClientConfig httpClientConfig = new HttpClientConfig.Builder("http://hadoop111:9200").build();
        jestClientFactory.setHttpClientConfig(httpClientConfig);
        JestClient client = jestClientFactory.getObject();

        Student student1 = new Student();
        Student student2 = new Student();
        student1.setStu_id("6");
        student2.setStu_id("7");
        student1.setName("aaa");
        student2.setName("bbb");

        Index index1 = new Index.Builder(student1).id("1006").build();
        Index index2 = new Index.Builder(student2).id("1007").build();

        //批量加入
        Bulk bulk = new Bulk.Builder()
                .addAction(index1)
                .addAction(index2)
                .defaultIndex("stu")
                .defaultType("_doc")
                .build();

        //执行
        client.execute(bulk);

        //关闭
        client.shutdownClient();
    }
}
