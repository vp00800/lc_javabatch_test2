package jp.co.yamaha_motor.gimac.le.batch.leaj0001.model;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Leaj0001ParameterModel {

    // Batch Parameter Values
    private String sysOwnerCd;             // システムオーナーコード
    // ワーク変数
    private String pymacDate;              // PYMAC DATE

    // 処理件数カウンタ変数
    public int pymacDateUpdateCount;       //工場処理日更新件数
    public int fixPeriodIdCount;           //確定期間ID検索件数
    public int thisTimeUpdateCount;        //今回確定期間更新件数
    public int latestUpdateCount;          //前回確定期間更新件数
    public int replicaThisTimeUpdateCount; //(レプリカ)今回確定期間更新件数
    public int replicaLatestUpdateCount;   //(レプリカ)前回確定期間更新件数

    // Batch Parameter Values
    private String mrpControlClass;        // MRP管理区分
    private String outputFilePath;         // 工場処理日ファイル

    Timestamp sysDatetime = new Timestamp(System.currentTimeMillis());
    private String userId = "Leaj0001";    // USER ID
}
