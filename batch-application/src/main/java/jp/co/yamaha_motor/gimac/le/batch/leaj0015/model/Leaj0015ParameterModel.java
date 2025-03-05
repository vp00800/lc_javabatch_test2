package jp.co.yamaha_motor.gimac.le.batch.leaj0015.model;

import lombok.Data;

import java.util.Map;

@Data
public class Leaj0015ParameterModel{

	private String sysOwnerCd;

	// Batch Parameter Value
	private String mrpControlClass;		// MRP管理区分
	private String cancelIrd;			// 発注取消（独立所要量）ファイル
	private String irdHistory;			// 独立所要量明細履歴ファイル
	private String scplanIrd;			// SCPLAN-IN独立所要量ファイル
	private String bypassIrd;			// SCPLAN BYPASS 独立所要量明細ファイル

	// Work Area
	private String scplanSysOwnerCd;

	// LcSystemPatameter
	private int orderDeteleDays;
	private int completeHoldingDays;

	// LzPymacDate
	private String pymacDate;

	// LcMstCalendarCode
	private String calendarCode;

	// Out Data Counte
	private int lcTrnIrdTxtFileOutputCount;
	private int lcTrnIrdHisTxtFileOutputCount;
	private int scplanInLcTrnIrdOutputCount;
	private int lcWorkBypassIrdOutputCount;
	private Map<String, String> MinValidShiftMap;	//先頭稼動シフトMap

	// OrderDeleteDate
	private String orderDeleteDate;

	// HistoryMoveDate
	private String historyMoveDate;

	// SCPLAN-INPUT独立所要量シフト番号セット
	private String 	scplanInShippingShiftNo;
}