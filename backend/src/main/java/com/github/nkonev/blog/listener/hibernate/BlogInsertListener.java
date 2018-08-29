package com.github.nkonev.blog.listener.hibernate;

import com.github.nkonev.blog.dto.PostDTO;
import com.github.nkonev.blog.entity.jpa.Post;
import com.github.nkonev.blog.services.PostService;
import com.github.nkonev.blog.services.SeoCacheListenerProxy;
import com.github.nkonev.blog.services.WebSocketService;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlogInsertListener implements PostInsertEventListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BlogInsertListener.class);

    private static final long serialVersionUID = 6798233539917338414L;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private PostService postService;

    @Autowired
    private SeoCacheListenerProxy seoCacheListenerProxy;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        LOGGER.trace("object: {}", event.getEntity());
        if (event.getEntity() instanceof Post) {
            Post post = (Post) event.getEntity();
            PostDTO postDTO = postService.convertToPostDTOWithCleanTags(post);
            webSocketService.sendInsertPostEvent(postDTO);
            seoCacheListenerProxy.rewriteCachedPage(post.getId());
            seoCacheListenerProxy.rewriteCachedIndex();
            LOGGER.debug("sql insert: {}", post);
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }
}
