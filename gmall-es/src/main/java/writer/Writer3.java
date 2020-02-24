package writer;

import bean.Student;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;

import java.io.IOException;

public class Writer3 {
    public static void main(String[] args) throws IOException {
        //创建连接
        JestClientFactory jestClientFactory = new JestClientFactory();
        HttpClientConfig httpClientConfig = new HttpClientConfig.Builder("http://hadoop111:9200").build();
        jestClientFactory.setHttpClientConfig(httpClientConfig);
        JestClient client = jestClientFactory.getObject();

        Student student = new Student();
        student.setStu_id("3");
        student.setName("zhaoliu");

        Index index = new Index.Builder(student)
                .index("stu")
                .type("_doc")
                .id("1003")
                .build();

        //执行
        client.execute(index);

        //关闭
        client.shutdownClient();
    }
}
