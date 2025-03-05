package jp.co.yamaha_motor.gimac.le.batch.leaj0002.model;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Leaj0002ParameterModel {

    // ワーク変数
    private String scplanSysOwnerCd;
    private String sysOwnerCd;          // システムオーナーコード

    // Batch Parameter Values
    private String mrpControlClass;		// MRP管理区分
    private String calendarFile;		// 稼動日カレンダーファイル

    // 処理件数カウンタ変数
    private int cntMstCalendarSel;
    private int outputDataCount;

    Timestamp sysDatetime = new Timestamp(System.currentTimeMillis());

}
