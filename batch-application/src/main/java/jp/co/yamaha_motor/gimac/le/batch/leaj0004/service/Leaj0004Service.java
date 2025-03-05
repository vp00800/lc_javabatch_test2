package jp.co.yamaha_motor.gimac.le.batch.leaj0004.service;

import com.ymsl.solid.base.util.StringUtils;
import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0001.service.Leaj0001Service;
import jp.co.yamaha_motor.gimac.le.batch.leaj0004.model.Leaj0004MstDelivStdDayModel;
import jp.co.yamaha_motor.gimac.le.batch.leaj0004.model.Leaj0004ParameterModel;
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
 * LEAJ0005
 * ＜ 納入基準日取得 ＞
 *
 * 納入基準日の情報をテキスト化する
 *
 * @author  YMSL R.Mochizuki
 * @version 1.0.0
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 *
 * MODIFICATION HISTORY
 *  (Ver) (Date)          (Name)      (Comment)
 *  1.0.0 2025/03/04 YMSL R.Mochizuki New making
 */

@Service
@Slf4j
public class Leaj0004Service {

    //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Add)
    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataSource dataSource;

    public void start(Map<String, Object> map) throws Exception{
        Leaj0004ParameterModel pm = new Leaj0004ParameterModel();
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
    protected void setParameter(Leaj0004ParameterModel pm, Map<String, Object> map) throws Exception {

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        if ( "arg1".equals(name) ) {
//            // MRP管理区分
//            pm.setMrpControlClass(value);
//        }
//        if ( "outf".equals(name) ) {
//            // 納入基準日ファイル
//            pm.setDelivStdDayFile(value);
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
            // 納入基準日ファイル
            pm.setDelivStdDayFile(map.get("out1").toString());
        }
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }

    /**
     * 引数取得後処理
     */
    protected void prepareParameterAfter(Leaj0004ParameterModel pm) throws Exception {

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        //引数必須チェック
//        if ( CheckUtil.isEmptyBlank(pm.getMrpControlClass()) ) {
//            throw new BatchApplicationException("Argument error [arg1:mrpControlClass] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getDelivStdDayFile()) ) {
//            throw new BatchApplicationException("Argument error [outf:delivStdDayFile] is blank");
//        }
//
//        // 引数内容ログ出力
//        writer.writeLog("", "[Argument Value]", null, null, null);
//        writer.writeLog("", " sysOwnerCd      : " + pm.getSysOwnerCd(),      null, null, null);
//        writer.writeLog("", " mrpControlClass : " + pm.getMrpControlClass(), null, null, null);
//        writer.writeLog("", " delivStdDayFile : " + pm.getDelivStdDayFile(), null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0001Service.class);

        // 引数必須チェック
        if ( StringUtils.isBlankText(pm.getMrpControlClass()) ) {
            throw new JobParametersInvalidException("Argument error [arg1:mrpControlClass] is blank");
        }
        if ( StringUtils.isBlankText(pm.getDelivStdDayFile()) ) {
            throw new JobParametersInvalidException("Argument error [out1:delivStdDayFile] is blank");
        }

        // 引数内容ログ出力
        logger.info("[Argument Value]");
        logger.info("sysOwnerCd      : {}", pm.getSysOwnerCd());
        logger.info("mrpControlClass : {}", pm.getMrpControlClass());
        logger.info("delivStdDayFile : {}", pm.getDelivStdDayFile());
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }

    /**
     * 初期処理
     */
    protected void init(Leaj0004ParameterModel pm) throws Exception {

        // カウンタ変数の初期化
        pm.setOutputDataCount(0);

        // SCPLANシステムオーナーコードの取得
        selectScplanSysOwnerCd(pm);
    }

    /**
     * 主処理
     */
    protected void main(Leaj0004ParameterModel pm) throws Exception {

        String sql      = "";
        String writeRec = "";

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(pm.getDelivStdDayFile());
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

        try {
            //納入基準日の検索SQL
            sql += " SELECT supplier                 ";
            sql += "      , deliv_std_day            ";
            sql += "      , deliv_std_class          ";
            sql += "   FROM lc_inp_deliv_std_day     ";
            sql += "  WHERE mrp_control_class = ?    ";
            sql += "    AND sys_owner_cd      = ?    ";
            sql += "  ORDER BY supplier              ";
            sql += "         , deliv_std_day         ";

            //SQLパラメータの生成

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
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
//                    Leaj0004MstDelivStdDayModel model = BatchUtils.<Leaj0004MstDelivStdDayModel>convertResultModel(result, Leaj0004MstDelivStdDayModel.class);
//
//
//                    // テキストファイルへ出力
//                    writeRec = padCharRight(pm.getScplanSysOwnerCd(), 	 2, ' ')
//                            + "|"
//                            + padCharRight(model.getSupplier(),      	 4, ' ')
//                            + "|"
//                            + padCharRight(model.getDelivStdDay(),   	 3, ' ')
//                            + "|"
//                            + padCharRight(model.getDelivStdClass(), 	 1, ' ')
//                    ;
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
            try (Connection conn = dataSource.getConnection()) {
                // SQLパラメータの生成
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, (String) pm.getMrpControlClass());
                    pstmt.setString(2, (String) pm.getSysOwnerCd());

                    // Select文の発行
                    try (ResultSet result = pstmt.executeQuery()) {

                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pm.getDelivStdDayFile(), false))) {
                            Leaj0004MstDelivStdDayModel model = null;

                            while (result.next()) {

                                // ResultSetをFetchしてデータを取り出す
                                model = new Leaj0004MstDelivStdDayModel();
                                model.setScplanSysOwnerCd(result.getString(1));
                                model.setSupplier(result.getString(2));
                                model.setDelivStdDay(result.getString(3));
                                model.setDelivStdClass(result.getString(4));

                                // テキストファイルへ出力
                                writeRec = padCharRight(pm.getScplanSysOwnerCd(), 2, ' ')
                                        + "|"
                                        + padCharRight(model.getSupplier(), 4, ' ')
                                        + "|"
                                        + padCharRight(model.getDelivStdDay(), 3, ' ')
                                        + "|"
                                        + padCharRight(model.getDelivStdClass(), 1, ' ')
                                ;

                                writer.write(writeRec);
                                writer.newLine();

                                pm.setOutputDataCount(pm.getOutputDataCount() + 1);
                            }
                        }
                    }
                }
            }
        } finally {
        }
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }

    /**
     * 後処理
     */
    protected void term(Leaj0004ParameterModel pm) throws Exception {

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        writer.writeLog("", "delivStdDayFile Output File Write Count = " + pm.getOutputDataCount(), null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0008Service.class);

        logger.info("delivStdDayFile Output File Write Count = {}", pm.getOutputDataCount());
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }


    /**
     * SCPLAN用システムオーナーコード取得
     *
     * @param pm
     * @throws Exception
     */
    private void selectScplanSysOwnerCd(Leaj0004ParameterModel pm) throws Exception {

        //SCPLAN用システムオーナーコードの取得SQL
        String	sql  = " SELECT scplan_sys_owner_cd  ";
        sql += "   FROM lc_system_parameter  ";
        sql += "  WHERE sys_owner_cd = ?     ";
        sql += "    AND system_code  = 'LC'  ";

        // Select文の発行

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        Object[] obj = dao.getRecord(sql, new Object[] {pm.getSysOwnerCd()});
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
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        if (obj == null) {
//            //該当データが存在しない場合エラーとする
//            String msg	= " [lc_system_parameter] No Data Found Error."
//                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
//            throw new BatchApplicationException(msg);
//        }else{
//            pm.setScplanSysOwnerCd(StringUtil.toString(obj[0]));
//        }
        if (StringUtils.isBlankText(scplanSysOwnerCd)) {
            //該当データが存在しない場合エラーとする
            String msg	= " [lc_system_parameter] No Data Found Error."
                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
            throw new JobParametersInvalidException(msg);
        }else{
            pm.setScplanSysOwnerCd(scplanSysOwnerCd);
        }
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

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
    public String padCharRight(String str, int len, char padChar)
    {
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