package com.example.aforum.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.content.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
