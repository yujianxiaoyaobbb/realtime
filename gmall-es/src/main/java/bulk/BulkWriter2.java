package bulk;

import bean.Student;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;

import java.io.IOException;

public class BulkWriter2 {
    public static void main(String[] args) throws IOException {
        //创建连接
        JestClientFactory jestClientFactory = new JestClientFactory();
        HttpClientConfig httpClientConfig = new HttpClientConfig.Builder("http://hadoop111:9200").build();
        jestClientFactory.setHttpClientConfig(httpClientConfig);
        JestClient client = jestClientFactory.getObject();

        Student student = new Student();
        student.setStu_id("8");
        student.setName("ss");

        Student student1 = new Student();
        student1.setStu_id("9");
        student1.setName("ff");

        Index index = new Index.Builder(student)
                .id("1008")
                .build();
        Index index1 = new Index.Builder(student1)
                .id("1009")
                .build();
        //批量加入
        Bulk bulk = new Bulk.Builder()
                .defaultIndex("stu")
                .defaultType("_doc")
                .addAction(index)
                .addAction(index1)
                .build();

        //执行
        client.execute(bulk);
        //关闭
        client.shutdownClient();

    }
}
