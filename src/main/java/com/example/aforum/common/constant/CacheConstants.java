package com.example.aforum.common.constant;

/**
 * 缓存常量
 */
public class CacheConstants {

    private CacheConstants() {
    }

    /** 用户缓存前缀 */
    public static final String USER_CACHE_PREFIX = "user:";

    /** 帖子缓存前缀 */
    public static final String POST_CACHE_PREFIX = "post:";

    /** 验证码缓存前缀 */
    public static final String CAPTCHA_CACHE_PREFIX = "captcha:";

    /** Token 黑名单前缀 */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 用户 Token 前缀 */
    public static final String USER_TOKEN_PREFIX = "user:token:";

    /** 热门帖子缓存 */
    public static final String HOT_POSTS_CACHE = "hot:posts";

    /** 分类缓存 */
    public static final String CATEGORY_CACHE = "categories";

    /** 板块缓存 */
    public static final String BOARD_CACHE = "boards";

    /** 默认缓存过期时间（秒） */
    public static final long DEFAULT_EXPIRE_SECONDS = 3600;

    /** 验证码过期时间（秒） */
    public static final long CAPTCHA_EXPIRE_SECONDS = 300;

    /** Shared generation used by cached public post data. */
    public static final String POST_GENERATION_KEY = "post-generation";

    /** Direct cache entry for active categories and boards. */
    public static final String SEARCH_OPTIONS_CACHE_KEY = "search-options";
}
