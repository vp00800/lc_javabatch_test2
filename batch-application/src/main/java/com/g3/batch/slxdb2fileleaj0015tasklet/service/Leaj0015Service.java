package com.g3.batch.slxdb2fileleaj0015tasklet.service;

import com.g3.batch.slxdb2fileleaj0015tasklet.model.*;
import com.ymsl.solid.base.util.DateUtils;
import com.ymsl.solid.base.util.StringUtils;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leaj0015
 *＜ 発注残独立所要量作成 ＞
 *
 * 独立所要量明細から全情報を抽出し、
 * 発注取消対象独立所要量、履歴移動対象独立所要量を除外したSCPLAN対象独立所要量を作成する
 * なお、発注取消対象独立所要量は、発注取消（独立所要量）に、
 * 履歴移動対象独立所要量は、独立所要量明細履歴へそれぞれ出力する。
 *
 * @author  YMSLX.ChenDeChun
 * @version 1.0.0
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 *
 * MODIFICATION HISTORY
 *  (Ver)  (Date)     (Name)        (Comment)
 *  1.0.0  2012/07/16 YMSLX.ChenDeChun   New making
 */

@Service
@Slf4j
public class Leaj0015Service{

    // データデリミタ
    private static final String DELIMITER = "|";

    @Inject
    private JdbcClient jdbcClient;
    
    public void startLeaj0015Tasklet(Map<String, Object> map) throws Exception {
		Leaj0015ParameterModel pm = new Leaj0015ParameterModel();
		setParameter(pm, map);
		prepareParameterAfter(pm);
        init(pm);
        main(pm);
        term(pm);
    }

	protected void setParameter(Leaj0015ParameterModel pm, Map<String, Object> map) {

		if (map.get("arg0") != null) {
			// sysOwnerCd
			pm.setSysOwnerCd(map.get("arg0").toString());
		}
		if (map.get("arg1") != null) {
			// MRP管理区分
			pm.setMrpControlClass(map.get("arg1").toString());
		}
		if (map.get("out1") != null) {
			// 発注取消（独立所要量）ファイル
			pm.setCancelIrd(map.get("out1").toString());
		}
		if (map.get("out2") != null) {
			// 独立所要量明細履歴ファイル
			pm.setIrdHistory(map.get("out2").toString());
		}
		if (map.get("out3") != null) {
			// SCPLAN-IN独立所要量ファイル
			pm.setScplanIrd(map.get("out3").toString());
		}
		if (map.get("out4") != null) {
			// SCPLAN BYPASS 独立所要量明細ファイル
			pm.setBypassIrd(map.get("out4").toString());
		}
	}

    public void prepareParameterAfter(Leaj0015ParameterModel pm) throws Exception {

        if ( StringUtils.isEmpty(pm.getMrpControlClass()) ) {
            String msg = "Argument error [arg1:mrpControlClass] is blank";
            throw new JobParametersInvalidException(msg);
        }
        if ( StringUtils.isEmpty(pm.getCancelIrd()) ) {
            String msg = "Argument error [out1:cancelIrd] is blank";
            throw new JobParametersInvalidException(msg);
        }
        if ( StringUtils.isEmpty(pm.getIrdHistory()) ) {
            String msg = "Argument error [out2:irdHistory] is blank";
            throw new JobParametersInvalidException(msg);
        }
        if ( StringUtils.isEmpty(pm.getScplanIrd()) ) {
            String msg = "Argument error [out3:scplanIrd] is blank";
            throw new JobParametersInvalidException(msg);
        }
        if ( StringUtils.isEmpty(pm.getBypassIrd()) ) {
            String msg = "Argument error [out4:bypassIrd] is blank";
            throw new JobParametersInvalidException(msg);
        }
        log.info("[Argument Value]");
        log.info(" sysOwnerCd      : " + pm.getSysOwnerCd());
        log.info(" mrpControlClass : " + pm.getMrpControlClass());
        log.info(" cancelIrd       : " + pm.getCancelIrd());
        log.info(" irdHistory      : " + pm.getIrdHistory());
        log.info(" scplanIrd       : " + pm.getScplanIrd());
        log.info(" bypassIrd       : " + pm.getBypassIrd());
    }

    /**
     * 初期処理
     */
    protected void init(Leaj0015ParameterModel pm) throws Exception {

        // MRPシステムパラメータの発注削除日数、完了保有日数を取得
        getLcSystemParameterModel(pm);

        // 工場処理日(PYMAC日)取得
        getPymacDateFromLzPymacDate(pm);
        checkExistOfPymacDate(pm);

        // 標準カレンダーコード取得
        getCalendarCodeFromLcMstCalendarCode(pm);
        checkExistOfCalendarCode(pm);

        // 独立所要量発注削除日計算 ( 工場処理日(PYMAC日) - 発注削除日数 = 発注取消基準日 ) 稼働日ベース
        getOrderDeleteDateFromSp(pm);

        // 独立所要量明細履歴移動日計算 ( 工場処理日(PYMAC日) - 完了保有日数 = 履歴移動基準日 ) 暦日ベース
        pm.setHistoryMoveDate(DateUtils.addDate(-pm.getCompleteHoldingDays(), pm.getPymacDate()));

        // 先頭稼動シフトを取得  2017-09-13 ADD
        getMinValidShift(pm);

        // 取得した値をログ出力
        log.info("pymacDate       : " + pm.getPymacDate());
        log.info("calendarCode    : " + pm.getCalendarCode());
        log.info("orderDeleteDate : " + pm.getOrderDeleteDate());
        log.info("historyMoveDate : " + pm.getHistoryMoveDate());
    }

    /**
     * 主処理
     */
    protected void main(Leaj0015ParameterModel pm) throws Exception {

        // 発注取消（独立所要量）ファイル出力 --> out1:
        List<Leaj0015LcTrnIrdModel> lcTrnIrdSet = getLcTrnIrdSet(pm);
        lcTrnIrdTxtFileOutputProcess(pm.getCancelIrd(), pm, lcTrnIrdSet);

        // 独立所要量履歴ファイル出力         --> out2:
        List<Leaj0015LcTrnIrdHisModel> lcTrnIrdHisSet = getLcTrnIrdHisSet(pm);
        lcTrnIrdHisTxtFileOutputProcess(pm.getIrdHistory(), pm, lcTrnIrdHisSet);

        // SCPLAN-IN 独立所要量ファイル出力   --> out3:
        List<Leaj0015ScplanInLcTrnIrdModel> scplanInLcTrnIrdSet = getScplanInLcTrnIrdSet(pm);
        scplanInLcTrnIrdTxtFileOutputProcess(pm.getScplanIrd(), pm, scplanInLcTrnIrdSet);

        // 独立所要量バイパスファイル出力     --> out4:
        List<Leaj0015LcWorkBypassIrdModel> lcWorkBypassIrdSet = getLcWorkBypassIrdSet(pm);
        lcWorkBypassIrdTxtFileOutputProcess(pm.getBypassIrd(), pm, lcWorkBypassIrdSet);
    }

    /**
     * 後処理
     */
    protected void term(Leaj0015ParameterModel pm) throws Exception {

        // 処理件数ログ出力
        log.info("[IRD Cancel]      Output Count = " + pm.getLcTrnIrdTxtFileOutputCount());
        log.info("[IRD HistoryMove] Output Count = " + pm.getLcTrnIrdHisTxtFileOutputCount());
        log.info("[IRD SCPLAN-IN]   Output Count = " + pm.getScplanInLcTrnIrdOutputCount());
        log.info("[IRD BYPASS]      Output Count = " + pm.getLcWorkBypassIrdOutputCount());
    }

    /**
     * MRPシステムパラメータ検索
     */
    private void getLcSystemParameterModel(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        //MRPシステムパラメータの取得SQL
        sql += " SELECT order_delete_days     "; // 発注削除日数
        sql += "       ,complete_holding_days "; // 完了保有日数
        sql += "       ,scplan_sys_owner_cd   "; // SCPLAN用システムパラメータ
        sql += "   FROM lc_system_parameter   ";
        sql += "  WHERE sys_owner_cd = ?      ";
        sql += "    AND system_code  = 'LC'   ";

        // Select文の発行
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getSysOwnerCd());

        List<Leaj0015ParameterModel> results = statementSpec.query((rs, rowNum) -> {
            Leaj0015ParameterModel results1 = new Leaj0015ParameterModel();
            results1.setOrderDeteleDays(rs.getInt("order_delete_days"));
            results1.setCompleteHoldingDays(rs.getInt("complete_holding_days"));
            results1.setScplanSysOwnerCd(rs.getString("scplan_sys_owner_cd"));
            return results1;
        }).list();

