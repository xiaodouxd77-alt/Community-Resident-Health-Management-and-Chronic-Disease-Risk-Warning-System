package com.cdvtc.demo.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationDetailVO {

    private ConsultationVO consultation;

    private List<ConsultationMessageVO> messages;
}
