package jp.co.yamaha_motor.gimac.le.batch.leaj0001.service;

import com.ymsl.solid.base.util.StringUtils;
import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0001.model.Leaj0001FixPeriodIdModel;
import jp.co.yamaha_motor.gimac.le.batch.leaj0001.model.Leaj0001ParameterModel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * LEAJ0001
 * ＜ 確定期間更新＆工場処理日(PYMAC日)取得 ＞
 *
 *  工場処理日(PYMAC日)の情報を取得し、テキスト化する
 *  確定期間テーブルの今回確定期間のレコードを対象に、確定実施日を工場処理日(PYMAC日)で更新する
 *
 * @author  YMSL R.Mochizuki
 * @version 1.0.0
 *
 *  MODIFICATION HISTORY
 *  (Ver) (Date)     (Name)           (Comment)
 *  1.0.0 2025/03/03 YMSL R.Mochizuki New making
 */

@Service
@Slf4j
public class Leaj0001Service {

    //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Add)
    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataSource dataSource;

    public void start(Map<String, Object> map) throws Exception{
        Leaj0001ParameterModel pm = new Leaj0001ParameterModel();
        setParameter(pm, map);
        prepareParameterAfter(pm);
        init(pm);
        main(pm);
        term(pm);
    }
    //---------------- 2025/02/18 Tao Xiaochuan g3 End(Add)

