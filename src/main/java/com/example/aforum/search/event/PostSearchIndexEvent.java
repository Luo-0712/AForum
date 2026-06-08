package com.example.aforum.search.event;

public record PostSearchIndexEvent(Long postId, Action action) {

    public enum Action {
        UPSERT,
        DELETE
    }
}
