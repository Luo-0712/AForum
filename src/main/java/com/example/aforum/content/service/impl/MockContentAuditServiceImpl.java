package com.example.aforum.content.service.impl;

import com.example.aforum.content.entity.ContentReview;
import com.example.aforum.content.mapper.ContentReviewMapper;
import com.example.aforum.content.service.AuditDecision;
import com.example.aforum.content.service.ContentAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MockContentAuditServiceImpl implements ContentAuditService {

    private static final List<String> BLOCKED_TERMS = List.of("违法", "诈骗", "色情", "暴力", "spam");

    private final ContentReviewMapper contentReviewMapper;

    @Override
    public AuditDecision review(String contentType, Long contentId, String title, String content) {
        String text = ((title == null ? "" : title) + " " + (content == null ? "" : content)).toLowerCase(Locale.ROOT);
        boolean blocked = BLOCKED_TERMS.stream().anyMatch(text::contains);
        AuditDecision decision = blocked
                ? new AuditDecision(false, "rejected", "high", new BigDecimal("0.9200"),
                "内容命中了基础风控词，需要人工修改后再发布。", "请删除违规、欺诈或攻击性表达后重新提交。", suggestTags(title, content))
                : new AuditDecision(true, "approved", "low", new BigDecimal("0.0800"),
                "mock-rule-v1 基础审核通过。", null, suggestTags(title, content));

        ContentReview review = new ContentReview();
        review.setContentType(contentType);
        review.setContentId(contentId);
        review.setDecision(decision.decision());
        review.setRiskLevel(decision.riskLevel());
        review.setRiskScore(decision.riskScore());
        review.setReason(decision.reason());
        review.setSuggestion(decision.suggestion());
        review.setModelName("mock-rule-v1");
        review.setPromptVersion("rule-v1");
        contentReviewMapper.insert(review);
        return decision;
    }

    private List<String> suggestTags(String title, String content) {
        String text = ((title == null ? "" : title) + " " + (content == null ? "" : content)).toLowerCase(Locale.ROOT);
        List<String> tags = new ArrayList<>();
        if (text.contains("spring") || text.contains("java")) {
            tags.add("Java");
        }
        if (text.contains("react") || text.contains("前端")) {
            tags.add("前端");
        }
        if (text.contains("ai") || text.contains("智能") || text.contains("agent")) {
            tags.add("AI");
        }
        if (text.contains("问题") || text.contains("求助") || text.contains("?") || text.contains("？")) {
            tags.add("问题求助");
        }
        if (tags.isEmpty() && StringUtils.hasText(content)) {
            tags.add("经验分享");
        }
        return tags;
    }
}
