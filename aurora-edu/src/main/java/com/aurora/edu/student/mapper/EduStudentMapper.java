package com.aurora.edu.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.edu.student.entity.EduStudentDO;
import com.aurora.edu.student.model.resp.StudentAccountOptionResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EduStudentMapper extends BaseMapper<EduStudentDO> {

    @Select("""
            SELECT *
              FROM edu_student
             WHERE user_id = #{userId}
               AND deleted = 0
             ORDER BY create_time DESC, id DESC
             LIMIT 1
            """)
    EduStudentDO selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT
                   u.id AS userId,
                   u.username AS username,
                   u.nickname AS nickname
              FROM sys_user u
              JOIN sys_user_role ur
                ON ur.user_id = u.id
              JOIN sys_role r
                ON r.id = ur.role_id
               AND r.code = 'student'
               AND r.status = 1
               AND r.deleted = 0
             WHERE u.deleted = 0
               AND u.status = 1
               AND (
                    #{keyword} IS NULL
                    OR u.username LIKE CONCAT('%', #{keyword}, '%')
                    OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
               )
               AND NOT EXISTS (
                    SELECT 1
                      FROM edu_student occupied
                     WHERE occupied.user_id = u.id
                       AND occupied.deleted = 0
                       AND (
                            #{currentStudentId} IS NULL
                            OR occupied.id <> #{currentStudentId}
                       )
               )
             ORDER BY u.username ASC, u.id ASC
             LIMIT #{limit}
            """)
    List<StudentAccountOptionResp> selectBindableUsers(
            @Param("keyword") String keyword,
            @Param("currentStudentId") Long currentStudentId,
            @Param("limit") int limit);
}