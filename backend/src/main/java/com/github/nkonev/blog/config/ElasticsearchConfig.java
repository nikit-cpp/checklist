package com.github.nkonev.blog.config;

import com.github.nkonev.blog.converter.PostConverter;
import com.github.nkonev.blog.entity.elasticsearch.Post;
import com.github.nkonev.blog.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Optional;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.github.nkonev.blog.repo.elasticsearch")
@EntityScan(basePackages = "com.github.nkonev.blog.entity.elasticsearch")
public class ElasticsearchConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private PostConverter postConverter;

    @Value("${custom.elasticsearch.drop-first:true}")
    private boolean dropFirst;


    @Value("${custom.elasticsearch.create-index:true}")
    private boolean createIndex;

    @Value("classpath:/config/index-post.json")
    private Resource indexSettings;

    @Value("classpath:/config/index-post-mapping.json")
    private Resource postMapping;

    @PostConstruct
    public void pc(){

        if (dropFirst) {
            try {
                LOGGER.info("Dropping elasticsearch index");
                elasticsearchTemplate.deleteIndex(Post.INDEX);
            } catch (Exception e) {
                LOGGER.error("Error during dropping elasticsearch index");
            }
        }

        if (createIndex) {
            try {
                LOGGER.info("Creating elasticsearch index");
                final String settings = ResourceUtils.stringFromResource(indexSettings);
                elasticsearchTemplate.createIndex(Post.INDEX, settings);

                final String mapping = ResourceUtils.stringFromResource(postMapping);
                elasticsearchTemplate.putMapping(Post.INDEX, Post.TYPE, mapping);
                LOGGER.info("Successfully created elasticsearch index");
            } catch (Exception e) {
                LOGGER.error("Error during create elasticsearch index", e);
            }
        }
    }

}
