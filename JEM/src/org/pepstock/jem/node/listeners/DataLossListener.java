/**
    JEM, the BEE - Job Entry Manager, the Batch Execution Environment
    Copyright (C) 2012-2015   Andrea "Stock" Stocchero
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.pepstock.jem.node.listeners;

import com.hazelcast.map.MapPartitionLostEvent;
import com.hazelcast.map.listener.MapPartitionLostListener;

/**
 * @author Andrea "Stock" Stocchero
 * @version 3.0
 */
public class DataLossListener implements MapPartitionLostListener {
	
//	static final DataLossManager man = new DataLossManager();


	/* (non-Javadoc)
	 * @see com.hazelcast.map.listener.MapPartitionLostListener#partitionLost(com.hazelcast.map.MapPartitionLostEvent)
	 */
	@Override
	public void partitionLost(MapPartitionLostEvent event) {
		DataLossManager.getInstance().add(event.getMember().getUuid(), event.getName());
	}

}
