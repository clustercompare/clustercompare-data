/*
 * $Id: IModelComparator.java 3657 2006-01-06 16:33:50 +0000 (Fri, 06 Jan 2006)
 * joco01 $ $Revision: 5872 $ $Date: 2006-01-06 16:33:50 +0000 (Fri, 06 Jan
 * 2006) $
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
package wicket.model;

import wicket.Component;

/**
 * Implementations of this interface compare model object. The component is
 * given so that a developer can choose what the previous object is The default
 * implementation for form components is just component.getModelObject(); But
 * developers can choose to keep the last rendered value for that component and
 * compare this value with the newObject. So that it doesn't overwrite values
 * for an object that was changed by another session if the current session
 * didn't touch that specific value.
 * 
 * @author jcompagner
 * @author Jonathan Locke
 */
public interface IModelComparator
{
	/**
	 * @param component
	 *            The component which received the new object
	 * @param newObject
	 *            The newObject
	 * @return True if the previous components object is the same as the
	 *         newObject.
	 */
	boolean compare(Component component, Object newObject);
}
