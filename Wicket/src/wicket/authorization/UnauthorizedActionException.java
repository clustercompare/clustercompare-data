/*
 * $Id: UnauthorizedActionException.java,v 1.1 2005/12/22 22:27:01 jonathanlocke
 * Exp $ $Revision: 5869 $ $Date: 2006-05-26 00:34:11 +0200 (Fri, 26 May 2006) $
 * 
 * ==============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wicket.authorization;

import wicket.Component;

/**
 * Exception that is thrown when an action is not authorized.
 * 
 * @author Jonathan Locke
 * @author Eelco Hillenius
 */
public class UnauthorizedActionException extends AuthorizationException
{
	private static final long serialVersionUID = 1L;

	/** The action */
	private Action action;

	/** The component that caused the unauthorized exception */
	private final Component component;

	/**
	 * Construct.
	 * 
	 * @param component
	 *            The component that caused the unauthorized exception
	 * @param action
	 *            The action
	 */
	public UnauthorizedActionException(Component component, Action action)
	{
		super("Component " + component + " does not permit action " + action);
		this.component = component;
		this.action = action;
	}

	/**
	 * @return The action that was forbidden
	 */
	public Action getAction()
	{
		return action;
	}

	/**
	 * @return The component that caused the unauthorized exception
	 */
	public Component getComponent()
	{
		return component;
	}
}
