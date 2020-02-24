package writer;

import bean.Student;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;

import java.io.IOException;


public class Writer {
    public static void main(String[] args) throws IOException {
        //创建ES客户端
        JestClientFactory jestClientFactory = new JestClientFactory();
        HttpClientConfig build = new HttpClientConfig.Builder("http://hadoop111:9200").build();
        jestClientFactory.setHttpClientConfig(build);
        JestClient jestClient = jestClientFactory.getObject();

        Student student = new Student();
        student.setStu_id("2");
        student.setName("lisi");

        Index index = new Index.Builder(student)
                .index("stu")
                .type("_doc")
                .id("1002")
                .build();
        //执行
        jestClient.execute(index);
        //关闭
        jestClient.shutdownClient();

    }
}