    /**
     * 引数取得
     */
    protected void setParameter(Leaj0001ParameterModel pm, Map<String, Object> map) throws Exception {

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        if ( "arg1".equals(name) ) {
//            // MRP管理区分
//            pm.setMrpControlClass(value);
//        }
//        if ( "outf".equals(name) ) {
//            // ファイル出力先の取得(工場処理日)
//            pm.setOutputFilePath(value);
//        }
        if ( map.get("arg1") != null ) {
            // MRP管理区分
            pm.setMrpControlClass(map.get("arg1").toString());
        }
        if ( map.get("arg2") != null ) {
            // システムオーナーコード
            pm.setSysOwnerCd(map.get("arg2").toString());
        }
        if ( map.get("out1") != null ) {
            // ファイル出力先の取得(工場処理日)
            pm.setOutputFilePath(map.get("out1").toString());
        }
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

    }

    /**
     * 引数取得後処理
     */
    protected void prepareParameterAfter(Leaj0001ParameterModel pm) throws Exception {

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        // ログ出力用のクラス宣言
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        // 引数必須チェック
//        if ( CheckUtil.isEmptyBlank(pm.getMrpControlClass()) ) {
//            throw new BatchApplicationException("Argument error [arg1:mrpControlClass] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getOutputFilePath()) ) {
//            throw new BatchApplicationException("Argument error [outf:outputFilePath] is blank");
//        }
//
//        // 引数内容ログ出力
//        writer.writeLog("", "[Argument Value]", null, null, null);
//        writer.writeLog("", " sysOwnerCd      : " + pm.getSysOwnerCd(),      null, null, null);
//        writer.writeLog("", " mrpControlClass : " + pm.getMrpControlClass(), null, null, null);
//        writer.writeLog("", " outputFilePath  : " + pm.getOutputFilePath(),  null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0001Service.class);

        // 引数必須チェック
        if ( StringUtils.isBlankText(pm.getMrpControlClass()) ) {
            throw new JobParametersInvalidException("Argument error [arg1:mrpControlClass] is blank");
        }
        if ( StringUtils.isBlankText(pm.getOutputFilePath()) ) {
            throw new JobParametersInvalidException("Argument error [outf:outputFilePath] is blank");
        }

        // 引数内容ログ出力
        logger.info("[Argument Value]");
        logger.info("sysOwnerCd      : {}", pm.getSysOwnerCd());
        logger.info("mrpControlClass : {}", pm.getMrpControlClass());
        logger.info("outputFilePath  : {}", pm.getOutputFilePath());
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

    }

    /**
     * 初期処理
     */
    protected void init(Leaj0001ParameterModel pm) throws Exception {

        // カウンタ変数の初期化
        pm.pymacDateUpdateCount       = 0;
        pm.fixPeriodIdCount           = 0;
        pm.thisTimeUpdateCount        = 0;
        pm.latestUpdateCount          = 0;
        pm.replicaThisTimeUpdateCount = 0;
        pm.replicaLatestUpdateCount   = 0;
    }

    /**
     * 主処理
     */
    protected void main(Leaj0001ParameterModel pm) throws Exception {

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter(); // ログ出力用のクラス宣言
        Logger logger = LoggerFactory.getLogger(Leaj0001Service.class);
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

        Leaj0001FixPeriodIdModel fixPeriodIdModel = new Leaj0001FixPeriodIdModel();

        String pymacDate = new String();        //工場処理日(PYMAC日)

//        //ＰＹＭＡＣ日の取得SQL                                        //2017.04.28 削除
//        String pymacDateSql = " SELECT pymac_date            "
//                            + "   FROM lc_inp_pymac_date     "
//                            + "  WHERE mrp_control_class = ? "
//                            + "    AND sys_owner_cd      = ? "
//                            ;

        //未発行で最小の確定予定日の取得SQL
        String pymacDateSql = " SELECT MIN(a.mrp_plan_date)                       "
                            + "   FROM lc_inp_fix_period       a,                 "
                            + "        lc_inp_fix_period_id    b                  "
                            + "  WHERE a.mrp_control_class = ?                    "
                            + "    AND a.sys_owner_cd      = ?                    "
                            + "    AND a.mrp_running_date  = ' '                  "
                            + "    AND a.mrp_control_class = b.mrp_control_class  "
                            + "    AND a.sys_owner_cd      = b.sys_owner_cd       "
                            + "    AND a.fix_period_id     = b.fix_period_id      "
                            ;

        //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        Object[] pymacDateParams = new Object[]{ pm.getMrpControlClass()
//                , pm.getSysOwnerCd()
//        };
//
//        // Select文の発行
//        Object[] pymacDateObj = dao.getRecord(pymacDateSql, pymacDateParams);
//
//        if (pymacDateObj != null && pymacDateObj[0] != null) {
//
//            pymacDate = (String)pymacDateObj[0];
//
//            //取得したPYMAC DATEをテキストファイルへ出力
//            PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(pm.getOutputFilePath());
//
//            try {
//                fileWriter.println(padCharRight(pymacDate, 8, ' '));
//            } finally {
//                batchUtilityLogic.closePrintWriter(fileWriter);
//            }
//        } else {
//
//            // 該当データが存在しない場合エラーとする
//            String errMsgPymacDate = "PYMAC DATE NOT FOUND -->" + pymacDateSql;
//            throw new BatchApplicationException(errMsgPymacDate);
//        }
        ResultSet result;
        PreparedStatement pstmt = null;
        pstmt = executeSql(pm, pymacDateSql, pymacDate, fixPeriodIdModel, "1");
        result = pstmt.executeQuery();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pm.getOutputFilePath(), false))) {

            while (result.next()) {

                pymacDate = result.getString(1);
                // テキストファイルへ出力
                String writeRec = padCharRight(pymacDate, 8, ' ');

                writer.write(writeRec);
                writer.newLine();
            }
        }
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

        if (StringUtils.isBlankText(pymacDate)) {
            // 該当データが存在しない場合エラーとする
            String errMsgPymacDate = "PYMAC DATE NOT FOUND -->" + pymacDateSql;
            throw new JobParametersInvalidException(errMsgPymacDate);
        }

        // ログメッセージ生成
        String logMessage = "PYMAC DATE     --> " + pymacDate;
        // ログ出力

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        writer.writeLog("", logMessage, null, null, null);
        logger.info(logMessage);
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

        //工場処理日更新
        String pymacDateUpdateSql     = " UPDATE lc_inp_pymac_date                    "
                                      + "    SET pymac_date      = ?                  "
                                      + "      , update_counter  = update_counter + 1 "
                                      + "      , update_datetime = ?                  "
                                      + "      , update_author   = ?                  "
                                      + "      , update_pgmid    = ?                  "
                                      + "  WHERE mrp_control_class = ?                "
                                      + "    AND sys_owner_cd      = ?                "
                                      ;

        //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        Object[] pymacDateUpdateParams = new Object[]{    pymacDate
