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
package org.pepstock.jem.ant.tasks.utilities.certificate;

import java.text.MessageFormat;
import java.text.ParseException;

import org.pepstock.jem.ant.tasks.utilities.AntUtilMessage;
import org.pepstock.jem.node.rmi.InternalUtilities;
import org.pepstock.jem.node.rmi.UtilsInitiatorManager;
import org.pepstock.jem.node.tasks.JobId;

/**
 * @author Andrea "Stock" Stocchero
 * @version 2.3
 * 
 */
public class Delete extends Command {

	@SuppressWarnings("javadoc")
	public static final String COMMAND_KEYWORD = "DELETE";

	// format of GDG command
	private static final String REMOVE_FORMAT = "DELETE ALIAS {0}";

	private static final MessageFormat FORMAT = new MessageFormat(REMOVE_FORMAT);

	/**
	 * Is the delete command for an alias inside the JEM user keystore.
	 * <p>
	 * Example:
	 * <p>
	 * DELETE ALIAS PIPPO
	 * <p>
	 * delete the certificate with alias PIPPO inside the JEM user keystore
	 * 
	 * @param commandLine
	 * @throws ParseException
	 */
	public Delete(String commandLine) throws ParseException {
		super(commandLine);
		// parse command
		Object[] object = FORMAT.parse(commandLine);

		if (object.length == 1) {
			setAlias(object[0].toString());
		} else {
			throw new ParseException(AntUtilMessage.JEMZ004E.toMessage()
					.getFormattedMessage(COMMAND_KEYWORD, commandLine), 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pepstock.jem.ant.tasks.utilities.nodes.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		if (getAlias() != null) {
			InternalUtilities util = UtilsInitiatorManager
					.getInternalUtilities();
			util.deleteCertificate(JobId.VALUE, getAlias());
			System.out.println(AntUtilMessage.JEMZ056I.toMessage()
					.getFormattedMessage(getAlias()));
		}

	}
}