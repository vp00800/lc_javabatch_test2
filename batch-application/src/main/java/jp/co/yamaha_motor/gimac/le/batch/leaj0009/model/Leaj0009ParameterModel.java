package jp.co.yamaha_motor.gimac.le.batch.leaj0009.model;

import lombok.Data;

@Data
public class Leaj0009ParameterModel {

    // Batch Parameter Values
    private String sysOwnerCd;             // システムオーナーコード
    // Batch Parameter Area
    private String mrpControlClass;		// MRP管理区分
    private String fullPegItemFile;		// フルペグ対象品目ファイル

    // Work Area
    private String scplanSysOwnerCd;
    private int fullPegItemFileCount;

}
