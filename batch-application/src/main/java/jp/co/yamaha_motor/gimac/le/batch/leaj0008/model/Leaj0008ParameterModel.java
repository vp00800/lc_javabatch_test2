package jp.co.yamaha_motor.gimac.le.batch.leaj0008.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Leaj0008ParameterModel {

    // Batch Parameter Values
    private String sysOwnerCd;          // システムオーナーコード
    private String mrpControlClass;     // MRP管理区分
    private String prodStrcFile;        // 構成マスターファイル

    // ワーク変数
    private String     scplanSysOwnerCd;//SCPLANシステムオーナーコード
    private String     reqIssueControl; //所要量出庫/管理コード
    private BigDecimal compQty;         //員数
    private String     compOpPercent;   //OP率
    // 処理件数カウンタ変数
    private int    structureMastCount;
}
