/*
 * $Id: ErrorLevelFeedbackMessageFilter.java 3903 2006-01-19 19:57:34 +0000
 * (Thu, 19 Jan 2006) joco01 $ $Revision: 5871 $ $Date: 2006-01-19 19:57:34
 * +0000 (Thu, 19 Jan 2006) $
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
package wicket.feedback;

/**
 * Filter for accepting feedback messages of a certain error level.
 * 
 * @author Jonathan Locke
 */
public class ErrorLevelFeedbackMessageFilter implements IFeedbackMessageFilter
{
	private static final long serialVersionUID = 1L;

	/** The minimum error level */
	private final int minimumErrorLevel;

	/**
	 * Constructor
	 * 
	 * @param minimumErrorLevel
	 *            The component to filter on
	 */
	public ErrorLevelFeedbackMessageFilter(int minimumErrorLevel)
	{
		this.minimumErrorLevel = minimumErrorLevel;
	}

	/**
	 * @see wicket.feedback.IFeedbackMessageFilter#accept(wicket.feedback.FeedbackMessage)
	 */
	public boolean accept(FeedbackMessage message)
	{
		return message.isLevel(minimumErrorLevel);
	}
}
