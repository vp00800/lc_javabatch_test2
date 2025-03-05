package jp.co.yamaha_motor.gimac.le.batch.leaj0008.service;

import com.ymsl.solid.base.util.StringUtils;
import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0008.model.Leaj0008OutputFileModel;
import jp.co.yamaha_motor.gimac.le.batch.leaj0008.model.Leaj0008ParameterModel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * LEAJ0008
 * ＜ 構成マスター作成 ＞
 *
 * 製品構成の子品目KEYでMRP情報値テーブルから
 * 所要量出庫管理コード、MRP需要方針コード、Wビン管理コードを取得し、
 * 製品構成テーブルの情報と合算してテキストファイルに出力する
 *
 * @author  YMSLx XTL
 * @version 1.0.0
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
 *
 * MODIFICATION HISTORY
 *  (Ver)  (Date)     (Name)       (Comment)
 *  1.0.0 2012/07/10  YMSLx XTL    New making
 */

@Service
@Slf4j
public class Leaj0008Service {

    //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Add)
    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataSource dataSource;

    public void leaj0008Executing(Map<String, Object> map) throws Exception{
        Leaj0008ParameterModel pm = new Leaj0008ParameterModel();
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
    protected void setParameter(Leaj0008ParameterModel pm, Map<String, Object> map) {

        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //if ( "arg1".equals(name) ) {
            // MRP管理区分
        //    pm.setMrpControlClass(value);
        //}
        //if ( "outf".equals(name) ) {
            // 構成マスターファイル
        //    pm.setProdStrcFile(value);
        //}
        if ( map.get("arg1") != null ) {
            // システムオーナーコード
            pm.setSysOwnerCd(map.get("arg1").toString());
        }
        if ( map.get("arg2") != null ) {
            // MRP管理区分
            pm.setMrpControlClass(map.get("arg2").toString());
        }
        if ( map.get("out1") != null ) {
            // 構成マスターファイル
            pm.setProdStrcFile(map.get("out1").toString());
        }
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * 引数取得後処理
     */
    protected void prepareParameterAfter(Leaj0008ParameterModel pm) throws Exception {

        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //LogReportWriter writer = ThreadLogReportWriter.getWriter();

        // 引数必須チェック
        //if ( CheckUtil.isEmptyBlank(pm.getMrpControlClass()) ) {
        //    throw new BatchApplicationException("Argument error [arg1:mrpControlClass] is blank");
        //}
        //if ( CheckUtil.isEmptyBlank(pm.getProdStrcFile()) ) {
        //    throw new BatchApplicationException("Argument error [outf:prodStrcFile] is blank");
        //}

        // 引数内容ログ出力
        //writer.writeLog("", "[Argument Value]", null, null, null);
        //writer.writeLog("", " sysOwnerCd      : " + pm.getSysOwnerCd(),      null, null, null);
        //writer.writeLog("", " mrpControlClass : " + pm.getMrpControlClass(), null, null, null);
        //writer.writeLog("", " prodStrcFile    : " + pm.getProdStrcFile(),    null, null, null);

        Logger logger = LoggerFactory.getLogger(Leaj0008Service.class);

        // 引数必須チェック
        if ( StringUtils.isBlankText(pm.getMrpControlClass()) ) {
            throw new JobParametersInvalidException("Argument error [arg1:mrpControlClass] is blank");
        }
        if ( StringUtils.isBlankText(pm.getProdStrcFile()) ) {
            throw new JobParametersInvalidException("Argument error [outf:prodStrcFile] is blank");
        }

        // 引数内容ログ出力
        logger.info("[Argument Value]");
        logger.info("sysOwnerCd      :", pm.getSysOwnerCd());
        logger.info("mrpControlClass :", pm.getMrpControlClass());
        logger.info("prodStrcFile    :", pm.getProdStrcFile());
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * 初期処理
     */
    protected void init(Leaj0008ParameterModel pm) throws Exception{

        // SCPLAN用システムオーナーコード取得
        String sql  = " SELECT scplan_sys_owner_cd  ";
               sql += "   FROM lc_system_parameter  ";
               sql += "  WHERE sys_owner_cd = ?     ";

        // Select文の発行
        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //Object[] obj = dao.getRecord(sql, new Object[] {pm.getSysOwnerCd()});
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
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)

        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //if (obj == null) {
        if (StringUtils.isBlankText(scplanSysOwnerCd)) {
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
            //該当データが存在しない場合エラーとする
            String msg	= " [lc_system_parameter] No Data Found Error."
                    + " [sys_owner_cd:'" + pm.getSysOwnerCd() + "']";
            throw new JobParametersInvalidException(msg);
        }else{
            //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
            //pm.setScplanSysOwnerCd(StringUtil.toString(obj[0]));
            pm.setScplanSysOwnerCd(scplanSysOwnerCd);
            //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
        }
    }

    /**
     * 主処理
     */
    protected void main(Leaj0008ParameterModel pm) throws Exception{

        // 構成マスタ出力
        structureMastFileOutput(pm);
    }

    /**
     * 後処理
     */
    protected void term(Leaj0008ParameterModel pm) throws Exception {

        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //LogReportWriter writer = ThreadLogReportWriter.getWriter();

        //writer.writeLog("", "[prodStrcFile] Output File Write Count = " + pm.getStructureMastCount(), null, null, null);
        Logger logger = LoggerFactory.getLogger(Leaj0008Service.class);
        logger.info("[prodStrcFile] Output File Write Count = {}", pm.getStructureMastCount());
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * 構成マスタ出力
     * @param pm
     * @throws Exception
     */
    protected void structureMastFileOutput(Leaj0008ParameterModel pm) throws Exception{

        String sql;

        // sort用に拡張
        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Delete)
        //dao.executeStatement("set work_mem = '1GB'");
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Delete)

        //製品構成の検索SQL
        sql  = "";
        sql += " SELECT a.parent_itemno                           ";
        sql += "       ,a.parent_supplier                         ";
        sql += "       ,a.parent_usercd                           ";
        sql += "       ,a.structure_seq                           ";
        sql += "       ,a.comp_itemno                             ";
        sql += "       ,a.comp_supplier                           ";
        sql += "       ,a.comp_usercd                             ";
        sql += "       ,a.in_effective_ymd                        ";
        sql += "       ,a.out_effective_ymd                       ";
        sql += "       ,a.comp_sign                               ";
        sql += "       ,a.comp_qty                                ";
        sql += "       ,a.comp_qty_type                           ";
        sql += "       ,a.comp_op_percent                         ";
        sql += "       ,b.req_issue_control                       ";
        sql += "       ,b.demand_policy_code                      ";
        sql += "       ,b.wbin_control_code                       ";
        sql += "       ,c.item_type                               ";
        sql += "       ,d.item_type                               ";
        sql += "       ,d.item_class                              ";

        sql += " FROM   lc_inp_prodstrc      a                    ";
        sql += "       ,lc_inp_itemmast_mrp  b                    ";
        sql += "       ,lc_inp_itemmast      c                    ";
        sql += "       ,lc_inp_itemmast      d                    ";

        sql += " WHERE a.mrp_control_class = ?                    ";
        sql += " AND   a.sys_owner_cd      = ?                    ";
        sql += " AND   a.deleted_yn        = 'N'                  ";

        sql += " AND   a.mrp_control_class = b.mrp_control_class  ";
        sql += " AND   a.sys_owner_cd      = b.sys_owner_cd       ";
        sql += " AND   a.comp_itemno       = b.itemno             ";
        sql += " AND   a.comp_supplier     = b.supplier           ";
        sql += " AND   a.comp_usercd       = b.usercd             ";

        sql += " AND   a.mrp_control_class = c.mrp_control_class  ";
        sql += " AND   a.sys_owner_cd      = c.sys_owner_cd       ";
        sql += " AND   a.comp_itemno       = c.itemno             ";
        sql += " AND   a.comp_supplier     = c.supplier           ";
        sql += " AND   a.comp_usercd       = c.usercd             ";

        sql += " AND   a.mrp_control_class = d.mrp_control_class  ";
        sql += " AND   a.sys_owner_cd      = d.sys_owner_cd       ";
        sql += " AND   a.parent_itemno     = d.itemno             ";
        sql += " AND   a.parent_supplier   = d.supplier           ";
        sql += " AND   a.parent_usercd     = d.usercd             ";

        //---------------- 2018/10/29 Wu Zhijian 0336-A304 Start(Modify) ----PSのソート順の修正-------------//
        //sql += " ORDER BY 1,2,3,5,6,7,4                           ";
        sql += " ORDER BY 1,2,3,4,5,6,7,8,9                       ";
        //---------------- 2018/10/29 Wu Zhijian 0336-A304 End(Modify) ----PSのソート順の修正-------------//
        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //SQLパラメータの生成
        //Object[] structureMastSqlParams = new Object[] { pm.getMrpControlClass()
        //        , pm.getSysOwnerCd()
        //};

        // Select文の発行
        //ResultSet structureMastResult = dao.getResult(sql, structureMastSqlParams);

        //try {
            //ファイル出力
        //    structureMastFileOutputProcess(pm, structureMastResult, pm.getProdStrcFile());
        //} finally {
        //    structureMastResult.close();
        //}
        // データベース接続
        try (Connection conn = dataSource.getConnection()) {
            // sort用に拡張
            try (PreparedStatement setWorkMemStmt = conn.prepareStatement("SET work_mem = '1GB'")) {
                setWorkMemStmt.execute();
            }
            // SQLパラメータの生成
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, (String) pm.getMrpControlClass());
                pstmt.setString(2, (String) pm.getSysOwnerCd());

                // Select文の発行
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    // ファイル出力
                    structureMastFileOutputProcess(pm, resultSet, pm.getProdStrcFile());
                }
            }
        }
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
    }

    /**
     * ファイル出力
     * @param pm
     * @param result
     * @param outputFile
     * @throws Exception
     */
    protected void structureMastFileOutputProcess(Leaj0008ParameterModel pm, ResultSet result, String outputFile) throws Exception{
        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
        //PrintWriter fileWriter = batchUtilityLogic.getPrintWriter(outputFile);

        //try {
        //    Lcrj0009StructureMastModel model = null;

        //    while (BatchUtils.hasNext(result)) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
            Leaj0008OutputFileModel model = null;

            while (result.next()) {
        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)

                model = convertResultStructureMastModel(result);

                if (StringUtils.equals(model.getReqIssueControl(),  "11") &&
                        StringUtils.equals(model.getDemandPolicyCode(), "2" ) &&
                        StringUtils.equals(model.getWbinControlCode(),  "1" )) {

                    pm.setReqIssueControl("0");
                } else {

                    pm.setReqIssueControl(StringUtils.substring(model.getReqIssueControl(), 0, 1));
                }

                DecimalFormat df1        = new DecimalFormat("000000000.00000");
                BigDecimal compQty       = new BigDecimal(model.getCompQty());
                BigDecimal compOpPercent = new BigDecimal(model.getCompOpPercent());

                if (!(StringUtils.equals(model.getCompOpPercent(), "100" )) &&
                        StringUtils.equals(model.getParentItemType(),   "2") &&
                        !(StringUtils.equals(model.getParentItemClass(),"2" ))) {

                    pm.setCompQty(compQty.multiply(compOpPercent.divide(new BigDecimal("100"))));
                    pm.setCompOpPercent("100");
                } else {

                    pm.setCompQty(compQty);
                    pm.setCompOpPercent(model.getCompOpPercent());
                }

                // テキストファイルへ出力
                String writeRec = padSpaseRight(pm.getScplanSysOwnerCd(), 2)    //親システムオーナーコード(SCPLAN用)
                        + "|"
                        + padSpaseRight(model.getParentItemno(), 30)            //親品目番号
                        + "|"
                        + padSpaseRight(model.getParentSupplier(), 4)           //親供給者
                        + "|"
                        + padSpaseRight(model.getParentUsercd(), 4)             //親使用者
                        + "|"
//                              + padSpaseRight(StringUtil.padZeroLeft(StringUtil.trimString(model.getStructureSeq()), 3), 3)  //構成連番 2014/08/19 廃止
                        + padSpaseRight(model.getStructureSeq(), 3)             //構成連番
                        + "|"
                        + padSpaseRight(pm.getScplanSysOwnerCd(), 2)            //子システムオーナーコード(SCPLAN用)
                        + "|"
                        + padSpaseRight(model.getCompItemno(), 30)              //子品目番号
                        + "|"
                        + padSpaseRight(model.getCompSupplier(), 4)             //子供給者
                        + "|"
                        + padSpaseRight(model.getCompUsercd(), 4)               //子使用者
                        + "|"
                        + padSpaseRight(model.getInEffectiveYmd(), 8)           //IN発効年月日
                        + "|"
                        + padSpaseRight(model.getOutEffectiveYmd(), 8)          //OUT発効年月日
                        + "|"
                        + padSpaseRight(model.getCompItemType(), 1)             //子品目タイプ
                        + "|"
                        + model.getCompSign() + df1.format(pm.getCompQty())         //構成品サイン＋員数
                        + "|"
                        + padSpaseRight(model.getCompQtyType(), 1)              //員数タイプ
                        + "|"
                        //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
                        //+ padSpaseRight(StringUtil.padZeroLeft(pm.getCompOpPercent(), 3), 3)   //OP率
                        + padSpaseRight(StringUtils.padZeroLeft(pm.getCompOpPercent(), 3), 3)   //OP率
                        //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
                        + "|"
                        + padSpaseRight(pm.getReqIssueControl(), 1)             //所要量出庫管理
                        + "|"
                        + "000"                                                     //ソートキー
                        ;

                //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Modify)
                //fileWriter.println(writeRec);
                writer.write(writeRec);
                writer.newLine();
                //---------------- 2025/02/18 Tao Xiaochuan g3 End(Modify)
                pm.setStructureMastCount(pm.getStructureMastCount() + 1);
            }
        } finally {
            //---------------- 2025/02/18 Tao Xiaochuan g3 Start(Delete)
            //batchUtilityLogic.closePrintWriter(fileWriter);
            //---------------- 2025/02/18 Tao Xiaochuan g3 End(Delete)
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
     * StructureMastModel DBの返却値をセットする
     * @param result
     * @return Leaj0008StructureMastModel
     */
    public Leaj0008OutputFileModel convertResultStructureMastModel(ResultSet result) throws Exception{

        Leaj0008OutputFileModel model = new Leaj0008OutputFileModel();

        model.setParentItemno(result.getString(1));
        model.setParentSupplier(result.getString(2));
        model.setParentUsercd(result.getString(3));
        model.setStructureSeq(result.getString(4));
        model.setCompItemno(result.getString(5));
        model.setCompSupplier(result.getString(6));
        model.setCompUsercd(result.getString(7));
        model.setInEffectiveYmd(result.getString(8));
        model.setOutEffectiveYmd(result.getString(9));
        model.setCompSign(result.getString(10));
        model.setCompQty(result.getString(11));
        model.setCompQtyType(result.getString(12));
        model.setCompOpPercent(result.getString(13));
        model.setReqIssueControl(result.getString(14));
        model.setDemandPolicyCode(result.getString(15));
        model.setWbinControlCode(result.getString(16));
        model.setCompItemType(result.getString(17));
        model.setParentItemType(result.getString(18));
        model.setParentItemClass(result.getString(19));

        return model;
    }
}
