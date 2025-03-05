package com.g3.batch.slxdb2fileleaj0015tasklet.model;

import lombok.Data;

@Data
public class Leaj0015ScplanInLcTrnIrdModel {

	private String itemno;
	private String supplier;
	private String usercd;
	private String orderNo;
	private String startDate;
	private String pilotClass;
    private String requiredQty;
    private String shipQty;
    private String mrpDate;
    private String rdClass;
    private String orderThroughNo;			// 2017-09-14 MOD
    private String orderThroughNoSourceFlg;	// 2017-09-14 MOD
    private String timesDivideDay;			// 2017-09-14 ADD
}