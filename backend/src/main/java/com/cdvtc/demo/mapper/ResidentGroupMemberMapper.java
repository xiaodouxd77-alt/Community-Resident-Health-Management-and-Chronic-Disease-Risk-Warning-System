package com.cdvtc.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdvtc.demo.entity.ResidentGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ResidentGroupMemberMapper extends BaseMapper<ResidentGroupMember> {

    /**
     * 恢复被逻辑删除的成员关系。唯一约束 (group_id, resident_id) 使 tombstone 行无法再次插入，
     * 因此重新加入时先尝试恢复历史行，仅在不存在历史行时才真正插入。
     */
    @Update("UPDATE resident_group_member SET deleted = 0, added_by = #{addedBy}, remark = #{remark}, "
            + "update_time = NOW() WHERE group_id = #{groupId} AND resident_id = #{residentId} AND deleted = 1")
    int restoreMember(@Param("groupId") Long groupId,
                      @Param("residentId") Long residentId,
                      @Param("addedBy") Long addedBy,
                      @Param("remark") String remark);
}
