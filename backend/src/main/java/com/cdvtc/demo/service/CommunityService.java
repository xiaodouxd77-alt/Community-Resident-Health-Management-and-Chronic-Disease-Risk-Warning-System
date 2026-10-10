package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.entity.Community;
import com.cdvtc.demo.mapper.CommunityMapper;
import com.cdvtc.demo.vo.CommunityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService extends ServiceImpl<CommunityMapper, Community> {

    public List<CommunityVO> listAll() {
        return lambdaQuery().orderByAsc(Community::getCommunityCode).list()
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    public Map<Long, String> nameMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return listByIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Community::getId, Community::getCommunityName, (a, b) -> a));
    }

    public String nameOf(Long id) {
        if (id == null) {
            return null;
        }
        Community c = getById(id);
        return c == null ? null : c.getCommunityName();
    }

    /** 社区必须存在，否则参数错误。 */
    public Community requireExists(Long id) {
        Community c = getById(id);
        if (c == null) {
            throw new BusinessException(400, "所属社区不存在");
        }
        return c;
    }

    public CommunityVO toVO(Community c) {
        CommunityVO vo = new CommunityVO();
        vo.setId(c.getId());
        vo.setCommunityCode(c.getCommunityCode());
        vo.setCommunityName(c.getCommunityName());
        return vo;
    }

    public Map<Long, Community> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return listByIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Community::getId, Function.identity(), (a, b) -> a));
    }
}
