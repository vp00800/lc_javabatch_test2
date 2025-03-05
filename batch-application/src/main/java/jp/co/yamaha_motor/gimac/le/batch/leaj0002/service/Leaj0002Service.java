package jp.co.yamaha_motor.gimac.le.batch.leaj0002.service;

import com.ymsl.solid.base.util.StringUtils;
import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0002.model.Leaj0002MstCalendarModel;
import jp.co.yamaha_motor.gimac.le.batch.leaj0002.model.Leaj0002ParameterModel;
import jp.co.yamaha_motor.gimac.le.batch.leaj0008.service.Leaj0008Service;
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
 * LCRJ0002
 * ＜ 稼動日カレンダー取得 ＞
 *
 * カレンダーマスターから、稼動日カレンダー情報を取得し、テキスト化する
 *
 * @author  YMSLx Tao Xiaochuan
 * @version 1.0.0
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 *
 * MODIFICATION HISTORY
 *  (Ver)  (Date)     (Name)              (Comment)
 *  1.0.0  2025/03/03 YMSLx Tao Xiaochuan New making
 */

@Service
@Slf4j
public class Leaj0002Service {

    //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Add)
    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataSource dataSource;

    public void start(Map<String, Object> map) throws Exception{
        Leaj0002ParameterModel pm = new Leaj0002ParameterModel();
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
    protected void setParameter(Leaj0002ParameterModel pm, Map<String, Object> map) throws Exception {

        //---------------- 2025/03/03 Tao Xiaochuan g3 Start(Modify)
//        if ( "arg1".equals(name) ) {
//            // MRP管理区分
//            pm.setMrpControlClass(value);
//        }
//        if ( "outf".equals(name) ) {
//            // 稼動日カレンダーファイル
//            pm.setCalendarFile(value);
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
            // 稼動日カレンダーファイル
            pm.setCalendarFile(map.get("out1").toString());
        }
        //---------------- 2025/03/03 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * 引数取得後処理
     */
    protected void prepareParameterAfter(Leaj0002ParameterModel pm) throws Exception {

        //---------------- 2025/03/03 Tao Xiaochuan g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        //引数必須チェック
//        if ( CheckUtil.isEmptyBlank(pm.getMrpControlClass()) ) {
//            throw new BatchApplicationException("Argument error [arg1:mrpControlClass] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getCalendarFile()) ) {
//            throw new BatchApplicationException("Argument error [outf:calendarFile] is blank");
//        }
//
//        // 引数内容ログ出力
//        writer.writeLog("", "[Argument Value]", null, null, null);
//        writer.writeLog("", " sysOwnerCd      : " + pm.getSysOwnerCd(),      null, null, null);
//        writer.writeLog("", " mrpControlClass : " + pm.getMrpControlClass(), null, null, null);
//        writer.writeLog("", " calendarFile    : " + pm.getCalendarFile(),    null, null, null);
        //---------------- 2025/03/03 Tao Xiaochuan g3 End(Modify)

        Logger logger = LoggerFactory.getLogger(Leaj0008Service.class);

        // 引数必須チェック
        if ( StringUtils.isBlankText(pm.getMrpControlClass()) ) {
            throw new JobParametersInvalidException("Argument error [arg1:mrpControlClass] is blank");
        }
        if ( StringUtils.isBlankText(pm.getCalendarFile()) ) {
            throw new JobParametersInvalidException("Argument error [outf:calendarFile] is blank");
        }

        // 引数内容ログ出力
        logger.info("[Argument Value]");
        logger.info("sysOwnerCd      : {}", pm.getSysOwnerCd());
        logger.info("mrpControlClass : {}", pm.getMrpControlClass());
        logger.info("calendarFile    : {}", pm.getCalendarFile());
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * 初期処理
     */
    protected void init(Leaj0002ParameterModel pm) throws Exception {

        // カウンタ変数の初期化
        pm.setOutputDataCount(0);

        // SCPLANシステムオーナーコードの取得
        selectScplanSysOwnerCd(pm);
    }

    /**
     * 主処理
     */
    protected void main(Leaj0002ParameterModel pm) throws Exception {

        //---------------- 2025/03/03 Tao Xiaochuan g3 Start(Modify)
//        String sql = "";
//
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(pm.getCalendarFile());
//
//        try {
//            //カレンダーマスタの検索SQL
//            sql += " SELECT calendar_code         ";
//            sql += "      , calendar_ym           ";
//            sql += "      , calendar_data         ";
//            sql += "   FROM lc_inp_calendar       ";
//            sql += "  WHERE mrp_control_class = ? ";
//            sql += "    AND sys_owner_cd      = ? ";
//            sql += " ORDER BY  calendar_code      ";
//            sql += "         , calendar_ym        ";
//
//            //SQLパラメータの生成
//            Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                    , pm.getSysOwnerCd()
//            };
//
//            // Select文の発行
//            ResultSet result =  dao.getResult(sql, sqlParams);
//
//            try {
//                while (BatchUtils.hasNext(result)) {
//
//                    // ResultSetをFetchしてデータを取り出す
//                    Lcrj0002MstCalendarModel model = BatchUtils.<Lcrj0002MstCalendarModel>convertResultModel(result, Lcrj0002MstCalendarModel.class);
//
//                    pm.setCntMstCalendarSel(pm.getCntMstCalendarSel() + 1);
//
//                    // テキストファイルへ出力
//                    String writeRec = padCharRight(pm.getScplanSysOwnerCd(), 2, ' ')
//                            + "|"
//                            + padCharRight(model.getCalendarCode(),  2, ' ')
//                            + "|"
//                            + padCharRight(model.getCalendarYm(),    6, ' ')
//                            + "|"
//                            + padCharRight(model.getCalendarData(), 31, ' ')
//                            ;
//
//                    fileWriter.println(writeRec);
//                    pm.setOutputDataCount(pm.getOutputDataCount() + 1);
//                }
//            } finally {
//                result.close();
//            }
//        } finally {
//            batchUtilityLogic.closePrintWriter(fileWriter);
//        }
        String sql = "";

        //カレンダーマスタの検索SQL
        sql += " SELECT calendar_code         ";
        sql += "      , calendar_ym           ";
        sql += "      , calendar_data         ";
        sql += "   FROM lc_inp_calendar       ";
        sql += "  WHERE mrp_control_class = ? ";
        sql += "    AND sys_owner_cd      = ? ";
        sql += " ORDER BY  calendar_code      ";
        sql += "         , calendar_ym        ";

        try (Connection conn = dataSource.getConnection()) {
            // SQLパラメータの生成
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, (String) pm.getMrpControlClass());
                pstmt.setString(2, (String) pm.getSysOwnerCd());

                // Select文の発行
                try (ResultSet result = pstmt.executeQuery()) {

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(pm.getCalendarFile(), false))) {
                        Leaj0002MstCalendarModel model = null;

                        while (result.next()) {

                            // ResultSetをFetchしてデータを取り出す
                            model = new Leaj0002MstCalendarModel();
                            model.setCalendarCode(result.getString(1));
                            model.setCalendarYm(result.getString(2));
                            model.setCalendarData(result.getString(3));

                            pm.setCntMstCalendarSel(pm.getCntMstCalendarSel() + 1);

                            // テキストファイルへ出力
                            String writeRec = padCharRight(pm.getScplanSysOwnerCd(), 2, ' ')
                                    + "|"
                                    + padCharRight(model.getCalendarCode(), 2, ' ')
                                    + "|"
                                    + padCharRight(model.getCalendarYm(), 6, ' ')
                                    + "|"
                                    + padCharRight(model.getCalendarData(), 31, ' ');

                            writer.write(writeRec);
                            writer.newLine();

                            pm.setOutputDataCount(pm.getOutputDataCount() + 1);
                        }
                    }
                }
            }
        }
        //---------------- 2025/03/03 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * 後処理
     */
    protected void term(Leaj0002ParameterModel pm) throws Exception {

        //---------------- 2025/03/03 Tao Xiaochuan g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        writer.writeLog("", "[lc_inp_calendar]  Table Select Count = " + pm.getCntMstCalendarSel(), null, null, null);
//        writer.writeLog("", "calendarFile Output File  Write Count = " + pm.getOutputDataCount(),   null, null, null);

        Logger logger = LoggerFactory.getLogger(Leaj0008Service.class);

        logger.info("[lc_inp_calendar]  Table Select Count = {}", pm.getCntMstCalendarSel());
        logger.info("calendarFile Output File  Write Count = {}", pm.getOutputDataCount());
        //---------------- 2025/03/03 Tao Xiaochuan g3 End(Modify)
    }


    /**
     * SCPLAN用システムオーナーコード取得
     *
     * @param pm
     * @throws Exception
     */
    private void selectScplanSysOwnerCd(Leaj0002ParameterModel pm) throws Exception {

        //SCPLAN用システムオーナーコードの取得SQL
        String  sql  = " SELECT scplan_sys_owner_cd  ";
        sql += "   FROM lc_system_parameter  ";
        sql += "  WHERE sys_owner_cd = ?     ";
        sql += "    AND system_code  = 'LC'  ";

        //---------------- 2025/03/03 Tao Xiaochuan g3 Start(Modify)
        // Select文の発行
//        Object[] obj = dao.getRecord(sql, new Object[] {pm.getSysOwnerCd()});
//
//        if (obj == null) {
//            //該当データが存在しない場合エラーとする
//            String msg	= " [lc_system_parameter] No Data Found Error."
//                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
//            throw new BatchApplicationException(msg);
//        }else{
//            pm.setScplanSysOwnerCd(StringUtil.toString(obj[0]));
//        }

        String scplanSysOwnerCd = "";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, (String) pm.getSysOwnerCd());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    scplanSysOwnerCd = rs.getString("scplan_sys_owner_cd");
                }
            }
        }

        if (StringUtils.isBlankText(scplanSysOwnerCd)) {
            //該当データが存在しない場合エラーとする
            String msg	= " [lc_system_parameter] No Data Found Error."
                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
            throw new JobParametersInvalidException(msg);
        }else{
            pm.setScplanSysOwnerCd(scplanSysOwnerCd);
        }
        //---------------- 2025/03/03 Tao Xiaochuan g3 End(Modify)
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
    public String padCharRight(String str, int len, char padChar){
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


}
