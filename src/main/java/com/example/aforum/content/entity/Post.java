package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post")
public class Post extends BaseEntity {

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 摘要 */
    private String summary;

    /** 封面图URL */
    private String coverImage;

    /** 作者ID */
    private Long userId;

    /** 分类ID */
    private Long categoryId;

    /** 板块ID */
    private Long boardId;

    /** 状态：0-草稿 1-已发布 2-审核中 3-已拒绝 4-已关闭 */
    private Integer status;

    /** 是否置顶 */
    private Boolean top;

    /** 是否精华 */
    private Boolean essence;

    /** 浏览数 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 最后评论时间 */
    private java.time.LocalDateTime lastCommentAt;
}
