package jp.co.yamaha_motor.gimac.le.batch.leaj0004.model;

import lombok.Data;

@Data
public class Leaj0004ParameterModel {

    // Batch Parameter Values
    private String sysOwnerCd;             // システムオーナーコード
    // ワーク変数
    private String scplanSysOwnerCd;

    // Batch Parameter Values
    private String mrpControlClass;	 // MRP管理区分
    private String delivStdDayFile;	 // 納入基準日ファイル

    // 処理件数カウンタ変数
    private int    outputDataCount;

}
