package com.example.aforum.search.service;

import com.example.aforum.search.vo.SearchHealthVO;
import com.example.aforum.search.vo.SearchReindexVO;

public interface PostSearchIndexService {

    void indexPost(Long postId);

    void deletePost(Long postId);

    void updatePostStats(Long postId);

    SearchReindexVO reindexAllPublishedPosts();

    SearchHealthVO health();
}
