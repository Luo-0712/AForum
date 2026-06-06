package com.example.aforum.content.service;

public interface ContentAuditService {

    AuditDecision review(String contentType, Long contentId, String title, String content);
}
