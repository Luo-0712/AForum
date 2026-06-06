package com.example.aforum.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.content.entity.Board;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BoardMapper extends BaseMapper<Board> {
}
