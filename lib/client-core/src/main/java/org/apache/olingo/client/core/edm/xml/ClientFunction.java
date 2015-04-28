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
package org.apache.olingo.client.core.edm.xml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = ClientFunction.FunctionDeserializer.class)
class ClientFunction extends CsdlFunction {

  private static final long serialVersionUID = -5494898295282843362L;

  static class FunctionDeserializer extends AbstractClientEdmDeserializer<ClientFunction> {
    @Override
    protected ClientFunction doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientFunction functionImpl = new ClientFunction();

      for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.getCurrentToken();
        if (token == JsonToken.FIELD_NAME) {
          if ("Name".equals(jp.getCurrentName())) {
            functionImpl.setName(jp.nextTextValue());
          } else if ("IsBound".equals(jp.getCurrentName())) {
            functionImpl.setBound(BooleanUtils.toBoolean(jp.nextTextValue()));
          } else if ("IsComposable".equals(jp.getCurrentName())) {
            functionImpl.setComposable(BooleanUtils.toBoolean(jp.nextTextValue()));
          } else if ("EntitySetPath".equals(jp.getCurrentName())) {
            functionImpl.setEntitySetPath(jp.nextTextValue());
          } else if ("Parameter".equals(jp.getCurrentName())) {
            jp.nextToken();
            functionImpl.getParameters().add(jp.readValueAs(ClientParameter.class));
          } else if ("ReturnType".equals(jp.getCurrentName())) {
            functionImpl.setReturnType(parseReturnType(jp, "Function"));
          } else if ("Annotation".equals(jp.getCurrentName())) {
            jp.nextToken();
            functionImpl.getAnnotations().add(jp.readValueAs(ClientAnnotation.class));
          }
        }
      }

      return functionImpl;
    }
  }
}
