package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.entity.RiskRule;
import com.cdvtc.demo.enums.IndicatorType;
import com.cdvtc.demo.enums.MeasurementContext;
import com.cdvtc.demo.mapper.RiskRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则读取。只执行已启用、处于有效期内、且测量场景匹配的规则。
 */
@Service
@RequiredArgsConstructor
public class RiskRuleService extends ServiceImpl<RiskRuleMapper, RiskRule> {

    public List<RiskRule> listActiveRules(IndicatorType type, MeasurementContext context, LocalDateTime at) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRule::getIndicatorType, type.name())
                .eq(RiskRule::getEnabled, 1)
                // 生效时间范围：effective_from <= at <= effective_to，空表示不限制
                .and(q -> q.isNull(RiskRule::getEffectiveFrom).or().le(RiskRule::getEffectiveFrom, at))
                .and(q -> q.isNull(RiskRule::getEffectiveTo).or().ge(RiskRule::getEffectiveTo, at));

        if (context == null) {
            wrapper.isNull(RiskRule::getMeasurementContext);
        } else {
            wrapper.and(q -> q.isNull(RiskRule::getMeasurementContext)
                    .or().eq(RiskRule::getMeasurementContext, context.name()));
        }

        wrapper.orderByDesc(RiskRule::getThresholdValue);
        return list(wrapper);
    }
}
