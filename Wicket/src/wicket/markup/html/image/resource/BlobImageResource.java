/*
 * $Id: BlobImageResource.java 5791 2006-05-20 00:32:57 +0000 (Sat, 20 May 2006)
 * joco01 $ $Revision: 5871 $ $Date: 2006-05-20 00:32:57 +0000 (Sat, 20 May
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
package wicket.markup.html.image.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Locale;

import wicket.WicketRuntimeException;
import wicket.util.io.Streams;

/**
 * An ImageResource subclass for dynamic images that come from database BLOB
 * fields. Subclasses override getBlob() to provide the image data to send back
 * to the user. A given subclass may decide how to produce this data and
 * whether/how to buffer it.
 * 
 * @author Eelco Hillenius
 */
public abstract class BlobImageResource extends DynamicImageResource
{
	/**
	 * Construct.
	 * 
	 * @param locale
	 */
	public BlobImageResource(Locale locale)
	{
		super(locale);
	}

	/**
	 * Construct.
	 * 
	 * @param format
	 * @param locale
	 */
	public BlobImageResource(String format, Locale locale)
	{
		super(format, locale);
	}

	/**
	 * Construct.
	 * 
	 * @param format
	 */
	public BlobImageResource(String format)
	{
		super(format);
	}

	/**
	 * Construct.
	 */
	public BlobImageResource()
	{
	}

	/**
	 * @see wicket.markup.html.image.resource.DynamicImageResource#getImageData()
	 */
	@Override
	protected byte[] getImageData()
	{
		try
		{
			Blob blob = getBlob();
			if (blob != null)
			{
				InputStream in = blob.getBinaryStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Streams.copy(in, out);
				return out.toByteArray();
			}
			return new byte[0];
		}
		catch (SQLException e)
		{
			throw new WicketRuntimeException("Error while reading image data", e);
		}
		catch (IOException e)
		{
			throw new WicketRuntimeException("Error while reading image data", e);
		}
	}

	/**
	 * Gets the BLOB (Binary Large OBject) that holds the raw image data.
	 * 
	 * @return the BLOB
	 */
	protected abstract Blob getBlob();
}