        if(results.isEmpty()){
            //該当データが存在しない場合エラーとする
            String msg = "[lc_system_parameter] No Data Found Error."
                       + "[sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
            throw new JobParametersInvalidException(msg);
        }else{
            Leaj0015ParameterModel model = results.get(0);
            pm.setOrderDeteleDays(model.getOrderDeteleDays());
            pm.setCompleteHoldingDays(model.getCompleteHoldingDays());
            pm.setScplanSysOwnerCd(model.getScplanSysOwnerCd());
        }
    }

    /**
     * 工場処理日(PYMAC日)取得
     */
    private void getPymacDateFromLzPymacDate(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        //ＰＹＭＡＣ日の取得SQL
        sql += " SELECT pymac_date            ";
        sql += "   FROM lc_inp_pymac_date     ";
        sql += "  WHERE mrp_control_class = ? ";
        sql += "    AND sys_owner_cd      = ? ";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass(), pm.getSysOwnerCd());

        // Select文の発行
        List<Leaj0015ParameterModel> results = statementSpec.query((rs, rowNum) -> {
            Leaj0015ParameterModel results1 = new Leaj0015ParameterModel();
            results1.setPymacDate(rs.getString("pymac_date"));
            return results1;
        }).list();

        if(!results.isEmpty()){
            Leaj0015ParameterModel model = results.get(0);
            pm.setPymacDate(StringUtils.toString(model.getPymacDate()));
        }
    }

    /**
     * 工場処理日(PYMAC日)の存在チェック
     */
    private void checkExistOfPymacDate(Leaj0015ParameterModel pm) throws Exception {

        if (StringUtils.isEmpty(pm.getPymacDate())) {
            //該当データが存在しない場合エラーとする
            String msg = "[lc_inp_pymac_date] No Data Found Error."
                       + " [mrp_control_class:'" + pm.getMrpControlClass() + "']"
                       + " [sys_owner_cd:'"      + pm.getSysOwnerCd()      + "']";
            throw new JobParametersInvalidException(msg);
        }
    }

    /**
     * 標準カレンダーコード取得
     */
    private void getCalendarCodeFromLcMstCalendarCode(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        //カレンダーコードの取得SQL
        sql += " SELECT calendar_code            ";
        sql += "   FROM lc_inp_calendar_code     ";
        sql += "  WHERE mrp_control_class  = ?   ";
        sql += "    AND sys_owner_cd       = ?   ";
        sql += "    AND std_calendar_class = '1' ";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass(), pm.getSysOwnerCd());

        // Select文の発行
        List<Leaj0015ParameterModel> results = statementSpec.query((rs, rowNum) -> {
            Leaj0015ParameterModel results1 = new Leaj0015ParameterModel();
            results1.setCalendarCode(rs.getString("calendar_code"));
            return results1;
        }).list();

        if(!results.isEmpty()){
            Leaj0015ParameterModel model = results.get(0);
            pm.setCalendarCode(StringUtils.toString(model.getCalendarCode()));
        }
    }

    /**
     * 標準カレンダーコードの存在チェック
     */
    private void checkExistOfCalendarCode(Leaj0015ParameterModel pm) throws Exception {

        if (StringUtils.isEmpty(pm.getPymacDate())) {

            //該当データが存在しない場合エラーとする
            String msg    = "[lc_inp_calendar_code] No Data Found Error."
                    + " [mrp_control_class:'" + pm.getMrpControlClass() + "']"
                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";

            throw new JobParametersInvalidException(msg);
        }
    }

    /**
     * 独立所要量発注削除日計算
     * ( 工場処理日(PYMAC日) - 発注削除日数 = 発注取消基準日 ) 稼働日ベース
     */
    private void getOrderDeleteDateFromSp(Leaj0015ParameterModel pm) throws Exception {

        String sql;

        //稼働日計算（レプリカ）CALL
        sql = " select * from LCYS0008(?,?,?,?,?)";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql)
                .params(pm.getMrpControlClass()
                        , pm.getSysOwnerCd()
                        , pm.getCalendarCode()
                        , pm.getPymacDate()
                        , (-1) * pm.getOrderDeteleDays()
                );

        // Select文の発行
        List<Leaj0015SpModel> results = statementSpec.query((rs, rowNum) -> {
            Leaj0015SpModel results1 = new Leaj0015SpModel();
            results1.setRsTargetDate(rs.getString("rs_target_date"));
            return results1;
        }).list();

        if(!results.isEmpty()){
            pm.setOrderDeleteDate(results.get(0).getRsTargetDate());
        }
    }

    /**
     * 初期処理／先頭稼動シフト取得、Map作成
     */
    private void getMinValidShift(Leaj0015ParameterModel pm) throws Exception {

        HashMap<String, String> map    = new HashMap<String, String>() ;
        StringBuilder key             = new StringBuilder();

        String sql = "";

        //検索SQL
        sql += " SELECT   time.times_divide_day                                                                ";
        sql += "         ,time.manf_delv_type                                                                  ";
        sql += "         ,time.usercd                                                                          ";
        sql += "         ,time.shift_box_no                                                                    ";
        sql += " FROM     lc_inp_shift_divide_pattern    sft                                                   ";
        sql += "         ,lc_inp_divide_time             time                                                  ";

        sql += " WHERE   sft.mrp_control_class   = ?                                                           ";
        sql += " AND     sft.sys_owner_cd        = ?                                                           ";
        sql += " AND     sft.std_shift_flg       = '0'                                                         ";

        sql += " AND     sft.mrp_control_class   = time.mrp_control_class                                      ";
        sql += " AND     sft.sys_owner_cd        = time.sys_owner_cd                                           ";
        sql += " AND     sft.times_divide_day    = time.times_divide_day                                       ";
        sql += " AND     sft.manf_delv_type      = time.manf_delv_type                                         ";
        sql += " AND     sft.usercd              = time.usercd                                                 ";

        sql += " AND     time.shift_box_no       = (SELECT   MIN(time2.shift_box_no)                           ";
        sql += "                                    FROM     lc_inp_divide_time      time2                     ";
        sql += "                                    WHERE    time2.mrp_control_class = sft.mrp_control_class   ";
        sql += "                                    AND      time2.sys_owner_cd      = sft.sys_owner_cd        ";
        sql += "                                    AND      time2.times_divide_day  = sft.times_divide_day    ";
        sql += "                                    AND      time2.manf_delv_type    = sft.manf_delv_type      ";
        sql += "                                    AND      time2.usercd            = sft.usercd              ";
        sql += "                                    AND      time2.valid_flg         = '1')                    ";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass(), pm.getSysOwnerCd());

        // Select文の発行
        List<HashMap<String, String>> results = statementSpec.query(new RowMapper<HashMap<String, String>>() {
            HashMap<String, String> rsMap = new HashMap<String, String>() ;
            @Override
            public HashMap<String, String> mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                StringBuilder results = new StringBuilder();
                results.setLength(0);
                results.append(rs.getString("times_divide_day"));// 時刻指定回数
                results.append(DELIMITER);
                results.append(rs.getString("manf_delv_type"));// 製造／納入種類
                results.append(DELIMITER);
                results.append(rs.getString("usercd")); // 使用者
                rsMap.put(results.toString(), rs.getString("shift_box_no"));
                return rsMap;
            }
        }).list();

        if(!results.isEmpty()){
            map = results.get(0);
        }

        pm.setMinValidShiftMap(map);
    }

    /**
     * 発注取消（独立所要量） データ取得
     */
    private List<Leaj0015LcTrnIrdModel> getLcTrnIrdSet(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        // 独立所要量明細の検索SQL
        sql += " SELECT itemno                            ";
        sql += "       ,supplier                          ";
        sql += "       ,usercd                            ";
        sql += "       ,order_no                          ";
        sql += "       ,start_date                        ";
        sql += "       ,ind_user_class                    ";
        sql += "       ,ind_user_code                     ";
        sql += "       ,required_qty                      ";
        sql += "       ,ship_qty                          ";
        sql += "       ,transfer_class                    ";
        sql += "       ,transfer_code                     ";
        sql += "       ,account_heading                   ";
        sql += "       ,account_detail                    ";
        sql += "       ,budget_no                         ";
        sql += "       ,create_author                     ";
        sql += "       ,reason_code                       ";
        sql += "       ,create_datetime                   ";
        sql += "       ,update_datetime                   ";
        sql += "       ,mrp_date                          ";
        sql += "  FROM lc_inp_ird                         "; // (レプリカ)独立所要量明細ワーク
        sql += " WHERE mrp_control_class = ?              ";
        sql += "   AND sys_owner_cd      = ?              ";
        sql += "   AND order_status      <> '9'           "; // 未完了
        sql += "   AND start_date        < ?              "; // 発注取消対象
        sql += "   AND (pilot_class      = '4'            "; // 4:単品生試
        sql += "    OR  rd_class        in ('0','5'))     ";
        sql += " ORDER BY 1,2,3,4,5                       ";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass(), pm.getSysOwnerCd(), pm.getOrderDeleteDate());

        // Select文の発行
        List<Leaj0015LcTrnIrdModel> results = statementSpec.query((rs, rowNum) -> {
            try {
                Leaj0015LcTrnIrdModel model = convertResultLcTrnIrdModel(rs);
                return model;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).list();

        if(!results.isEmpty()){
            return results;
        }else{
            return new ArrayList<Leaj0015LcTrnIrdModel>();
        }
    }

    /**
     * 発注取消（独立所要量）ファイル出力 --> out1:
     */
    private void lcTrnIrdTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, List<Leaj0015LcTrnIrdModel> result) throws Exception {

        StringBuilder writeRec = new StringBuilder();
        SimpleDateFormat insdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat outsdf = new SimpleDateFormat("yyyyMMdd");

        String indUser     = "";
        String transfer    = "";
        String requiredQty = "";
        String shipQty     = "";
        String createDate  = "";
        String updateDate  = "";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
            for (Leaj0015LcTrnIrdModel model : result) {

                requiredQty = model.getRequiredQty();
                shipQty = model.getShipQty();

                if (Double.parseDouble(requiredQty) >= 0) {
                    requiredQty = "+" + requiredQty;
                }

                if (Double.parseDouble(shipQty) >= 0) {
                    shipQty = "+" + shipQty;
                }

                indUser = model.getIndUserClass() + model.getIndUserCode();
                transfer = model.getTransferClass() + model.getTransferCode();

                createDate = outsdf.format(insdf.parse(model.getCreateDatetime()));
                updateDate = outsdf.format(insdf.parse(model.getUpdateDatetime()));

                writeRec.setLength(0);

                // テキストファイルへ出力
                writeRec.append(pm.getSysOwnerCd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getItemno());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSupplier());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUsercd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getStartDate());
                writeRec.append(DELIMITER);
                writeRec.append(indUser);
                writeRec.append(DELIMITER);
                writeRec.append(requiredQty);
                writeRec.append(DELIMITER);
                writeRec.append(shipQty);
                writeRec.append(DELIMITER);
                writeRec.append(transfer);
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountHeading());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountDetail());
                writeRec.append(DELIMITER);
                writeRec.append(model.getBudgetNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getCreateAuthor());
                writeRec.append(DELIMITER);
                writeRec.append(model.getReasonCode());
                writeRec.append(DELIMITER);
                writeRec.append(createDate);
                writeRec.append(DELIMITER);
                writeRec.append(updateDate);
                writeRec.append(DELIMITER);
                writeRec.append(model.getMrpDate());

                writer.write(writeRec.toString());
                writer.newLine();
                pm.setLcTrnIrdTxtFileOutputCount(pm.getLcTrnIrdTxtFileOutputCount() + 1);
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing to file: " + outputFile, e);
        }
    }

    /**
     * LcTrnIrdModelの返却値をセットする
     */
    public Leaj0015LcTrnIrdModel convertResultLcTrnIrdModel(ResultSet result) throws Exception{

        Leaj0015LcTrnIrdModel model = new Leaj0015LcTrnIrdModel();

        model.setItemno(result.getString(1));
        model.setSupplier(result.getString(2));
        model.setUsercd(result.getString(3));
        model.setOrderNo(result.getString(4));
        model.setStartDate(result.getString(5));
        model.setIndUserClass(result.getString(6));
        model.setIndUserCode(result.getString(7));
        model.setRequiredQty(result.getString(8));
        model.setShipQty(result.getString(9));
        model.setTransferClass(result.getString(10));
        model.setTransferCode(result.getString(11));
        model.setAccountHeading(result.getString(12));
        model.setAccountDetail(result.getString(13));
        model.setBudgetNo(result.getString(14));
        model.setCreateAuthor(result.getString(15));
        model.setReasonCode(result.getString(16));
        model.setCreateDatetime(result.getString(17));
        model.setUpdateDatetime(result.getString(18));
        model.setMrpDate(result.getString(19));

        return model;
    }

    /**
     * 独立所要量履歴 データ取得
     */
    private List<Leaj0015LcTrnIrdHisModel> getLcTrnIrdHisSet(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        // 独立所要量明細の検索SQL
        sql += " SELECT sys_owner_cd                            ";
        sql += "       ,itemno                                  ";
        sql += "       ,supplier                                ";
        sql += "       ,usercd                                  ";
        sql += "       ,order_no                                ";
        sql += "       ,start_date                              ";
        sql += "       ,order_through_no                        "; // 2017-09-13 MOD
        sql += "       ,order_through_no_source_flg             "; // 2017-09-13 MOD
        sql += "       ,rd_class                                ";
        sql += "       ,ind_user_class                          ";
        sql += "       ,ind_user_code                           ";
        sql += "       ,rls_start_date                          ";
        sql += "       ,order_status                            ";
        sql += "       ,reason_code                             ";
        sql += "       ,fixed_ymd                               ";
        sql += "       ,pilot_class                             ";
        sql += "       ,pilot_condition_type                    ";
        sql += "       ,required_qty                            ";
        sql += "       ,remark                                  ";
        sql += "       ,request_system_code                     ";
        sql += "       ,group_receive_flg                       ";
        sql += "       ,ship_qty                                ";
        sql += "       ,bo_qty                                  ";
        sql += "       ,delivery_card_status                    ";
        sql += "       ,item_card_status                        ";
        sql += "       ,ship_date                               ";
        sql += "       ,transfer_class                          ";
        sql += "       ,transfer_code                           ";
        sql += "       ,transfer_reason_code                    ";
        sql += "       ,account_heading                         ";
        sql += "       ,account_detail                          ";
        sql += "       ,budget_no                               ";
        sql += "       ,account_code_sales                      ";
        sql += "       ,sp_order_class                          ";
        sql += "       ,sp_delivery_code                        ";
        sql += "       ,sp_dealer_no                            ";
        sql += "       ,sp_order_no                             ";
        sql += "       ,operation_no                            ";
        sql += "       ,operation_seq                           ";
        sql += "       ,batch_status                            ";
        sql += "       ,mrp_date                                ";
        sql += "       ,delete_ymd                              ";
        sql += "       ,register_user_name                      ";
        sql += "       ,update_counter                          ";
        sql += "       ,create_datetime                         ";
        sql += "       ,create_author                           ";
        sql += "       ,update_datetime                         ";
        sql += "       ,update_author                           ";
        sql += "       ,update_pgmid                            ";
        sql += "  FROM lc_inp_ird                               ";// (レプリカ)独立所要量明細ワーク
        sql += " WHERE mrp_control_class = ?                    ";
        sql += "   AND sys_owner_cd      = ?                    ";
        sql += "   AND order_status      = '9'                  "; // 9:完了
        sql += "   AND TO_CHAR(update_datetime, 'YYYYMMDD') < ? "; // 履歴移動対象
        sql += " ORDER BY 1,2,3,4,5,6                           ";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass(), pm.getSysOwnerCd(), pm.getHistoryMoveDate());

        // Select文の発行
        List<Leaj0015LcTrnIrdHisModel> results = statementSpec.query((rs, rowNum) -> {
            try {
                Leaj0015LcTrnIrdHisModel model = convertResultLcTrnIrdHisModel(rs);
                return model;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).list();

        if(!results.isEmpty()){
            return results;
        }else{
            return new ArrayList<Leaj0015LcTrnIrdHisModel>();
        }
    }

    /**
     * 独立所要量履歴ファイル出力--> out2:
     */
    private void lcTrnIrdHisTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, List<Leaj0015LcTrnIrdHisModel> result) throws Exception {


        StringBuilder writeRec = new StringBuilder();

        String requiredQty    = "";
        String shipQty        = "";
        String boQty          = "";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {

            for (Leaj0015LcTrnIrdHisModel model : result) {

                requiredQty = model.getRequiredQty();
                shipQty     = model.getShipQty();
                boQty       = model.getBoQty();

                if (StringUtils.isNotEmpty(requiredQty) && Double.parseDouble(requiredQty) >= 0) {
                    requiredQty = "+" + requiredQty;
                }

                if (StringUtils.isNotEmpty(shipQty) && Double.parseDouble(shipQty) >= 0) {
                    shipQty = "+" + shipQty;
                }

                if (StringUtils.isNotEmpty(boQty) && Double.parseDouble(boQty) >= 0) {
                    boQty = "+" + boQty;
                }

                writeRec.setLength(0);

                // テキストファイルへ出力
                writeRec.append(pm.getSysOwnerCd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getItemno());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSupplier());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUsercd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getStartDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderThroughNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderThroughNoSourceFlg());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRdClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getIndUserClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getIndUserCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRlsStartDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getReasonCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getFixedYmd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getPilotClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getPilotConditionType());
                writeRec.append(DELIMITER);
                writeRec.append(requiredQty);
                writeRec.append(DELIMITER);
                writeRec.append(model.getRemark());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRequestSystemCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getGroupReceiveFlg());
                writeRec.append(DELIMITER);
                writeRec.append(shipQty);
                writeRec.append(DELIMITER);
                writeRec.append(boQty);
                writeRec.append(DELIMITER);
                writeRec.append(model.getDeliveryCardStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getItemCardStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getShipDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getTransferClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getTransferCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getTransferReasonCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountHeading());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountDetail());
                writeRec.append(DELIMITER);
                writeRec.append(model.getBudgetNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountCodeSales());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpOrderClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpDeliveryCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpDealerNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpOrderNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOperationNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOperationSeq());
                writeRec.append(DELIMITER);
                writeRec.append(model.getBatchStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getMrpDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getDeleteYmd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRegisterUserName());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdateCounter());
                writeRec.append(DELIMITER);
                writeRec.append(model.getCreateDatetime());
                writeRec.append(DELIMITER);
                writeRec.append(model.getCreateAuthor());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdateDatetime());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdateAuthor());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdatePgmid());

                writer.write(writeRec.toString());
                writer.newLine();
                pm.setLcTrnIrdHisTxtFileOutputCount(pm.getLcTrnIrdHisTxtFileOutputCount() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing to file: " + outputFile, e);
        }
    }

    /**
     * LcTrnIrdHisの返却値をセットする
     */
    public Leaj0015LcTrnIrdHisModel convertResultLcTrnIrdHisModel(ResultSet result) throws Exception{

        Leaj0015LcTrnIrdHisModel model = new Leaj0015LcTrnIrdHisModel();

        model.setSysOwnerCd(result.getString(1));
        model.setItemno(result.getString(2));
        model.setSupplier(result.getString(3));
        model.setUsercd(result.getString(4));
        model.setOrderNo(result.getString(5));
        model.setStartDate(result.getString(6));
        model.setOrderThroughNo(result.getString(7));
        model.setOrderThroughNoSourceFlg(result.getString(8));
        model.setRdClass(result.getString(9));
        model.setIndUserClass(result.getString(10));
        model.setIndUserCode(result.getString(11));
        model.setRlsStartDate(result.getString(12));
        model.setOrderStatus(result.getString(13));
        model.setReasonCode(result.getString(14));
        model.setFixedYmd(result.getString(15));
        model.setPilotClass(result.getString(16));
        model.setPilotConditionType(result.getString(17));
        model.setRequiredQty(result.getString(18));
        model.setRemark(result.getString(19));
        model.setRequestSystemCode(result.getString(20));
        model.setGroupReceiveFlg(result.getString(21));
        model.setShipQty(result.getString(22));
        model.setBoQty(result.getString(23));
        model.setDeliveryCardStatus(result.getString(24));
        model.setItemCardStatus(result.getString(25));
        model.setShipDate(result.getString(26));
        model.setTransferClass(result.getString(27));
        model.setTransferCode(result.getString(28));
        model.setTransferReasonCode(result.getString(29));
        model.setAccountHeading(result.getString(30));
        model.setAccountDetail(result.getString(31));
        model.setBudgetNo(result.getString(32));
        model.setAccountCodeSales(result.getString(33));
        model.setSpOrderClass(result.getString(34));
        model.setSpDeliveryCode(result.getString(35));
        model.setSpDealerNo(result.getString(36));
        model.setSpOrderNo(result.getString(37));
        model.setOperationNo(result.getString(38));
        model.setOperationSeq(result.getString(39));
        model.setBatchStatus(result.getString(40));
        model.setMrpDate(result.getString(41));
        model.setDeleteYmd(result.getString(42));
        model.setRegisterUserName(result.getString(43));
        model.setUpdateCounter(result.getLong(44));
        model.setCreateDatetime(result.getTimestamp(45));
        model.setCreateAuthor(result.getString(46));
        model.setUpdateDatetime(result.getTimestamp(47));
        model.setUpdateAuthor(result.getString(48));
        model.setUpdatePgmid(result.getString(49));

        return model;
    }

    /**
     * SCPLAN-IN 独立所要量 データ取得
     */
    private List<Leaj0015ScplanInLcTrnIrdModel> getScplanInLcTrnIrdSet(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        // 独立所要量明細の検索SQL
        sql += " SELECT a.sys_owner_cd                               ";
        sql += "       ,a.itemno                                     ";
        sql += "       ,a.supplier                                   ";
        sql += "       ,a.usercd                                     ";
        sql += "       ,a.order_no                                   ";
        sql += "       ,a.start_date                                 ";
        sql += "       ,a.pilot_class                                ";
        sql += "       ,a.pilot_condition_type                       ";
        sql += "       ,a.required_qty                               ";
        sql += "       ,a.ship_qty                                   ";
        sql += "       ,a.mrp_date                                   ";
        sql += "       ,a.rd_class                                   ";
        sql += "       ,a.order_through_no                           ";
        sql += "       ,a.order_through_no_source_flg                ";
        sql += "       ,b.times_divide_day                           "; // 2017-09-14 ADD
        sql += "  FROM lc_inp_ird            a                       "; // (レプリカ)独立所要量明細ワーク
        sql += "      ,lc_inp_itemmast_mrp   b                       "; // 2017-09-14 MOD
        sql += " WHERE a.mrp_control_class = ?                       ";
        sql += "   AND a.sys_owner_cd      = ?                       ";
        sql += "   AND a.mrp_control_class = b.mrp_control_class     ";
        sql += "   AND a.sys_owner_cd      = b.sys_owner_cd          ";
        sql += "   AND a.itemno            = b.itemno                ";
        sql += "   AND a.supplier          = b.supplier              ";
        sql += "   AND a.usercd            = b.usercd                ";
        sql += "   AND a.order_status     <> '9'                     "; // 未完了
        sql += "   AND a.pilot_class      <> '4'                     "; // 単品生試は除く
        sql += "   AND (a.rd_class in ('1','2','3','4')              "; // 1:先行生産 2:変動安全在庫 3:自動安全在庫 4:生試所要量調整
        sql += "   OR  (a.rd_class not in ('1','2','3','4')          ";
        sql += "       AND a.start_date >= ?))                       ";
        sql += " ORDER BY 1,2,3,4,5,6                                ";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass(), pm.getSysOwnerCd(), pm.getOrderDeleteDate());

        // Select文の発行
        List<Leaj0015ScplanInLcTrnIrdModel> results = statementSpec.query((rs, rowNum) -> {
            try {
                Leaj0015ScplanInLcTrnIrdModel model = convertResultScplanInLcTrnIrdModel(rs);
                return model;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).list();

        if(!results.isEmpty()){
            return results;
        }else{
            return new ArrayList<Leaj0015ScplanInLcTrnIrdModel>();
        }
    }

    /**
     * SCPLAN-IN 独立所要量ファイル出力--> out3:
     */
    private void scplanInLcTrnIrdTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, List<Leaj0015ScplanInLcTrnIrdModel> result) throws Exception {

        StringBuilder writeRec = new StringBuilder();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
            for (Leaj0015ScplanInLcTrnIrdModel model : result) {
                // シフト番号セット
                setShiftNo(pm, model);

                String safetyStockFlag;

                // 安全在庫フラグのセット
                if("2".equals(model.getRdClass())){
                    safetyStockFlag = "1";
                }else{
                    safetyStockFlag = "0";
                }

                writeRec.setLength(0);

                // テキストファイルへ出力
                writeRec.append(padSpaseRight(pm.getScplanSysOwnerCd(), 2));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getItemno(),30));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getSupplier(), 4));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getUsercd(), 4));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getOrderNo(), 5));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getStartDate(), 8));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(pm.getScplanInShippingShiftNo(), 2));     // 2017-09-14 MOD
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getPilotClass(), 1));
                writeRec.append(DELIMITER);
                writeRec.append(decimalFormat(model.getRequiredQty()));
                writeRec.append(DELIMITER);
                writeRec.append(decimalFormat(model.getShipQty()));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getMrpDate(), 8));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(safetyStockFlag, 1));
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getOrderThroughNo(), 6));            // 2017-09-14 MOD
                writeRec.append(DELIMITER);
                writeRec.append(padSpaseRight(model.getOrderThroughNoSourceFlg(), 1));    // 2017-09-14 MOD

                writer.write(writeRec.toString());
                writer.newLine();
                pm.setScplanInLcTrnIrdOutputCount(pm.getScplanInLcTrnIrdOutputCount() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing to file: " + outputFile, e);
        }
    }

    /**
     * ScplanInLcTrnIrdの返却値をセットする
     */
    public Leaj0015ScplanInLcTrnIrdModel convertResultScplanInLcTrnIrdModel(ResultSet result) throws Exception{

        Leaj0015ScplanInLcTrnIrdModel model = new Leaj0015ScplanInLcTrnIrdModel();

        // sql=sys_owner_cd
        model.setItemno(result.getString(2));
        model.setSupplier(result.getString(3));
        model.setUsercd(result.getString(4));
        model.setOrderNo(result.getString(5));
        model.setStartDate(result.getString(6));
        model.setPilotClass(result.getString(7));
        model.setRequiredQty(result.getString(9));
        // sql=pilot_condition_type
        model.setShipQty(result.getString(10));
        model.setMrpDate(result.getString(11));
        model.setRdClass(result.getString(12));
        model.setOrderThroughNo(result.getString(13));            // 2017-09-14 MOD
        model.setOrderThroughNoSourceFlg(result.getString(14));    // 2017-09-14 MOD
        model.setTimesDivideDay(result.getString(15));            // 2017-09-14 ADD

        return model;
    }

    /**
     * シフト番号セット
     */
    private void setShiftNo(Leaj0015ParameterModel pm, Leaj0015ScplanInLcTrnIrdModel model) {

        StringBuilder searchKey1     = new StringBuilder();
        StringBuilder searchKey2     = new StringBuilder();
        String manfDelvType    = "";
        String shiftBoxNo1    = "";
        String shiftBoxNo2    = "";
        final String strReferenceKey = "****";

        // 時間指示品目のとき
        if (!"".equals(model.getTimesDivideDay().trim())) {

            // 製造納入種類セット
            if (model.getSupplier().equals(model.getUsercd())) {

                manfDelvType = "MANF";
            } else {

                manfDelvType = "DELV";
            }

            // 時刻指定回数 | 製造／納入種類 | 使用者 で検索
            searchKey1.setLength(0);
            searchKey1.append(model.getTimesDivideDay());
            searchKey1.append(DELIMITER);
            searchKey1.append(manfDelvType);
            searchKey1.append(DELIMITER);
            searchKey1.append(model.getUsercd());

            shiftBoxNo1    = pm.getMinValidShiftMap().get(searchKey1);

            if(shiftBoxNo1 != null) {

                pm.setScplanInShippingShiftNo(shiftBoxNo1);

            } else {

                // 時刻指定回数 | 製造／納入種類 | ****(ALL) で検索
                searchKey2.setLength(0);
                searchKey2.append(model.getTimesDivideDay());
                searchKey2.append(DELIMITER);
                searchKey2.append(manfDelvType);
                searchKey2.append(DELIMITER);
                searchKey2.append(strReferenceKey);

                shiftBoxNo2    = pm.getMinValidShiftMap().get(searchKey2);

                if(shiftBoxNo2 != null) {

                    pm.setScplanInShippingShiftNo(shiftBoxNo2);

                } else {

                    pm.setScplanInShippingShiftNo(" ");
                }
            }
        } else {

            //時間指示しない品目のとき
            pm.setScplanInShippingShiftNo(" ");
        }
    }

    /**
     * format(スペース右埋め)
     * StringUtils.padCharRight()を少し変更してある。(Null,ブランクの場合でも文字埋めされるように)
     */
    public String padSpaseRight(String str, int len) {
        char padChar = ' ';

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

    /**
     * BigDecimal Format
     * @param str
     * @return qty format
     */
    public String decimalFormat(String str) {

        DecimalFormat df = new DecimalFormat("+000000000.00000;-000000000.00000");
        return df.format(new BigDecimal(str));
    }

    /**
     * 独立所要量バイパスファイル出力
     *
     * @param pm
     * @return
     * @throws Exception
     */
    private List<Leaj0015LcWorkBypassIrdModel> getLcWorkBypassIrdSet(Leaj0015ParameterModel pm) throws Exception {

        String sql = "";

        // 独立所要量明細の検索SQL
        sql += " SELECT itemno                                    ";
        sql += "       ,supplier                                  ";
        sql += "       ,usercd                                    ";
        sql += "       ,order_no                                  ";
        sql += "       ,start_date                                ";
        sql += "       ,order_through_no                          "; // 2017-09-14 MOD
        sql += "       ,order_through_no_source_flg               "; // 2017-09-14 MOD
        sql += "       ,rd_class                                  ";
        sql += "       ,ind_user_class                            ";
        sql += "       ,ind_user_code                             ";
        sql += "       ,rls_start_date                            ";
        sql += "       ,order_status                              ";
        sql += "       ,reason_code                               ";
        sql += "       ,fixed_ymd                                 ";
        sql += "       ,pilot_class                               ";
        sql += "       ,pilot_condition_type                      ";
        sql += "       ,required_qty                              ";
        sql += "       ,remark                                    ";
        sql += "       ,request_system_code                       ";
        sql += "       ,group_receive_flg                         ";
        sql += "       ,ship_qty                                  ";
        sql += "       ,bo_qty                                    ";
        sql += "       ,delivery_card_status                      ";
        sql += "       ,item_card_status                          ";
        sql += "       ,ship_date                                 ";
        sql += "       ,transfer_class                            ";
        sql += "       ,transfer_code                             ";
        sql += "       ,transfer_reason_code                      ";
        sql += "       ,account_heading                           ";
        sql += "       ,account_detail                            ";
        sql += "       ,budget_no                                 ";
        sql += "       ,account_code_sales                        ";
        sql += "       ,sp_order_class                            ";
        sql += "       ,sp_delivery_code                          ";
        sql += "       ,sp_dealer_no                              ";
        sql += "       ,sp_order_no                               ";
        sql += "       ,operation_no                              ";
        sql += "       ,operation_seq                             ";
        sql += "       ,batch_status                              ";
        sql += "       ,mrp_date                                  ";
        sql += "       ,delete_ymd                                ";
        sql += "       ,register_user_name                        ";
        sql += "       ,update_counter                            ";
        sql += "       ,create_datetime                           ";
        sql += "       ,create_author                             ";
        sql += "       ,update_datetime                           ";
        sql += "       ,update_author                             ";
        sql += "       ,update_pgmid                              ";
        sql += "  FROM lc_inp_ird                                 "; // (レプリカ)独立所要量明細ワーク
        sql += " WHERE mrp_control_class = ?                      ";
        sql += "   AND sys_owner_cd      = ?                      ";

        sql += "   AND ((order_status  = '9' and TO_CHAR(update_datetime, 'YYYYMMDD') >= ?)    "; //履歴移動対象でない完了IRD
        sql += "    OR  (order_status <> '9'                                                   "; //発注取消対象でない未完了IRD
        sql += "         and ((pilot_class  = '4' and start_date >= ?)                         ";
        sql += "          or  (pilot_class <> '4'                                              ";
        sql += "               and (rd_class     in ('1','3','4')                              ";
        sql += "                or (rd_class not in ('1','2','3','4') and start_date >= ?))))))";

        //SQLパラメータの生成 STD 0000
        JdbcClient.StatementSpec statementSpec = jdbcClient.sql(sql).params(pm.getMrpControlClass()
                                                                           ,pm.getSysOwnerCd()
                                                                           ,pm.getHistoryMoveDate()
                                                                           ,pm.getOrderDeleteDate()
                                                                           ,pm.getOrderDeleteDate() );

        // Select文の発行
        List<Leaj0015LcWorkBypassIrdModel> results = statementSpec.query((rs, rowNum) -> {
            try {
                Leaj0015LcWorkBypassIrdModel model = convertResultLcWorkBypassIrdModel(rs);
                return model;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).list();

        if(!results.isEmpty()){
            return results;
        }else{
            return new ArrayList<Leaj0015LcWorkBypassIrdModel>();
        }
    }

    /**
     * 独立所要量バイパスファイル出力--> out4:
     */
    private void lcWorkBypassIrdTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, List<Leaj0015LcWorkBypassIrdModel> result) throws Exception {

        StringBuilder writeRec = new StringBuilder();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
            for (Leaj0015LcWorkBypassIrdModel model : result) {

                writeRec.setLength(0);

                // テキストファイルへ出力
                writeRec.append(pm.getMrpControlClass());
                writeRec.append(DELIMITER);
                writeRec.append(pm.getScplanSysOwnerCd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getItemno());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSupplier());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUsercd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getStartDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderThroughNo());                 // 2017-09-14 MOD
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderThroughNoSourceFlg());    // 2017-09-14 MOD
                writeRec.append(DELIMITER);
                writeRec.append(model.getRdClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getIndUserClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getIndUserCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRlsStartDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOrderStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getReasonCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getFixedYmd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getPilotClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getPilotConditionType());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRequiredQty());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRemark());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRequestSystemCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getGroupReceiveFlg());
                writeRec.append(DELIMITER);
                writeRec.append(model.getShipQty());
                writeRec.append(DELIMITER);
                writeRec.append(model.getBoQty());
                writeRec.append(DELIMITER);
                writeRec.append(model.getDeliveryCardStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getItemCardStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getShipDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getTransferClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getTransferCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getTransferReasonCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountHeading());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountDetail());
                writeRec.append(DELIMITER);
                writeRec.append(model.getBudgetNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getAccountCodeSales());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpOrderClass());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpDeliveryCode());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpDealerNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getSpOrderNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOperationNo());
                writeRec.append(DELIMITER);
                writeRec.append(model.getOperationSeq());
                writeRec.append(DELIMITER);
                writeRec.append(model.getBatchStatus());
                writeRec.append(DELIMITER);
                writeRec.append(model.getMrpDate());
                writeRec.append(DELIMITER);
                writeRec.append(model.getDeleteYmd());
                writeRec.append(DELIMITER);
                writeRec.append(model.getRegisterUserName());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdateCounter());
                writeRec.append(DELIMITER);
                writeRec.append(model.getCreateDatetime());
                writeRec.append(DELIMITER);
                writeRec.append(model.getCreateAuthor());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdateDatetime());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdateAuthor());
                writeRec.append(DELIMITER);
                writeRec.append(model.getUpdatePgmid());

                writer.write(writeRec.toString());
                writer.newLine();
                pm.setLcWorkBypassIrdOutputCount(pm.getLcWorkBypassIrdOutputCount() + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing to file: " + outputFile, e);
        }
    }

    /**
     * LcWorkBypassIrdの返却値をセットする
     */
    public Leaj0015LcWorkBypassIrdModel convertResultLcWorkBypassIrdModel(ResultSet result) throws Exception{

        Leaj0015LcWorkBypassIrdModel model = new Leaj0015LcWorkBypassIrdModel();

        model.setItemno(result.getString(1));
        model.setSupplier(result.getString(2));
        model.setUsercd(result.getString(3));
        model.setOrderNo(result.getString(4));
        model.setStartDate(result.getString(5));
        model.setOrderThroughNo(result.getString(6));
        model.setOrderThroughNoSourceFlg(result.getString(7));
        model.setRdClass(result.getString(8));
        model.setIndUserClass(result.getString(9));
        model.setIndUserCode(result.getString(10));
        model.setRlsStartDate(result.getString(11));
        model.setOrderStatus(result.getString(12));
        model.setReasonCode(result.getString(13));
        model.setFixedYmd(result.getString(14));
        model.setPilotClass(result.getString(15));
        model.setPilotConditionType(result.getString(16));
        model.setRequiredQty(result.getBigDecimal(17));
        model.setRemark(result.getString(18));
        model.setRequestSystemCode(result.getString(19));
        model.setGroupReceiveFlg(result.getString(20));
        model.setShipQty(result.getBigDecimal(21));
        model.setBoQty(result.getBigDecimal(22));
        model.setDeliveryCardStatus(result.getString(23));
        model.setItemCardStatus(result.getString(24));
        model.setShipDate(result.getString(25));
        model.setTransferClass(result.getString(26));
        model.setTransferCode(result.getString(27));
        model.setTransferReasonCode(result.getString(28));
        model.setAccountHeading(result.getString(29));
        model.setAccountDetail(result.getString(30));
        model.setBudgetNo(result.getString(31));
        model.setAccountCodeSales(result.getString(32));
        model.setSpOrderClass(result.getString(33));
        model.setSpDeliveryCode(result.getString(34));
        model.setSpDealerNo(result.getString(35));
        model.setSpOrderNo(result.getString(36));
        model.setOperationNo(Integer.valueOf(result.getString(37)));
        model.setOperationSeq(Integer.valueOf(result.getString(38)));
        model.setBatchStatus(result.getString(39));
        model.setMrpDate(result.getString(40));
        model.setDeleteYmd(result.getString(41));
        model.setRegisterUserName(result.getString(42));
        model.setUpdateCounter(result.getLong(43));
        model.setCreateDatetime(result.getTimestamp(44));
        model.setCreateAuthor(result.getString(45));
        model.setUpdateDatetime(result.getTimestamp(46));
        model.setUpdateAuthor(result.getString(47));
        model.setUpdatePgmid(result.getString(48));

        return model;
    }


