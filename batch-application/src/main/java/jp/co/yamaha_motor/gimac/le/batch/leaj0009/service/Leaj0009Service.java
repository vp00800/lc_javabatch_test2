package jp.co.yamaha_motor.gimac.le.batch.leaj0009.service;

import com.ymsl.solid.base.util.StringUtils;
import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0001.service.Leaj0001Service;
import jp.co.yamaha_motor.gimac.le.batch.leaj0008.service.Leaj0008Service;
import jp.co.yamaha_motor.gimac.le.batch.leaj0009.model.Leaj0009LaItemmastMrpModel;
import jp.co.yamaha_motor.gimac.le.batch.leaj0009.model.Leaj0009ParameterModel;
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

import static com.ymsl.solid.base.util.StringUtils.padCharRight;

/**
 * LEAJ0009
 * ＜ フルペグ対象品目抽出 ＞
 *
 * MRP情報値テーブルから、フルペグ出力サイン「1」（出力する）の品目を抽出し、テキスト出力する
 *
 * @author  YMSL R.Mochizuki
 * @version 1.0.0
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 *
 * MODIFICATION HISTORY
 *  (Ver) (Date)     (Name)           (Comment)
 *  1.0.0 2025/03/04 YMSL R.Mochizuki New making
 */

@Service
@Slf4j
public class Leaj0009Service {

    //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Add)
    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataSource dataSource;