//                , pm.getSysDatetime()
//                , pm.getUserId()
//                , pm.getUserId()
//                , pm.getMrpControlClass()
//                , pm.getSysOwnerCd()
//        };
//
//        // Update文の発行
//        int resultPymacDateUpdate = dao.setRecord(pymacDateUpdateSql, pymacDateUpdateParams);
        pstmt = executeSql(pm, pymacDateUpdateSql, pymacDate, fixPeriodIdModel, "2");
        int resultPymacDateUpdate = pstmt.executeUpdate();
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

        if (resultPymacDateUpdate != 0) {

            pm.pymacDateUpdateCount += resultPymacDateUpdate;
        }

        //確定期間IDの検索SQL
        String fixPeriodIdSql = " SELECT fix_period_id         "
                + "   FROM lc_inp_fix_period_id  "
                + "  WHERE mrp_control_class = ? "
                + "    AND sys_owner_cd      = ? "
                ;

        //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        Object[] fixPeriodIdParams = new Object[]{ pm.getMrpControlClass()
//                , pm.getSysOwnerCd()
//        };
//
//        // Select文の発行
//        ResultSet fixPeriodIdResultSet = dao.getResult(fixPeriodIdSql, fixPeriodIdParams);

//        while(BatchUtils.hasNext(fixPeriodIdResultSet)){
        pstmt = executeSql(pm, fixPeriodIdSql, pymacDate, fixPeriodIdModel, "3");
        result = pstmt.executeQuery();
        while (result.next()) {
            String fixPeriodId = result.getString(1);
            fixPeriodIdModel.setFixPeriodId(fixPeriodId);

//            fixPeriodIdModel = BatchUtils.convertResultModel(fixPeriodIdResultSet, Lcrj0001FixPeriodIdModel.class);
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

            //更新対象データの存在チェックSQL
            String existCheckSql = " SELECT 1                     "
                    + "   FROM lc_inp_fix_period     "
                    + "  WHERE mrp_control_class = ? "
                    + "    AND sys_owner_cd      = ? "
                    + "    AND fix_period_id     = ? "
                    + "    AND mrp_plan_date     = ? "
                    ;

            //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//            Object[] existCheckParams = new Object[]{ pm.getMrpControlClass()
//                    , pm.getSysOwnerCd()
//                    , fixPeriodIdModel.getFixPeriodId()
//                    , pymacDate
//            };
//
//            // Select文の発行
//            Object[] existCheckObj = dao.getRecord(existCheckSql, existCheckParams);
            pstmt = executeSql(pm, existCheckSql, pymacDate, fixPeriodIdModel, "4");
            result = pstmt.executeQuery();
            String existCheckObj = "";
            while (result.next()) {
                existCheckObj = result.getString(1);
            }
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

            //検索結果0件の場合、次の確定期間IDで存在チェックを行う

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//            if(existCheckObj == null){
            if(StringUtils.isBlankText(existCheckObj)) {
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

            }
            //検索結果が存在した場合、更新処理に続く
            else{

                /* 今回確定期間の更新 (レプリカ) */
                String replicaThisTimeUpdateSql = ""
                        + " UPDATE lc_inp_fix_period    "
                        + "    SET mrp_running_date = ? "
                        + "      , fix_status       = ? "
                        + "      , update_counter   = update_counter + 1 "
                        + "      , update_datetime  = ? "
                        + "      , update_author    = ? "
                        + "      , update_pgmid     = ? "
                        + "   WHERE mrp_control_class = ? "
                        + "     AND sys_owner_cd      = ? "
                        + "     AND fix_period_id     = ? "
                        + "     AND mrp_plan_date     = ? "
                        ;

                //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//                Object[] replicaThisTimeUpdateParams = new Object[]{ pymacDate
//                        , 'T'
//                        , pm.getSysDatetime()
//                        , pm.getUserId()
//                        , pm.getUserId()
//                        , pm.getMrpControlClass()
//                        , pm.getSysOwnerCd()
//                        , fixPeriodIdModel.getFixPeriodId()
//                        , pymacDate
//                };
//                // Update文の発行
//                int resultReplicaThisTimeUpdate = dao.setRecord(replicaThisTimeUpdateSql, replicaThisTimeUpdateParams);
                pstmt = executeSql(pm, replicaThisTimeUpdateSql, pymacDate, fixPeriodIdModel, "5");
                int resultReplicaThisTimeUpdate = pstmt.executeUpdate();
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

                if ( resultReplicaThisTimeUpdate == 0 ) {

                    // 更新件数が0件の場合エラーとする
                    String errMsg = "MRP WORK fix period master update error";

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//                    throw new BatchApplicationException(errMsg);
                    throw new JobParametersInvalidException(errMsg);
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

                }
                else{
                    pm.replicaThisTimeUpdateCount += resultReplicaThisTimeUpdate;
                }

                //引数.MRP管理区分＝"STD"（オーダー発行MRP）の場合更新する
                if (StringUtils.equals(pm.getMrpControlClass(),  "STD")) {

                    /* 今回確定期間の更新 (本体) */
                    String thisTimeUpdateSql = " UPDATE lc_mst_fix_period    "
                            + "    SET mrp_running_date = ? "
                            + "      , fix_status       = ? "
                            + "      , update_counter   = update_counter + 1 "
                            + "      , update_datetime  = ? "
                            + "      , update_author    = ? "
                            + "      , update_pgmid     = ? "
                            + "   WHERE sys_owner_cd    = ? "
                            + "     AND fix_period_id   = ? "
                            + "     AND mrp_plan_date   = ? "
                            ;

                    //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//                    Object[] thisTimeUpdateParams = new Object[]{ pymacDate
//                            , 'T'
//                            , pm.getSysDatetime()
//                            , pm.getUserId()
//                            , pm.getUserId()
//                            , pm.getSysOwnerCd()
//                            , fixPeriodIdModel.getFixPeriodId()
//                            , pymacDate
//                    };
//                    // Update文の発行
//                    int resultThisTimeUpdate = dao.setRecord(thisTimeUpdateSql, thisTimeUpdateParams);
                    pstmt = executeSql(pm, thisTimeUpdateSql, pymacDate, fixPeriodIdModel, "6");
                    int resultThisTimeUpdate = pstmt.executeUpdate();
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

                    if ( resultThisTimeUpdate == 0 ) {

                        // 更新件数が0件の場合エラーとする
                        String errMsg = "MRP fix period master update error";

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//                        throw new BatchApplicationException(errMsg);
                        throw new JobParametersInvalidException(errMsg);
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

                    }
                    else{
                        pm.thisTimeUpdateCount += resultThisTimeUpdate;
                    }
                }

                /* 前回確定期間の更新 (レプリカ) */
                String replicaLastUpdateSql = ""
                        + " UPDATE lc_inp_fix_period    "
                        + "    SET fix_status       = ? "
                        + "      , update_counter   = update_counter + 1 "
                        + "      , update_datetime  = ? "
                        + "      , update_author    = ? "
                        + "      , update_pgmid     = ? "
                        + "   WHERE mrp_control_class = ? "
                        + "     AND sys_owner_cd      = ? "
                        + "     AND fix_period_id     = ? "
                        + "     AND mrp_plan_date    <> ? "
                        + "     AND fix_status        = ? "
                        ;

                //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//                Object[] replicaLastUpdateParams
//                        = new Object[]{ 'L'
//                        , pm.getSysDatetime()
//                        , pm.getUserId()
//                        , pm.getUserId()
//                        , pm.getMrpControlClass()
//                        , pm.getSysOwnerCd()
//                        , fixPeriodIdModel.getFixPeriodId()
//                        , pymacDate
//                        , 'T'
//                };
//                // Update文の発行
//                int resultReplicaLastUpdate = dao.setRecord(replicaLastUpdateSql, replicaLastUpdateParams);
                pstmt = executeSql(pm, replicaLastUpdateSql, pymacDate, fixPeriodIdModel, "7");
                int resultReplicaLastUpdate = pstmt.executeUpdate();
    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)

                pm.replicaLatestUpdateCount += resultReplicaLastUpdate;

                //引数.MRP管理区分＝"STD"（オーダー発行MRP）の場合更新する
                if (StringUtils.equals(pm.getMrpControlClass(),  "STD")) {

                    /* 前回確定期間の更新 (本体) */
                    String lastUpdateSql = " UPDATE lc_mst_fix_period    "
                            + "    SET fix_status       = ? "
                            + "      , update_counter   = update_counter + 1 "
                            + "      , update_datetime  = ? "
                            + "      , update_author    = ? "
                            + "      , update_pgmid     = ? "
                            + "   WHERE sys_owner_cd    = ? "
                            + "     AND fix_period_id   = ? "
                            + "     AND mrp_plan_date  <> ? "
                            + "     AND fix_status      = ? "
                            ;

                    //SQLパラメータの生成

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//                    Object[] lastUpdateParams = new Object[]{ 'L'
//                            , pm.getSysDatetime()
//                            , pm.getUserId()
//                            , pm.getUserId()
//                            , pm.getSysOwnerCd()
//                            , fixPeriodIdModel.getFixPeriodId()
//                            , pymacDate
//                            , 'T'
//                    };
//                    // Update文の発行
//                    int resultLastUpdate = dao.setRecord(lastUpdateSql, lastUpdateParams);
                    pstmt = executeSql(pm, lastUpdateSql, pymacDate, fixPeriodIdModel, "8");
                    int resultLastUpdate = pstmt.executeUpdate();
    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)

                    pm.latestUpdateCount += resultLastUpdate;
                }
            }
            pm.fixPeriodIdCount++;
        }
    }

    /**
     * 後処理
     */
    protected void term(Leaj0001ParameterModel pm) throws Exception {

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        // 処理結果ログ出力
//        writer.writeLog("", "[lc_inp_pymac_date]            Update Count  = " + pm.pymacDateUpdateCount,       null, null, null);
//        writer.writeLog("", "[fix_period_id]                Select Count  = " + pm.fixPeriodIdCount,           null, null, null);
//        writer.writeLog("", "[lc_mst_fix_period] This Time  Update Count  = " + pm.thisTimeUpdateCount,        null, null, null);
//        writer.writeLog("", "[lc_mst_fix_period] Latest     Update Count  = " + pm.latestUpdateCount,          null, null, null);
//        writer.writeLog("", "[lc_inp_fix_period] This Time  Update Count  = " + pm.replicaThisTimeUpdateCount, null, null, null);
//        writer.writeLog("", "[lc_inp_fix_period] Latest     Update Count  = " + pm.replicaLatestUpdateCount,   null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0001Service.class);

        logger.info("[lc_inp_pymac_date]            Update Count  = {}", pm.getPymacDateUpdateCount());
        logger.info("[fix_period_id]                Select Count  = {}", pm.getFixPeriodIdCount());
        logger.info("[lc_mst_fix_period] This Time  Update Count  = {}", pm.getThisTimeUpdateCount());
        logger.info("[lc_mst_fix_period] Latest     Update Count  = {}", pm.getLatestUpdateCount());
        logger.info("[lc_inp_fix_period] This Time  Update Count  = {}", pm.getReplicaThisTimeUpdateCount());
        logger.info("[lc_inp_fix_period] Latest     Update Count  = {}", pm.getReplicaLatestUpdateCount());
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

    }

    /**
     * 文字列右埋め
     * StringUtils.padCharRight()を少し変更してある。(Null,ブランクの場合でも文字埋めされるように)
     * <br> Pads to the right of a String with specified character.
     *
     * @param str     (toPad     - the String to be padded)
     * @param len     (padAmount - the amount to pad)
     * @param padChar (delimiter - character to pad the String with)
     * @return the padded string
     */
    public String padCharRight(String str, int len, char padChar) {
        if(str == null) str = "";
        if(str.length() >= len)
            return str;
        StringBuffer sb = new StringBuffer(str);
        int max = len - str.length();
        for(int i = 0; i < max; i++)
        {
            str = (new StringBuilder()).append(padChar).append(str).toString();
            sb.append(padChar);
        }

        return sb.toString();
    }

    //---------------- 2025/03/03 R.Mochizuki g3 Start(Modify)
    public PreparedStatement executeSql(Leaj0001ParameterModel pm, String sql, String pymacDate, Leaj0001FixPeriodIdModel fixPeriodIdModel, String sqlClass) throws Exception {
        Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = null;
        ResultSet result = null;

        try {
            pstmt = conn.prepareStatement(sql);
            if ("1".equals(sqlClass) || "3".equals(sqlClass)) {
                pstmt.setString(1, (String) pm.getMrpControlClass());
                pstmt.setString(2, (String) pm.getSysOwnerCd());
            } else if ("2".equals(sqlClass)) {
                pstmt.setString(1, (String) pymacDate);
                pstmt.setTimestamp(2, pm.getSysDatetime());
                pstmt.setString(3, (String) pm.getUserId());
                pstmt.setString(4, (String) pm.getUserId());
                pstmt.setString(5, (String) pm.getMrpControlClass());
                pstmt.setString(6, (String) pm.getSysOwnerCd());
            } else if ("4".equals(sqlClass)) {
                pstmt.setString(1, (String) pm.getMrpControlClass());
                pstmt.setString(2, (String) pm.getSysOwnerCd());
                pstmt.setString(3, (String) fixPeriodIdModel.getFixPeriodId());
                pstmt.setString(4, (String) pymacDate);
            } else if ("5".equals(sqlClass)) {
                pstmt.setString(1, (String) pymacDate);
                pstmt.setString(2, (String) "T");
                pstmt.setTimestamp(3, pm.getSysDatetime());
                pstmt.setString(4, (String) pm.getUserId());
                pstmt.setString(5, (String) pm.getUserId());
                pstmt.setString(6, (String) pm.getMrpControlClass());
                pstmt.setString(7, (String) pm.getSysOwnerCd());
                pstmt.setString(8, (String) fixPeriodIdModel.getFixPeriodId());
                pstmt.setString(9, (String) pymacDate);
            } else if ("6".equals(sqlClass)) {
                pstmt.setString(1, (String) pymacDate);
                pstmt.setString(2, (String) "T");
                pstmt.setTimestamp(3, pm.getSysDatetime());
                pstmt.setString(4, (String) pm.getUserId());
                pstmt.setString(5, (String) pm.getUserId());
                pstmt.setString(6, (String) pm.getSysOwnerCd());
                pstmt.setString(7, (String) fixPeriodIdModel.getFixPeriodId());
                pstmt.setString(8, (String) pymacDate);
            } else if ("7".equals(sqlClass)) {
                pstmt.setString(1, (String) "L");
                pstmt.setTimestamp(2, pm.getSysDatetime());
                pstmt.setString(3, (String) pm.getUserId());
                pstmt.setString(4, (String) pm.getUserId());
                pstmt.setString(5, (String) pm.getMrpControlClass());
                pstmt.setString(6, (String) pm.getSysOwnerCd());
                pstmt.setString(7, (String) fixPeriodIdModel.getFixPeriodId());
                pstmt.setString(8, (String) pymacDate);
                pstmt.setString(9, (String) "T");
            } else if ("8".equals(sqlClass)) {
                pstmt.setString(1, (String) "L");
                pstmt.setTimestamp(2, pm.getSysDatetime());
                pstmt.setString(3, (String) pm.getUserId());
                pstmt.setString(4, (String) pm.getUserId());
                pstmt.setString(5, (String) pm.getSysOwnerCd());
                pstmt.setString(6, (String) fixPeriodIdModel.getFixPeriodId());
                pstmt.setString(7, (String) pymacDate);
                pstmt.setString(8, (String) "T");
            }

            return pstmt;
        } catch (Exception e) {
            throw e;
        }
    }
    //---------------- 2025/03/03 R.Mochizuki g3 End(Modify)

}
