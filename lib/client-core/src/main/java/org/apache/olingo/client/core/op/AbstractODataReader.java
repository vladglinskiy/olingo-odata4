/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.op;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.olingo.client.api.CommonODataClient;
import org.apache.olingo.commons.api.domain.ODataError;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.domain.ODataEntity;
import org.apache.olingo.commons.api.domain.ODataEntitySet;
import org.apache.olingo.client.api.domain.ODataEntitySetIterator;
import org.apache.olingo.commons.api.domain.ODataProperty;
import org.apache.olingo.commons.api.domain.ODataServiceDocument;
import org.apache.olingo.commons.api.domain.ODataValue;
import org.apache.olingo.client.api.edm.xml.XMLMetadata;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.commons.api.format.ODataPubFormat;
import org.apache.olingo.commons.api.format.ODataValueFormat;
import org.apache.olingo.client.api.op.CommonODataReader;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractODataReader implements CommonODataReader {

  private static final long serialVersionUID = -1988865870981207079L;

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractODataReader.class);

  protected final CommonODataClient client;

  protected AbstractODataReader(final CommonODataClient client) {
    this.client = client;
  }

  @Override
  public ODataEntitySet readEntitySet(final InputStream input, final ODataPubFormat format) {
    return client.getBinder().getODataEntitySet(client.getDeserializer().toFeed(input, format));
  }

  @Override
  public ODataEntity readEntity(final InputStream input, final ODataPubFormat format) {
    return client.getBinder().getODataEntity(client.getDeserializer().toEntry(input, format));
  }

  @Override
  public ODataProperty readProperty(final InputStream input, final ODataFormat format) {
    final Property property = client.getDeserializer().toProperty(input, format);
    return client.getBinder().getODataProperty(property);
  }

  @Override
  public ODataError readError(final InputStream inputStream, final boolean isXML) {
    return client.getDeserializer().toError(inputStream, isXML);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T read(final InputStream src, final String format, final Class<T> reference) {
    Object res;

    try {
      if (ODataEntitySetIterator.class.isAssignableFrom(reference)) {
        res = new ODataEntitySetIterator(client, src, ODataPubFormat.fromString(format));
      } else if (ODataEntitySet.class.isAssignableFrom(reference)) {
        res = readEntitySet(src, ODataPubFormat.fromString(format));
      } else if (ODataEntity.class.isAssignableFrom(reference)) {
        res = readEntity(src, ODataPubFormat.fromString(format));
      } else if (ODataProperty.class.isAssignableFrom(reference)) {
        res = readProperty(src, ODataFormat.fromString(format));
      } else if (ODataValue.class.isAssignableFrom(reference)) {
        res = client.getPrimitiveValueBuilder().
                setType(ODataValueFormat.fromString(format) == ODataValueFormat.TEXT
                        ? EdmPrimitiveTypeKind.String : EdmPrimitiveTypeKind.Stream).
                setText(IOUtils.toString(src)).
                build();
      } else if (XMLMetadata.class.isAssignableFrom(reference)) {
        res = readMetadata(src);
      } else if (ODataServiceDocument.class.isAssignableFrom(reference)) {
        res = readServiceDocument(src, ODataFormat.fromString(format));
      } else if (ODataError.class.isAssignableFrom(reference)) {
        res = readError(src, !format.toString().contains("json"));
      } else {
        throw new IllegalArgumentException("Invalid reference type " + reference);
      }
    } catch (Exception e) {
      LOG.warn("Cast error", e);
      res = null;
    } finally {
      if (!ODataEntitySetIterator.class.isAssignableFrom(reference)) {
        IOUtils.closeQuietly(src);
      }
    }

    return (T) res;
  }
}
