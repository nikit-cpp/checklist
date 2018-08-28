package com.github.nkonev.blog.services;

import com.github.nkonev.blog.converter.PostConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Optional;

@Service
public class ElasticsearchPopulator {
    @Autowired
    private ElasticsearchOperations es;

    @Autowired
    private com.github.nkonev.blog.repo.elasticsearch.IndexPostRepository indexPostRepositoryElastic;

    @Autowired
    private com.github.nkonev.blog.repo.jpa.PostRepository postRepositoryJpa;

    @Autowired
    private PostConverter postConverter;

    private static final Logger LOGGER  = LoggerFactory.getLogger(ElasticsearchPopulator.class);

    @PostConstruct
    public void pc(){

//        boolean index = es.createIndex("blog");
//        LOGGER.info("Elasticsearch index created: {}", index);

        LOGGER.info("Starting populate elasticsearch index");
        final Collection<Long> postIds = postRepositoryJpa.findPostIds();
        for (Long id: postIds) {

            Optional<com.github.nkonev.blog.entity.jpa.Post> post = postRepositoryJpa.findById(id);
            if (post.isPresent()){
                com.github.nkonev.blog.entity.jpa.Post jpaPost = post.get();
                LOGGER.info("Copying post: {}", id);
                indexPostRepositoryElastic.save(postConverter.toElasticsearchPost(jpaPost));
            }
        }
        LOGGER.info("Finished populate elasticsearch index");
    }

}