package com.g3.batch.slxdb2fileleaj0015tasklet.model;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class Leaj0015LcWorkBypassIrdModel {

	private String itemno;
	private String supplier;
	private String usercd;
	private String orderNo;
	private String startDate;
	private String orderThroughNo;			// 2017-09-14 MOD
	private String orderThroughNoSourceFlg;	// 2017-09-14 MOD
	private String rdClass;
	private String indUserClass;
	private String indUserCode;
	private String rlsStartDate;
	private String orderStatus;
	private String reasonCode;
	private String fixedYmd;
	private String pilotClass;
	private String pilotConditionType;
	private BigDecimal requiredQty;
	private String remark;
	private String requestSystemCode;
	private String groupReceiveFlg;
	private BigDecimal shipQty;
	private BigDecimal boQty;
	private String deliveryCardStatus;
	private String itemCardStatus;
	private String shipDate;
	private String transferClass;
	private String transferCode;
	private String transferReasonCode;
	private String accountHeading;
	private String accountDetail;
	private String budgetNo;
	private String accountCodeSales;
	private String spOrderClass;
	private String spDeliveryCode;
	private String spDealerNo;
	private String spOrderNo;
	private Integer operationNo;
	private Integer operationSeq;
	private String batchStatus;
	private String mrpDate;
	private String deleteYmd;
	private String registerUserName;
	private long updateCounter;
	private Timestamp createDatetime;
	private String createAuthor;
	private Timestamp updateDatetime;
	private String updateAuthor;
	private String updatePgmid;
}
