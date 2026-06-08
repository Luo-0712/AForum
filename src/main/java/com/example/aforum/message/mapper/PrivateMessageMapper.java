package com.example.aforum.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.message.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    @Update("""
            UPDATE private_message
            SET read_status = 1, updated_at = CURRENT_TIMESTAMP
            WHERE receiver_id = #{receiverId}
              AND sender_id = #{senderId}
              AND read_status = 0
              AND deleted = 0
            """)
    int markConversationRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);
}
