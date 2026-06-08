package com.example.aforum.search.listener;

import com.example.aforum.search.event.PostSearchIndexEvent;
import com.example.aforum.search.service.PostSearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostSearchIndexEventListener {

    private final PostSearchIndexService postSearchIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostSearchIndexEvent(PostSearchIndexEvent event) {
        if (event.action() == PostSearchIndexEvent.Action.UPSERT) {
            postSearchIndexService.indexPost(event.postId());
        } else {
            postSearchIndexService.deletePost(event.postId());
        }
    }
}
