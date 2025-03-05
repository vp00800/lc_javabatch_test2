package jp.co.yamaha_motor.gimac.le.batch.leaj0008.model;

import lombok.Data;

@Data
public class Leaj0008OutputFileModel {

    private String parentItemno;
    private String parentSupplier;
    private String parentUsercd;
    private String structureSeq;
    private String compItemno;
    private String compSupplier;
    private String compUsercd;
    private String inEffectiveYmd;
    private String outEffectiveYmd;
    private String compSign;
    private String compQty;
    private String compQtyType;
    private String compOpPercent;
    private String reqIssueControl;
    private String demandPolicyCode;
    private String wbinControlCode;
    private String compItemType;
    private String parentItemType;
    private String parentItemClass;
}