//    /**
//     * パラメータモデル（グローバル変数）生成
//     */
//    @Override
//    protected Leaj0015ParameterModel getParameterModelInstance() throws Exception {
//
//        return new Leaj0015ParameterModel();
//    }
//    /**
//     * 引数取得
//     */
//    @Override
//    protected void setParameter(Leaj0015ParameterModel pm, String name, String value) throws Exception {
//
//        if ( "arg1".equals(name) ) {
//            // MRP管理区分
//            pm.setMrpControlClass(value);
//        }
//        if ( "out1".equals(name) ) {
//            // 発注取消（独立所要量）ファイル
//            pm.setCancelIrd(value);
//        }
//        if ( "out2".equals(name) ) {
//            // 独立所要量明細履歴ファイル
//            pm.setIrdHistory(value);
//        }
//        if ( "out3".equals(name) ) {
//            // SCPLAN-IN独立所要量ファイル
//            pm.setScplanIrd(value);
//        }
//        if ( "out4".equals(name) ) {
//            // SCPLAN BYPASS 独立所要量明細ファイル
//            pm.setBypassIrd(value);
//        }
//    }
//    /**
//     * 引数取得後処理
//     */
//    @Override
//    public void prepareParameterAfter(Leaj0015ParameterModel pm) throws Exception {
//
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        // Parameter Check
//        if ( CheckUtil.isEmptyBlank(pm.getMrpControlClass()) ) {
//            throw new BatchApplicationException("Argument error [arg1:mrpControlClass] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getCancelIrd()) ) {
//            throw new BatchApplicationException("Argument error [out1:cancelIrd] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getIrdHistory()) ) {
//            throw new BatchApplicationException("Argument error [out2:irdHistory] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getScplanIrd()) ) {
//            throw new BatchApplicationException("Argument error [out3:scplanIrd] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getBypassIrd()) ) {
//            throw new BatchApplicationException("Argument error [out4:bypassIrd] is blank");
//        }
//
//        // 引数内容ログ出力
//        writer.writeLog("", "[Argument Value]", null, null, null);
//        writer.writeLog("", " sysOwnerCd      : " + pm.getSysOwnerCd(),      null, null, null);
//        writer.writeLog("", " mrpControlClass : " + pm.getMrpControlClass(), null, null, null);
//        writer.writeLog("", " cancelIrd       : " + pm.getCancelIrd(),       null, null, null);
//        writer.writeLog("", " irdHistory      : " + pm.getIrdHistory(),      null, null, null);
//        writer.writeLog("", " scplanIrd       : " + pm.getScplanIrd(),       null, null, null);
//        writer.writeLog("", " bypassIrd       : " + pm.getBypassIrd(),       null, null, null);
//    }
//    /**
//     * 初期処理
//     */
//    @Override
//    protected void init(Leaj0015ParameterModel pm) throws Exception {
//
//        // MRPシステムパラメータの発注削除日数、完了保有日数を取得
//        getLcSystemParameterModel(pm);
//
//        // 工場処理日(PYMAC日)取得
//        getPymacDateFromLzPymacDate(pm);
//        checkExistOfPymacDate(pm);
//
//        // 標準カレンダーコード取得
//        getCalendarCodeFromLcMstCalendarCode(pm);
//        checkExistOfCalendarCode(pm);
//
//        // 独立所要量発注削除日計算 ( 工場処理日(PYMAC日) - 発注削除日数 = 発注取消基準日 ) 稼働日ベース
//        getOrderDeleteDateFromSp(pm);
//
//        // 独立所要量明細履歴移動日計算 ( 工場処理日(PYMAC日) - 完了保有日数 = 履歴移動基準日 ) 暦日ベース
//        pm.setHistoryMoveDate(DateUtil.addDate(-pm.getCompleteHoldingDays(), pm.getPymacDate()));
//
//        // 先頭稼動シフトを取得  2017-09-13 ADD
//        getMinValidShift(pm);
//
//        // 取得した値をログ出力
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//        writer.writeLog("", "pymacDate       : " + pm.getPymacDate(), null, null, null);
//        writer.writeLog("", "calendarCode    : " + pm.getCalendarCode(), null, null, null);
//        writer.writeLog("", "orderDeleteDate : " + pm.getOrderDeleteDate(), null, null, null);
//        writer.writeLog("", "historyMoveDate : " + pm.getHistoryMoveDate(), null, null, null);
//    }
//
//    /**
//     * 主処理
//     */
//    @Override
//    protected void main(Leaj0015ParameterModel pm) throws Exception {
//
//        // 発注取消（独立所要量）ファイル出力 --> out1:
//        ResultSet lcTrnIrdSet = getLcTrnIrdSet(pm);
//        lcTrnIrdTxtFileOutputProcess(pm.getCancelIrd(), pm, lcTrnIrdSet);
//
//        // 独立所要量履歴ファイル出力         --> out2:
//        ResultSet lcTrnIrdHisSet = getLcTrnIrdHisSet(pm);
//        lcTrnIrdHisTxtFileOutputProcess(pm.getIrdHistory(), pm, lcTrnIrdHisSet);
//
//        // SCPLAN-IN 独立所要量ファイル出力   --> out3:
//        ResultSet scplanInLcTrnIrdSet = getScplanInLcTrnIrdSet(pm);
//        scplanInLcTrnIrdTxtFileOutputProcess(pm.getScplanIrd(), pm, scplanInLcTrnIrdSet);
//
//        // 独立所要量バイパスファイル出力     --> out4:
//        ResultSet lcWorkBypassIrdSet = getLcWorkBypassIrdSet(pm);
//        lcWorkBypassIrdTxtFileOutputProcess(pm.getBypassIrd(), pm, lcWorkBypassIrdSet);
//    }
//
//    /**
//     * 後処理
//     */
//    @Override
//    protected void term(Leaj0015ParameterModel pm) throws Exception {
//
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        // 処理件数ログ出力
//        writer.writeLog("", "[IRD Cancel]       Output Count = " + pm.getLcTrnIrdTxtFileOutputCount(),    null, null, null);
//        writer.writeLog("", "[IRD HistoryMove]  Output Count = " + pm.getLcTrnIrdHisTxtFileOutputCount(), null, null, null);
//        writer.writeLog("", "[IRD SCPLAN-IN]    Output Count = " + pm.getScplanInLcTrnIrdOutputCount(),   null, null, null);
//        writer.writeLog("", "[IRD BYPASS]       Output Count = " + pm.getLcWorkBypassIrdOutputCount(),    null, null, null);
//    }
//
//
//    /**
//     * MRPシステムパラメータ検索
//     *
//     * @param pm
//     * @throws Exception
//     */
//    private void getLcSystemParameterModel(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        //MRPシステムパラメータの取得SQL
//        sql += " SELECT order_delete_days     "; // 発注削除日数
//        sql += "       ,complete_holding_days "; // 完了保有日数
//        sql += "       ,scplan_sys_owner_cd   "; // SCPLAN用システムパラメータ
//        sql += "   FROM lc_system_parameter   ";
//        sql += "  WHERE sys_owner_cd = ?      ";
//        sql += "    AND system_code  = 'LC'   ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = new Object[] {pm.getSysOwnerCd()};
//
//        // Select文の発行
//        Object[] sqlResult = dao.getRecord(sql, sqlParams);
//
//        if (sqlResult != null) {
//            pm.setOrderDeteleDays(NumberUtil.toInt(StringUtil.toString(sqlResult[0])));
//            pm.setCompleteHoldingDays(NumberUtil.toInt(StringUtil.toString(sqlResult[1])));
//            pm.setScplanSysOwnerCd(StringUtil.toString(sqlResult[2]));
//
//        } else {
//            //該当データが存在しない場合エラーとする
//            String msg    = "[lc_system_parameter] No Data Found Error."
//                        + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
//            throw new BatchApplicationException(msg);
//        }
//    }
//    /**
//     * 工場処理日(PYMAC日)取得
//     *
//     * @param pm
//     * @throws Exception
//     */
//    private void getPymacDateFromLzPymacDate(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        //ＰＹＭＡＣ日の取得SQL
//        sql += " SELECT pymac_date            ";
//        sql += "   FROM lc_inp_pymac_date     ";
//        sql += "  WHERE mrp_control_class = ? ";
//        sql += "    AND sys_owner_cd      = ? ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                                          , pm.getSysOwnerCd()
//                                          };
//
//        // Select文の発行
//        Object[] sqlResult = dao.getRecord(sql, sqlParams);
//
//        if (sqlResult != null) {
//
//            pm.setPymacDate(StringUtil.toString(sqlResult[0]));
//        }
//     }
//    /**
//     * 工場処理日(PYMAC日)の存在チェック
//     *
//     * @param pm
//     * @throws Exception
//     */
//    private void checkExistOfPymacDate(Leaj0015ParameterModel pm) throws Exception {
//
//        if ( CheckUtil.isEmptyBlank(pm.getPymacDate())) {
//
//            //該当データが存在しない場合エラーとする
//            String msg    = "[lc_inp_pymac_date] No Data Found Error."
//                    + " [mrp_control_class:'" + pm.getMrpControlClass() + "']"
//                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']"
//                    ;
//
//            throw new BatchApplicationException(msg);
//        }
//    }
//
//    /**
//     * 標準カレンダーコード取得
//     *
//     * @param pm
//     * @throws Exception
//     */
//    private void getCalendarCodeFromLcMstCalendarCode(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        //カレンダーコードの取得SQL
//        sql += " SELECT calendar_code            ";
//        sql += "   FROM lc_inp_calendar_code     ";
//        sql += "  WHERE mrp_control_class  = ?   ";
//        sql += "    AND sys_owner_cd       = ?   ";
//        sql += "    AND std_calendar_class = '1' ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                                          , pm.getSysOwnerCd()
//                                          };
//
//        // Select文の発行
//        Object[] sqlResult = dao.getRecord(sql, sqlParams);
//
//        if (sqlResult != null) {
//
//            pm.setCalendarCode(StringUtil.toString(sqlResult[0]));
//        }
//    }
//
//    /**
//     * 標準カレンダーコードの存在チェック
//     *
//     * @param pm
//     * @throws Exception
//     */
//    private void checkExistOfCalendarCode(Leaj0015ParameterModel pm) throws Exception {
//
//        if (CheckUtil.isEmptyBlank(pm.getCalendarCode())) {
//
//            //該当データが存在しない場合エラーとする
//            String msg    = "[lc_inp_calendar_code] No Data Found Error."
//                    + " [mrp_control_class:'" + pm.getMrpControlClass() + "']"
//                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
//
//            throw new BatchApplicationException(msg);
//        }
//    }
//
//    /**
//     * 独立所要量発注削除日計算
//     * ( 工場処理日(PYMAC日) - 発注削除日数 = 発注取消基準日 ) 稼働日ベース
//     *
//     * @param pm
//     * @throws Exception
//     */
//    private void getOrderDeleteDateFromSp(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql;
//
//        //稼働日計算（レプリカ）CALL
//        sql = "";
//        sql = " select * from LCYS0008(?,?,?,?,?)";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                                          , pm.getSysOwnerCd()
//                                          , pm.getCalendarCode()
//                                          , pm.getPymacDate()
//                                          , (-1) * pm.getOrderDeteleDays()
//                                          };
//
//        // Select文の発行
//        ResultSet sqlResult = dao.getResult(sql, sqlParams);
//
//        try {
//
//            while (BatchUtils.hasNext(sqlResult)){
//
//                Leaj0015SpModel model = BatchUtils.convertResultModel(sqlResult, Leaj0015SpModel.class);
//
//                pm.setOrderDeleteDate(model.getRsTargetDate());
//            }
//        } finally {
//
//            sqlResult.close();
//        }
//    }
//
//    /**
//     * 初期処理／先頭稼動シフト取得、Map作成
//     * @param pm
//     */
//    private void getMinValidShift(Leaj0015ParameterModel pm) throws Exception {
//
//        HashMap<String, String> map    = new HashMap<String, String>() ;
//        StringBuilder key             = new StringBuilder();
//
//        String sql = "";
//
//        //検索SQL
//        sql += " SELECT   time.times_divide_day                                                                ";
//        sql += "         ,time.manf_delv_type                                                                  ";
//        sql += "         ,time.usercd                                                                          ";
//        sql += "         ,time.shift_box_no                                                                    ";
//        sql += " FROM     lc_inp_shift_divide_pattern    sft                                                   ";
//        sql += "         ,lc_inp_divide_time             time                                                  ";
//
//        sql += " WHERE   sft.mrp_control_class   = ?                                                           ";
//        sql += " AND     sft.sys_owner_cd        = ?                                                           ";
//        sql += " AND     sft.std_shift_flg       = '0'                                                         ";
//
//        sql += " AND     sft.mrp_control_class   = time.mrp_control_class                                      ";
//        sql += " AND     sft.sys_owner_cd        = time.sys_owner_cd                                           ";
//        sql += " AND     sft.times_divide_day    = time.times_divide_day                                       ";
//        sql += " AND     sft.manf_delv_type      = time.manf_delv_type                                         ";
//        sql += " AND     sft.usercd              = time.usercd                                                 ";
//
//        sql += " AND     time.shift_box_no       = (SELECT   MIN(time2.shift_box_no)                           ";
//        sql += "                                    FROM     lc_inp_divide_time      time2                     ";
//        sql += "                                    WHERE    time2.mrp_control_class = sft.mrp_control_class   ";
//        sql += "                                    AND      time2.sys_owner_cd      = sft.sys_owner_cd        ";
//        sql += "                                    AND      time2.times_divide_day  = sft.times_divide_day    ";
//        sql += "                                    AND      time2.manf_delv_type    = sft.manf_delv_type      ";
//        sql += "                                    AND      time2.usercd            = sft.usercd              ";
//        sql += "                                    AND      time2.valid_flg         = '1')                    ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                                           ,pm.getSysOwnerCd()
//                                           };
//
//        // Select文の発行
//        ResultSet resultSet = dao.getResult(sql, sqlParams);
//
//        try {
//            while ( BatchUtils.hasNext(resultSet) ) {
//
//                key.setLength(0);
//                key.append(resultSet.getString(1));        // 時刻指定回数
//                key.append(DELIMITER);
//                key.append(resultSet.getString(2));     // 製造／納入種類
//                key.append(DELIMITER);
//                key.append(resultSet.getString(3));     // 使用者
//
//                map.put(key.toString(), resultSet.getString(4));
//            }
//        } finally {
//            // ResultSetをクローズ
//            resultSet.close();
//        }
//
//        pm.setMinValidShiftMap(map);
//    }
//
//    /**
//     * 発注取消（独立所要量） データ取得
//     *
//     * @param pm
//     * @return
//     * @throws Exception
//     */
//    private ResultSet getLcTrnIrdSet(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        // 独立所要量明細の検索SQL
//        sql += " SELECT itemno                            ";
//        sql += "       ,supplier                          ";
//        sql += "       ,usercd                            ";
//        sql += "       ,order_no                          ";
//        sql += "       ,start_date                        ";
//        sql += "       ,ind_user_class                    ";
//        sql += "       ,ind_user_code                     ";
//        sql += "       ,required_qty                      ";
//        sql += "       ,ship_qty                          ";
//        sql += "       ,transfer_class                    ";
//        sql += "       ,transfer_code                     ";
//        sql += "       ,account_heading                   ";
//        sql += "       ,account_detail                    ";
//        sql += "       ,budget_no                         ";
//        sql += "       ,create_author                     ";
//        sql += "       ,reason_code                       ";
//        sql += "       ,create_datetime                   ";
//        sql += "       ,update_datetime                   ";
//        sql += "       ,mrp_date                          ";
//        sql += "  FROM lc_inp_ird                         "; // (レプリカ)独立所要量明細ワーク
//        sql += " WHERE mrp_control_class = ?              ";
//        sql += "   AND sys_owner_cd      = ?              ";
//        sql += "   AND order_status      <> '9'           "; // 未完了
//        sql += "   AND start_date        < ?              "; // 発注取消対象
//        sql += "   AND (pilot_class      = '4'            "; // 4:単品生試
//        sql += "    OR  rd_class        in ('0','5'))     ";
//        sql += " ORDER BY 1,2,3,4,5                       ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = null;
//
//        sqlParams = new Object[] { pm.getMrpControlClass()
//                                  , pm.getSysOwnerCd()
//                                  , pm.getOrderDeleteDate()
//                                  };
//
//        // Select文の発行
//        return dao.getResult(sql, sqlParams);
//    }
//
//    /**
//     * 発注取消（独立所要量）ファイル出力 --> out1:
//     *
//     * @param outputFile
//     * @param pm
//     * @param result
//     * @throws Exception
//     */
//    private void lcTrnIrdTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, ResultSet result) throws Exception {
//
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(outputFile);
//
//        StringBuilder writeRec = new StringBuilder();
//
//        String indUser     = "";
//         String transfer    = "";
//        String requiredQty = "";
//        String shipQty     = "";
//        String createDate  = "";
//        String updateDate  = "";
//
//        try {
//            Leaj0015LcTrnIrdModel model = null;
//            while (BatchUtils.hasNext(result)) {
//
//                //Leaj0015LcTrnIrdModel model = BatchUtils.<Leaj0015LcTrnIrdModel>convertResultModel(result, Leaj0015LcTrnIrdModel.class);
//                model = convertResultLcTrnIrdModel(result);
//
//                requiredQty = model.getRequiredQty();
//                shipQty     = model.getShipQty();
//
//                if (NumberUtil.toDouble(requiredQty) >= 0) {
//                    requiredQty = "+" + requiredQty;
//                }
//
//                if (NumberUtil.toDouble(shipQty) >= 0) {
//                    shipQty = "+" + shipQty;
//                }
//
//                indUser  = model.getIndUserClass() + model.getIndUserCode();
//                transfer = model.getTransferClass() + model.getTransferCode();
//
//                createDate = conversionDateLogic.fromTimestampToYMDFormat(DateUtil.toTimestamp(model.getCreateDatetime()));
//                updateDate = conversionDateLogic.fromTimestampToYMDFormat(DateUtil.toTimestamp(model.getUpdateDatetime()));
//
//                writeRec.setLength(0);
//
//                // テキストファイルへ出力
//                writeRec.append(pm.getSysOwnerCd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getItemno());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSupplier());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUsercd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getStartDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(indUser);
//                writeRec.append(DELIMITER);
//                writeRec.append(requiredQty);
//                writeRec.append(DELIMITER);
//                writeRec.append(shipQty);
//                writeRec.append(DELIMITER);
//                writeRec.append(transfer);
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountHeading());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountDetail());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getBudgetNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getCreateAuthor());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getReasonCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(createDate);
//                writeRec.append(DELIMITER);
//                writeRec.append(updateDate);
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getMrpDate());
//
//                fileWriter.println(writeRec);
//                pm.setLcTrnIrdTxtFileOutputCount(pm.getLcTrnIrdTxtFileOutputCount() + 1);
//            }
//        } finally {
//            batchUtilityLogic.closePrintWriter(fileWriter);
//        }
//    }
//
//    /**
//     * 独立所要量履歴 データ取得
//     *
//     * @param pm
//     * @return
//     * @throws Exception
//     */
//    private ResultSet getLcTrnIrdHisSet(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        // 独立所要量明細の検索SQL
//        sql += " SELECT sys_owner_cd                            ";
//        sql += "       ,itemno                                  ";
//        sql += "       ,supplier                                ";
//        sql += "       ,usercd                                  ";
//        sql += "       ,order_no                                ";
//        sql += "       ,start_date                              ";
//        sql += "       ,order_through_no                        "; // 2017-09-13 MOD
//        sql += "       ,order_through_no_source_flg             "; // 2017-09-13 MOD
//        sql += "       ,rd_class                                ";
//        sql += "       ,ind_user_class                          ";
//        sql += "       ,ind_user_code                           ";
//        sql += "       ,rls_start_date                          ";
//        sql += "       ,order_status                            ";
//        sql += "       ,reason_code                             ";
//        sql += "       ,fixed_ymd                               ";
//        sql += "       ,pilot_class                             ";
//        sql += "       ,pilot_condition_type                    ";
//        sql += "       ,required_qty                            ";
//        sql += "       ,remark                                  ";
//        sql += "       ,request_system_code                     ";
//        sql += "       ,group_receive_flg                       ";
//        sql += "       ,ship_qty                                ";
//        sql += "       ,bo_qty                                  ";
//        sql += "       ,delivery_card_status                    ";
//        sql += "       ,item_card_status                        ";
//        sql += "       ,ship_date                               ";
//        sql += "       ,transfer_class                          ";
//        sql += "       ,transfer_code                           ";
//        sql += "       ,transfer_reason_code                    ";
//        sql += "       ,account_heading                         ";
//        sql += "       ,account_detail                          ";
//        sql += "       ,budget_no                               ";
//        sql += "       ,account_code_sales                      ";
//        sql += "       ,sp_order_class                          ";
//        sql += "       ,sp_delivery_code                        ";
//        sql += "       ,sp_dealer_no                            ";
//        sql += "       ,sp_order_no                             ";
//        sql += "       ,operation_no                            ";
//        sql += "       ,operation_seq                           ";
//        sql += "       ,batch_status                            ";
//        sql += "       ,mrp_date                                ";
//        sql += "       ,delete_ymd                              ";
//        sql += "       ,register_user_name                      ";
//        sql += "       ,update_counter                          ";
//        sql += "       ,create_datetime                         ";
//        sql += "       ,create_author                           ";
//        sql += "       ,update_datetime                         ";
//        sql += "       ,update_author                           ";
//        sql += "       ,update_pgmid                            ";
//        sql += "  FROM lc_inp_ird                               ";// (レプリカ)独立所要量明細ワーク
//        sql += " WHERE mrp_control_class = ?                    ";
//        sql += "   AND sys_owner_cd      = ?                    ";
//        sql += "   AND order_status      = '9'                  "; // 9:完了
//        sql += "   AND TO_CHAR(update_datetime, 'YYYYMMDD') < ? "; // 履歴移動対象
//        sql += " ORDER BY 1,2,3,4,5,6                           ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = null;
//
//        sqlParams = new Object[] { pm.getMrpControlClass()
//                                  , pm.getSysOwnerCd()
//                                  , pm.getHistoryMoveDate()
//                                  };
//
//        // Select文の発行
//        return dao.getResult(sql, sqlParams);
//    }
//
//    /**
//     * 独立所要量履歴ファイル出力--> out2:
//     *
//     * @param outputFile
//     * @param pm
//     * @param result
//     * @throws Exception
//     */
//    private void lcTrnIrdHisTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, ResultSet result) throws Exception {
//
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(outputFile);
//
//        StringBuilder writeRec = new StringBuilder();
//
//        String requiredQty    = "";
//        String shipQty        = "";
//        String boQty          = "";
//
//        try {
//
//            Leaj0015LcTrnIrdHisModel model = null;
//            while (BatchUtils.hasNext(result)) {
//
//                //Leaj0015LcTrnIrdHisModel model = BatchUtils.<Leaj0015LcTrnIrdHisModel>convertResultModel(result, Leaj0015LcTrnIrdHisModel.class);
//                model = convertResultLcTrnIrdHisModel(result);
//
//                requiredQty = model.getRequiredQty();
//                shipQty     = model.getShipQty();
//                boQty       = model.getBoQty();
//
//                if (NumberUtil.toDouble(requiredQty) >= 0) {
//                    requiredQty = "+" + requiredQty;
//                }
//
//                if (NumberUtil.toDouble(shipQty) >= 0) {
//                    shipQty = "+" + shipQty;
//                }
//
//                if (NumberUtil.toDouble(boQty) >= 0) {
//                    boQty = "+" + boQty;
//                }
//
//                writeRec.setLength(0);
//
//                // テキストファイルへ出力
//                writeRec.append(pm.getSysOwnerCd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getItemno());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSupplier());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUsercd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getStartDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderThroughNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderThroughNoSourceFlg());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRdClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getIndUserClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getIndUserCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRlsStartDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getReasonCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getFixedYmd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getPilotClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getPilotConditionType());
//                writeRec.append(DELIMITER);
//                writeRec.append(requiredQty);
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRemark());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRequestSystemCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getGroupReceiveFlg());
//                writeRec.append(DELIMITER);
//                writeRec.append(shipQty);
//                writeRec.append(DELIMITER);
//                writeRec.append(boQty);
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getDeliveryCardStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getItemCardStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getShipDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getTransferClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getTransferCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getTransferReasonCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountHeading());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountDetail());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getBudgetNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountCodeSales());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpOrderClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpDeliveryCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpDealerNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpOrderNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOperationNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOperationSeq());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getBatchStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getMrpDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getDeleteYmd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRegisterUserName());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdateCounter());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getCreateDatetime());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getCreateAuthor());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdateDatetime());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdateAuthor());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdatePgmid());
//
//                fileWriter.println(writeRec);
//                pm.setLcTrnIrdHisTxtFileOutputCount(pm.getLcTrnIrdHisTxtFileOutputCount() + 1);
//            }
//        } finally {
//
//            batchUtilityLogic.closePrintWriter(fileWriter);
//        }
//    }
//
//    /**
//     * SCPLAN-IN 独立所要量 データ取得
//     *
//     * @param pm
//     * @return
//     * @throws Exception
//     */
//    private ResultSet getScplanInLcTrnIrdSet(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        // 独立所要量明細の検索SQL
//        sql += " SELECT a.sys_owner_cd                               ";
//        sql += "       ,a.itemno                                     ";
//        sql += "       ,a.supplier                                   ";
//        sql += "       ,a.usercd                                     ";
//        sql += "       ,a.order_no                                   ";
//        sql += "       ,a.start_date                                 ";
//        sql += "       ,a.pilot_class                                ";
//        sql += "       ,a.pilot_condition_type                       ";
//        sql += "       ,a.required_qty                               ";
//        sql += "       ,a.ship_qty                                   ";
//        sql += "       ,a.mrp_date                                   ";
//        sql += "       ,a.rd_class                                   ";
//        sql += "       ,a.order_through_no                           ";
//        sql += "       ,a.order_through_no_source_flg                ";
//        sql += "       ,b.times_divide_day                           "; // 2017-09-14 ADD
//        sql += "  FROM lc_inp_ird            a                       "; // (レプリカ)独立所要量明細ワーク
//        sql += "      ,lc_inp_itemmast_mrp   b                       "; // 2017-09-14 MOD
//        sql += " WHERE a.mrp_control_class = ?                       ";
//        sql += "   AND a.sys_owner_cd      = ?                       ";
//        sql += "   AND a.mrp_control_class = b.mrp_control_class     ";
//        sql += "   AND a.sys_owner_cd      = b.sys_owner_cd          ";
//        sql += "   AND a.itemno            = b.itemno                ";
//        sql += "   AND a.supplier          = b.supplier              ";
//        sql += "   AND a.usercd            = b.usercd                ";
//        sql += "   AND a.order_status     <> '9'                     "; // 未完了
//        sql += "   AND a.pilot_class      <> '4'                     "; // 単品生試は除く
//        sql += "   AND (a.rd_class in ('1','2','3','4')              "; // 1:先行生産 2:変動安全在庫 3:自動安全在庫 4:生試所要量調整
//        sql += "   OR  (a.rd_class not in ('1','2','3','4') and a.start_date >= ?))";
//        sql += " ORDER BY 1,2,3,4,5,6                                ";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                                          , pm.getSysOwnerCd()
//                                          , pm.getOrderDeleteDate()
//                                          };
//
//        // Select文の発行
//        return dao.getResult(sql, sqlParams);
//    }
//
//    /**
//     * SCPLAN-IN 独立所要量ファイル出力--> out3:
//     *
//     * @param outputFile
//     * @param pm
//     * @param result
//     * @throws Exception
//     */
//    private void scplanInLcTrnIrdTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, ResultSet result) throws Exception {
//
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(outputFile);
//
//        StringBuilder writeRec = new StringBuilder();
//
//        try {
//
//            Leaj0015ScplanInLcTrnIrdModel model = null;
//            while (BatchUtils.hasNext(result)) {
//
//                //Leaj0015ScplanInLcTrnIrdModel model = BatchUtils.<Leaj0015ScplanInLcTrnIrdModel>convertResultModel(result, Leaj0015ScplanInLcTrnIrdModel.class);
//                model = convertResultScplanInLcTrnIrdModel(result);
//
//                // シフト番号セット
//                setShiftNo(pm, model);
//
//                String safetyStockFlag;
//
//                // 安全在庫フラグのセット
//                if("2".equals(model.getRdClass())){
//                    safetyStockFlag = "1";
//                }else{
//                    safetyStockFlag = "0";
//                }
//
//                writeRec.setLength(0);
//
//                // テキストファイルへ出力
//                writeRec.append(padSpaseRight(pm.getScplanSysOwnerCd(), 2));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getItemno(),30));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getSupplier(), 4));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getUsercd(), 4));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getOrderNo(), 5));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getStartDate(), 8));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(pm.getScplanInShippingShiftNo(), 2));     // 2017-09-14 MOD
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getPilotClass(), 1));
//                writeRec.append(DELIMITER);
//                writeRec.append(decimalFormat(model.getRequiredQty()));
//                writeRec.append(DELIMITER);
//                writeRec.append(decimalFormat(model.getShipQty()));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getMrpDate(), 8));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(safetyStockFlag, 1));
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getOrderThroughNo(), 6));            // 2017-09-14 MOD
//                writeRec.append(DELIMITER);
//                writeRec.append(padSpaseRight(model.getOrderThroughNoSourceFlg(), 1));    // 2017-09-14 MOD
//
//                fileWriter.println(writeRec);
//                pm.setScplanInLcTrnIrdOutputCount(pm.getScplanInLcTrnIrdOutputCount() + 1);
//            }
//        } finally {
//
//            batchUtilityLogic.closePrintWriter(fileWriter);
//        }
//    }
//
//    /**
//     * シフト番号セット
//     */
//    private void setShiftNo(Leaj0015ParameterModel pm, Leaj0015ScplanInLcTrnIrdModel model) {
//
//        StringBuilder searchKey1     = new StringBuilder();
//        StringBuilder searchKey2     = new StringBuilder();
//        String manfDelvType    = "";
//        String shiftBoxNo1    = "";
//        String shiftBoxNo2    = "";
//        final String strReferenceKey = "****";
//
//        // 時間指示品目のとき
//        if (!"".equals(model.getTimesDivideDay().trim())) {
//
//            // 製造納入種類セット
//            if (model.getSupplier().equals(model.getUsercd())) {
//
//                manfDelvType = "MANF";
//            } else {
//
//                manfDelvType = "DELV";
//            }
//
//            // 時刻指定回数 | 製造／納入種類 | 使用者 で検索
//            searchKey1.setLength(0);
//            searchKey1.append(model.getTimesDivideDay());
//            searchKey1.append(DELIMITER);
//            searchKey1.append(manfDelvType);
//            searchKey1.append(DELIMITER);
//            searchKey1.append(model.getUsercd());
//
//            shiftBoxNo1    = pm.getMinValidShiftMap().get(searchKey1);
//
//            if(shiftBoxNo1 != null) {
//
//                pm.setScplanInShippingShiftNo(shiftBoxNo1);
//
//            } else {
//
//                // 時刻指定回数 | 製造／納入種類 | ****(ALL) で検索
//                searchKey2.setLength(0);
//                searchKey2.append(model.getTimesDivideDay());
//                searchKey2.append(DELIMITER);
//                searchKey2.append(manfDelvType);
//                searchKey2.append(DELIMITER);
//                searchKey2.append(strReferenceKey);
//
//                shiftBoxNo2    = pm.getMinValidShiftMap().get(searchKey2);
//
//                if(shiftBoxNo2 != null) {
//
//                    pm.setScplanInShippingShiftNo(shiftBoxNo2);
//
//                } else {
//
//                    pm.setScplanInShippingShiftNo(" ");
//                }
//            }
//        } else {
//
//            //時間指示しない品目のとき
//            pm.setScplanInShippingShiftNo(" ");
//        }
//    }
//
//    /**
//     * 独立所要量バイパスファイル出力
//     *
//     * @param pm
//     * @return
//     * @throws Exception
//     */
//    private ResultSet getLcWorkBypassIrdSet(Leaj0015ParameterModel pm) throws Exception {
//
//        String sql = "";
//
//        // 独立所要量明細の検索SQL
//        sql += " SELECT itemno                                    ";
//        sql += "       ,supplier                                  ";
//        sql += "       ,usercd                                    ";
//        sql += "       ,order_no                                  ";
//        sql += "       ,start_date                                ";
//        sql += "       ,order_through_no                          "; // 2017-09-14 MOD
//        sql += "       ,order_through_no_source_flg               "; // 2017-09-14 MOD
//        sql += "       ,rd_class                                  ";
//        sql += "       ,ind_user_class                            ";
//        sql += "       ,ind_user_code                             ";
//        sql += "       ,rls_start_date                            ";
//        sql += "       ,order_status                              ";
//        sql += "       ,reason_code                               ";
//        sql += "       ,fixed_ymd                                 ";
//        sql += "       ,pilot_class                               ";
//        sql += "       ,pilot_condition_type                      ";
//        sql += "       ,required_qty                              ";
//        sql += "       ,remark                                    ";
//        sql += "       ,request_system_code                       ";
//        sql += "       ,group_receive_flg                         ";
//        sql += "       ,ship_qty                                  ";
//        sql += "       ,bo_qty                                    ";
//        sql += "       ,delivery_card_status                      ";
//        sql += "       ,item_card_status                          ";
//        sql += "       ,ship_date                                 ";
//        sql += "       ,transfer_class                            ";
//        sql += "       ,transfer_code                             ";
//        sql += "       ,transfer_reason_code                      ";
//        sql += "       ,account_heading                           ";
//        sql += "       ,account_detail                            ";
//        sql += "       ,budget_no                                 ";
//        sql += "       ,account_code_sales                        ";
//        sql += "       ,sp_order_class                            ";
//        sql += "       ,sp_delivery_code                          ";
//        sql += "       ,sp_dealer_no                              ";
//        sql += "       ,sp_order_no                               ";
//        sql += "       ,operation_no                              ";
//        sql += "       ,operation_seq                             ";
//        sql += "       ,batch_status                              ";
//        sql += "       ,mrp_date                                  ";
//        sql += "       ,delete_ymd                                ";
//        sql += "       ,register_user_name                        ";
//        sql += "       ,update_counter                            ";
//        sql += "       ,create_datetime                           ";
//        sql += "       ,create_author                             ";
//        sql += "       ,update_datetime                           ";
//        sql += "       ,update_author                             ";
//        sql += "       ,update_pgmid                              ";
//        sql += "  FROM lc_inp_ird                                 "; // (レプリカ)独立所要量明細ワーク
//        sql += " WHERE mrp_control_class = ?                      ";
//        sql += "   AND sys_owner_cd      = ?                      ";
//
//        sql += "   AND ((order_status  = '9' and TO_CHAR(update_datetime, 'YYYYMMDD') >= ?)    "; //履歴移動対象でない完了IRD
//        sql += "    OR  (order_status <> '9'                                                   "; //発注取消対象でない未完了IRD
//        sql += "         and ((pilot_class  = '4' and start_date >= ?)                         ";
//        sql += "          or  (pilot_class <> '4'                                              ";
//        sql += "               and (rd_class     in ('1','3','4')                              ";
//        sql += "                or (rd_class not in ('1','2','3','4') and start_date >= ?))))))";
//
//        //SQLパラメータの生成
//        Object[] sqlParams = null;
//
//        sqlParams = new Object[] { pm.getMrpControlClass()
//                                  , pm.getSysOwnerCd()
//                                  , pm.getHistoryMoveDate()
//                                  , pm.getOrderDeleteDate()
//                                  , pm.getOrderDeleteDate()
//                                  };
//
//        // Select文の発行
//        return dao.getResult(sql, sqlParams);
//    }
//
//    /**
//     * 独立所要量バイパスファイル出力--> out4:
//     *
//     * @param outputFile
//     * @param pm
//     * @param result
//     * @throws Exception
//     */
//    private void lcWorkBypassIrdTxtFileOutputProcess(String outputFile, Leaj0015ParameterModel pm, ResultSet result) throws Exception {
//
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(outputFile);
//
//        StringBuilder writeRec = new StringBuilder();
//
//
//        try {
//
//            Leaj0015LcWorkBypassIrdModel model = null;
//            while (BatchUtils.hasNext(result)) {
//
//                //Leaj0015LcWorkBypassIrdModel model = BatchUtils.<Leaj0015LcWorkBypassIrdModel>convertResultModel(result, Leaj0015LcWorkBypassIrdModel.class);
//                model = convertResultLcWorkBypassIrdModel(result);
//
//                writeRec.setLength(0);
//
//                // テキストファイルへ出力
//                writeRec.append(pm.getMrpControlClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(pm.getScplanSysOwnerCd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getItemno());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSupplier());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUsercd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getStartDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderThroughNo());                 // 2017-09-14 MOD
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderThroughNoSourceFlg());    // 2017-09-14 MOD
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRdClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getIndUserClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getIndUserCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRlsStartDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOrderStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getReasonCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getFixedYmd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getPilotClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getPilotConditionType());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRequiredQty());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRemark());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRequestSystemCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getGroupReceiveFlg());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getShipQty());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getBoQty());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getDeliveryCardStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getItemCardStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getShipDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getTransferClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getTransferCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getTransferReasonCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountHeading());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountDetail());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getBudgetNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getAccountCodeSales());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpOrderClass());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpDeliveryCode());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpDealerNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getSpOrderNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOperationNo());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getOperationSeq());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getBatchStatus());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getMrpDate());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getDeleteYmd());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getRegisterUserName());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdateCounter());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getCreateDatetime());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getCreateAuthor());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdateDatetime());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdateAuthor());
//                writeRec.append(DELIMITER);
//                writeRec.append(model.getUpdatePgmid());
//
//                fileWriter.println(writeRec);
//                pm.setLcWorkBypassIrdOutputCount(pm.getLcWorkBypassIrdOutputCount() + 1);
//            }
//        } finally {
//
//            batchUtilityLogic.closePrintWriter(fileWriter);
//        }
//    }
//
//    /**
//     * format(スペース右埋め)
//     * StringUtils.padCharRight()を少し変更してある。(Null,ブランクの場合でも文字埋めされるように)
//     */
//    public String padSpaseRight(String str, int len) {
//        char padChar = ' ';
//
//        if(str == null) str = "";
//        if(str.length() >= len)
//            return str;
//        StringBuffer sb = new StringBuffer(str);
//        int max = len - str.length();
//        for(int i = 0; i < max; i++)
//        {
//            str = (new StringBuilder()).append(padChar).append(str).toString();
//            sb.append(padChar);
//        }
//
//        return sb.toString();
//    }
//
//    /**
//     * BigDecimal Format
//     * @param str
//     * @return qty format
//     */
//    public String decimalFormat(String str) {
//
//        DecimalFormat df = new DecimalFormat("+000000000.00000;-000000000.00000");
//        return df.format(new BigDecimal(str));
//    }
//
//    /**
//     * LcTrnIrdHisの返却値をセットする
//     * @param result
//     * @return Leaj0015LcTrnIrdHisModel
//     */
//    public Leaj0015LcTrnIrdHisModel convertResultLcTrnIrdHisModel(ResultSet result) throws Exception{
//
//        Leaj0015LcTrnIrdHisModel model = new Leaj0015LcTrnIrdHisModel();
//
//        model.setSysOwnerCd(result.getString(1));
//        model.setItemno(result.getString(2));
//        model.setSupplier(result.getString(3));
//        model.setUsercd(result.getString(4));
//        model.setOrderNo(result.getString(5));
//        model.setStartDate(result.getString(6));
//        model.setOrderThroughNo(result.getString(7));
//        model.setOrderThroughNoSourceFlg(result.getString(8));
//        model.setRdClass(result.getString(9));
//        model.setIndUserClass(result.getString(10));
//        model.setIndUserCode(result.getString(11));
//        model.setRlsStartDate(result.getString(12));
//        model.setOrderStatus(result.getString(13));
//        model.setReasonCode(result.getString(14));
//        model.setFixedYmd(result.getString(15));
//        model.setPilotClass(result.getString(16));
//        model.setPilotConditionType(result.getString(17));
//        model.setRequiredQty(result.getString(18));
//        model.setRemark(result.getString(19));
//        model.setRequestSystemCode(result.getString(20));
//        model.setGroupReceiveFlg(result.getString(21));
//        model.setShipQty(result.getString(22));
//        model.setBoQty(result.getString(23));
//        model.setDeliveryCardStatus(result.getString(24));
//        model.setItemCardStatus(result.getString(25));
//        model.setShipDate(result.getString(26));
//        model.setTransferClass(result.getString(27));
//        model.setTransferCode(result.getString(28));
//        model.setTransferReasonCode(result.getString(29));
//        model.setAccountHeading(result.getString(30));
//        model.setAccountDetail(result.getString(31));
//        model.setBudgetNo(result.getString(32));
//        model.setAccountCodeSales(result.getString(33));
//        model.setSpOrderClass(result.getString(34));
//        model.setSpDeliveryCode(result.getString(35));
//        model.setSpDealerNo(result.getString(36));
//        model.setSpOrderNo(result.getString(37));
//        model.setOperationNo(result.getString(38));
//        model.setOperationSeq(result.getString(39));
//        model.setBatchStatus(result.getString(40));
//        model.setMrpDate(result.getString(41));
//        model.setDeleteYmd(result.getString(42));
//        model.setRegisterUserName(result.getString(43));
//        model.setUpdateCounter(result.getLong(44));
//        model.setCreateDatetime(result.getTimestamp(45));
//        model.setCreateAuthor(result.getString(46));
//        model.setUpdateDatetime(result.getTimestamp(47));
//        model.setUpdateAuthor(result.getString(48));
//        model.setUpdatePgmid(result.getString(49));
//
//        return model;
//    }
//
//    /**
//     * LcTrnIrdModelの返却値をセットする
//     * @param result
//     * @return Leaj0015LcTrnIrdModel
//     */
//    public Leaj0015LcTrnIrdModel convertResultLcTrnIrdModel(ResultSet result) throws Exception{
//
//        Leaj0015LcTrnIrdModel model = new Leaj0015LcTrnIrdModel();
//
//        model.setItemno(result.getString(1));
//        model.setSupplier(result.getString(2));
//        model.setUsercd(result.getString(3));
//        model.setOrderNo(result.getString(4));
//        model.setStartDate(result.getString(5));
//        model.setIndUserClass(result.getString(6));
//        model.setIndUserCode(result.getString(7));
//        model.setRequiredQty(result.getString(8));
//        model.setShipQty(result.getString(9));
//        model.setTransferClass(result.getString(10));
//        model.setTransferCode(result.getString(11));
//        model.setAccountHeading(result.getString(12));
//        model.setAccountDetail(result.getString(13));
//        model.setBudgetNo(result.getString(14));
//        model.setCreateAuthor(result.getString(15));
//        model.setReasonCode(result.getString(16));
//        model.setCreateDatetime(result.getString(17));
//        model.setUpdateDatetime(result.getString(18));
//        model.setMrpDate(result.getString(19));
//
//        return model;
//    }
//
//    /**
//     * LcWorkBypassIrdの返却値をセットする
//     * @param result
//     * @return Leaj0015LcWorkBypassIrdModel
//     */
//    public Leaj0015LcWorkBypassIrdModel convertResultLcWorkBypassIrdModel(ResultSet result) throws Exception{
//
//        Leaj0015LcWorkBypassIrdModel model = new Leaj0015LcWorkBypassIrdModel();
//
//        model.setItemno(result.getString(1));
//        model.setSupplier(result.getString(2));
//        model.setUsercd(result.getString(3));
//        model.setOrderNo(result.getString(4));
//        model.setStartDate(result.getString(5));
//        model.setOrderThroughNo(result.getString(6));
//        model.setOrderThroughNoSourceFlg(result.getString(7));
//        model.setRdClass(result.getString(8));
//        model.setIndUserClass(result.getString(9));
//        model.setIndUserCode(result.getString(10));
//        model.setRlsStartDate(result.getString(11));
//        model.setOrderStatus(result.getString(12));
//        model.setReasonCode(result.getString(13));
//        model.setFixedYmd(result.getString(14));
//        model.setPilotClass(result.getString(15));
//        model.setPilotConditionType(result.getString(16));
//        model.setRequiredQty(result.getBigDecimal(17));
//        model.setRemark(result.getString(18));
//        model.setRequestSystemCode(result.getString(19));
//        model.setGroupReceiveFlg(result.getString(20));
//        model.setShipQty(result.getBigDecimal(21));
//        model.setBoQty(result.getBigDecimal(22));
//        model.setDeliveryCardStatus(result.getString(23));
//        model.setItemCardStatus(result.getString(24));
//        model.setShipDate(result.getString(25));
//        model.setTransferClass(result.getString(26));
//        model.setTransferCode(result.getString(27));
//        model.setTransferReasonCode(result.getString(28));
//        model.setAccountHeading(result.getString(29));
//        model.setAccountDetail(result.getString(30));
//        model.setBudgetNo(result.getString(31));
//        model.setAccountCodeSales(result.getString(32));
//        model.setSpOrderClass(result.getString(33));
//        model.setSpDeliveryCode(result.getString(34));
//        model.setSpDealerNo(result.getString(35));
//        model.setSpOrderNo(result.getString(36));
//        model.setOperationNo(Integer.valueOf(result.getString(37)));
//        model.setOperationSeq(Integer.valueOf(result.getString(38)));
//        model.setBatchStatus(result.getString(39));
//        model.setMrpDate(result.getString(40));
//        model.setDeleteYmd(result.getString(41));
//        model.setRegisterUserName(result.getString(42));
//        model.setUpdateCounter(result.getLong(43));
//        model.setCreateDatetime(result.getTimestamp(44));
//        model.setCreateAuthor(result.getString(45));
//        model.setUpdateDatetime(result.getTimestamp(46));
//        model.setUpdateAuthor(result.getString(47));
//        model.setUpdatePgmid(result.getString(48));
//
//        return model;
//    }
//
//    /**
//     * ScplanInLcTrnIrdの返却値をセットする
//     * @param result
//     * @return Leaj0015ScplanInLcTrnIrdModel
//     */
//    public Leaj0015ScplanInLcTrnIrdModel convertResultScplanInLcTrnIrdModel(ResultSet result) throws Exception{
//
//        Leaj0015ScplanInLcTrnIrdModel model = new Leaj0015ScplanInLcTrnIrdModel();
//
//        // sql=sys_owner_cd
//        model.setItemno(result.getString(2));
//        model.setSupplier(result.getString(3));
//        model.setUsercd(result.getString(4));
//        model.setOrderNo(result.getString(5));
//        model.setStartDate(result.getString(6));
//        model.setPilotClass(result.getString(7));
//        model.setRequiredQty(result.getString(9));
//        // sql=pilot_condition_type
//        model.setShipQty(result.getString(10));
//        model.setMrpDate(result.getString(11));
//        model.setRdClass(result.getString(12));
//        model.setOrderThroughNo(result.getString(13));            // 2017-09-14 MOD
//        model.setOrderThroughNoSourceFlg(result.getString(14));    // 2017-09-14 MOD
//        model.setTimesDivideDay(result.getString(15));            // 2017-09-14 ADD
//
//        return model;
//    }
}
