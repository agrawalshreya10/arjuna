/*******************************************************************************
 * Copyright 2015-16 AutoCognite Testing Research Pvt Ltd
 * 
 * Website: www.AutoCognite.com
 * Email: support [at] autocognite.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.autocognite.pvt.batteries.databroker;

import java.util.Iterator;
import java.util.Set;

import com.autocognite.arjuna.exceptions.DataSourceFinishedException;
import com.autocognite.arjuna.interfaces.DataSource;
import com.autocognite.arjuna.interfaces.ReadOnlyDataRecord;
import com.autocognite.pvt.batteries.filehandler.IniFileReader;

public class IniFileDataSource implements DataSource {
	IniFileReader reader = null;
	Set<String> sections = null;
	Iterator<String> iter = null;

	public IniFileDataSource(String path) throws Exception {
		reader = new IniFileReader(path);
		sections = this.reader.getAllSections();
		iter = sections.iterator();
	}

	@Override
	public synchronized ReadOnlyDataRecord next() throws DataSourceFinishedException {
		if (iter.hasNext()) {
			return new DataRecord(this.reader.getSectionDataObjects((String) iter.next()));
		} else {
			throw new DataSourceFinishedException("Records Finished.");
		}
	}

}
