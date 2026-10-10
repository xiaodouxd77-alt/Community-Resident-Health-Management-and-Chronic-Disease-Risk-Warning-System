package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.entity.DoctorResident;
import com.cdvtc.demo.mapper.DoctorResidentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorResidentService extends ServiceImpl<DoctorResidentMapper, DoctorResident> {

    /** 居民的负责医生；无授权关系时返回 null，表示待分配，不做随机分配。 */
    public Long findDoctorIdForResident(Long residentId) {
        DoctorResident relation = lambdaQuery()
                .eq(DoctorResident::getResidentId, residentId)
                .one();
        return relation == null ? null : relation.getDoctorId();
    }

    public boolean isResidentInDoctorScope(Long doctorId, Long residentId) {
        return lambdaQuery()
                .eq(DoctorResident::getDoctorId, doctorId)
                .eq(DoctorResident::getResidentId, residentId)
                .exists();
    }

    public List<Long> listResidentIdsOfDoctor(Long doctorId) {
        return lambdaQuery()
                .eq(DoctorResident::getDoctorId, doctorId)
                .list()
                .stream()
                .map(DoctorResident::getResidentId)
                .collect(Collectors.toList());
    }
}
