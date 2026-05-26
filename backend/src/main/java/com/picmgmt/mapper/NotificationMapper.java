package com.picmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.Notification;
import com.picmgmt.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("""
        <script>
        SELECT n.id, n.type, n.recipient_user_id, n.actor_user_id,
               n.image_id, i.image_name, i.storage_key AS image_storage_key,
               n.comment_id, n.content_preview, n.read_flag AS `read`, n.create_time
        FROM notifications n
        LEFT JOIN images i ON n.image_id = i.id
        WHERE n.recipient_user_id = #{recipientUserId}
        <if test='unreadOnly'>AND n.read_flag = 0</if>
        ORDER BY n.create_time DESC, n.id DESC
        </script>
    """)
    Page<NotificationVO> selectNotificationVOPage(Page<NotificationVO> page,
                                                   @Param("recipientUserId") Long recipientUserId,
                                                   @Param("unreadOnly") boolean unreadOnly);

    @Select("SELECT COUNT(*) FROM notifications WHERE recipient_user_id = #{recipientUserId} AND read_flag = 0")
    Long countUnread(@Param("recipientUserId") Long recipientUserId);

    @Update("UPDATE notifications SET read_flag = 1 WHERE id = #{id} AND recipient_user_id = #{recipientUserId}")
    int markRead(@Param("id") Long id, @Param("recipientUserId") Long recipientUserId);

    @Update("UPDATE notifications SET read_flag = 1 WHERE recipient_user_id = #{recipientUserId} AND read_flag = 0")
    int markAllRead(@Param("recipientUserId") Long recipientUserId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM notifications WHERE id = #{id} AND recipient_user_id = #{recipientUserId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("recipientUserId") Long recipientUserId);
}
