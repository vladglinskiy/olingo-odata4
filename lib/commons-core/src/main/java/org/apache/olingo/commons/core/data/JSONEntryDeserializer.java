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
package org.apache.olingo.commons.core.data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Container;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.domain.ODataLinkType;
import org.apache.olingo.commons.api.domain.ODataOperation;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.core.edm.EdmTypeInfo;

/**
 * Reads JSON string into an entry.
 * <br/>
 * If metadata information is available, the corresponding entry fields and content will be populated.
 */
public class JSONEntryDeserializer extends AbstractJsonDeserializer<JSONEntryImpl> {

  @Override
  protected Container<JSONEntryImpl> doDeserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException, JsonProcessingException {

    final ObjectNode tree = parser.getCodec().readTree(parser);

    if (tree.has(Constants.VALUE) && tree.get(Constants.VALUE).isArray()) {
      throw new JsonParseException("Expected OData Entity, found EntitySet", parser.getCurrentLocation());
    }

    final JSONEntryImpl entry = new JSONEntryImpl();

    final URI contextURL;
    if (tree.hasNonNull(Constants.JSON_CONTEXT)) {
      contextURL = URI.create(tree.get(Constants.JSON_CONTEXT).textValue());
      tree.remove(Constants.JSON_CONTEXT);
    } else if (tree.hasNonNull(Constants.JSON_METADATA)) {
      contextURL = URI.create(tree.get(Constants.JSON_METADATA).textValue());
      tree.remove(Constants.JSON_METADATA);
    } else {
      contextURL = null;
    }
    if (contextURL != null) {
      entry.setBaseURI(StringUtils.substringBefore(contextURL.toASCIIString(), Constants.METADATA));
    }

    final String metadataETag;
    if (tree.hasNonNull(Constants.JSON_METADATA_ETAG)) {
      metadataETag = tree.get(Constants.JSON_METADATA_ETAG).textValue();
      tree.remove(Constants.JSON_METADATA_ETAG);
    } else {
      metadataETag = null;
    }

    if (tree.hasNonNull(jsonETag)) {
      entry.setETag(tree.get(jsonETag).textValue());
      tree.remove(jsonETag);
    }

    if (tree.hasNonNull(jsonType)) {
      entry.setType(new EdmTypeInfo.Builder().setTypeExpression(tree.get(jsonType).textValue()).build().internal());
      tree.remove(jsonType);
    }

    if (tree.hasNonNull(jsonId)) {
      entry.setId(tree.get(jsonId).textValue());
      tree.remove(jsonId);
    }

    if (tree.hasNonNull(jsonReadLink)) {
      final LinkImpl link = new LinkImpl();
      link.setRel(Constants.SELF_LINK_REL);
      link.setHref(tree.get(jsonReadLink).textValue());
      entry.setSelfLink(link);

      tree.remove(jsonReadLink);
    }

    if (tree.hasNonNull(jsonEditLink)) {
      final LinkImpl link = new LinkImpl();
      // Server mode
      if (serverMode) {
        link.setRel(Constants.EDIT_LINK_REL);
      }
      link.setHref(tree.get(jsonEditLink).textValue());
      entry.setEditLink(link);

      tree.remove(jsonEditLink);
    }

    if (tree.hasNonNull(jsonMediaReadLink)) {
      entry.setMediaContentSource(tree.get(jsonMediaReadLink).textValue());
      tree.remove(jsonMediaReadLink);
    }
    if (tree.hasNonNull(jsonMediaEditLink)) {
      entry.setMediaContentSource(tree.get(jsonMediaEditLink).textValue());
      tree.remove(jsonMediaEditLink);
    }
    if (tree.hasNonNull(jsonMediaContentType)) {
      entry.setMediaContentType(tree.get(jsonMediaContentType).textValue());
      tree.remove(jsonMediaContentType);
    }
    if (tree.hasNonNull(jsonMediaETag)) {
      entry.setMediaETag(tree.get(jsonMediaETag).textValue());
      tree.remove(jsonMediaETag);
    }

    final Set<String> toRemove = new HashSet<String>();
    for (final Iterator<Map.Entry<String, JsonNode>> itor = tree.fields(); itor.hasNext();) {
      final Map.Entry<String, JsonNode> field = itor.next();

      links(field, entry, toRemove, tree, parser.getCodec());
      if (field.getKey().endsWith(getJSONAnnotation(jsonMediaEditLink))) {
        final LinkImpl link = new LinkImpl();
        link.setTitle(getTitle(field));
        link.setRel(version.getNamespaceMap().get(ODataServiceVersion.MEDIA_EDIT_LINK_REL) + getTitle(field));
        link.setHref(field.getValue().textValue());
        link.setType(ODataLinkType.MEDIA_EDIT.toString());
        entry.getMediaEditLinks().add(link);

        if (tree.has(link.getTitle() + getJSONAnnotation(jsonMediaETag))) {
          link.setMediaETag(tree.get(link.getTitle() + getJSONAnnotation(jsonMediaETag)).asText());
          toRemove.add(link.getTitle() + getJSONAnnotation(jsonMediaETag));
        }

        toRemove.add(field.getKey());
        toRemove.add(setInline(field.getKey(), getJSONAnnotation(jsonMediaEditLink), tree, parser.getCodec(), link));
      } else if (field.getKey().endsWith(getJSONAnnotation(jsonMediaContentType))) {
        final String linkTitle = getTitle(field);
        for (Link link : entry.getMediaEditLinks()) {
          if (linkTitle.equals(link.getTitle())) {
            ((LinkImpl) link).setType(field.getValue().asText());
          }
        }
        toRemove.add(field.getKey());
      } else if (field.getKey().charAt(0) == '#') {
        final ODataOperation operation = new ODataOperation();
        operation.setMetadataAnchor(field.getKey());

        final ObjectNode opNode = (ObjectNode) tree.get(field.getKey());
        operation.setTitle(opNode.get(Constants.ATTR_TITLE).asText());
        operation.setTarget(URI.create(opNode.get(Constants.ATTR_TARGET).asText()));

        entry.getOperations().add(operation);

        toRemove.add(field.getKey());
      }
    }
    tree.remove(toRemove);

    String type = null;
    for (final Iterator<Map.Entry<String, JsonNode>> itor = tree.fields(); itor.hasNext();) {
      final Map.Entry<String, JsonNode> field = itor.next();

      if (type == null && field.getKey().endsWith(getJSONAnnotation(jsonType))) {
        type = field.getValue().asText();
      } else {
        final JSONPropertyImpl property = new JSONPropertyImpl();
        property.setName(field.getKey());
        property.setType(type == null
                ? null
                : new EdmTypeInfo.Builder().setTypeExpression(type).build().internal());
        type = null;

        value(property, field.getValue(), parser.getCodec());
        entry.getProperties().add(property);
      }
    }

    return new Container<JSONEntryImpl>(contextURL, metadataETag, entry);
  }
}