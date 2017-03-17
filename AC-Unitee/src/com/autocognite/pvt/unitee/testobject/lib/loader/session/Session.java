package com.autocognite.pvt.unitee.testobject.lib.loader.session;

import com.autocognite.pvt.batteries.value.DefaultStringKeyValueContainer;
import com.google.gson.JsonObject;

public interface Session {

	int getTestMethodCount();

	SessionNode next() throws Exception;

	void load() throws Exception;

	String getName();

	JsonObject getConfigObject();

	String getSessionFilePath();

	JsonObject getUDVObject();

	DefaultStringKeyValueContainer getUDV();

	void setUDVs(DefaultStringKeyValueContainer udvs);
}