    public void start(Map<String, Object> map) throws Exception{
        Leaj0009ParameterModel pm = new Leaj0009ParameterModel();
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
    protected void setParameter(Leaj0009ParameterModel pm, Map<String, Object> map) throws Exception {

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        if ( "arg1".equals(name) ) {
//            // MRP管理区分
//            pm.setMrpControlClass(value);
//        }
//        if ( "outf".equals(name) ) {
//            // フルペグ対象品目ファイル
//            pm.setFullPegItemFile(value);
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
            // フルペグ対象品目ファイル
            pm.setFullPegItemFile(map.get("out1").toString());
        }
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }

    /**
     * 引数取得後処理
     */
    protected void prepareParameterAfter(Leaj0009ParameterModel pm) throws Exception {

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        //引数必須チェック
//        if ( CheckUtil.isEmptyBlank(pm.getMrpControlClass()) ) {
//            throw new BatchApplicationException("Argument error [arg1:mrpControlClass] is blank");
//        }
//        if ( CheckUtil.isEmptyBlank(pm.getFullPegItemFile()) ) {
//            throw new BatchApplicationException("Argument error [outf:fullPegItemFile] is blank");
//        }
//
//        // 引数内容ログ出力
//        writer.writeLog("", "[Argument Value]", null, null, null);
//        writer.writeLog("", " sysOwnerCd      : " + pm.getSysOwnerCd(),      null, null, null);
//        writer.writeLog("", " mrpControlClass : " + pm.getMrpControlClass(), null, null, null);
//        writer.writeLog("", " fullPegItemFile : " + pm.getFullPegItemFile(), null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0001Service.class);

        // 引数必須チェック
        if ( StringUtils.isBlankText(pm.getMrpControlClass()) ) {
            throw new JobParametersInvalidException("Argument error [arg1:mrpControlClass] is blank");
        }
        if ( StringUtils.isBlankText(pm.getFullPegItemFile()) ) {
            throw new JobParametersInvalidException("Argument error [out1:fullPegItemFile] is blank");
        }

        // 引数内容ログ出力
        logger.info("[Argument Value]");
        logger.info("sysOwnerCd      : {}", pm.getSysOwnerCd());
        logger.info("mrpControlClass : {}", pm.getMrpControlClass());
        logger.info("fullPegItemFile : {}", pm.getFullPegItemFile());
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }

    /**
     * 初期処理
     */
    protected void init(Leaj0009ParameterModel pm) throws Exception {

        // カウント変数初期化
        pm.setFullPegItemFileCount(0);

        // SCPLAN用システムオーナーコード取得
        String	sql  = " SELECT scplan_sys_owner_cd  ";
        sql += "   FROM lc_system_parameter  ";
        sql += "  WHERE sys_owner_cd = ?     ";

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
     * 主処理
     */
    protected void main(Leaj0009ParameterModel pm) throws Exception {

        String sql = "";

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(pm.getFullPegItemFile());
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

        // MRP情報値テーブルから品目を抽出
        try {
            // MRP情報値テーブル取得SQL
            sql += " SELECT itemno                  ";
            sql += "      , supplier                ";
            sql += "      , usercd                  ";
            sql += "   FROM lc_inp_itemmast_mrp     ";
            sql += "  WHERE mrp_control_class = ?   ";
            sql += "    AND sys_owner_cd      = ?   ";

            //SQLパラメータの生成

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//            Object[] sqlParams = new Object[] { pm.getMrpControlClass()
//                    , pm.getSysOwnerCd()
//            };
//
//            // Select文の発行
//            ResultSet result =  dao.getResult(sql, sqlParams);
//
//            // テキストファイル出力
//            try {
//                while (BatchUtils.hasNext(result)) {
//
//                    Leaj0009LaItemmastMrpModel model = BatchUtils.<Leaj0009LaItemmastMrpModel>convertResultModel(result, Leaj0009LaItemmastMrpModel.class);
//
//                    // テキストファイルへ出力
//                    String writeRec = padSpaseRight(pm.getScplanSysOwnerCd(), 2)
//                            + "|"
//                            + padSpaseRight(model.getItemno(), 30)
//                            + "|"
//                            + padSpaseRight(model.getSupplier(), 4)
//                            + "|"
//                            + padSpaseRight(model.getUsercd(), 4)
//                            + "|"
//                            + "COMP"
//                            ;
//
//                    fileWriter.println(writeRec);
//                    pm.setFullPegItemFileCount(pm.getFullPegItemFileCount() + 1);
//                }
//            } finally {
//
//                result.close();
//            }
//        }finally {
//            batchUtilityLogic.closePrintWriter(fileWriter);
//        }
//    }
            try (Connection conn = dataSource.getConnection()) {
                // SQLパラメータの生成
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, (String) pm.getMrpControlClass());
                    pstmt.setString(2, (String) pm.getSysOwnerCd());

                    // Select文の発行
                    try (ResultSet result = pstmt.executeQuery()) {

                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pm.getFullPegItemFile(), false))) {
                            Leaj0009LaItemmastMrpModel model = null;

                            while (result.next()) {

                                // ResultSetをFetchしてデータを取り出す
                                model = new Leaj0009LaItemmastMrpModel();
                                model.setScplanSysOwnerCd(result.getString(1));
                                model.setItemno(result.getString(2));
                                model.setSupplier(result.getString(3));
                                model.setUsercd(result.getString(4));

                                // テキストファイルへ出力
                                String writeRec = padCharRight(pm.getScplanSysOwnerCd(), 2, ' ')
                                        + "|"
                                        + padCharRight(model.getItemno(), 30, ' ')
                                        + "|"
                                        + padCharRight(model.getSupplier(), 4, ' ')
                                        + "|"
                                        + padCharRight(model.getUsercd(), 4, ' ')
                                        + "|"
                                        + "COMP";

                                writer.write(writeRec);
                                writer.newLine();

                                pm.setFullPegItemFileCount(pm.getFullPegItemFileCount() + 1);
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
    protected void term(Leaj0009ParameterModel pm) throws Exception {

    //---------------- 2025/03/04 R.Mochizuki g3 Start(Modify)
//        LogReportWriter writer = ThreadLogReportWriter.getWriter();
//
//        writer.writeLog("", "FullPegItemFile Output Count = " + pm.getFullPegItemFileCount(), null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0008Service.class);

        logger.info("FullPegItemFile Output Count = {}", pm.getFullPegItemFileCount());
    //---------------- 2025/03/04 R.Mochizuki g3 End(Modify)

    }

    /**
     * format(スペース右埋め)
     * StringUtils.padCharRight()を少し変更してある。(Null,ブランクの場合でも文字埋めされるように)
     */
    public String padSpaseRight(String str, int len)
    {
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
}