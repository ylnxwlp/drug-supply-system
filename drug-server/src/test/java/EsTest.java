import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.supply.config.ElasticsearchConfig;
import com.supply.properties.ElasticsearchProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;

@SpringBootTest(classes = ElasticsearchConfig.class)
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class EsTest {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Test
    public void test0() throws IOException {
        boolean isConnected = elasticsearchClient.ping().value();
        if (isConnected) {
            System.out.println("Successfully connected to Elasticsearch!");
        } else {
            System.out.println("Failed to connect to Elasticsearch.");
        }
    }

    @Test
    public void test() throws IOException {
        CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(c -> c
                .index("my_index") // 索引名称
                .mappings(m -> m    // 设置映射
                        .properties("content", p -> p
                                .text(t -> t
                                        .analyzer("ik_max_word"))    // content 字段为文本类型
                        )
                        .properties("title", p -> p
                                .keyword(k -> k) // title 字段为 keyword 类型
                        )
                        .properties("author", p -> p
                                .keyword(k -> k) // author 字段为 keyword 类型
                        )
                )
        );
        System.out.println("Index created with mappings and settings: " + createIndexResponse.acknowledged());
    }

    @Test
    public void test1() throws IOException {
        // 创建一个新文档并索引到 "my_index"
        IndexResponse indexResponse = elasticsearchClient.index(i -> i
                .index("my_index")  // 索引名称
                .id("1")            // 文档ID，如果不指定会自动生成
                .document(new com.supply.entity.Test("福州大学水电费爆炸？", "真相", "S")) // 文档内容，MyDocument是自定义对象
        );
        System.out.println("Document indexed with ID: " + indexResponse.id());
    }

    @Test
    public void test2() throws IOException {
        // 从 "my_index" 中获取 ID 为 1 的文档
        GetResponse<com.supply.entity.Test> getResponse = elasticsearchClient.get(g -> g
                        .index("my_index") // 索引名称
                        .id("1"),          // 文档ID
                com.supply.entity.Test.class   // 将返回的文档反序列化为 MyDocument 类型
        );
        com.supply.entity.Test document = getResponse.source();
        if (document != null) {
            System.out.println("Retrieved document content: " + document.getContent());
        }
    }

    @Test
    public void test3() throws IOException {
        // 在 "my_index" 中搜索字段 content 中包含文档
        SearchResponse<com.supply.entity.Test> searchResponse = elasticsearchClient.search(s -> s
                        .index("my_index")
                        .query(q -> q
                                .match(m -> m
                                        .field("content")  // 查询字段
                                        .query("狗屎大学") // 查询关键词
                                )
                        ),
                com.supply.entity.Test.class  // 文档类型
        );

        for (Hit<com.supply.entity.Test> hit : searchResponse.hits().hits()) {
            if (hit.source() != null) {
                System.out.println("Found document: " + hit.source().getContent());
            }
        }
    }

    @Test
    public void test4() throws IOException {
        // 部分更新文档，修改字段
        UpdateResponse<com.supply.entity.Test> updateResponse = elasticsearchClient.update(u -> u
                        .index("my_index") // 索引名称
                        .id("1")           // 文档ID
                        .doc(new com.supply.entity.Test("福州大学水电费真是一滩狗屎", "吐槽", "S")), // 更新文档内容
                com.supply.entity.Test.class // 文档类型
        );
        System.out.println("Document updated with new content: " + updateResponse.result());
    }

    @Test
    public void test5() throws IOException {
        // 删除 ID 为 1 的文档
        elasticsearchClient.delete(d -> d
                .index("my_index") // 索引名称
                .id("1")           // 文档ID
        );
    }

    @Test
    public void test6() throws IOException {
        // 检查 ID 为 1 的文档是否存在
        BooleanResponse existsResponse = elasticsearchClient.exists(e -> e
                .index("my_index")
                .id("1")
        );
        if (existsResponse.value()) {
            System.out.println("Document exists.");
        } else {
            System.out.println("Document does not exist.");
        }
    }

    @Test
    public void test7() throws IOException {
        /// 调用删除索引的 API
        DeleteIndexResponse deleteIndexResponse = elasticsearchClient.indices().delete(d -> d
                .index("my_index")  // 要删除的索引名称
        );
        // 判断删除操作是否成功
        System.out.println("Index deleted: " + deleteIndexResponse.acknowledged());
    }

    @Test
    public void test8() throws IOException {
        // 使用 exists 方法检查索引是否存在
        BooleanResponse indexExistsResponse = elasticsearchClient.indices().exists(e -> e
                .index("my_index")  // 要检查的索引名称
        );
        // 输出索引是否存在
        if (indexExistsResponse.value()) {
            System.out.println("Index exists.");
        } else {
            System.out.println("Index does not exist.");
        }
    }

    @Test
    public void testCompositeQuery() throws IOException {
        // 执行复合查询
        SearchResponse<com.supply.entity.Test> searchResponse = elasticsearchClient.search(s -> s
                        .index("my_index") // 索引名称
                        .query(q -> q
                                .bool(b -> b // 使用 bool 查询
                                        .must(m -> m // must 子句，表示必须满足的条件
                                                .match(mq -> mq     //match表示关键字查询
                                                        .field("content") // 查询字段
                                                        .query("狗屎大学") // 查询关键词
                                                )
                                        )
                                        .filter(f -> f // filter 子句，表示过滤条件
                                                .term(t -> t    //term表示精确匹配
                                                        .field("title") // 过滤字段
                                                        .value("真相") // 过滤值
                                                )
                                        )
                                )
                        ),
                com.supply.entity.Test.class // 文档类型
        );

        // 输出查询结果
        for (Hit<com.supply.entity.Test> hit : searchResponse.hits().hits()) {
            if (hit.source() != null) {
                System.out.println("Found document: " + hit.source().getContent());
            }
        }
    }

    @Test
    public void testHighlightQuery() throws IOException {
        // 执行搜索并启用高亮
        SearchResponse<com.supply.entity.Test> searchResponse = elasticsearchClient.search(s -> s
                        .index("my_index") // 索引名称
                        .query(q -> q
                                .match(m -> m
                                        .field("content") // 查询字段
                                        .query("吃狗屎") // 查询关键词
                                )
                        )
                        .highlight(h -> h // 添加高亮选项
                                .fields("content", hf -> hf // 指定高亮字段
                                        .preTags("<em>") // 高亮前缀
                                        .postTags("</em>") // 高亮后缀
                                )
                        ),
                com.supply.entity.Test.class // 文档类型
        );

        // 输出查询结果和高亮内容
        for (Hit<com.supply.entity.Test> hit : searchResponse.hits().hits()) {
            System.out.println("Found document ID: " + hit.id());

            // 获取高亮内容
            if (hit.highlight() != null && hit.highlight().containsKey("content")) {
                for (String highlight : hit.highlight().get("content")) {
                    System.out.println("Highlighted content: " + highlight);
                }
            } else {
                System.out.println("No highlighted content.");
            }
        }
    }

    @Test
    public void testAggregationQuery() throws IOException {
        // 执行聚合查询
        SearchResponse<Void> searchResponse = elasticsearchClient.search(s -> s
                        .index("my_index") // 索引名称
                        .size(0) // 不需要返回实际文档，只需要聚合结果
                        .aggregations("author_count", a -> a
                                .terms(t -> t
                                        .field("author.keyword") // 聚合字段
                                        .size(10)  // 限制返回的结果数量
                                )
                        ),
                Void.class // 不需要文档结果，只关注聚合结果
        );

        // 获取聚合结果
        Map<String, Aggregate> aggregations = searchResponse.aggregations();

        // 从聚合结果中提取指定的terms聚合
        StringTermsAggregate authorCountAggregation = aggregations.get("author_count").sterms();

        for (StringTermsBucket stringTermsBucket : authorCountAggregation.buckets().array()) {
            System.out.println("Author: " + stringTermsBucket.key().stringValue() + ", Document Count: " + stringTermsBucket.docCount());
        }
    }
}
